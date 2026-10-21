import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Badge, Button, Card, EmptyState, ErrorNotice, Field, Input, PageHeader, PageLoading, Textarea } from '@/components/ui';
import { useToast } from '@/components/toast';
import { ApiError, api } from '@/lib/api';
import { formatDate } from '@/lib/dates';
import { parseTags } from '@/lib/mood';
import { useJournalEntry, useJournalList, useToolkit } from '@/lib/queries';
import type { JournalDetail, JournalInput } from '@/types';

const fieldError = (e: unknown, field: string) => (e instanceof ApiError ? e.fieldErrors.find((f) => f.field === field)?.message : undefined);

export function JournalListPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const tag = params.get('tag') ?? '';
  const from = params.get('from') ?? '';
  const to = params.get('to') ?? '';
  const page = Number(params.get('page') ?? '0') || 0;
  const [draft, setDraft] = useState(q);
  const { data, isLoading, isFetching, error, refetch } = useJournalList({ q, tag, from, to, page });

  const set = (next: Record<string, string>) => {
    const sp = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) (v ? sp.set(k, v) : sp.delete(k));
    if (!('page' in next)) sp.delete('page');
    setParams(sp);
  };
  const filtered = !!(q || tag || from || to);

  return (
    <div className="space-y-5">
      <PageHeader title="Journal" subtitle="Private. Only you can read your entries." actions={<Link to="/app/journal/new"><Button>New entry</Button></Link>} />
      <form role="search" className="grid gap-3 sm:grid-cols-[1fr_auto]" onSubmit={(e) => { e.preventDefault(); set({ q: draft.trim() }); }}>
        <Field label="Search entries">{(p) => <Input {...p} type="search" value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="Words in a title or entry" />}</Field>
        <Button type="submit" className="self-end">Search</Button>
      </form>
      <div className="grid gap-3 sm:grid-cols-3">
        <Field label="Tag">{(p) => <Input {...p} value={tag} onChange={(e) => set({ tag: e.target.value.trim().toLowerCase() })} />}</Field>
        <Field label="From">{(p) => <Input {...p} type="date" value={from} onChange={(e) => set({ from: e.target.value })} />}</Field>
        <Field label="To">{(p) => <Input {...p} type="date" value={to} onChange={(e) => set({ to: e.target.value })} />}</Field>
      </div>
      {filtered && <Button variant="ghost" onClick={() => { setDraft(''); setParams(new URLSearchParams()); }}>Clear filters</Button>}

      <div aria-live="polite" aria-busy={isFetching}>
        {isLoading ? <PageLoading /> : error ? <ErrorNotice error={error} onRetry={() => void refetch()} /> :
          !data || data.items.length === 0 ? (
            <EmptyState title={filtered ? 'No entries match' : 'Your journal is empty'} body={filtered ? 'Try different words or clear the filters.' : 'Write whatever is on your mind. It stays private to you.'}
              action={!filtered ? <Link to="/app/journal/new"><Button>Write the first entry</Button></Link> : undefined} />
          ) : (
            <>
              <p className="mb-2 text-sm text-muted">{data.totalItems} entr{data.totalItems === 1 ? 'y' : 'ies'}</p>
              <ul className="space-y-3">
                {data.items.map((e) => (
                  <li key={e.id}>
                    <Card as="article">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <h2 className="font-semibold"><Link to={`/app/journal/${e.id}`} className="hover:underline">{e.title || 'Untitled'}</Link></h2>
                        <span className="text-xs text-muted">{formatDate(e.entryDate)}</span>
                      </div>
                      <p className="mt-1 text-sm text-muted">{e.excerpt}</p>
                      {e.tags.length > 0 && <p className="mt-2 flex flex-wrap gap-1.5">{e.tags.map((t) => <Badge key={t}>{t}</Badge>)}</p>}
                    </Card>
                  </li>
                ))}
              </ul>
              {data.totalPages > 1 && (
                <nav aria-label="Pagination" className="mt-4 flex items-center justify-between">
                  <Button variant="secondary" disabled={page <= 0} onClick={() => set({ page: String(page - 1) })}>Previous</Button>
                  <span className="text-sm text-muted">Page {page + 1} of {data.totalPages}</span>
                  <Button variant="secondary" disabled={page + 1 >= data.totalPages} onClick={() => set({ page: String(page + 1) })}>Next</Button>
                </nav>
              )}
            </>
          )}
      </div>
    </div>
  );
}

export function JournalEditorPage() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const toast = useToast();
  const entry = useJournalEntry(id);
  const toolkit = useToolkit();

  const [title, setTitle] = useState('');
  const [body, setBody] = useState(params.get('body') ?? '');
  const [tags, setTags] = useState('');
  const [prompt, setPrompt] = useState<string | null>(params.get('prompt'));

  useEffect(() => {
    if (!entry.data) return;
    setTitle(entry.data.title ?? ''); setBody(entry.data.body); setTags(entry.data.tags.join(', ')); setPrompt(entry.data.promptText);
  }, [entry.data]);

  const refresh = () => { void qc.invalidateQueries({ queryKey: ['journal'] }); void qc.invalidateQueries({ queryKey: ['dashboard'] }); void qc.invalidateQueries({ queryKey: ['calendar'] }); };
  const save = useMutation({
    mutationFn: (input: JournalInput) => (id ? api.put<JournalDetail>(`/journal/${id}`, input) : api.post<JournalDetail>('/journal', input)),
    onSuccess: (saved) => { toast('Entry saved'); refresh(); navigate(`/app/journal/${saved.id}`, { replace: true }); },
  });
  const remove = useMutation({
    mutationFn: () => api.del(`/journal/${id}`),
    onSuccess: () => { toast('Entry deleted'); refresh(); navigate('/app/journal', { replace: true }); },
    onError: () => toast('Could not delete the entry', 'error'),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!body.trim()) return;
    save.mutate({ title: title.trim() || null, body, promptText: prompt, tags: parseTags(tags) });
  }

  if (id && entry.isLoading) return <PageLoading />;
  if (id && entry.error) return entry.error instanceof ApiError && entry.error.status === 404
    ? <EmptyState title="Entry not found" body="It may have been deleted." action={<Link to="/app/journal"><Button>Back to journal</Button></Link>} />
    : <ErrorNotice error={entry.error} onRetry={() => void entry.refetch()} />;

  const bodyBlank = !body.trim();
  return (
    <div className="space-y-5">
      <PageHeader title={id ? 'Edit entry' : 'New entry'} actions={<Link className="text-sm underline" to="/app/journal">Back to journal</Link>} />
      <form onSubmit={submit} className="space-y-4" noValidate>
        {prompt && <Card><p className="text-sm"><span className="font-medium">Prompt: </span>{prompt}</p><Button type="button" variant="ghost" onClick={() => setPrompt(null)}>Remove prompt</Button></Card>}
        {!prompt && toolkit.data && (
          <Button type="button" variant="secondary" onClick={() => { const p = toolkit.data.reflectionPrompts; setPrompt(p[Math.floor(Math.random() * p.length)]); }}>Suggest a prompt</Button>
        )}
        <Field label="Title (optional)" error={fieldError(save.error, 'title')}>{(p) => <Input {...p} maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} />}</Field>
        <Field label="Entry" hint={`${body.length}/10000`} error={bodyBlank && save.isError ? 'Write something first' : fieldError(save.error, 'body')}>
          {(p) => <Textarea {...p} required maxLength={10000} className="min-h-[260px]" value={body} onChange={(e) => setBody(e.target.value)} />}
        </Field>
        <Field label="Tags" hint="Comma separated, up to 10" error={fieldError(save.error, 'tags')}>{(p) => <Input {...p} value={tags} onChange={(e) => setTags(e.target.value)} />}</Field>
        {save.error && !(save.error instanceof ApiError && save.error.fieldErrors.length > 0) && <ErrorNotice error={save.error} />}
        <div className="flex flex-wrap gap-3">
          <Button type="submit" loading={save.isPending} disabled={bodyBlank}>Save entry</Button>
          {id && <ConfirmDialog trigger={<Button type="button" variant="secondary">Delete</Button>} title="Delete this entry?" description="This permanently removes the entry. It cannot be recovered." confirmLabel="Delete entry" busy={remove.isPending} onConfirm={() => remove.mutate()} />}
        </div>
      </form>
    </div>
  );
}
