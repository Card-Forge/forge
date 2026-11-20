// The name, avatar and sleeve this browser last played under, kept between visits

import { store, stored } from './storage';

const NAME_KEY = 'forge.playerName';
const AVATAR_KEY = 'forge.avatar';
const SLEEVE_KEY = 'forge.sleeve';

/** The name this browser last played under. It is sent to a server that does not know this browser yet. */
export function rememberedName(): string | null {
  return stored(NAME_KEY);
}

export function rememberName(name: string | null): void {
  store(NAME_KEY, name || null);
}

/** The avatar this browser last played under. The default is the first avatar. */
export function rememberedAvatar(): number {
  const saved = Number(stored(AVATAR_KEY));
  return Number.isInteger(saved) && saved >= 0 ? saved : 0;
}

export function rememberAvatar(index: number): void {
  store(AVATAR_KEY, String(index));
}

/** The sleeve this browser last chose for its own seat, if it chose one. A guest's seat gets it back at each table. */
export function rememberedSleeve(): number | undefined {
  const saved = stored(SLEEVE_KEY);
  const index = Number(saved);
  return saved !== null && Number.isInteger(index) && index >= 0 ? index : undefined;
}

export function rememberSleeve(index: number): void {
  store(SLEEVE_KEY, String(index));
}
