/** Scores are 1-5 everywhere (matches the database CHECK constraints and the API). */
export const MIN_SCORE = 1;
export const MAX_SCORE = 5;

export const MOOD_LABELS: Record<number, string> = { 1: 'Very low', 2: 'Low', 3: 'Okay', 4: 'Good', 5: 'Very good' };
export const STRESS_LABELS: Record<number, string> = { 1: 'Very calm', 2: 'Calm', 3: 'Some stress', 4: 'Stressed', 5: 'Very stressed' };
export const ENERGY_LABELS: Record<number, string> = { 1: 'Very low', 2: 'Low', 3: 'Moderate', 4: 'Good', 5: 'High' };

export function moodLabel(score: number): string {
  return MOOD_LABELS[Math.min(MAX_SCORE, Math.max(MIN_SCORE, Math.round(score)))];
}

/** Sequential single-hue scale (light -> dark) so ordering is readable without colour vision; the number is always shown too. */
export function moodShade(score: number | null): string {
  if (score === null) return 'transparent';
  const alpha = 0.12 + (Math.min(MAX_SCORE, Math.max(MIN_SCORE, score)) / MAX_SCORE) * 0.7;
  return `rgb(var(--c-accent) / ${alpha.toFixed(2)})`;
}

export function normaliseTag(raw: string): string {
  return raw.trim().replace(/\s+/g, ' ').toLowerCase();
}

export function parseTags(input: string, max = 10): string[] {
  const seen = new Set<string>();
  for (const part of input.split(',')) {
    const t = normaliseTag(part);
    if (t) seen.add(t);
  }
  return [...seen].slice(0, max);
}

export const EMOTION_SUGGESTIONS = ['calm', 'happy', 'grateful', 'hopeful', 'tired', 'anxious', 'sad', 'irritable', 'lonely', 'overwhelmed'];
export const ACTIVITY_SUGGESTIONS = ['exercise', 'walk', 'work', 'study', 'social time', 'reading', 'cooking', 'screen time'];
