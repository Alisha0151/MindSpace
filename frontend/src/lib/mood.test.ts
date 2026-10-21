import { moodLabel, moodShade, normaliseTag, parseTags } from './mood';
import { qs, readCookie } from './api';

describe('mood helpers (scale is 1-5, same as the backend)', () => {
  it('labels cover the whole scale and clamp out-of-range values', () => {
    expect([1, 2, 3, 4, 5].map(moodLabel)).toEqual(['Very low', 'Low', 'Okay', 'Good', 'Very good']);
    expect(moodLabel(0)).toBe('Very low');
    expect(moodLabel(9)).toBe('Very good');
  });
  it('shade is transparent without data and darker for higher mood', () => {
    expect(moodShade(null)).toBe('transparent');
    const a = Number(moodShade(1).match(/\/ ([\d.]+)\)/)![1]);
    const b = Number(moodShade(5).match(/\/ ([\d.]+)\)/)![1]);
    expect(b).toBeGreaterThan(a);
  });
  it('tags are trimmed, lower-cased, de-duplicated and capped', () => {
    expect(normaliseTag('  Social   Time ')).toBe('social time');
    expect(parseTags('Calm, calm , , Tired')).toEqual(['calm', 'tired']);
    expect(parseTags(Array.from({ length: 20 }, (_, i) => `t${i}`).join(','))).toHaveLength(10);
  });
});

describe('api helpers', () => {
  it('readCookie finds and decodes a value', () => {
    expect(readCookie('XSRF-TOKEN', 'a=1; XSRF-TOKEN=abc%3D%3D; b=2')).toBe('abc==');
    expect(readCookie('missing', 'a=1')).toBeNull();
  });
  it('qs drops empty values and encodes the rest', () => {
    expect(qs({ q: 'a b', tag: '', page: 0, from: undefined })).toBe('?q=a+b&page=0');
    expect(qs({})).toBe('');
  });
});
