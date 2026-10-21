import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { PageLoading } from '@/components/ui';
import { useSession } from './session';

export function ProtectedRoute() {
  const { user, loading } = useSession();
  const location = useLocation();
  if (loading) return <div className="mx-auto max-w-3xl p-6"><PageLoading label="Checking your session" /></div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { user, loading } = useSession();
  if (loading) return <div className="mx-auto max-w-md p-6"><PageLoading /></div>;
  if (user) return <Navigate to="/app" replace />;
  return <Outlet />;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  componentDidCatch(error: Error, info: ErrorInfo) {
    // Log only the error type and component stack; never user content.
    console.error('UI error', error.name, info.componentStack);
  }
  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <div role="alert" className="mx-auto max-w-md p-10 text-center">
        <h1 className="text-xl font-semibold">Something went wrong</h1>
        <p className="mt-2 text-sm text-muted">Your data is safe. Reload the page to continue.</p>
        <button className="mt-4 rounded-xl bg-accent px-4 py-2 text-accent-ink" onClick={() => window.location.reload()}>Reload</button>
      </div>
    );
  }
}
