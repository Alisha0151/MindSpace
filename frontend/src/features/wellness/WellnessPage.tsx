import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Alert, Button, Card, EmptyState, ErrorNotice, Field, Input, PageHeader, PageLoading, Textarea } from '@/components/ui';
import { useToast } from '@/components/toast';
import { ApiError, api } from '@/lib/api';
import { PHASE_LABEL, positionAt, totalSeconds } from '@/lib/breathing';
import { keys, useCoping, useToolkit } from '@/lib/queries';
import type { Coping, Toolkit } from '@/types';

type Tab = 'breathing' | 'grounding' | 'prompts' | 'coping';
const TABS: { id: Tab; label: string }[] = [
  { id: 'breathing', label: 'Breathing' }, { id: 'grounding', label: 'Grounding' }, { id: 'prompts', label: 'Prompts' }, { id: 'coping', label: 'My strategies' },
];

export function WellnessPage() {
  const [tab, setTab] = useState<Tab>('breathing');
  const toolkit = useToolkit();
  return (
    <div className="space-y-5">
      <PageHeader title="Wellness toolkit" subtitle="Short exercises you can do any time." />
      <div role="tablist" aria-label="Toolkit sections" className="flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button key={t.id} role="tab" id={`tab-${t.id}`} aria-selected={tab === t.id} aria-controls={`panel-${t.id}`} tabIndex={tab === t.id ? 0 : -1}
            onClick={() => setTab(t.id)}
            onKeyDown={(e) => {
              const i = TABS.findIndex((x) => x.id === tab);
              if (e.key === 'ArrowRight') setTab(TABS[(i + 1) % TABS.length].id);
              if (e.key === 'ArrowLeft') setTab(TABS[(i + TABS.length - 1) % TABS.length].id);
            }}
            className={`min-h-[44px] rounded-xl border border-line px-4 text-sm ${tab === t.id ? 'bg-sunken font-semibold' : ''}`}>{t.label}</button>
        ))}
      </div>
      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`}>
        {toolkit.isLoading ? <PageLoading /> : toolkit.error || !toolkit.data ? <ErrorNotice error={toolkit.error} onRetry={() => void toolkit.refetch()} /> : (
          <>
            {tab === 'breathing' && <Breathing toolkit={toolkit.data} />}
            {tab === 'grounding' && <Grounding toolkit={toolkit.data} />}
            {tab === 'prompts' && <Prompts toolkit={toolkit.data} />}
            {tab === 'coping' && <CopingList />}
            <p className="mt-6 text-xs text-muted">{toolkit.data.disclaimer} <Link className="underline" to="/app/help">Get help now</Link></p>
          </>
        )}
      </div>
    </div>
  );
}

function Breathing({ toolkit }: { toolkit: Toolkit }) {
  const [patternId, setPatternId] = useState(toolkit.breathing[0].id);
  const pattern = toolkit.breathing.find((p) => p.id === patternId) ?? toolkit.breathing[0];
  const [cycles, setCycles] = useState(pattern.defaultCycles);
  const [elapsed, setElapsed] = useState<number | null>(null); // null = not started
  const [paused, setPaused] = useState(false);
  const timer = useRef<number | undefined>(undefined);

  useEffect(() => { setCycles(pattern.defaultCycles); setElapsed(null); }, [pattern.id, pattern.defaultCycles]);
  useEffect(() => {
    window.clearInterval(timer.current);
    if (elapsed === null || paused) return;
    timer.current = window.setInterval(() => setElapsed((e) => (e === null ? e : e + 1)), 1000);
    return () => window.clearInterval(timer.current);
  }, [elapsed === null, paused]); // eslint-disable-line react-hooks/exhaustive-deps

  const pos = useMemo(() => (elapsed === null ? null : positionAt(pattern, cycles, elapsed)), [elapsed, pattern, cycles]);
  useEffect(() => { if (pos?.done) window.clearInterval(timer.current); }, [pos?.done]);
  const total = totalSeconds(pattern, cycles);
  const active = pos !== null && !pos.done;
  const reduceMotion = typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  return (
    <Card className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Pattern" hint={pattern.description}>
          {(p) => <select {...p} disabled={active} value={patternId} onChange={(e) => setPatternId(e.target.value)} className="min-h-[44px] w-full rounded-xl border border-line bg-surface px-3 text-sm">{toolkit.breathing.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}</select>}
        </Field>
        <Field label={`Rounds: ${cycles} (about ${Math.ceil(total / 60)} min)`}>
          {(p) => <input {...p} type="range" min={1} max={12} disabled={active} value={cycles} onChange={(e) => setCycles(Number(e.target.value))} className="w-full" />}
        </Field>
      </div>
      <div className="flex flex-col items-center py-6" aria-live="polite">
        <div aria-hidden="true" className="flex h-40 w-40 items-center justify-center rounded-full border-4 border-accent text-center transition-transform duration-1000"
          style={{ transform: !reduceMotion && pos && (pos.phase === 'inhale' ? 'scale(1.15)' : pos.phase === 'exhale' ? 'scale(0.9)' : 'scale(1.05)') || undefined }}>
          <span className="text-lg font-semibold">{pos ? (pos.done ? 'Done' : PHASE_LABEL[pos.phase]) : 'Ready'}</span>
        </div>
        <p className="mt-3 text-sm">{pos ? (pos.done ? 'Well done. Notice how you feel.' : `${PHASE_LABEL[pos.phase]} · ${pos.secondsLeftInPhase}s · round ${pos.cycle} of ${cycles}`) : 'Sit comfortably and press start.'}</p>
      </div>
      <div className="flex flex-wrap justify-center gap-3">
        {!active ? <Button onClick={() => { setPaused(false); setElapsed(0); }}>{pos?.done ? 'Go again' : 'Start'}</Button> : (
          <>
            <Button variant="secondary" onClick={() => setPaused((p) => !p)}>{paused ? 'Resume' : 'Pause'}</Button>
            <Button variant="secondary" onClick={() => { setElapsed(null); setPaused(false); }}>Stop</Button>
          </>
        )}
      </div>
      {pattern.holdSeconds > 0 && <Alert tone="info">You can skip the hold and just breathe slowly. Stop any time if you feel dizzy or uncomfortable.</Alert>}
    </Card>
  );
}

function Grounding({ toolkit }: { toolkit: Toolkit }) {
  const [i, setI] = useState(0);
  const step = toolkit.grounding[i];
  return (
    <Card className="space-y-4">
      <h2 className="text-lg font-semibold">5-4-3-2-1 grounding</h2>
      <p className="text-sm text-muted">Use your senses to come back to the present.</p>
      <div className="rounded-xl bg-sunken p-6 text-center" aria-live="polite">
        <p className="text-5xl font-semibold" aria-hidden="true">{step.count}</p>
        <p className="mt-2 text-lg">{step.prompt}</p>
        <p className="mt-1 text-xs text-muted">Step {i + 1} of {toolkit.grounding.length}</p>
      </div>
      <div className="flex justify-between">
        <Button variant="secondary" disabled={i === 0} onClick={() => setI(i - 1)}>Back</Button>
        {i < toolkit.grounding.length - 1 ? <Button onClick={() => setI(i + 1)}>Next</Button> : <Button variant="secondary" onClick={() => setI(0)}>Start again</Button>}
      </div>
    </Card>
  );
}

function Prompts({ toolkit }: { toolkit: Toolkit }) {
  const navigate = useNavigate();
  const [kind, setKind] = useState<'reflection' | 'gratitude'>('reflection');
  const list = kind === 'reflection' ? toolkit.reflectionPrompts : toolkit.gratitudePrompts;
  const [i, setI] = useState(0);
  const prompt = list[i % list.length];
  return (
    <Card className="space-y-4">
      <div className="flex gap-2">
        <Button variant={kind === 'reflection' ? 'primary' : 'secondary'} aria-pressed={kind === 'reflection'} onClick={() => { setKind('reflection'); setI(0); }}>Reflection</Button>
        <Button variant={kind === 'gratitude' ? 'primary' : 'secondary'} aria-pressed={kind === 'gratitude'} onClick={() => { setKind('gratitude'); setI(0); }}>Gratitude</Button>
      </div>
      <blockquote className="rounded-xl bg-sunken p-5 text-lg" aria-live="polite">{prompt}</blockquote>
      <div className="flex flex-wrap gap-3">
        <Button variant="secondary" onClick={() => setI((n) => n + 1)}>Another prompt</Button>
        <Button onClick={() => navigate(`/app/journal/new?prompt=${encodeURIComponent(prompt)}`)}>Write about this in my journal</Button>
      </div>
    </Card>
  );
}

function CopingList() {
  const { data, isLoading, error, refetch } = useCoping();
  const qc = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [details, setDetails] = useState('');
  const [editId, setEditId] = useState<string | null>(null);
  const done = () => void qc.invalidateQueries({ queryKey: keys.coping });
  const fail = (e: unknown) => toast(e instanceof ApiError ? e.message : 'That did not work. Please try again.', 'error');

  const save = useMutation({
    mutationFn: () => (editId ? api.put(`/wellness/coping-strategies/${editId}`, { title, details: details || null }) : api.post('/wellness/coping-strategies', { title, details: details || null })),
    onSuccess: () => { setTitle(''); setDetails(''); setEditId(null); done(); },
    onError: fail,
  });
  const del = useMutation({ mutationFn: (id: string) => api.del(`/wellness/coping-strategies/${id}`), onSuccess: done, onError: fail });
  const reorder = useMutation({ mutationFn: (ids: string[]) => api.put('/wellness/coping-strategies/order', { ids }), onSuccess: done, onError: fail });

  function move(list: Coping[], index: number, delta: number) {
    const ids = list.map((c) => c.id);
    const j = index + delta;
    if (j < 0 || j >= ids.length) return;
    [ids[index], ids[j]] = [ids[j], ids[index]];
    reorder.mutate(ids);
  }
  function submit(e: FormEvent) { e.preventDefault(); if (title.trim()) save.mutate(); }

  if (isLoading) return <PageLoading />;
  if (error || !data) return <ErrorNotice error={error} onRetry={() => void refetch()} />;
  return (
    <div className="space-y-4">
      <Card>
        <form onSubmit={submit} className="space-y-3" noValidate>
          <h2 className="text-lg font-semibold">{editId ? 'Edit strategy' : 'Add something that helps you'}</h2>
          <Field label="Title">{(p) => <Input {...p} required maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />}</Field>
          <Field label="Details (optional)">{(p) => <Textarea {...p} className="min-h-[70px]" maxLength={2000} value={details} onChange={(e) => setDetails(e.target.value)} />}</Field>
          <div className="flex gap-3">
            <Button type="submit" loading={save.isPending} disabled={!title.trim()}>{editId ? 'Save' : 'Add'}</Button>
            {editId && <Button type="button" variant="secondary" onClick={() => { setEditId(null); setTitle(''); setDetails(''); }}>Cancel</Button>}
          </div>
        </form>
      </Card>
      {data.length === 0 ? <EmptyState title="No strategies yet" body="Write down what has helped you before, so it's easy to find on a hard day." /> : (
        <ol className="space-y-2">
          {data.map((c, i) => (
            <li key={c.id}>
              <Card as="article">
                <h3 className="font-medium">{c.title}</h3>
                {c.details && <p className="mt-1 whitespace-pre-wrap text-sm text-muted">{c.details}</p>}
                <div className="mt-2 flex flex-wrap gap-2">
                  <Button variant="ghost" aria-label={`Move ${c.title} up`} disabled={i === 0 || reorder.isPending} onClick={() => move(data, i, -1)}>↑</Button>
                  <Button variant="ghost" aria-label={`Move ${c.title} down`} disabled={i === data.length - 1 || reorder.isPending} onClick={() => move(data, i, 1)}>↓</Button>
                  <Button variant="ghost" onClick={() => { setEditId(c.id); setTitle(c.title); setDetails(c.details ?? ''); }}>Edit</Button>
                  <ConfirmDialog trigger={<Button variant="ghost">Delete</Button>} title={`Delete "${c.title}"?`} description="This cannot be undone." confirmLabel="Delete" onConfirm={() => del.mutate(c.id)} />
                </div>
              </Card>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
