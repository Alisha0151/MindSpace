import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Badge, Button, Card, EmptyState, ErrorNotice, PageHeader, PageLoading } from '@/components/ui';
import { useToast } from '@/components/toast';
import { api } from '@/lib/api';
import { formatDate } from '@/lib/dates';
import { moodLabel, moodShade } from '@/lib/mood';
import { keys, useDashboard } from '@/lib/queries';

export function DashboardPage() {
  const { data, isLoading, error, refetch } = useDashboard();
  const qc = useQueryClient();
  const toast = useToast();
  const toggle = useMutation({
    mutationFn: ({ id, done, date }: { id: string; done: boolean; date: string }) =>
      done ? api.del(`/habits/${id}/completions/${date}`) : api.put(`/habits/${id}/completions/${date}`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: keys.dashboard });
      void qc.invalidateQueries({ queryKey: keys.habits });
    },
    onError: () => toast('Could not update the habit. Please try again.', 'error'),
  });

  if (isLoading) return <PageLoading label="Loading your dashboard" />;
  if (error || !data) return <ErrorNotice error={error} onRetry={() => void refetch()} />;

  const activeHabits = data.habits.filter((h) => !h.paused);
  return (
    <div className="space-y-5">
      <PageHeader title={`Hello, ${data.displayName}`} subtitle={formatDate(data.today, { weekday: 'long', day: 'numeric', month: 'long' })} />

      <Card>
        <h2 className="text-lg font-semibold">Today's check-in</h2>
        {data.todayCheckIn ? (
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <Badge>Mood {data.todayCheckIn.moodScore}/5 · {moodLabel(data.todayCheckIn.moodScore)}</Badge>
            <Badge>Stress {data.todayCheckIn.stressScore}/5</Badge>
            <Badge>Energy {data.todayCheckIn.energyScore}/5</Badge>
            <Link className="text-sm underline" to="/app/check-in">Edit</Link>
          </div>
        ) : (
          <div className="mt-2">
            <p className="text-sm text-muted">You haven't checked in today. It takes about a minute.</p>
            <Link to="/app/check-in"><Button className="mt-3">Check in now</Button></Link>
          </div>
        )}
        {data.checkInStreak > 0 && <p className="mt-3 text-sm text-muted">{data.checkInStreak} day{data.checkInStreak === 1 ? '' : 's'} in a row of checking in.</p>}
      </Card>

      <Card>
        <h2 className="text-lg font-semibold">Last 7 days</h2>
        <ol className="mt-3 grid grid-cols-7 gap-2" aria-label="Mood over the last seven days">
          {data.last7Days.map((d) => (
            <li key={d.date} className="text-center">
              <div
                className="flex h-12 items-center justify-center rounded-xl border border-line text-sm font-medium"
                style={{ background: moodShade(d.mood) }}
                aria-label={`${formatDate(d.date, { weekday: 'long' })}: ${d.mood === null ? 'no check-in' : `mood ${d.mood} of 5`}`}
              >
                {d.mood ?? '–'}
              </div>
              <span className="text-xs text-muted">{formatDate(d.date, { weekday: 'short' })}</span>
            </li>
          ))}
        </ol>
      </Card>

      <Card>
        <div className="flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">Habits today</h2>
          {activeHabits.length > 0 && <span className="text-sm text-muted">{data.habitsDoneToday} of {data.habitsDueToday} done</span>}
        </div>
        {activeHabits.length === 0 ? (
          <div className="mt-3"><EmptyState title="No habits yet" body="Start with one small thing you'd like to do regularly." action={<Link to="/app/habits"><Button>Add a habit</Button></Link>} /></div>
        ) : (
          <ul className="mt-3 divide-y divide-line">
            {activeHabits.map((h) => (
              <li key={h.id} className="flex items-center justify-between gap-3 py-2">
                <div>
                  <p className="font-medium">{h.name}</p>
                  <p className="text-xs text-muted">{h.cadence === 'DAILY' ? `${h.currentStreak}-day streak` : `${h.thisWeekDone}/${h.targetCount} this week`}</p>
                </div>
                <Button
                  variant={h.doneToday ? 'secondary' : 'primary'}
                  aria-pressed={h.doneToday}
                  onClick={() => toggle.mutate({ id: h.id, done: h.doneToday, date: data.today })}
                  loading={toggle.isPending && toggle.variables?.id === h.id}
                >
                  {h.doneToday ? 'Done ✓ (undo)' : 'Mark done'}
                </Button>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card>
        <div className="flex items-baseline justify-between">
          <h2 className="text-lg font-semibold">Recent journal entries</h2>
          <Link className="text-sm underline" to="/app/journal">All entries</Link>
        </div>
        {data.recentJournal.length === 0 ? (
          <div className="mt-3"><EmptyState title="Nothing written yet" body="Your journal is private to you." action={<Link to="/app/journal/new"><Button>Write an entry</Button></Link>} /></div>
        ) : (
          <ul className="mt-3 divide-y divide-line">
            {data.recentJournal.map((e) => (
              <li key={e.id} className="py-2">
                <Link to={`/app/journal/${e.id}`} className="font-medium underline-offset-2 hover:underline">{e.title || 'Untitled'}</Link>
                <p className="text-xs text-muted">{formatDate(e.entryDate)}</p>
                <p className="mt-1 line-clamp-2 text-sm text-muted">{e.excerpt}</p>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
