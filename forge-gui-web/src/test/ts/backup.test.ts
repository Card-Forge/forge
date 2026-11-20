import { describe, expect, it } from 'vitest';
import { backupOf, readBackup } from '../../main/ts/backup';

const DECK = { id: 'd1', text: '[metadata]\nName=Elves\n[Main]\n4 Forest', format: 'Constructed' };

describe('a browser backup', () => {
  it("holds Forge's own values and the decks, leaves out which browser this is, a half-typed import and anything not of Forge, and comes back as it went", () => {
    const made = backupOf({ 'forge.settings': '{"pace":2}', 'forge.clientId': 'abc', 'forge.importDraft': '4 Forest', 'other.site': 'x' }, [DECK]);
    expect(made.storage).toEqual({ 'forge.settings': '{"pace":2}' });
    expect(made.decks).toEqual([DECK]);
    expect(readBackup(JSON.stringify(made))).toEqual(made);
  });
  it('takes nothing from a file that is not one of these', () => {
    expect(readBackup('not json')).toBeNull();
    expect(readBackup('{"storage":{"forge.settings":"{}"}}')).toBeNull();
    expect(readBackup('null')).toBeNull();
  });
  it('drops from a file what a backup would never hold', () => {
    const read = readBackup(JSON.stringify({ app: 'forge-web', version: 1, storage: { 'forge.clientId': 'x', 'evil': 'y', 'forge.settings': 5, 'forge.avatar': '3' },
      decks: [DECK, { id: 7 }, null] }));
    expect(read?.storage).toEqual({ 'forge.avatar': '3' });
    expect(read?.decks).toEqual([DECK]);
  });
});
