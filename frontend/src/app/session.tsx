import { useQuery, useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect } from 'react';
import type { ReactNode } from 'react';
import { ApiError, SESSION_EXPIRED_EVENT, api } from '@/lib/api';
import type { Theme, User } from '@/types';

interface SessionValue {
  user: User | null;
  loading: boolean;
  setUser: (u: User | null) => void;
  signOut: () => Promise<void>;
}
const SessionContext = createContext<SessionValue | null>(null);
export const ME_KEY = ['me'] as const;

export function applyTheme(theme: Theme) {
  const dark = theme === 'DARK' || (theme === 'SYSTEM' && window.matchMedia?.('(prefers-color-scheme: dark)').matches);
  document.documentElement.dataset.theme = dark ? 'dark' : 'light';
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient();
  const query = useQuery<User | null>({
    queryKey: ME_KEY,
    queryFn: async () => {
      try {
        return await api.get<User>('/users/me');
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null;
        throw e;
      }
    },
    staleTime: 5 * 60_000,
    retry: false,
  });
  const user = query.data ?? null;

  const setUser = useCallback((u: User | null) => {
    qc.setQueryData(ME_KEY, u);
    if (!u) qc.removeQueries({ predicate: (q) => q.queryKey[0] !== 'me' }); // drop cached private data
  }, [qc]);

  const signOut = useCallback(async () => {
    try {
      await api.post('/auth/logout', undefined, true);
    } finally {
      setUser(null);
    }
  }, [setUser]);

  useEffect(() => { applyTheme(user?.theme ?? 'SYSTEM'); }, [user?.theme]);
  useEffect(() => {
    const expired = () => setUser(null);
    window.addEventListener(SESSION_EXPIRED_EVENT, expired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expired);
  }, [setUser]);

  return <SessionContext.Provider value={{ user, loading: query.isLoading, setUser, signOut }}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionValue {
  const v = useContext(SessionContext);
  if (!v) throw new Error('useSession must be used inside SessionProvider');
  return v;
}
