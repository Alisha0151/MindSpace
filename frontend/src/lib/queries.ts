import { useQuery } from '@tanstack/react-query';
import { api, qs } from './api';
import type { Analytics, CalendarMonth, CheckIn, Coping, Dashboard, Habit, JournalDetail, JournalSummary, Page, Toolkit } from '@/types';

export const keys = {
  dashboard: ['dashboard'] as const,
  checkinToday: ['checkin', 'today'] as const,
  checkins: (from?: string, to?: string) => ['checkin', 'list', from, to] as const,
  habits: ['habits'] as const,
  journal: (params: object) => ['journal', 'list', params] as const,
  journalEntry: (id: string) => ['journal', 'entry', id] as const,
  calendar: (month: string, f: object) => ['calendar', month, f] as const,
  analytics: (p: object) => ['analytics', p] as const,
  toolkit: ['toolkit'] as const,
  coping: ['coping'] as const,
};

export const useDashboard = () => useQuery({ queryKey: keys.dashboard, queryFn: () => api.get<Dashboard>('/dashboard') });
export const useHabits = () => useQuery({ queryKey: keys.habits, queryFn: () => api.get<Habit[]>('/habits') });
export const useToolkit = () => useQuery({ queryKey: keys.toolkit, queryFn: () => api.get<Toolkit>('/wellness/toolkit'), staleTime: Infinity });
export const useCoping = () => useQuery({ queryKey: keys.coping, queryFn: () => api.get<Coping[]>('/wellness/coping-strategies') });
export const useCheckIns = (from: string, to: string) =>
  useQuery({ queryKey: keys.checkins(from, to), queryFn: () => api.get<CheckIn[]>(`/checkins${qs({ from, to })}`) });
export const useJournalEntry = (id: string | undefined) =>
  useQuery({ queryKey: keys.journalEntry(id ?? ''), queryFn: () => api.get<JournalDetail>(`/journal/${id}`), enabled: !!id });
export const useJournalList = (p: { q: string; tag: string; from: string; to: string; page: number }) =>
  useQuery({
    queryKey: keys.journal(p),
    queryFn: () => api.get<Page<JournalSummary>>(`/journal${qs({ q: p.q, tag: p.tag, from: p.from, to: p.to, page: p.page, size: 10 })}`),
    placeholderData: (prev) => prev,
  });
export const useCalendar = (month: string, f: { minMood?: number; emotion?: string; activity?: string }) =>
  useQuery({ queryKey: keys.calendar(month, f), queryFn: () => api.get<CalendarMonth>(`/calendar${qs({ month, ...f })}`) });
export const useAnalytics = (p: { from: string; to: string; compareFrom?: string; compareTo?: string; habitId?: string }) =>
  useQuery({ queryKey: keys.analytics(p), queryFn: () => api.get<Analytics>(`/analytics/summary${qs(p)}`), placeholderData: (prev) => prev });
