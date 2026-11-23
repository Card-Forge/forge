// The player's preferred printing of each card, stored in this browser. The catalogue shows it, and a card new to a deck is added in it

import type { Actions } from '../actions';
import type { DeckSection, EditorState } from '../protocol';
import { storeJson, storedJson } from '../storage';
import { redrawUi } from '../ui';

const KEY = 'forge.printings';
let byName: Record<string, string> | null = null;

const all = (): Record<string, string> => (byName ??= storedJson<Record<string, string>>(KEY, {}));

/** Whether this deck can use any printing of any card. A limited pool's deck or a collection deck cannot. */
export const choosesPrintings = (state: EditorState): boolean => !state.limited;

/** The image key of the printing preferred for a card, or undefined when none was chosen. */
export const preferredPrinting = (name: string): string | undefined => all()[name];

/** Stores a card's preferred printing, or clears it when key is null. The page is redrawn because the catalogue shows the printing. */
export function preferPrinting(name: string, key: string | null): void {
  const next = { ...all() };
  if (key === null) delete next[name];
  else next[name] = key;
  byName = next;
  storeJson(KEY, next);
  redrawUi();
}

/** Adds a card to the deck. A single copy of a card new to the deck is added in its preferred printing. A card the deck holds keeps the deck's printing. */
export function addCard(actions: Actions, state: EditorState, name: string, to: DeckSection, count = 1): void {
  const held = [...state.main.flatMap(g => g.cards), ...state.sideboard, ...state.commanders].some(card => card.name === name);
  const key = choosesPrintings(state) && !held && count === 1 ? preferredPrinting(name) : undefined;
  actions.edit(key ? { op: 'add', name, to, count, printings: [{ name: key, count: 1 }] } : { op: 'add', name, to, count });
}
