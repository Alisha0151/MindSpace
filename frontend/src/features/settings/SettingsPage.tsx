import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '@/app/session';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Alert, Button, Card, ErrorNotice, Field, Input, PageHeader, PageLoading, Select } from '@/components/ui';
import { useToast } from '@/components/toast';
import { ApiError, api } from '@/lib/api';
import { formatDate, isValidTimeZone } from '@/lib/dates';
import type { Consents, SessionInfo, Theme, User } from '@/types';

const fieldError = (e: unknown, field: string) => (e instanceof ApiError ? e.fieldErrors.find((f) => f.field === field)?.message : undefined);

export function SettingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader title="Settings" />
      <ProfileCard />
      <PasswordCard />
      <PrivacyCard />
      <SessionsCard />
      <DataCard />
    </div>
  );
}

function ProfileCard() {
  const { user, setUser } = useSession();
  const toast = useToast();
  const [name, setName] = useState(user?.displayName ?? '');
  const [tz, setTz] = useState(user?.timezone ?? 'UTC');
  const [theme, setTheme] = useState<Theme>(user?.theme ?? 'SYSTEM');
  const tzBad = !isValidTimeZone(tz);
  const save = useMutation({
    mutationFn: () => api.patch<User>('/users/me', { displayName: name.trim(), timezone: tz.trim(), theme }),
    onSuccess: (u) => { setUser(u); toast('Profile saved'); },
  });
  return (
    <Card>
      <form className="space-y-4" noValidate onSubmit={(e: FormEvent) => { e.preventDefault(); if (!tzBad && name.trim()) save.mutate(); }}>
        <h2 className="text-lg font-semibold">Profile and appearance</h2>
        <p className="text-sm text-muted">Signed in as {user?.email}</p>
        <Field label="Display name" error={fieldError(save.error, 'displayName')}>{(p) => <Input {...p} maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />}</Field>
        <Field label="Time zone" hint="Decides which calendar day a check-in or habit belongs to, e.g. Asia/Kolkata" error={tzBad ? 'Not a valid IANA time zone' : fieldError(save.error, 'timezone')}>
          {(p) => <Input {...p} value={tz} onChange={(e) => setTz(e.target.value)} />}
        </Field>
        <Field label="Theme">{(p) => <Select {...p} value={theme} onChange={(e) => setTheme(e.target.value as Theme)}><option value="SYSTEM">Match my device</option><option value="LIGHT">Light</option><option value="DARK">Dark</option></Select>}</Field>
        {save.error && !(save.error instanceof ApiError && save.error.fieldErrors.length > 0) && <ErrorNotice error={save.error} />}
        <Button type="submit" loading={save.isPending} disabled={tzBad || !name.trim()}>Save</Button>
      </form>
    </Card>
  );
}

function PasswordCard() {
  const toast = useToast();
  const [cur, setCur] = useState(''); const [next, setNext] = useState(''); const [again, setAgain] = useState('');
  const mismatch = again !== '' && next !== again;
  const change = useMutation({
    mutationFn: () => api.put('/users/me/password', { currentPassword: cur, newPassword: next, confirmPassword: again }),
    onSuccess: () => { setCur(''); setNext(''); setAgain(''); toast('Password changed. Your other devices were signed out.'); },
  });
  return (
    <Card>
      <form className="space-y-4" noValidate onSubmit={(e: FormEvent) => { e.preventDefault(); if (!mismatch && cur && next) change.mutate(); }}>
        <h2 className="text-lg font-semibold">Change password</h2>
        <Field label="Current password" error={fieldError(change.error, 'currentPassword')}>{(p) => <Input {...p} type="password" autoComplete="current-password" value={cur} onChange={(e) => setCur(e.target.value)} />}</Field>
        <Field label="New password" hint="At least 12 characters. A few random words work well." error={fieldError(change.error, 'newPassword') ?? fieldError(change.error, 'password')}>{(p) => <Input {...p} type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />}</Field>
        <Field label="Repeat new password" error={mismatch ? 'Passwords do not match' : fieldError(change.error, 'confirmPassword')}>{(p) => <Input {...p} type="password" autoComplete="new-password" value={again} onChange={(e) => setAgain(e.target.value)} />}</Field>
        {change.error && !(change.error instanceof ApiError && change.error.fieldErrors.length > 0) && <ErrorNotice error={change.error} />}
        <Button type="submit" loading={change.isPending} disabled={!cur || !next || mismatch}>Change password</Button>
      </form>
    </Card>
  );
}

function PrivacyCard() {
  const qc = useQueryClient();
  const toast = useToast();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['consents'], queryFn: () => api.get<Consents>('/users/me/consents') });
  const set = useMutation({
    mutationFn: (v: boolean) => api.put<Consents>('/users/me/consents', { optionalUsageAnalytics: v }),
    onSuccess: (c) => { qc.setQueryData(['consents'], c); toast('Preference saved'); },
    onError: () => toast('Could not save the preference', 'error'),
  });
  return (
    <Card>
      <h2 className="text-lg font-semibold">Privacy</h2>
      {isLoading ? <PageLoading /> : error || !data ? <ErrorNotice error={error} onRetry={() => void refetch()} /> : (
        <div className="mt-2 space-y-3 text-sm">
          <p className="text-muted">Your entries are never shared, sold or used for advertising. Policy version {data.policyVersion}.</p>
          <label className="flex min-h-[44px] items-start gap-3">
            <input type="checkbox" className="mt-1 h-5 w-5" checked={data.optionalUsageAnalytics} disabled={set.isPending} onChange={(e) => set.mutate(e.target.checked)} />
            <span>Allow anonymous usage statistics (optional). Off by default; never includes what you write.</span>
          </label>
        </div>
      )}
    </Card>
  );
}

function SessionsCard() {
  const qc = useQueryClient();
  const toast = useToast();
  const navigate = useNavigate();
  const { setUser } = useSession();
  const { data, isLoading, error, refetch } = useQuery({ queryKey: ['sessions'], queryFn: () => api.get<SessionInfo[]>('/auth/sessions') });
  const revoke = useMutation({
    mutationFn: (id: string) => api.del(`/auth/sessions/${id}`),
    onSuccess: () => { void qc.invalidateQueries({ queryKey: ['sessions'] }); toast('Device signed out'); },
    onError: () => toast('Could not sign that device out', 'error'),
  });
  const all = useMutation({
    mutationFn: () => api.post('/auth/logout-all'),
    onSuccess: () => { setUser(null); navigate('/login'); },
    onError: () => toast('Could not sign out everywhere', 'error'),
  });
  return (
    <Card>
      <h2 className="text-lg font-semibold">Devices signed in</h2>
      {isLoading ? <PageLoading /> : error || !data ? <ErrorNotice error={error} onRetry={() => void refetch()} /> : (
        <>
          <ul className="mt-2 divide-y divide-line">
            {data.map((s) => (
              <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
                <div>
                  <p className="font-medium">{s.userAgent ? s.userAgent.slice(0, 80) : 'Unknown device'}{s.current && ' (this device)'}</p>
                  <p className="text-xs text-muted">Signed in {formatDate(s.issuedAt.slice(0, 10))}</p>
                </div>
                {!s.current && <Button variant="secondary" loading={revoke.isPending && revoke.variables === s.id} onClick={() => revoke.mutate(s.id)}>Sign out</Button>}
              </li>
            ))}
          </ul>
          <div className="mt-3"><Button variant="secondary" loading={all.isPending} onClick={() => all.mutate()}>Sign out everywhere</Button></div>
        </>
      )}
    </Card>
  );
}

function DataCard() {
  const toast = useToast();
  const navigate = useNavigate();
  const { setUser } = useSession();
  const [password, setPassword] = useState('');
  const [exporting, setExporting] = useState(false);

  async function download() {
    setExporting(true);
    try {
      const data = await api.get<unknown>('/privacy/export');
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'mindspace-export.json';
      a.click();
      URL.revokeObjectURL(url);
      toast('Export downloaded');
    } catch (e) {
      toast(e instanceof ApiError ? e.message : 'Export failed', 'error');
    } finally {
      setExporting(false);
    }
  }
  const del = useMutation({
    mutationFn: () => api.del('/account', { password }),
    onSuccess: () => { setUser(null); navigate('/', { replace: true }); },
  });
  return (
    <Card>
      <h2 className="text-lg font-semibold">Your data</h2>
      <div className="mt-3 space-y-5">
        <div>
          <p className="text-sm text-muted">Download everything MindSpace holds about you as a JSON file: profile, check-ins, journal, habits and strategies. It contains your private writing, so store it safely.</p>
          <Button className="mt-2" variant="secondary" loading={exporting} onClick={() => void download()}>Download my data</Button>
        </div>
        <div className="border-t border-line pt-4">
          <h3 className="font-medium text-danger">Delete account</h3>
          <p className="text-sm text-muted">Permanently deletes your account and all your data. This cannot be undone. Export first if you want a copy.</p>
          <ConfirmDialog
            trigger={<Button className="mt-2" variant="danger">Delete my account…</Button>}
            title="Delete your account?" description="Everything is erased immediately and cannot be recovered. Enter your password to confirm."
            confirmLabel="Delete everything" busy={del.isPending} onConfirm={() => del.mutate()}>
            <div className="mt-3">
              <Field label="Password" error={fieldError(del.error, 'password')}>{(p) => <Input {...p} type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />}</Field>
              {del.error && !fieldError(del.error, 'password') && <Alert>{del.error instanceof ApiError ? del.error.message : 'Deletion failed'}</Alert>}
            </div>
          </ConfirmDialog>
        </div>
      </div>
    </Card>
  );
}
