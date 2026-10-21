/** Pure breathing-session state machine so the timing logic is testable without a DOM or timers. */
export type Phase = 'inhale' | 'hold' | 'exhale' | 'rest';

export interface Pattern {
  inhaleSeconds: number;
  holdSeconds: number;
  exhaleSeconds: number;
  holdAfterExhaleSeconds: number;
}

export interface Step {
  phase: Phase;
  seconds: number;
}

export function phasesOf(p: Pattern): Step[] {
  const steps: Step[] = [{ phase: 'inhale', seconds: p.inhaleSeconds }];
  if (p.holdSeconds > 0) steps.push({ phase: 'hold', seconds: p.holdSeconds });
  steps.push({ phase: 'exhale', seconds: p.exhaleSeconds });
  if (p.holdAfterExhaleSeconds > 0) steps.push({ phase: 'rest', seconds: p.holdAfterExhaleSeconds });
  return steps;
}

export function cycleSeconds(p: Pattern): number {
  return phasesOf(p).reduce((sum, s) => sum + s.seconds, 0);
}

export function totalSeconds(p: Pattern, cycles: number): number {
  return cycleSeconds(p) * cycles;
}

export interface Position {
  done: boolean;
  cycle: number; // 1-based
  phase: Phase;
  secondsLeftInPhase: number;
}

/** Where a session is after `elapsed` whole seconds. */
export function positionAt(p: Pattern, cycles: number, elapsed: number): Position {
  const steps = phasesOf(p);
  const per = cycleSeconds(p);
  if (elapsed >= per * cycles) {
    return { done: true, cycle: cycles, phase: steps[steps.length - 1].phase, secondsLeftInPhase: 0 };
  }
  const cycle = Math.floor(elapsed / per) + 1;
  let t = elapsed % per;
  for (const s of steps) {
    if (t < s.seconds) return { done: false, cycle, phase: s.phase, secondsLeftInPhase: s.seconds - t };
    t -= s.seconds;
  }
  /* unreachable: t < per */
  return { done: true, cycle, phase: 'rest', secondsLeftInPhase: 0 };
}

export const PHASE_LABEL: Record<Phase, string> = {
  inhale: 'Breathe in',
  hold: 'Hold',
  exhale: 'Breathe out',
  rest: 'Rest',
};
