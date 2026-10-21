import { forwardRef, useId } from 'react';
import type { ButtonHTMLAttributes, HTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { ApiError } from '@/lib/api';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost';

const variants: Record<Variant, string> = {
  primary: 'bg-accent text-accent-ink hover:opacity-90',
  secondary: 'border border-line bg-surface text-ink hover:bg-sunken',
  danger: 'bg-danger text-white hover:opacity-90',
  ghost: 'text-ink hover:bg-sunken',
};

export function Button({
  variant = 'primary',
  loading = false,
  className = '',
  children,
  disabled,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`inline-flex min-h-[44px] items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 ${variants[variant]} ${className}`}
    >
      {loading && <Spinner small />}
      {children}
    </button>
  );
}

export function Spinner({ small = false, label = 'Loading' }: { small?: boolean; label?: string }) {
  const size = small ? 'h-4 w-4' : 'h-6 w-6';
  return (
    <span role="status" className="inline-flex items-center">
      <span className={`${size} animate-spin rounded-full border-2 border-current border-t-transparent`} aria-hidden="true" />
      <span className="sr-only">{label}</span>
    </span>
  );
}

export function Card({ children, className = '', as: Tag = 'section', ...rest }: { children: ReactNode; className?: string; as?: 'section' | 'div' | 'article' } & HTMLAttributes<HTMLElement>) {
  return <Tag {...rest} className={`rounded-xl border border-line bg-surface p-4 sm:p-5 ${className}`}>{children}</Tag>;
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-muted">{subtitle}</p>}
      </div>
      {actions}
    </div>
  );
}

interface FieldProps {
  label: string;
  error?: string;
  hint?: string;
  children: (props: { id: string; 'aria-describedby'?: string; 'aria-invalid'?: boolean }) => ReactNode;
}

/** Label + control + hint + error, wired together for screen readers. */
export function Field({ label, error, hint, children }: FieldProps) {
  const id = useId();
  const describedBy = [error ? `${id}-err` : null, hint ? `${id}-hint` : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      {children({ id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined })}
      {hint && !error && (
        <p id={`${id}-hint`} className="text-xs text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-err`} role="alert" className="text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  );
}

const control =
  'w-full min-h-[44px] rounded-xl border border-line bg-surface px-3 py-2 text-sm placeholder:text-muted aria-[invalid=true]:border-danger';

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input({ className = '', ...p }, ref) {
  return <input ref={ref} {...p} className={`${control} ${className}`} />;
});

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(function Textarea({ className = '', ...p }, ref) {
  return <textarea ref={ref} {...p} className={`${control} min-h-[120px] ${className}`} />;
});

export function Select({ className = '', ...p }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...p} className={`${control} ${className}`} />;
}

export function Alert({ tone = 'error', children }: { tone?: 'error' | 'info' | 'success'; children: ReactNode }) {
  const tones = { error: 'border-danger text-danger', info: 'border-line text-ink', success: 'border-success text-success' };
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`rounded-xl border bg-surface p-3 text-sm ${tones[tone]}`}>
      {children}
    </div>
  );
}

export function ErrorNotice({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
  const requestId = error instanceof ApiError ? error.requestId : undefined;
  return (
    <Alert>
      <p>{message}</p>
      {requestId && <p className="mt-1 text-xs text-muted">Reference: {requestId}</p>}
      {onRetry && (
        <Button variant="secondary" className="mt-2" onClick={onRetry}>
          Try again
        </Button>
      )}
    </Alert>
  );
}

export function EmptyState({ title, body, action }: { title: string; body?: string; action?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-line p-8 text-center">
      <p className="font-medium">{title}</p>
      {body && <p className="mx-auto mt-1 max-w-md text-sm text-muted">{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-xl bg-sunken ${className}`} />;
}

export function PageLoading({ label = 'Loading' }: { label?: string }) {
  return (
    <div role="status" aria-live="polite" className="space-y-3">
      <span className="sr-only">{label}</span>
      <Skeleton className="h-8 w-1/3" />
      <Skeleton className="h-32" />
      <Skeleton className="h-32" />
    </div>
  );
}

export function Badge({ children }: { children: ReactNode }) {
  return <span className="inline-flex items-center rounded-full bg-sunken px-2.5 py-0.5 text-xs font-medium">{children}</span>;
}

/** Number slider 1-5 with visible value; native range keeps keyboard support. */
export function Scale({ label, value, onChange, min = 1, max = 5, lowLabel, highLabel }: {
  label: string; value: number; onChange: (n: number) => void; min?: number; max?: number; lowLabel?: string; highLabel?: string;
}) {
  return (
    <Field label={`${label}: ${value}`}>
      {(p) => (
        <div>
          <input {...p} type="range" min={min} max={max} value={value} onChange={(e) => onChange(Number(e.target.value))} className="w-full accent-[rgb(var(--c-accent))]" />
          {(lowLabel || highLabel) && (
            <div className="flex justify-between text-xs text-muted">
              <span>{lowLabel}</span>
              <span>{highLabel}</span>
            </div>
          )}
        </div>
      )}
    </Field>
  );
}
