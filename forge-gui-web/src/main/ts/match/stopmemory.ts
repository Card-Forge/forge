// Stores a guest's phase stops in its browser. The server keeps them only for the length of the guest's session

import type { Controls, PhaseType } from '../protocol';
import { storeJson, storedJson } from '../storage';

const KEY = 'forge.guestStops';

export function restoreStops(setStops: (mine: boolean, phases: PhaseType[]) => void): void {
  const saved = storedJson<{ mine: PhaseType[]; others: PhaseType[] } | null>(KEY, null);
  if (saved) {
    setStops(true, saved.mine);
    setStops(false, saved.others);
  }
}

/** Stores a guest's stops as the server has them. The host's stops are saved in Forge's preferences, which persist after the server stops. */
export function rememberStops(controls: Controls): void {
  storeJson(KEY, { mine: controls.myStops, others: controls.otherStops });
}
