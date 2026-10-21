import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { useSession } from '@/app/session';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Badge, Button, Card, EmptyState, ErrorNotice, Field, Input, PageHeader, PageLoading, Select, Textarea } from '@/components/ui';
import { useToast } from '@/components/toast';
import { ApiError, api } from '@/lib/api';
import { addDays, formatDate, todayIn } from '@/lib/dates';
import { keys, useHabits } from '@/lib/queries';
import type { Cadence, Habit, HabitInput, HabitProgress } from '@/types';

const fieldError = (e: unknown, field: string) => (e instanceof ApiError ? e.fieldErrors.find((f) => f.field === field)?.message : undefined);

export function HabitsPage() {
  const { user } = useSession();
  const today = todayIn(user?.timezone ?? 'UTC');
  const { data, isLoading, error, refetch } = useHabits();
  const qc = useQueryClient();
  const toast = useToast();
  const [editing, setEditing] = useState<Habit | 'new' | null>(null);
  const [open, setOpen] = useState<string | null>(null);

  const refresh = () => { void qc.invalidateQueries({ queryKey: keys.habits }); void qc.invalidateQueries({ queryKey: keys.dashboard }); void qc.invalidateQueries({ queryKey: ['habit-progress'] }); void qc.invalidateQueries({ queryKey: ['analytics'] }); };
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: refresh,
    onError: (e) => toast(e instanceof ApiError ? e.message : 'That did not work. Please try again.', 'error'),
  });

  if (isLoading) return <PageLoading />;
  if (error || !data) return <ErrorNotice error={error} onRetry={() => void refetch()} />;

  return (
    <div className="space-y-5">
      <PageHeader title="Habits" subtitle="Small, repeatable things. Weeks run Monday to Sunday." actions={<Button onClick={() => setEditing('new')}>Add habit</Button>} />
      {editing && <HabitForm habit={editing === 'new' ? null : editing} onDone={() => { setEditing(null); refresh(); }} onCancel={() => setEditing(null)} />}
      {data.length === 0 && !editing ? (
        <EmptyState title="No habits yet" body="Pick one thing you'd like to do regularly. Missing a day doesn't erase your history." action={<Button onClick={() => setEditing('new')}>Add your first habit</Button>} />
      ) : (
        <ul className="space-y-3">
          {data.map((h) => (
            <li key={h.id}>
              <Card as="article">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 className="font-semibold">{h.name} {h.paused && <Badge>Paused</Badge>}</h2>
                    {h.description && <p className="text-sm text-muted">{h.description}</p>}
                    <p className="mt-1 text-sm text-muted">
                      {h.cadence === 'DAILY' ? `Daily · streak ${h.currentStreak} (best ${h.longestStreak})` : `${h.targetCount}× per week · ${h.thisWeekDone}/${h.targetCount} this week · ${h.weeklyStreak}-week streak`}
                    </p>
                    <p className="text-xs text-muted">Last 30 days: {h.consistency30.completedDays} of {h.consistency30.activeDays} days ({h.consistency30.percent}%)</p>
                  </div>
                  {!h.paused && (
                    <Button variant={h.doneToday ? 'secondary' : 'primary'} aria-pressed={h.doneToday}
                      onClick={() => act.mutate(() => (h.doneToday ? api.del(`/habits/${h.id}/completions/${today}`) : api.put(`/habits/${h.id}/completions/${today}`)))}>
                      {h.doneToday ? 'Done today ✓ (undo)' : 'Mark done today'}
                    </Button>
                  )}
                </div>
                <ol className="mt-3 flex gap-1.5" aria-label="Last seven days, oldest first">
                  {h.last7Days.map((done, i) => (
                    <li key={i} className={`flex h-8 w-8 items-center justify-center rounded-lg border border-line text-xs ${done ? 'bg-accent text-accent-ink' : ''}`}
                      aria-label={`${formatDate(addDays(today, i - 6), { weekday: 'long' })}: ${done ? 'done' : 'not done'}`}>{done ? '✓' : ''}</li>
                  ))}
                </ol>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button variant="ghost" onClick={() => setOpen(open === h.id ? null : h.id)} aria-expanded={open === h.id}>{open === h.id ? 'Hide progress' : 'Progress'}</Button>
                  <Button variant="ghost" onClick={() => setEditing(h)}>Edit</Button>
                  <Button variant="ghost" onClick={() => act.mutate(() => api.post(`/habits/${h.id}/${h.paused ? 'resume' : 'pause'}`))}>{h.paused ? 'Resume' : 'Pause'}</Button>
                  <ConfirmDialog trigger={<Button variant="ghost">Delete</Button>} title={`Delete "${h.name}"?`} description="The habit and its whole history will be removed permanently. Pause it instead if you want to keep the history."
                    confirmLabel="Delete habit" onConfirm={() => act.mutate(() => api.del(`/habits/${h.id}`))} />
                </div>
                {open === h.id && <Progress habitId={h.id} today={today} />}
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Progress({ habitId, today }: { habitId: string; today: string }) {
  const from = addDays(today, -55);
  const { data, isLoading, error } = useQuery({ queryKey: ['habit-progress', habitId, from, today], queryFn: () => api.get<HabitProgress>(`/habits/${habitId}/progress?from=${from}&to=${today}`) });
  if (isLoading) return <PageLoading />;
  if (error || !data) return <ErrorNotice error={error} />;
  return (
    <div className="mt-3 border-t border-line pt-3">
      <p className="text-sm">Consistency over this range: <strong>{data.consistency.percent}%</strong> ({data.consistency.completedDays}/{data.consistency.activeDays} active days)</p>
      <table className="mt-2 w-full text-left text-sm">
        <caption className="sr-only">Weekly progress</caption>
        <thead className="text-xs text-muted"><tr><th scope="col">Week starting</th><th scope="col">Done</th><th scope="col">Target</th><th scope="col">Met</th></tr></thead>
        <tbody>{data.weeks.map((w) => <tr key={w.weekStart} className="border-t border-line"><th scope="row" className="py-1 font-normal">{formatDate(w.weekStart, { day: 'numeric', month: 'short' })}</th><td>{w.done}</td><td>{w.target}</td><td>{w.met ? 'Yes' : 'No'}</td></tr>)}</tbody>
      </table>
    </div>
  );
}

function HabitForm({ habit, onDone, onCancel }: { habit: Habit | null; onDone: () => void; onCancel: () => void }) {
  const toast = useToast();
  const [name, setName] = useState(habit?.name ?? '');
  const [description, setDescription] = useState(habit?.description ?? '');
  const [cadence, setCadence] = useState<Cadence>(habit?.cadence ?? 'DAILY');
  const [target, setTarget] = useState(String(habit?.targetCount ?? 3));
  const save = useMutation({
    mutationFn: (input: HabitInput) => (habit ? api.put(`/habits/${habit.id}`, input) : api.post('/habits', input)),
    onSuccess: () => { toast(habit ? 'Habit updated' : 'Habit added'); onDone(); },
  });
  function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    save.mutate({ name: name.trim(), description: description.trim() || null, cadence, targetCount: cadence === 'WEEKLY' ? Number(target) : 1 });
  }
  return (
    <Card>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <h2 className="text-lg font-semibold">{habit ? 'Edit habit' : 'New habit'}</h2>
        <Field label="Name" error={fieldError(save.error, 'name') ?? (save.isError && !name.trim() ? 'Required' : undefined)}>{(p) => <Input {...p} required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />}</Field>
        <Field label="Description (optional)" error={fieldError(save.error, 'description')}>{(p) => <Textarea {...p} className="min-h-[70px]" maxLength={500} value={description} onChange={(e) => setDescription(e.target.value)} />}</Field>
        <Field label="How often">{(p) => <Select {...p} value={cadence} onChange={(e) => setCadence(e.target.value as Cadence)}><option value="DAILY">Every day</option><option value="WEEKLY">Some days each week</option></Select>}</Field>
        {cadence === 'WEEKLY' && <Field label="Days per week" error={fieldError(save.error, 'targetCount')}>{(p) => <Select {...p} value={target} onChange={(e) => setTarget(e.target.value)}>{[1, 2, 3, 4, 5, 6, 7].map((n) => <option key={n} value={n}>{n}</option>)}</Select>}</Field>}
        {save.error && !(save.error instanceof ApiError && save.error.fieldErrors.length > 0) && <ErrorNotice error={save.error} />}
        <div className="flex gap-3"><Button type="submit" loading={save.isPending}>Save</Button><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button></div>
      </form>
    </Card>
  );
}
