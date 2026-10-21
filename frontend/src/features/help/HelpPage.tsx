import { Alert, Card, PageHeader } from '@/components/ui';

/** Verified against each service's own site on 2026-10-20. Numbers change: re-verify before every release. */
export const HELPLINES = [
  { region: 'India', name: 'Tele-MANAS (government mental-health helpline)', contact: '14416 or 1800-891-4416', detail: 'Free, 24x7, multiple languages.', emergency: '112' },
  { region: 'United States', name: '988 Suicide & Crisis Lifeline', contact: 'Call or text 988 (chat on 988lifeline.org)', detail: 'Free, 24/7.', emergency: '911' },
  { region: 'United Kingdom & Ireland', name: 'Samaritans', contact: '116 123', detail: 'Free from landlines and mobiles, 24 hours a day.', emergency: '999 (UK) / 112 or 999 (Ireland)' },
] as const;

export const HELPLINES_VERIFIED_ON = '2026-10-20';

export function HelpPage() {
  return (
    <div className="space-y-5">
      <PageHeader title="Get help now" subtitle="If you are in danger or might hurt yourself, contact emergency services or a crisis line right away." />
      <Alert tone="info">MindSpace is a self-reflection tool. It is not therapy, medical advice, or an emergency service, and nobody monitors your entries.</Alert>
      <ul className="space-y-3">
        {HELPLINES.map((h) => (
          <li key={h.region}>
            <Card as="article">
              <h2 className="font-semibold">{h.region}</h2>
              <p className="mt-1">{h.name}</p>
              <p className="text-lg font-semibold">{h.contact}</p>
              <p className="text-sm text-muted">{h.detail}</p>
              <p className="mt-1 text-sm">Emergency services: <strong>{h.emergency}</strong></p>
            </Card>
          </li>
        ))}
      </ul>
      <p className="text-sm text-muted">Elsewhere: search for your country's crisis line or ask a local clinic. Contact details were last checked on {HELPLINES_VERIFIED_ON} and can change.</p>
      <Card>
        <h2 className="font-semibold">Talking to someone you trust</h2>
        <p className="mt-1 text-sm text-muted">Telling a friend, family member, teacher or doctor how you feel is a good step. If your mood stays low for weeks or gets in the way of daily life, a health professional can help.</p>
      </Card>
    </div>
  );
}
