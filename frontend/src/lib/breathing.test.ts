import { cycleSeconds, phasesOf, positionAt, totalSeconds } from './breathing';

const box = { inhaleSeconds: 4, holdSeconds: 4, exhaleSeconds: 4, holdAfterExhaleSeconds: 4 };
const calm = { inhaleSeconds: 4, holdSeconds: 0, exhaleSeconds: 6, holdAfterExhaleSeconds: 0 };

describe('breathing state machine', () => {
  it('skips zero-length phases', () => {
    expect(phasesOf(calm).map((s) => s.phase)).toEqual(['inhale', 'exhale']);
    expect(phasesOf(box).map((s) => s.phase)).toEqual(['inhale', 'hold', 'exhale', 'rest']);
  });
  it('cycle and total durations', () => {
    expect(cycleSeconds(box)).toBe(16);
    expect(totalSeconds(calm, 6)).toBe(60);
  });
  it('position at the start, on a boundary and mid-phase', () => {
    expect(positionAt(box, 4, 0)).toEqual({ done: false, cycle: 1, phase: 'inhale', secondsLeftInPhase: 4 });
    expect(positionAt(box, 4, 4)).toEqual({ done: false, cycle: 1, phase: 'hold', secondsLeftInPhase: 4 });
    expect(positionAt(box, 4, 14)).toEqual({ done: false, cycle: 1, phase: 'rest', secondsLeftInPhase: 2 });
    expect(positionAt(box, 4, 16)).toEqual({ done: false, cycle: 2, phase: 'inhale', secondsLeftInPhase: 4 });
  });
  it('finishes exactly after the last cycle', () => {
    expect(positionAt(calm, 2, 19).done).toBe(false);
    expect(positionAt(calm, 2, 20).done).toBe(true);
    expect(positionAt(calm, 2, 999).done).toBe(true);
  });
});
