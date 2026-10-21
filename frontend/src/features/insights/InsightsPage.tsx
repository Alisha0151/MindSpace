import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useSession } from '@/app/session';
import { Alert, Card, EmptyState, ErrorNotice, Field, PageHeader, PageLoading, Select } from '@/components/ui';
import { addDays, daysBetween, formatDate, todayIn } from '@/lib/dates';
import { useAnalytics, useHabits } from '@/lib/queries';

const RANGES = [{ label: 'Last 7 days', days: 7 }, { label: 'Last 30 days', days: 30 }, { label: 'Last 90 days', days: 90 }, { label: 'Last 12 months', days: 365 }];
const AXIS = { fill: 'rgb(var(--c-muted))', fontSize: 12 };
const ACCENT = 'rgb(var(--c-accent))';
/** The API serialises the enum name; show a readable label. */
const SLEEP_LABEL: Record<string, string> = { UNDER_6H: 'Under 6 hours', SIX_TO_8H: '6 to 8 hours', EIGHT_PLUS: '8 hours or more' };

export function InsightsPage() {
  const { user } = useSession();
  const today = todayIn(user?.timezone ?? 'UTC');
  const [days, setDays] = useState(30);
  const [compare, setCompare] = useState(false);
  const [habitId, setHabitId] = useState('');
  const from = addDays(today, -(days - 1));
  const habits = useHabits();
  const { data, isLoading, error, refetch } = useAnalytics({
    from, to: today, habitId: habitId || undefined,
    compareFrom: compare ? addDays(from, -days) : undefined, compareTo: compare ? addDays(from, -1) : undefined,
  });

  return (
    <div className="space-y-5">
      <PageHeader title="Insights" subtitle="Summaries of what you recorded. They describe patterns in your entries; they don't explain causes or diagnose anything." />
      <div className="grid gap-3 sm:grid-cols-3">
        <Field label="Period">{(p) => <Select {...p} value={days} onChange={(e) => setDays(Number(e.target.value))}>{RANGES.map((r) => <option key={r.days} value={r.days}>{r.label}</option>)}</Select>}</Field>
        <Field label="Compare mood with a habit">{(p) => (
          <Select {...p} value={habitId} onChange={(e) => setHabitId(e.target.value)}>
            <option value="">None</option>{habits.data?.map((h) => <option key={h.id} value={h.id}>{h.name}</option>)}
          </Select>)}
        </Field>
        <Field label="Previous period">{(p) => (
          <label className="flex min-h-[44px] items-center gap-2 text-sm"><input {...p} type="checkbox" checked={compare} onChange={(e) => setCompare(e.target.checked)} /> Compare with the period before</label>)}
        </Field>
      </div>

      {isLoading ? <PageLoading /> : error || !data ? <ErrorNotice error={error} onRetry={() => void refetch()} /> : data.observations === 0 ? (
        <EmptyState title="No check-ins in this period" body="Insights appear once you have recorded some check-ins." />
      ) : (
        <>
          {!data.enoughData && <Alert tone="info">Only {data.observations} check-in{data.observations === 1 ? '' : 's'} so far. With fewer than 5, patterns would be misleading, so comparisons are hidden until there is more data.</Alert>}
          <div className="grid gap-3 sm:grid-cols-3">
            <Stat label="Average mood" value={data.averageMood === null ? '–' : `${data.averageMood}/5`} />
            <Stat label="Check-ins" value={String(data.observations)} />
            <Stat label="Days logged" value={`${data.coverage.daysLogged} of ${data.coverage.daysInRange} (${data.coverage.percent}%)`} />
          </div>

          <ChartCard title="Mood, stress and energy by day" table={
            <Table headers={['Date', 'Mood', 'Stress', 'Energy', 'Sleep (h)']} rows={data.daily.map((d) => [formatDate(d.date, { day: 'numeric', month: 'short' }), d.mood, d.stress, d.energy, (d.sleepMinutes / 60).toFixed(1)])} />}>
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={data.daily} margin={{ left: -20, right: 8, top: 8 }}>
                <CartesianGrid stroke="rgb(var(--c-line))" strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={AXIS} tickFormatter={(d: string) => formatDate(d, { day: 'numeric', month: 'short' })} minTickGap={24} />
                <YAxis domain={[1, 5]} ticks={[1, 2, 3, 4, 5]} tick={AXIS} />
                <Tooltip labelFormatter={(d: string) => formatDate(d)} />
                <Line type="monotone" dataKey="mood" name="Mood" stroke={ACCENT} strokeWidth={2} dot={data.daily.length < 40} />
                <Line type="monotone" dataKey="stress" name="Stress" stroke="rgb(var(--c-warn))" strokeWidth={2} strokeDasharray="5 3" dot={false} />
                <Line type="monotone" dataKey="energy" name="Energy" stroke="rgb(var(--c-ink))" strokeWidth={1.5} strokeDasharray="1 3" dot={false} />
              </LineChart>
            </ResponsiveContainer>
            <p className="mt-1 text-xs text-muted">Solid: mood · dashed: stress · dotted: energy (1–5).</p>
          </ChartCard>

          {data.weeklyTrend.length > 1 && (
            <ChartCard title="Weekly average mood" table={<Table headers={['Week starting', 'Check-ins', 'Average mood']} rows={data.weeklyTrend.map((w) => [formatDate(w.periodStart, { day: 'numeric', month: 'short' }), w.observations, w.avgMood ?? '–'])} />}>
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={data.weeklyTrend} margin={{ left: -20, right: 8 }}>
                  <CartesianGrid stroke="rgb(var(--c-line))" strokeDasharray="3 3" />
                  <XAxis dataKey="periodStart" tick={AXIS} tickFormatter={(d: string) => formatDate(d, { day: 'numeric', month: 'short' })} />
                  <YAxis domain={[0, 5]} tick={AXIS} />
                  <Tooltip labelFormatter={(d: string) => `Week of ${formatDate(d)}`} />
                  <Bar dataKey="avgMood" name="Average mood" fill={ACCENT} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </ChartCard>
          )}

          <ChartCard title="How often each mood score occurred" table={<Table headers={['Mood', 'Days']} rows={data.moodDistribution.map((n, i) => [i + 1, n])} />}>
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={data.moodDistribution.map((count, i) => ({ mood: String(i + 1), count }))} margin={{ left: -20, right: 8 }}>
                <XAxis dataKey="mood" tick={AXIS} /><YAxis allowDecimals={false} tick={AXIS} /><Tooltip />
                <Bar dataKey="count" name="Days" fill={ACCENT} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>

          {data.topEmotions.length > 0 && (
            <Card><h2 className="font-semibold">Emotions you recorded most</h2>
              <ol className="mt-2 space-y-1 text-sm">{data.topEmotions.map((e) => <li key={e.label} className="flex justify-between"><span>{e.label}</span><span className="text-muted">{e.count}×</span></li>)}</ol>
            </Card>
          )}

          {data.enoughData && data.sleepVersusMood.length > 0 && (
            <Card><h2 className="font-semibold">Sleep and mood</h2>
              <Table headers={['Sleep', 'Check-ins', 'Average mood']} rows={data.sleepVersusMood.map((s) => [SLEEP_LABEL[s.bucket] ?? s.bucket, s.observations, s.avgMood ?? '–'])} />
              <p className="mt-2 text-xs text-muted">An observed association in your own entries, not proof that sleep changes your mood.</p>
            </Card>
          )}

          {data.habitVersusMood && (
            <Card><h2 className="font-semibold">Mood on days you did this habit</h2>
              {data.enoughData ? <Table headers={['', 'Days', 'Average mood']} rows={[['Habit done', data.habitVersusMood.doneDays, data.habitVersusMood.avgMoodDone ?? '–'], ['Not done', data.habitVersusMood.notDoneDays, data.habitVersusMood.avgMoodNotDone ?? '–']]} /> : <p className="mt-1 text-sm text-muted">Not enough check-ins yet.</p>}
              <p className="mt-2 text-xs text-muted">Only days with a check-in are counted. This shows co-occurrence, not cause.</p>
            </Card>
          )}

          {data.comparison && (
            <Card><h2 className="font-semibold">This period vs the one before</h2>
              <Table headers={['Period', 'Check-ins', 'Average mood']} rows={[
                [`${formatDate(data.from, { day: 'numeric', month: 'short' })} – ${formatDate(data.to, { day: 'numeric', month: 'short' })}`, data.comparison.observations1, data.comparison.avgMood1 ?? '–'],
                [`${formatDate(data.compareFrom!, { day: 'numeric', month: 'short' })} – ${formatDate(data.compareTo!, { day: 'numeric', month: 'short' })}`, data.comparison.observations2, data.comparison.avgMood2 ?? '–'],
              ]} />
              {(data.comparison.observations1 < 5 || data.comparison.observations2 < 5) && <p className="mt-2 text-xs text-muted">One of the periods has fewer than 5 check-ins, so treat the difference with caution.</p>}
            </Card>
          )}
          <p className="text-xs text-muted">{daysBetween(data.from, data.to) + 1} days shown. Averages are rounded to two decimals.</p>
        </>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return <Card><p className="text-xs text-muted">{label}</p><p className="text-xl font-semibold">{value}</p></Card>;
}

function ChartCard({ title, children, table }: { title: string; children: React.ReactNode; table: React.ReactNode }) {
  return (
    <Card>
      <h2 className="font-semibold">{title}</h2>
      <div className="mt-2" role="img" aria-label={`${title} chart. The same data is available as a table below.`}>{children}</div>
      <details className="mt-2"><summary className="cursor-pointer text-sm underline">View as a table</summary><div className="mt-2 overflow-x-auto">{table}</div></details>
    </Card>
  );
}

function Table({ headers, rows }: { headers: string[]; rows: (string | number)[][] }) {
  return (
    <table className="mt-2 w-full text-left text-sm">
      <thead className="text-xs text-muted"><tr>{headers.map((h, i) => <th key={i} scope="col" className="py-1 pr-3">{h}</th>)}</tr></thead>
      <tbody>{rows.map((r, i) => <tr key={i} className="border-t border-line">{r.map((c, j) => j === 0 ? <th key={j} scope="row" className="py-1 pr-3 font-normal">{c}</th> : <td key={j} className="pr-3">{c}</td>)}</tr>)}</tbody>
    </table>
  );
}
