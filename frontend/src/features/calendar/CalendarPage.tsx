import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSession } from '@/app/session';
import { Button, Card, ErrorNotice, Field, Input, PageHeader, PageLoading, Select } from '@/components/ui';
import { formatDate, formatMonth, monthGrid, monthOf, shiftMonth, todayIn } from '@/lib/dates';
import { moodLabel, moodShade } from '@/lib/mood';
import { useCalendar } from '@/lib/queries';
import type { CalendarDay } from '@/types';

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function CalendarPage() {
  const { user } = useSession();
  const today = todayIn(user?.timezone ?? 'UTC');
  const [month, setMonth] = useState(monthOf(today));
  const [minMood, setMinMood] = useState('');
  const [emotion, setEmotion] = useState('');
  const [activity, setActivity] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const { data, isLoading, error, refetch } = useCalendar(month, {
    minMood: minMood ? Number(minMood) : undefined,
    emotion: emotion.trim() || undefined,
    activity: activity.trim() || undefined,
  });
  const byDate = new Map<string, CalendarDay>((data?.days ?? []).map((d) => [d.date, d]));
  const chosen = selected ? byDate.get(selected) : undefined;
  const filtered = !!(minMood || emotion.trim() || activity.trim());

  const go = (delta: number) => { setMonth(shiftMonth(month, delta)); setSelected(null); };

  return (
    <div className="space-y-5">
      <PageHeader title="Calendar" subtitle="Shade shows mood; the number is always printed too." />
      <div className="flex items-center justify-between">
        <Button variant="secondary" onClick={() => go(-1)} aria-label="Previous month">←</Button>
        <h2 className="text-lg font-semibold" aria-live="polite">{formatMonth(month)}</h2>
        <Button variant="secondary" onClick={() => go(1)} aria-label="Next month">→</Button>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <Field label="Mood at least">{(p) => (
          <Select {...p} value={minMood} onChange={(e) => setMinMood(e.target.value)}>
            <option value="">Any</option>{[2, 3, 4, 5].map((n) => <option key={n} value={n}>{n}</option>)}
          </Select>)}
        </Field>
        <Field label="Emotion">{(p) => <Input {...p} value={emotion} onChange={(e) => setEmotion(e.target.value)} placeholder="e.g. calm" />}</Field>
        <Field label="Activity">{(p) => <Input {...p} value={activity} onChange={(e) => setActivity(e.target.value)} placeholder="e.g. walk" />}</Field>
      </div>
      {filtered && <Button variant="ghost" onClick={() => { setMinMood(''); setEmotion(''); setActivity(''); }}>Clear filters</Button>}

      {isLoading ? <PageLoading /> : error ? <ErrorNotice error={error} onRetry={() => void refetch()} /> : (
        <Card>
          <div role="grid" aria-label={`Check-ins for ${formatMonth(month)}`} className="grid grid-cols-7 gap-1.5">
            {WEEKDAYS.map((w) => <div key={w} role="columnheader" className="pb-1 text-center text-xs text-muted">{w}</div>)}
            {monthGrid(month).map((date, i) => {
              if (!date) return <div key={`pad-${i}`} role="gridcell" aria-hidden="true" />;
              const day = byDate.get(date);
              const label = `${formatDate(date, { weekday: 'long', day: 'numeric', month: 'long' })}: ${day?.moodScore ? `mood ${day.moodScore} of 5` : 'no check-in'}${day?.journalEntries ? `, ${day.journalEntries} journal entr${day.journalEntries === 1 ? 'y' : 'ies'}` : ''}`;
              return (
                <button key={date} role="gridcell" aria-label={label} aria-pressed={selected === date}
                  onClick={() => setSelected(selected === date ? null : date)}
                  style={{ background: moodShade(day?.moodScore ?? null) }}
                  className={`relative flex min-h-[48px] flex-col items-center justify-center rounded-xl border text-sm ${date === today ? 'border-accent border-2' : 'border-line'} ${selected === date ? 'ring-2 ring-accent' : ''}`}>
                  <span>{Number(date.slice(8))}</span>
                  {day?.moodScore ? <span className="text-xs font-semibold">{day.moodScore}</span> : null}
                  {day && day.journalEntries > 0 && <span aria-hidden="true" className="absolute right-1 top-1 h-1.5 w-1.5 rounded-full bg-ink" />}
                </button>
              );
            })}
          </div>
          <p className="mt-3 text-xs text-muted">A dot marks a day with journal entries. Today has a thick outline.</p>
        </Card>
      )}

      {selected && (
        <Card aria-live="polite">
          <h2 className="font-semibold">{formatDate(selected, { weekday: 'long', day: 'numeric', month: 'long' })}</h2>
          {chosen ? (
            <div className="mt-2 space-y-1 text-sm">
              {chosen.moodScore && <p>Mood {chosen.moodScore}/5 · {moodLabel(chosen.moodScore)}</p>}
              {chosen.emotions.length > 0 && <p>Emotions: {chosen.emotions.join(', ')}</p>}
              {chosen.activities.length > 0 && <p>Activities: {chosen.activities.join(', ')}</p>}
              {chosen.journalEntries > 0 && <p><Link className="underline" to={`/app/journal?from=${selected}&to=${selected}`}>{chosen.journalEntries} journal entr{chosen.journalEntries === 1 ? 'y' : 'ies'}</Link></p>}
            </div>
          ) : <p className="mt-2 text-sm text-muted">{filtered ? 'Nothing matches your filters on this day.' : 'Nothing recorded on this day.'}</p>}
        </Card>
      )}
    </div>
  );
}
