import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AppShell } from '@/components/AppShell';
import { PageLoading } from '@/components/ui';
import { CalendarPage } from '@/features/calendar/CalendarPage';
import { CheckInPage } from '@/features/checkin/CheckInPage';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { HabitsPage } from '@/features/habits/HabitsPage';
import { HelpPage } from '@/features/help/HelpPage';
import { JournalEditorPage, JournalListPage } from '@/features/journal/JournalPages';
import { LandingPage, NotFoundPage, PrivacyPage } from '@/features/public/PublicPages';
import { LoginPage, RegisterPage } from '@/features/auth/AuthPages';
import { SettingsPage } from '@/features/settings/SettingsPage';
import { WellnessPage } from '@/features/wellness/WellnessPage';
import { ProtectedRoute, PublicOnlyRoute } from './guards';

const InsightsPage = lazy(() => import('@/features/insights/InsightsPage').then((m) => ({ default: m.InsightsPage })));

export function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="check-in" element={<CheckInPage />} />
          <Route path="journal" element={<JournalListPage />} />
          <Route path="journal/new" element={<JournalEditorPage />} />
          <Route path="journal/:id" element={<JournalEditorPage />} />
          <Route path="habits" element={<HabitsPage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="insights" element={<Suspense fallback={<PageLoading />}><InsightsPage /></Suspense>} />
          <Route path="wellness" element={<WellnessPage />} />
          <Route path="help" element={<HelpPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
