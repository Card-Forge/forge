// Shortens a question to one line. The engine's prompt text also holds the source card's rules text, its id and the engine's own notes

import { deref, stateOf, type Model } from '../model';
import type { CardView, Ref } from '../protocol';

/** A question's text, split into the source card (when the text names one) and what it asks. sourceId is the source card's id. */
export interface Asked {
  source: string;
  sourceId?: number;
  ask: string;
}

// "Gravedigger (121) - When Gravedigger enters, ...": the source card, its id, and its rules text
const SOURCE = /^(.+?) \((\d+)\) - (.*)$/;
// A later step of the same effect names only the source and its id, without the rules text
const BARE_SOURCE = /^(.+?) \((\d+)\)$/;
// The engine adds its own notes in square brackets, such as the trigger an ability belongs to
const BRACKETED = /\s*\[[^\]]*\]/g;
const CARD_ID = / \(\d+\)/g;

/** Splits the engine's text into the source card's name and the instruction on its last line. Ids and bracketed notes are removed. */
export function splitQuestion(text: string): Asked {
  const lines = text.split('\n').map(line => line.trim());
  // The targets picked so far come after the instruction. They are dropped because the board already shows them
  const targeted = lines.findIndex(line => line.startsWith('Targeted:'));
  const kept = (targeted < 0 ? lines : lines.slice(0, targeted)).filter(line => line);
  if (!kept.length) return { source: '', ask: '' };
  // Some prompts break the line after the source's id, before the dash and its rules text
  if (kept.length > 1 && kept[1].startsWith('- ')) kept.splice(0, 2, `${kept[0]} ${kept[1]}`);
  const named = SOURCE.exec(kept[0]) ?? (kept.length > 1 ? BARE_SOURCE.exec(kept[0]) : null);
  // When the text names a source, only what it asks is kept. The stack and the prompt already show the source and its rules text
  const ask = (!named ? kept.join(' ') : kept.length > 1 ? kept[kept.length - 1] : named[3]).replace(BRACKETED, '').replace(CARD_ID, '').trim();
  return named ? { source: named[1], sourceId: Number(named[2]), ask } : { source: '', ask };
}

/** The name of the card a question comes from: the card the text gives the id of, else the card the request names, else the name in the text. */
export function sourceName(model: Model, asked: Asked, card: Ref | null | undefined): string {
  // A card's key is its id
  const from = (asked.sourceId === undefined ? undefined : model.objects.get(asked.sourceId)) ?? deref(model, card);
  return from ? stateOf(model, from as CardView).Name ?? asked.source : asked.source;
}
