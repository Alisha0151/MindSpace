import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useSession } from '@/app/session';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Alert, Button, Card, ErrorNotice, Field, Input, PageHeader, PageLoading, Scale, Textarea } from '@/components/ui';
import { useToast } from '@/components/toast';
import { ApiError, api } from '@/lib/api';
import { addDays, formatDate, formatDuration, todayIn } from '@/lib/dates';
import { ACTIVITY_SUGGESTIONS, EMOTION_SUGGESTIONS, ENERGY_LABELS, MOOD_LABELS, STRESS_LABELS, parseTags } from '@/lib/mood';
import { keys, useCheckIns } from '@/lib/queries';
import type { CheckIn, CheckInInput } from '@/types';

const fieldError = (e: unknown, field: string) => (e instanceof ApiError ? e.fieldErrors.find((f) => f.field === field)?.message : undefined);

export function CheckInPage() {
  const { user } = useSession();
  const today = todayIn(user?.timezone ?? 'UTC');
  const qc = useQueryClient();
  const toast = useToast();
  const todayQuery = useQuery({
    queryKey: keys.checkinToday,
    // 204 No Content (no check-in yet) resolves to undefined; React Query forbids undefined data, so map to null.
    queryFn: async () => (await api.get<CheckIn | undefined>('/checkins/today')) ?? null,
  });
  const existing = todayQuery.data ?? null;

  const [mood, setMood] = useState(3);
  const [stress, setStress] = useState(3);
  const [energy, setEnergy] = useState(3);
  const [sleepHours, setSleepHours] = useState('7');
  const [emotions, setEmotions] = useState('');
  const [activities, setActivities] = useState('');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (!existing) return;
    setMood(existing.moodScore); setStress(existing.stressScore); setEnergy(existing.energyScore);
    setSleepHours(String(existing.sleepMinutes / 60));
    setEmotions(existing.emotions.join(', ')); setActivities(existing.activities.join(', '));
    setNotes(existing.notes ?? '');
  }, [existing]);

  const save = useMutation({
    mutationFn: (input: CheckInInput) => (existing ? api.put<CheckIn>(`/checkins/${existing.id}`, input) : api.post<CheckIn>('/checkins', input)),
    onSuccess: () => {
      toast(existing ? 'Check-in updated' : 'Check-in saved');
      for (const k of [keys.checkinToday, keys.dashboard, ['checkin'], ['analytics'], ['calendar']]) void qc.invalidateQueries({ queryKey: k });
    },
  });
  const remove = useMutation({
    mutationFn: () => api.del(`/checkins/${existing!.id}`),
    onSuccess: () => {
      toast('Check-in deleted');
      setNotes('');
      for (const k of [keys.checkinToday, keys.dashboard, ['checkin'], ['analytics'], ['calendar']]) void qc.invalidateQueries({ queryKey: k });
    },
    onError: () => toast('Could not delete the check-in', 'error'),
  });

  const hours = Number(sleepHours);
  const sleepInvalid = sleepHours.trim() === '' || Number.isNaN(hours) || hours < 0 || hours > 24;

  function submit(e: FormEvent) {
    e.preventDefault();
    if (sleepInvalid) return;
    save.mutate({
      date: existing ? undefined : today,
      moodScore: mood, stressScore: stress, energyScore: energy,
      sleepMinutes: Math.round(hours * 60),
      emotions: parseTags(emotions), activities: parseTags(activities), contexts: existing?.contexts ?? [],
      notes: notes.trim() || null,
    });
  }

  if (todayQuery.isLoading) return <PageLoading />;
  if (todayQuery.error) return <ErrorNotice error={todayQuery.error} onRetry={() => void todayQuery.refetch()} />;

  return (
    <div className="space-y-6">
      <PageHeader title={existing ? "Today's check-in" : 'Daily check-in'} subtitle={`${formatDate(today, { weekday: 'long', day: 'numeric', month: 'long' })} · these are your own observations, not a diagnosis`} />
      <form onSubmit={submit} className="space-y-5" noValidate>
        <Card className="space-y-5">
          <Scale label="Mood" value={mood} onChange={setMood} lowLabel={MOOD_LABELS[1]} highLabel={MOOD_LABELS[5]} />
          <Scale label="Stress" value={stress} onChange={setStress} lowLabel={STRESS_LABELS[1]} highLabel={STRESS_LABELS[5]} />
          <Scale label="Energy" value={energy} onChange={setEnergy} lowLabel={ENERGY_LABELS[1]} highLabel={ENERGY_LABELS[5]} />
          <Field label="Hours slept" hint={sleepInvalid ? undefined : `About ${formatDuration(Math.round(hours * 60))}`} error={sleepInvalid ? 'Enter a number between 0 and 24' : fieldError(save.error, 'sleepMinutes')}>
            {(p) => <Input {...p} inputMode="decimal" value={sleepHours} onChange={(e) => setSleepHours(e.target.value)} />}
          </Field>
        </Card>
        <Card className="space-y-5">
          <Field label="Emotions" hint={`Comma separated, up to 10. Ideas: ${EMOTION_SUGGESTIONS.slice(0, 6).join(', ')}`} error={fieldError(save.error, 'emotions')}>
            {(p) => <Input {...p} value={emotions} onChange={(e) => setEmotions(e.target.value)} />}
          </Field>
          <Field label="Activities" hint={`Ideas: ${ACTIVITY_SUGGESTIONS.slice(0, 5).join(', ')}`} error={fieldError(save.error, 'activities')}>
            {(p) => <Input {...p} value={activities} onChange={(e) => setActivities(e.target.value)} />}
          </Field>
          <Field label="Notes (private, optional)" hint={`${notes.length}/4000`} error={fieldError(save.error, 'notes')}>
            {(p) => <Textarea {...p} maxLength={4000} value={notes} onChange={(e) => setNotes(e.target.value)} />}
          </Field>
        </Card>
        {save.error && !(save.error instanceof ApiError && save.error.fieldErrors.length > 0) && <ErrorNotice error={save.error} />}
        <div className="flex flex-wrap gap-3">
          <Button type="submit" loading={save.isPending} disabled={sleepInvalid}>{existing ? 'Update check-in' : 'Save check-in'}</Button>
          {existing && (
            <ConfirmDialog
              trigger={<Button type="button" variant="secondary">Delete</Button>}
              title="Delete today's check-in?" description="This permanently removes it. You can check in again afterwards."
              confirmLabel="Delete" busy={remove.isPending} onConfirm={() => remove.mutate()} />
          )}
        </div>
      </form>
      <History today={today} />
    </div>
  );
}

function History({ today }: { today: string }) {
  const { data, isLoading, error, refetch } = useCheckIns(addDays(today, -29), today);
  return (
    <Card>
      <h2 className="text-lg font-semibold">Last 30 days</h2>
      {isLoading ? <PageLoading /> : error ? <ErrorNotice error={error} onRetry={() => void refetch()} /> :
        !data || data.length === 0 ? <p className="mt-2 text-sm text-muted">No check-ins in this period yet.</p> : (
          <div className="mt-3 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <caption className="sr-only">Check-in history, newest first</caption>
              <thead className="text-xs text-muted"><tr><th scope="col" className="py-1 pr-3">Date</th><th scope="col" className="pr-3">Mood</th><th scope="col" className="pr-3">Stress</th><th scope="col" className="pr-3">Energy</th><th scope="col">Sleep</th></tr></thead>
              <tbody>
                {[...data].reverse().map((c) => (
                  <tr key={c.id} className="border-t border-line">
                    <th scope="row" className="py-1.5 pr-3 font-normal">{formatDate(c.date, { day: 'numeric', month: 'short' })}</th>
                    <td className="pr-3">{c.moodScore}</td><td className="pr-3">{c.stressScore}</td><td className="pr-3">{c.energyScore}</td><td>{formatDuration(c.sleepMinutes)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      <Alert tone="info">Scores are 1–5 self-ratings and only show what you recorded.</Alert>
    </Card>
  );
}
