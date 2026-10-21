import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useSession } from '@/app/session';
import { Alert, Button, Card, Field, Input } from '@/components/ui';
import { ApiError, api } from '@/lib/api';
import { isValidTimeZone } from '@/lib/dates';
import type { User } from '@/types';

function fieldMessage(err: unknown, field: string): string | undefined {
  return err instanceof ApiError ? err.fieldErrors.find((f) => f.field === field)?.message : undefined;
}

function Frame({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center p-4">
      <Link to="/" className="mb-6 text-center text-xl font-semibold">MindSpace</Link>
      <Card>
        <h1 className="mb-4 text-xl font-semibold">{title}</h1>
        {children}
      </Card>
    </main>
  );
}

export function LoginPage() {
  const { setUser } = useSession();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const user = await api.post<User>('/auth/login', { email, password }, true);
      setUser(user);
      navigate((location.state as { from?: string } | null)?.from ?? '/app', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const message = error instanceof ApiError
    ? error.status === 401 ? 'E-mail or password is incorrect.' : error.message
    : error ? 'Something went wrong. Please try again.' : null;

  return (
    <Frame title="Sign in">
      <form onSubmit={submit} className="space-y-4" noValidate>
        {message && <Alert>{message}</Alert>}
        <Field label="E-mail">{(p) => <Input {...p} type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />}</Field>
        <Field label="Password">{(p) => <Input {...p} type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />}</Field>
        <Button type="submit" loading={busy} className="w-full">Sign in</Button>
      </form>
      <p className="mt-4 text-center text-sm text-muted">New here? <Link className="underline" to="/register">Create an account</Link></p>
    </Frame>
  );
}

export function RegisterPage() {
  const { setUser } = useSession();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    displayName: '', email: '', password: '', confirmPassword: '',
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC', acceptTerms: false,
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  async function submit(e: FormEvent) {
    e.preventDefault();
    const problems: Record<string, string> = {};
    if (!form.displayName.trim()) problems.displayName = 'Enter a name.';
    if (!/^\S+@\S+\.\S+$/.test(form.email)) problems.email = 'Enter a valid e-mail address.';
    if (form.password.length < 12) problems.password = 'Use at least 12 characters.';
    if (form.password !== form.confirmPassword) problems.confirmPassword = 'Passwords do not match.';
    if (!isValidTimeZone(form.timezone)) problems.timezone = 'Unknown time zone.';
    if (!form.acceptTerms) problems.acceptTerms = 'Please acknowledge the privacy notice.';
    setClientErrors(problems);
    if (Object.keys(problems).length) return;
    setBusy(true);
    setError(null);
    try {
      setUser(await api.post<User>('/auth/register', form, true));
      navigate('/app', { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }
  const err = (k: string) => clientErrors[k] ?? fieldMessage(error, k);

  return (
    <Frame title="Create your account">
      <form onSubmit={submit} className="space-y-4" noValidate>
        {error instanceof ApiError && !error.fieldErrors.length && <Alert>{error.message}</Alert>}
        <Field label="Name" error={err('displayName')}>{(p) => <Input {...p} autoComplete="name" value={form.displayName} onChange={set('displayName')} />}</Field>
        <Field label="E-mail" error={err('email')}>{(p) => <Input {...p} type="email" autoComplete="email" value={form.email} onChange={set('email')} />}</Field>
        <Field label="Password" hint="At least 12 characters. A short passphrase of several words works well." error={err('password')}>
          {(p) => <Input {...p} type="password" autoComplete="new-password" value={form.password} onChange={set('password')} />}
        </Field>
        <Field label="Confirm password" error={err('confirmPassword')}>{(p) => <Input {...p} type="password" autoComplete="new-password" value={form.confirmPassword} onChange={set('confirmPassword')} />}</Field>
        <Field label="Time zone" hint="Decides what counts as 'today' for check-ins and habits." error={err('timezone')}>{(p) => <Input {...p} value={form.timezone} onChange={set('timezone')} />}</Field>
        <div>
          <label className="flex items-start gap-2 text-sm">
            <input type="checkbox" className="mt-1 h-4 w-4" checked={form.acceptTerms} onChange={set('acceptTerms')} aria-invalid={!!err('acceptTerms')} />
            <span>I have read the <Link to="/privacy" className="underline" target="_blank">privacy notice</Link> and understand MindSpace is a self-reflection tool, not medical care.</span>
          </label>
          {err('acceptTerms') && <p role="alert" className="mt-1 text-xs text-danger">{err('acceptTerms')}</p>}
        </div>
        <Button type="submit" loading={busy} className="w-full">Create account</Button>
      </form>
      <p className="mt-4 text-center text-sm text-muted">Already have an account? <Link className="underline" to="/login">Sign in</Link></p>
    </Frame>
  );
}
