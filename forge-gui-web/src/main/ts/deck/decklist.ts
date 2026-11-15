// Views of the editor's deck that the server does not send. The deck is regrouped, written out as a text list, or drawn as a sample hand

import type { EditorCard, EditorGroup, EditorState } from '../protocol';
import { COLOUR_NAMES } from '../symbols';
import { t, type TextKey } from '../text';

export type GroupBy = 'type' | 'mv' | 'colour';

// The colour headings in display order. M is for multicolour cards and C is for colourless cards
const COLOUR_ORDER = ['W', 'U', 'B', 'R', 'G', 'M', 'C'];
const COLOUR_HEADINGS: Record<string, TextKey> = { ...COLOUR_NAMES, M: 'lblMulticolor' };

/** The card types used to group and filter cards. Each entry holds the type-line word, the server's group id and the heading's text key. A card with two types goes under the first one listed. */
export const CARD_TYPES: [type: string, group: string, heading: TextKey][] = [['Creature', 'Creatures', 'lblCreatures'],
  ['Planeswalker', 'Planeswalkers', 'lblPlaneswalkers'], ['Instant', 'Instants', 'lblInstants'], ['Sorcery', 'Sorceries', 'lblSorceries'],
  ['Artifact', 'Artifacts', 'lblArtifacts'], ['Enchantment', 'Enchantments', 'lblEnchantments'], ['Battle', 'Battles', 'lblBattles'],
  ['Land', 'Lands', 'lblLands']];
const TYPE_HEADINGS: Record<string, TextKey> = Object.fromEntries(CARD_TYPES.map(([, group, heading]) => [group, heading]));

/** The display text of a group's heading. The server sends card type headings as English ids, so they are translated here. */
export const headingText = (heading: string): string => (TYPE_HEADINGS[heading] ? t(TYPE_HEADINGS[heading]) : heading);

/** Groups the main deck by type, mana value or colour. Lands always keep their own heading. */
export function regroup(state: EditorState, by: GroupBy): EditorGroup[] {
  if (by === 'type') return state.main;
  const lands = state.main.find(g => g.heading === 'Lands');
  const spells = state.main.filter(g => g !== lands).flatMap(g => g.cards);
  const groups = new Map<string, EditorCard[]>();
  for (const card of spells) {
    const heading = by === 'mv' ? mvGroup(card.mv) : colourLetter(card.colors);
    groups.set(heading, [...(groups.get(heading) ?? []), card]);
  }
  const order = by === 'mv'
    ? [...groups.keys()].sort((a, b) => parseInt(a, 10) - parseInt(b, 10))
    : COLOUR_ORDER.filter(h => groups.has(h));
  const out = order.map(key => ({ heading: by === 'mv' ? key : t(COLOUR_HEADINGS[key]), cards: groups.get(key) ?? [] }));
  return lands ? [...out, lands] : out;
}

/** The heading for a mana value. Mana values of seven and above share one heading. */
export const mvGroup = (mv: number): string => (mv >= 7 ? '7+' : String(mv));

function colourLetter(colors: string): string {
  if (colors.length > 1) return 'M';
  return colors.length === 1 && 'WUBRG'.includes(colors) ? colors : 'C';
}

export const colourHeading = (colors: string): string => t(COLOUR_HEADINGS[colourLetter(colors)]);

/** The deck as a text list that other sites and desktop Forge can read. Each section has a heading, then one line per card with a count and a name. */
export function deckText(state: EditorState): string {
  const sections: [string, EditorCard[]][] = [
    ['Commander', state.commanders],
    ['Deck', state.main.flatMap(g => g.cards)],
    ['Sideboard', state.sideboard],
  ];
  return sections.filter(([, cards]) => cards.length)
    .map(([title, cards]) => `${title}\n${cards.map(c => `${c.count} ${c.name}`).join('\n')}\n`)
    .join('\n');
}

/** Draws a hand from a shuffled copy of the main deck. Each copy of a card is shuffled separately. */
export function drawHand(state: EditorState, size: number, random: () => number = Math.random): EditorCard[] {
  const library = state.main.flatMap(g => g.cards).flatMap(c => Array.from({ length: c.count }, () => c));
  for (let i = library.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [library[i], library[j]] = [library[j], library[i]];
  }
  return library.slice(0, size);
}
