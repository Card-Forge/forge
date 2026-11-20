import { describe, expect, it } from 'vitest';
import { format } from '../../main/ts/text';

describe('a pattern from the language files', () => {
  it('is filled as MessageFormat fills it: two quotes are one, and a quoted run is taken as it stands', () => {
    expect(format("{0} can''t pay {1}", ['Alice', 1200]).join('')).toBe("Alice can't pay 1,200");
    expect(format('\'{0}\' is literal', ['x']).join('')).toBe('{0} is literal');
    expect(format('Play {0} now', [{ card: 1 }])).toEqual(['Play ', { card: 1 }, ' now']);
  });
});
