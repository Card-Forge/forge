// Exports what this browser stores for its player as one file. The file can be loaded in another browser, or restored after storage is cleared

import { listDeviceDecks, putDeviceDeck, type DeviceDeck } from './deck/devicedecks';
import { saveText } from './dom';

const APP = 'forge-web';
// The browser's client id and a half-typed importer list are not the player's settings
const LEFT_OUT = ['forge.clientId', 'forge.importDraft'];

interface Backup {
  app: string;
  version: number;
  storage: Record<string, string>;
  decks: DeviceDeck[];
}

const kept = (key: string): boolean => key.startsWith('forge.') && !LEFT_OUT.includes(key);

/** Builds the file contents from what a browser holds: every Forge value in storage, and the decks stored in it. */
export function backupOf(storage: Record<string, string>, decks: DeviceDeck[]): Backup {
  return { app: APP, version: 1, storage: Object.fromEntries(Object.entries(storage).filter(([key]) => kept(key))), decks };
}

/** Parses a backup file, or returns null if it is not one. Any entry that is not a Forge value is dropped. */
export function readBackup(text: string): Backup | null {
  try {
    const file = JSON.parse(text) as Partial<Backup> | null;
    if (file?.app !== APP || typeof file.storage !== 'object' || file.storage === null) return null;
    const storage = Object.fromEntries(Object.entries(file.storage).filter(([key, value]) => kept(key) && typeof value === 'string'));
    const decks = (Array.isArray(file.decks) ? file.decks : [])
      .filter(d => typeof d?.id === 'string' && typeof d.text === 'string' && typeof d.format === 'string');
    return { app: APP, version: 1, storage, decks };
  } catch {
    return null;
  }
}

/** Saves the backup file for the player. If the browser blocks storage, the file has no stored values. */
export async function exportBackup(): Promise<void> {
  const storage: Record<string, string> = {};
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key !== null) storage[key] = localStorage.getItem(key) ?? '';
    }
  } catch {
    // Blocked storage has nothing to export
  }
  saveText(JSON.stringify(backupOf(storage, await listDeviceDecks()), null, 2), 'forge-web-settings.json', 'application/json');
}

/** What an import did: restored the file, refused a file that is not a backup, or could not save all of it. */
export type ImportResult = 'done' | 'notBackup' | 'notSaved';

/** Writes a backup file over what the browser holds, then reloads the page to use it. A browser that could not save all of it is not reloaded, so the player sees why. */
export async function importBackup(text: string): Promise<ImportResult> {
  const backup = readBackup(text);
  if (!backup) return 'notBackup';
  try {
    for (const [key, value] of Object.entries(backup.storage)) localStorage.setItem(key, value);
  } catch {
    return 'notSaved';
  }
  let saved = true;
  for (const deck of backup.decks) saved = await putDeviceDeck(deck) && saved;
  if (!saved) return 'notSaved';
  location.reload();
  return 'done';
}
