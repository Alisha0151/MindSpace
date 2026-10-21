import { addDays, daysBetween, formatDuration, isValidTimeZone, monthGrid, shiftMonth, todayIn, weekStart } from './dates';

describe('dates', () => {
  it('todayIn uses the zone, not UTC: 23:30 UTC is already tomorrow in Kolkata', () => {
    const instant = new Date('2026-03-14T23:30:00Z');
    expect(todayIn('UTC', instant)).toBe('2026-03-14');
    expect(todayIn('Asia/Kolkata', instant)).toBe('2026-03-15');
    expect(todayIn('America/Los_Angeles', instant)).toBe('2026-03-14');
  });
  it('addDays crosses month, year and leap boundaries', () => {
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01');
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01');
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });
  it('daysBetween is signed and DST-proof', () => {
    expect(daysBetween('2026-03-01', '2026-03-31')).toBe(30);
    expect(daysBetween('2026-03-31', '2026-03-01')).toBe(-30);
    expect(daysBetween('2026-03-07', '2026-03-09')).toBe(2);
  });
  it('weekStart returns the Monday (ISO week)', () => {
    expect(weekStart('2026-10-14')).toBe('2026-10-12'); // Wednesday
    expect(weekStart('2026-10-12')).toBe('2026-10-12'); // Monday
    expect(weekStart('2026-10-18')).toBe('2026-10-12'); // Sunday belongs to the previous Monday
  });
  it('shiftMonth wraps years', () => {
    expect(shiftMonth('2026-12', 1)).toBe('2027-01');
    expect(shiftMonth('2026-01', -1)).toBe('2025-12');
  });
  it('monthGrid is Monday-first, padded to whole weeks', () => {
    const grid = monthGrid('2026-10'); // 1 Oct 2026 is a Thursday
    expect(grid.length % 7).toBe(0);
    expect(grid.slice(0, 3)).toEqual([null, null, null]);
    expect(grid[3]).toBe('2026-10-01');
    expect(grid.filter(Boolean).length).toBe(31);
  });
  it('formatDuration', () => {
    expect(formatDuration(450)).toBe('7h 30m');
    expect(formatDuration(480)).toBe('8h');
  });
  it('isValidTimeZone', () => {
    expect(isValidTimeZone('Asia/Kolkata')).toBe(true);
    expect(isValidTimeZone('Mars/Olympus')).toBe(false);
  });
});
