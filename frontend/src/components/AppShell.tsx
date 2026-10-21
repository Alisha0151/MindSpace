import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useSession } from '@/app/session';
import { Button } from './ui';

const NAV = [
  { to: '/app', label: 'Home', end: true },
  { to: '/app/check-in', label: 'Check-in' },
  { to: '/app/journal', label: 'Journal' },
  { to: '/app/habits', label: 'Habits' },
  { to: '/app/calendar', label: 'Calendar' },
  { to: '/app/insights', label: 'Insights' },
  { to: '/app/wellness', label: 'Toolkit' },
  { to: '/app/help', label: 'Get help' },
  { to: '/app/settings', label: 'Settings' },
];

export function AppShell() {
  const { user, signOut } = useSession();
  const navigate = useNavigate();
  const link = ({ isActive }: { isActive: boolean }) =>
    `block rounded-xl px-3 py-2 text-sm ${isActive ? 'bg-sunken font-semibold' : 'text-muted hover:bg-sunken hover:text-ink'}`;
  return (
    <div className="min-h-screen md:grid md:grid-cols-[14rem_1fr]">
      <a href="#main" className="sr-only-focusable fixed left-2 top-2 z-50 rounded-xl bg-surface p-2 text-sm">Skip to content</a>
      <aside className="border-b border-line bg-surface md:border-b-0 md:border-r">
        <div className="flex items-center justify-between px-4 py-3 md:block">
          <span className="text-lg font-semibold">MindSpace</span>
          <span className="text-xs text-muted md:mt-1 md:block">{user?.displayName}</span>
        </div>
        <nav aria-label="Main" className="flex gap-1 overflow-x-auto px-2 pb-2 md:flex-col md:overflow-visible md:pb-0">
          {NAV.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={link}>{n.label}</NavLink>
          ))}
        </nav>
        <div className="hidden p-3 md:block">
          <Button variant="secondary" className="w-full" onClick={async () => { await signOut(); navigate('/'); }}>Sign out</Button>
        </div>
      </aside>
      <main id="main" tabIndex={-1} className="mx-auto w-full max-w-4xl p-4 sm:p-6">
        <Outlet />
        <div className="mt-8 md:hidden">
          <Button variant="secondary" className="w-full" onClick={async () => { await signOut(); navigate('/'); }}>Sign out</Button>
        </div>
      </main>
    </div>
  );
}
