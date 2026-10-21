/**
 * Thin fetch wrapper for the MindSpace API.
 *  - credentials: 'include' so the HttpOnly session cookies are sent (tokens never touch JS or storage)
 *  - echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN on mutating requests (double-submit CSRF)
 *  - on 401, performs ONE shared refresh and retries the request once
 *  - errors are RFC 9457 problem documents, surfaced as ApiError
 */
export interface FieldError {
  field: string;
  message: string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public title: string,
    message: string,
    public fieldErrors: FieldError[] = [],
    public requestId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const BASE = '/api/v1';
export const SESSION_EXPIRED_EVENT = 'mindspace:session-expired';

export function readCookie(name: string, source: string = document.cookie): string | null {
  for (const part of source.split(';')) {
    const [k, ...rest] = part.trim().split('=');
    if (k === name) return decodeURIComponent(rest.join('='));
  }
  return null;
}

let refreshInFlight: Promise<boolean> | null = null;

async function ensureCsrf(): Promise<string | null> {
  const existing = readCookie('XSRF-TOKEN');
  if (existing) return existing;
  await fetch(`${BASE}/auth/csrf`, { credentials: 'include' });
  return readCookie('XSRF-TOKEN');
}

function refreshOnce(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const token = await ensureCsrf();
        const res = await fetch(`${BASE}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: token ? { 'X-XSRF-TOKEN': token } : {},
        });
        return res.ok;
      } catch {
        return false;
      }
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

async function toError(res: Response): Promise<ApiError> {
  let body: { title?: string; detail?: string; errors?: FieldError[]; requestId?: string } = {};
  try {
    body = await res.json();
  } catch {
    /* non-JSON error body */
  }
  const fallback =
    res.status === 429 ? 'Too many attempts. Please wait a moment and try again.' : 'Something went wrong. Please try again.';
  return new ApiError(res.status, body.title ?? 'Error', body.detail ?? fallback, body.errors ?? [], body.requestId);
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  /** Do not try to refresh on 401 (used for login/refresh themselves). */
  noRefresh?: boolean;
  signal?: AbortSignal;
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  const method = opts.method ?? 'GET';
  const attempt = async (): Promise<Response> => {
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET') {
      const token = await ensureCsrf();
      if (token) headers['X-XSRF-TOKEN'] = token;
    }
    return fetch(`${BASE}${path}`, {
      method,
      credentials: 'include',
      headers,
      body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
      signal: opts.signal,
    });
  };

  let res: Response;
  try {
    res = await attempt();
  } catch (e) {
    if ((e as Error).name === 'AbortError') throw e;
    throw new ApiError(0, 'Network error', 'Cannot reach the server. Check your connection and try again.');
  }
  if (res.status === 401 && !opts.noRefresh) {
    if (await refreshOnce()) {
      res = await attempt();
    }
    if (res.status === 401) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    }
  }
  if (!res.ok) throw await toError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  get: <T>(path: string, signal?: AbortSignal) => request<T>(path, { signal }),
  post: <T>(path: string, body?: unknown, noRefresh = false) => request<T>(path, { method: 'POST', body, noRefresh }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body }),
  del: <T = void>(path: string, body?: unknown) => request<T>(path, { method: 'DELETE', body }),
};

export function qs(params: Record<string, string | number | undefined | null>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') sp.set(k, String(v));
  }
  const s = sp.toString();
  return s ? `?${s}` : '';
}
