// Mirrors the backend DTOs (see backend/src/main/java/com/mindspace/**/**Dtos.java).
export type Theme = 'SYSTEM' | 'LIGHT' | 'DARK';
export type Cadence = 'DAILY' | 'WEEKLY';

export interface User {
  id: string;
  email: string;
  displayName: string;
  timezone: string;
  theme: Theme;
  createdAt: string;
}

export interface CheckIn {
  id: string;
  date: string;
  timezone: string;
  moodScore: number;
  stressScore: number;
  energyScore: number;
  sleepMinutes: number;
  sleepQuality: number | null;
  emotions: string[];
  activities: string[];
  contexts: string[];
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CheckInInput {
  date?: string;
  moodScore: number;
  stressScore: number;
  energyScore: number;
  sleepMinutes: number;
  sleepQuality?: number | null;
  emotions: string[];
  activities: string[];
  contexts: string[];
  notes?: string | null;
}

export interface JournalSummary {
  id: string;
  entryDate: string;
  title: string | null;
  excerpt: string;
  tags: string[];
  createdAt: string;
}

export interface JournalDetail {
  id: string;
  entryDate: string;
  title: string | null;
  body: string;
  promptText: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface JournalInput {
  title?: string | null;
  body: string;
  promptText?: string | null;
  entryDate?: string;
  tags: string[];
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Consistency {
  activeDays: number;
  completedDays: number;
  percent: number;
}

export interface Habit {
  id: string;
  name: string;
  description: string | null;
  cadence: Cadence;
  targetCount: number;
  startDate: string;
  endDate: string | null;
  paused: boolean;
  pausedSince: string | null;
  doneToday: boolean;
  currentStreak: number;
  longestStreak: number;
  weeklyStreak: number;
  thisWeekDone: number;
  last7Days: boolean[];
  consistency30: Consistency;
}

export interface HabitInput {
  name: string;
  description?: string | null;
  cadence: Cadence;
  targetCount?: number;
  startDate?: string;
  endDate?: string | null;
}

export interface Coping {
  id: string;
  title: string;
  details: string | null;
  sortOrder: number;
}

export interface Toolkit {
  breathing: {
    id: string;
    name: string;
    description: string;
    inhaleSeconds: number;
    holdSeconds: number;
    exhaleSeconds: number;
    holdAfterExhaleSeconds: number;
    defaultCycles: number;
  }[];
  grounding: { count: number; sense: string; prompt: string }[];
  reflectionPrompts: string[];
  gratitudePrompts: string[];
  disclaimer: string;
}

export interface TrendBucket {
  periodStart: string;
  observations: number;
  avgMood: number | null;
  avgStress?: number | null;
  avgEnergy?: number | null;
}

export interface Analytics {
  from: string;
  to: string;
  observations: number;
  enoughData: boolean;
  averageMood: number | null;
  daily: { date: string; mood: number; stress: number; energy: number; sleepMinutes: number }[];
  weeklyTrend: TrendBucket[];
  monthlyTrend: TrendBucket[];
  moodDistribution: number[];
  topEmotions: { label: string; count: number }[];
  sleepVersusMood: { bucket: string; observations: number; avgMood: number | null }[];
  coverage: { daysLogged: number; daysInRange: number; percent: number };
  habitId: string | null;
  habitVersusMood: { doneDays: number; avgMoodDone: number | null; notDoneDays: number; avgMoodNotDone: number | null } | null;
  comparison: { observations1: number; avgMood1: number | null; observations2: number; avgMood2: number | null } | null;
  compareFrom: string | null;
  compareTo: string | null;
}

export interface CalendarDay {
  date: string;
  checkInId: string | null;
  moodScore: number | null;
  emotions: string[];
  activities: string[];
  journalEntries: number;
}

export interface Dashboard {
  displayName: string;
  today: string;
  todayCheckIn: CheckIn | null;
  checkInStreak: number;
  last7Days: { date: string; mood: number | null }[];
  habits: Habit[];
  habitsDoneToday: number;
  habitsDueToday: number;
  recentJournal: JournalSummary[];
}

export interface SessionInfo {
  id: string;
  issuedAt: string;
  expiresAt: string;
  userAgent: string | null;
  current: boolean;
}

export interface Consents {
  privacyNoticeAcknowledged: boolean;
  wellnessDisclaimerAcknowledged: boolean;
  optionalUsageAnalytics: boolean;
  policyVersion: string;
}

export interface WeekProgress {
  weekStart: string;
  done: number;
  target: number;
  met: boolean;
}

export interface HabitProgress {
  habitId: string;
  from: string;
  to: string;
  completions: string[];
  weeks: WeekProgress[];
  consistency: Consistency;
  currentStreak: number;
  longestStreak: number;
  weeklyStreak: number;
}

export interface CalendarMonth {
  month: string;
  from: string;
  to: string;
  days: CalendarDay[];
}
