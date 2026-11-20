// Ability-word conditions and delve, counted from what the page already has: the player's zones, life and the cards' types and power

import { deref, stateOf, zone, type Model } from '../model';
import type { CardView, PlayerView } from '../protocol';

/** The card types that rule 205.2a lists, in its order. Delirium and similar cards count how many a graveyard has. */
const CORE_TYPES = ['Artifact', 'Battle', 'Creature', 'Enchantment', 'Instant', 'Kindred', 'Land', 'Planeswalker', 'Sorcery'];
const PERMANENT_TYPES = ['Artifact', 'Battle', 'Creature', 'Enchantment', 'Land', 'Planeswalker'];

/** The card types in these type lines, in the rules' order. The desktop graveyard label counts them the same way. */
export function cardTypes(typeLines: readonly string[]): string[] {
  const found = new Set<string>();
  for (const line of typeLines) {
    // Only the words before the dash are types. Subtypes such as "Forest" come after it
    for (const word of line.split(/\s[-—]\s/)[0].split(/\s+/)) {
      if (CORE_TYPES.includes(word)) found.add(word);
    }
  }
  return CORE_TYPES.filter(t => found.has(t));
}

/** How far a condition is from being met. `have` counts up to `need`, except for an `upTo` condition, which is met at `need` or less. */
export interface Progress {
  word: string;
  have: number;
  need: number;
  met: boolean;
  upTo?: 'hand' | 'life';
  /** For delve, the cost with the graveyard exiled, and the cards in the graveyard. */
  cost?: string;
  pool?: number;
}

type Count = (p: Counts) => number;

interface Counts {
  /** The type line of each card in the graveyard. */
  graveyard: string[];
  battlefield: CardView[];
  model: Model;
}

const creatures = (c: Counts) => c.battlefield.filter(card => /Creature/.test(stateOf(c.model, card).Type ?? ''));
const powers = (c: Counts) => creatures(c).map(card => stateOf(c.model, card).Power ?? 0);
const isType = (line: string, list: string[]) => cardTypes([line]).some(t => list.includes(t));

// Each ability word's count and the number it needs
const WORDS: Record<string, [Count, number]> = {
  'Threshold': [c => c.graveyard.length, 7],
  'Delirium': [c => cardTypes(c.graveyard).length, 4],
  'Spell mastery': [c => c.graveyard.filter(t => isType(t, ['Instant', 'Sorcery'])).length, 2],
  'Descend 4': [c => c.graveyard.filter(t => isType(t, PERMANENT_TYPES)).length, 4],
  'Descend 8': [c => c.graveyard.filter(t => isType(t, PERMANENT_TYPES)).length, 8],
  'Metalcraft': [c => c.battlefield.filter(card => /Artifact/.test(stateOf(c.model, card).Type ?? '')).length, 3],
  'Ferocious': [c => Math.max(0, ...powers(c)), 4],
  'Formidable': [c => powers(c).reduce((a, b) => a + Math.max(0, b), 0), 8],
  'Coven': [c => new Set(powers(c)).size, 3],
};
const ABILITY_WORD = new RegExp(`^(${[...Object.keys(WORDS), 'Hellbent', 'Fateful hour'].join('|')}) — `, 'gm');

/** The conditions on a card that the page can count, for the player who controls it. */
export function conditionsOf(model: Model, card: CardView): Progress[] {
  const state = stateOf(model, card);
  const player = deref(model, card.Controller ?? card.Owner) as PlayerView | undefined;
  if (!player || card.Facedown) return [];
  const counts: Counts = {
    graveyard: zone(model, player, 'Graveyard').map(c => stateOf(model, c).Type ?? ''),
    battlefield: zone(model, player, 'Battlefield'),
    model,
  };
  const found: Progress[] = [];
  for (const [, word] of (state.OracleText ?? '').matchAll(ABILITY_WORD)) {
    if (found.some(p => p.word === word)) continue;
    if (word === 'Hellbent') {
      // The card itself leaves the hand when it is cast, so it does not count
      const hand = zone(model, player, 'Hand').filter(c => c.$key !== card.$key).length;
      found.push({ word, have: hand, need: 0, met: hand === 0, upTo: 'hand' });
    } else if (word === 'Fateful hour') {
      const life = player.Life ?? 0;
      found.push({ word, have: life, need: 5, met: life <= 5, upTo: 'life' });
    } else {
      const [count, need] = WORDS[word];
      const have = count(counts);
      found.push({ word, have, need, met: have >= need });
    }
  }
  const delve = state.Keywords?.find(k => k.keyword === 'DELVE');
  const generic = /^\{(\d+)\}/.exec(state.ManaCost ?? '');
  if (delve && generic) {
    const need = Number(generic[1]);
    const have = Math.min(counts.graveyard.length, need);
    const rest = (state.ManaCost ?? '').slice(generic[0].length);
    found.push({ word: delve.title, have, need, met: have === need, pool: counts.graveyard.length, cost: need - have || !rest ? `{${need - have}}${rest}` : rest });
  }
  return found;
}
