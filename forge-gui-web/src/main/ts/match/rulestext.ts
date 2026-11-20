// Sorts the host's single block of rules text into the parts of a printed card. A line that is not recognised stays a line of text

import { t } from '../text';
import type { KeywordText } from '../protocol';

/** A run of text. `muted` marks text that does not apply right now. `struck` marks a word that a text-changing effect replaced. */
interface Run {
  text: string;
  muted: boolean;
  struck?: boolean;
}

export type Line = Run[];

export type Block =
  /** Keywords as chips, with the reminder text any of them carries. */
  | { kind: 'keywords'; items: Line[]; reminders: Line[] }
  /** An activated or loyalty ability, with its cost in a column and then its effect. `label` is an ability word or a name. */
  | { kind: 'ability'; cost: Line; label?: Line; effect: Line; loyalty?: 'plus' | 'minus' | 'zero' }
  /** One of a modal spell's modes. */
  | { kind: 'mode'; text: Line }
  /** A paragraph. `label` names the other half of the card that it describes, such as its Adventure. */
  | { kind: 'text'; text: Line; label?: Line };

/** A change the game made to the card that fits in a chip, such as counters, damage or shields. */
interface Chip {
  kind: 'counter' | 'damage' | 'shield' | 'status';
  text: string;
}

export interface SortedText {
  /** Token, Emblem and similar tags. They say what the object is when it is not a card. */
  tag?: string;
  blocks: Block[];
  chips: Chip[];
  /** The game's other notes on the card: what it is attached to, what was chosen for it, who owns it. */
  notes: Line[];
}

export const plain = (line: Line): string => line.map(r => r.text).join('');

/** The characters from..to of a line, keeping how each run is marked. */
export function slice(line: Line, from: number, to = Infinity): Line {
  const out: Line = [];
  let at = 0;
  for (const run of line) {
    const start = Math.max(from, at), end = Math.min(to, at + run.text.length);
    if (end > start) out.push({ ...run, text: run.text.slice(start - at, end - at) });
    at += run.text.length;
  }
  return out;
}

function trim(line: Line): Line {
  const text = plain(line);
  const start = text.length - text.trimStart().length;
  return slice(line, start, text.trimEnd().length);
}

const TAGS = /^(Token|Token card|Emblem|Boon|Effect)$/;

// Counters that already show in the card's corner (loyalty, defence) are left out of the chips
const CORNER_COUNTERS = /^(loyalty|defense)$/i;

/** A chip for a line of game state, 'skip' for a counter the corner already shows, or null when the line is something else. */
function chipFor(text: string): Chip | null | 'skip' {
  let m = /^(.+) counters: (-?\d+)$/.exec(text);
  if (m) return CORNER_COUNTERS.test(m[1]) ? 'skip' : { kind: 'counter', text: `${m[1]} ×${m[2]}` };
  if ((m = /^Damage: (\d+)$/.exec(text))) return { kind: 'damage', text: t('lblWebDetailDamageChip', m[1]) };
  if ((m = /^Assigned Damage: (\d+)$/.exec(text))) return { kind: 'damage', text: t('lblWebDetailAssignedChip', m[1]) };
  if ((m = /^Regeneration Shields: (\d+)$/.exec(text))) return { kind: 'shield', text: t(m[1] === '1' ? 'lblWebDetailRegenShield' : 'lblWebDetailRegenShields', m[1]) };
  if (text === 'Phased Out') return { kind: 'status', text: t('lblPhasedOut') };
  if (text === '^Exerted^') return { kind: 'status', text: t('lblExerted') };
  if (text === '^Detained^') return { kind: 'status', text: t('lblWebDetailDetained') };
  return null;
}

// The formats CardDetailUtil uses for the other notes on a card. Some are wrapped in marks that desktop colours them by. The marks are dropped
const NOTE_PREFIXES = [
  'Text changed:', 'Intensity:', 'Prevent the next ', 'Draft Notes:', 'Sector:', 'Sprocket:', 'Protected by:', 'Imprinting:',
  'Exiled:', 'Exiled until this leaves the battlefield:', 'Haunted by:', 'Haunting ', 'Encoded:', 'Must block ',
  'Current Card Colors:', 'Current Storm Count:', 'Owner:',
];
const NOTE_ASIDES = /^\((chosen|noted type|named card|stored dice|in room|class level|ring level)/i;
// These two notes start with a translated label
const TRANSLATED_ASIDES = ['lblSelected', 'lblChosenColors'] as const;
// The labels of the card's other half. The host writes each one translated, then " — "
const HALVES = ['lblAdventure', 'lblOmen', 'lblVengeance', 'lblPrepared'] as const;
const MARKED = /^([=*+^])(.*)\1$/;

function noteFor(line: Line): Line | null {
  const text = plain(line);
  const marked = MARKED.exec(text);
  if (marked && /^(Attached|Enchanting|Controlling|Cloned via)\b/.test(marked[2])) return slice(line, 1, text.length - 1);
  if (NOTE_ASIDES.test(text) || TRANSLATED_ASIDES.some(k => text.startsWith(`(${t(k)}`))) return slice(line, 1, text.endsWith(')') ? text.length - 1 : text.length);
  return NOTE_PREFIXES.some(p => text.startsWith(p)) ? line : null;
}

// A loyalty ability's cost, such as +1, −2, 0 or −X. Forge writes a true minus sign, but a hyphen is accepted too
const LOYALTY = /^([+−–-](?:\d+|X)|0): /;

// The words that a cost without a mana or tap symbol starts with
const COST_VERBS = /^(Sacrifice|Discard|Pay|Exile|Remove|Tap|Untap|Return|Put|Reveal|Collect|Forage|Mill)\b/;

/** Where an activated ability's cost ends, or -1 when the line is not one. */
function costEnd(text: string): number {
  const at = text.indexOf(': ');
  if (at <= 0 || at > 60) return -1;
  const cost = text.slice(0, at);
  if (/[.]/.test(cost)) return -1;
  const bare = cost.includes(' — ') ? cost.slice(cost.lastIndexOf(' — ') + 3) : cost;
  return /\{[^}]+\}/.test(bare) || COST_VERBS.test(bare) ? at : -1;
}

/** Where a line of comma-separated keywords ends, before any reminder text in brackets, or -1 when the line is not one. */
function keywordsEnd(text: string): number {
  const reminder = /\s*\((?:[^()]|\([^()]*\))*\)$/.exec(text);
  const head = reminder ? text.slice(0, reminder.index) : text;
  if (!head || head.length > 60 || /[.:;—]/.test(head) || !/^[A-Z]/.test(head)) return -1;
  const parts = head.split(/,\s*/);
  // Only names, numbers and costs are allowed. An apostrophe or a bracket means a sentence or a name, such as "Alice's Commander"
  return parts.every(p => p.length <= 32 && /^[A-Za-z][A-Za-z0-9 {}/+-]*$/.test(p)) ? head.length : -1;
}

/** Splits a line of keywords at its commas, keeping any cost with its keyword. */
function keywordItems(line: Line, end: number): Line[] {
  const text = plain(line).slice(0, end);
  const items: Line[] = [];
  let start = 0;
  for (const m of text.matchAll(/,\s*/g)) {
    items.push(slice(line, start, m.index));
    start = m.index + m[0].length;
  }
  items.push(slice(line, start, end));
  return items;
}

/** The reminder text after a line's keywords, without its brackets. */
function reminderOf(line: Line, end: number): Line | null {
  const text = plain(line);
  const open = text.indexOf('(', end);
  return open < 0 ? null : slice(line, open + 1, text.endsWith(')') ? text.length - 1 : text.length);
}

/** How a card's rules differ from its printed text, for the preview to show. */
export interface Changes {
  /** Keywords the card has that are not printed on it, in lower case. */
  gained: Set<string>;
  /** Printed keywords the card no longer has. */
  lostKeywords: string[];
  /** Printed paragraphs that are no longer among the card's rules. */
  lostText: string[];
}

// Normalises rules text for comparison. It removes reminder text, tags and a final full stop, leaves one space between words and lower-cases it
const normal = (text: string) => text.replace(/<[^>]*>/g, '').replace(/\((?:[^()]|\([^()]*\))*\)/g, '')
  .replace(/\s+/g, ' ').trim().replace(/\.$/, '').toLowerCase();
const paragraphs = (text: string) => text.split(/\r?\n/).map(normal).filter(Boolean);

/** Compares the printed rules with the current rules. The swaps are applied to the printed text first, so a rewritten paragraph is not counted as lost. */
export function changesOf(printed: string, now: string, swaps: Readonly<Record<string, string>>,
  keywords: readonly KeywordText[]): Changes {
  let swapped = printed;
  for (const [from, to] of Object.entries(swaps)) {
    swapped = swapped.replace(new RegExp(`\\b${from.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'gi'), to);
  }
  const printedAll = paragraphs(printed).join(' ');
  const nowParagraphs = paragraphs(now.replace(/<(strike|s|del)>.*?<\/\1>/gi, ''));
  const nowAll = nowParagraphs.join(' ');
  const titles = keywords.map(k => k.title.toLowerCase());
  const changes: Changes = { gained: new Set(titles.filter(k => !printedAll.includes(k))), lostKeywords: [], lostText: [] };
  for (const raw of swapped.split(/\r?\n/)) {
    const p = normal(raw);
    if (!p || nowAll.includes(p)) continue;
    const head = raw.replace(/\((?:[^()]|\([^()]*\))*\)/g, '').trim().replace(/\.$/, '');
    if (keywordsEnd(head) >= 0) {
      for (const item of head.split(/,\s*/)) {
        if (!titles.includes(item.toLowerCase()) && !nowParagraphs.includes(item.toLowerCase())) changes.lostKeywords.push(item.charAt(0).toUpperCase() + item.slice(1));
      }
    } else {
      changes.lostText.push(raw.trim());
    }
  }
  return changes;
}

/** Sorts the rules text. The host writes no reminder text for evergreen keywords, so a keyword line without any takes it from `keywords`. */
export function sortRulesText(lines: Line[], keywords: readonly KeywordText[] = []): SortedText {
  const sorted: SortedText = { blocks: [], chips: [], notes: [] };
  let first = true;
  for (const raw of lines) {
    const line = trim(raw);
    const text = plain(line);
    if (!text) continue;
    if (first && TAGS.test(text)) {
      sorted.tag = text;
      first = false;
      continue;
    }
    first = false;
    const chip = chipFor(text);
    if (chip === 'skip') continue;
    if (chip) {
      sorted.chips.push(chip);
      continue;
    }
    const note = noteFor(line);
    if (note) {
      sorted.notes.push(note);
      continue;
    }
    if (/^[•●]\s*/.test(text)) {
      sorted.blocks.push({ kind: 'mode', text: slice(line, (/^[•●]\s*/.exec(text) as RegExpExecArray)[0].length) });
      continue;
    }
    const loyalty = LOYALTY.exec(text);
    if (loyalty) {
      const cost = loyalty[1];
      sorted.blocks.push({ kind: 'ability', cost: slice(line, 0, cost.length), effect: slice(line, loyalty[0].length),
        loyalty: cost.startsWith('+') ? 'plus' : cost === '0' ? 'zero' : 'minus' });
      continue;
    }
    const names = keywordsEnd(text);
    if (names >= 0) {
      const last = sorted.blocks[sorted.blocks.length - 1];
      const block = last?.kind === 'keywords' ? last : { kind: 'keywords' as const, items: [], reminders: [] };
      if (block !== last) sorted.blocks.push(block);
      const items = keywordItems(line, names);
      block.items.push(...items);
      const reminder = reminderOf(line, names);
      if (reminder) {
        block.reminders.push(reminder);
        continue;
      }
      for (const item of items) {
        const known = keywords.find(k => k.title.toLowerCase() === plain(item).toLowerCase())?.reminder;
        if (known) block.reminders.push([{ text: known, muted: item.every(r => r.muted) }]);
      }
      continue;
    }
    // The spell on the other half of the card. Its cost is a casting cost, not an activation cost
    const half = HALVES.map(k => t(k)).find(label => text.startsWith(`${label} — `));
    if (half !== undefined) {
      sorted.blocks.push({ kind: 'text', label: slice(line, 0, half.length), text: slice(line, half.length + 3) });
      continue;
    }
    const cost = costEnd(text);
    if (cost >= 0) {
      const word = text.slice(0, cost).lastIndexOf(' — ');
      sorted.blocks.push({
        kind: 'ability',
        label: word >= 0 ? slice(line, 0, word) : undefined,
        cost: slice(line, word >= 0 ? word + 3 : 0, cost),
        effect: slice(line, cost + 2),
      });
      continue;
    }
    sorted.blocks.push({ kind: 'text', text: line });
  }
  return sorted;
}
