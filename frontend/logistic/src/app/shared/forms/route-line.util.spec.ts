import { buildRouteLine, parseRouteLine } from './route-line.util';

describe('route-line.util', () => {
  describe('buildRouteLine', () => {
    it('returns null for empty', () => {
      expect(buildRouteLine('', '')).toBeNull();
    });
    it('returns address only', () => {
      expect(buildRouteLine('Склад', '')).toBe('Склад');
    });
    it('concatenates contact on second line', () => {
      expect(buildRouteLine('Склад', '+7')).toBe('Склад\nКонтакт: +7');
    });
  });

  describe('parseRouteLine', () => {
    it('round-trips with buildRouteLine', () => {
      const line = buildRouteLine('A', 'B')!;
      expect(parseRouteLine(line)).toEqual({ address: 'A', contact: 'B' });
    });
    it('parses contact-only legacy line', () => {
      expect(parseRouteLine('Контакт: X')).toEqual({
        address: '',
        contact: 'X',
      });
    });
    it('puts unknown second line into empty contact', () => {
      expect(parseRouteLine('A\nB')).toEqual({ address: 'A', contact: '' });
    });
  });
});
