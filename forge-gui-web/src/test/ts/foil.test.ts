import { describe, expect, it } from 'vitest';
import { imageUrl, smallImage } from '../../main/ts/images';
import { foilSeed } from '../../main/ts/match/foil';

describe('a foil picture', () => {
  // The server renders the board's cards and the browser renders the preview, so both must use the same foil seed. WebServerTest checks the same number
  it('has the seed the server gives it at either size, and is told from a plain one by the mark on its name alone', () => {
    const src = imageUrl('c:Shivan Dragon+|M14|1');
    expect(foilSeed(src)).toBe(28);
    expect(foilSeed(smallImage(src))).toBe(28);
    expect(foilSeed(imageUrl('c:Shivan Dragon+|M14|1$alt'))).toBeGreaterThan(0);
    expect(foilSeed(imageUrl('c:Shivan Dragon|M14|1'))).toBe(0);
    expect(foilSeed(imageUrl('c:Shivan Dragon|M14+|1'))).toBe(0);
    expect(foilSeed(imageUrl('t:goblin+|m14'))).toBe(0);
  });
});
