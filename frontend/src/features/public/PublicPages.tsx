import { Link } from 'react-router-dom';
import { Card } from '@/components/ui';

const FEATURES = [
  ['Mood check-ins', 'Record mood, stress, energy and sleep in under a minute, with the emotions and activities behind them.'],
  ['Private journal', 'Write freely, search and filter by tag. Entries are visible only to your signed-in account.'],
  ['Habits', 'Daily and weekly habits with streaks that forgive pauses instead of punishing them.'],
  ['Insights', 'Trends from your own entries, shown with how much data they rest on. Observations, not diagnoses.'],
  ['Toolkit', 'Guided breathing, 5-4-3-2-1 grounding and reflection prompts.'],
  ['Your data, your call', 'Export everything as JSON or delete your account and all data at any time.'],
];

export function LandingPage() {
  return (
    <div className="mx-auto max-w-4xl p-6">
      <header className="flex items-center justify-between py-2">
        <span className="text-lg font-semibold">MindSpace</span>
        <nav aria-label="Account" className="flex gap-3 text-sm">
          <Link to="/login" className="underline">Sign in</Link>
          <Link to="/register" className="rounded-xl bg-accent px-3 py-1.5 text-accent-ink">Get started</Link>
        </nav>
      </header>
      <main>
        <section className="py-14">
          <h1 className="max-w-2xl text-4xl font-semibold tracking-tight">A private place to notice how you are doing.</h1>
          <p className="mt-4 max-w-xl text-muted">Check in, write things down, build small habits and see your own patterns over time. No ads, no social feed, no selling data.</p>
          <Link to="/register" className="mt-6 inline-block rounded-xl bg-accent px-5 py-3 text-accent-ink">Create a free account</Link>
        </section>
        <section aria-labelledby="features" className="grid gap-3 sm:grid-cols-2">
          <h2 id="features" className="sr-only">Features</h2>
          {FEATURES.map(([t, d]) => (
            <Card key={t} as="article"><h3 className="font-medium">{t}</h3><p className="mt-1 text-sm text-muted">{d}</p></Card>
          ))}
        </section>
        <p className="mt-10 rounded-xl border border-line p-4 text-sm text-muted">
          MindSpace is a self-reflection tool. It is not medical advice, diagnosis or treatment, and it cannot respond in an emergency. If you are in crisis, contact local emergency services or a crisis line.
        </p>
      </main>
    </div>
  );
}

export function PrivacyPage() {
  return (
    <main className="mx-auto max-w-2xl space-y-4 p-6">
      <Link to="/" className="text-sm underline">Back</Link>
      <h1 className="text-2xl font-semibold">Privacy notice</h1>
      <p className="text-sm text-muted">Policy version 2026-01. Template text for a portfolio project; have it reviewed before real-world use.</p>
      <h2 className="text-lg font-medium">What is stored</h2>
      <p className="text-sm">Your e-mail, display name, time zone and a salted password hash; the check-ins, journal entries, habits and coping strategies you create; and hashed session tokens. Nothing is shared with third parties and there is no advertising or tracking.</p>
      <h2 className="text-lg font-medium">Your controls</h2>
      <p className="text-sm">Settings lets you export all data as JSON, sign out other devices, and permanently delete your account and everything linked to it.</p>
      <h2 className="text-lg font-medium">Not a medical service</h2>
      <p className="text-sm">Insights are summaries of what you entered. They are not diagnoses or treatment. If you feel unsafe, contact local emergency services or a crisis line.</p>
    </main>
  );
}

export function NotFoundPage() {
  return (
    <main className="mx-auto max-w-md p-10 text-center">
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <p className="mt-2 text-sm text-muted">That address does not exist.</p>
      <Link to="/" className="mt-4 inline-block underline">Go home</Link>
    </main>
  );
}
