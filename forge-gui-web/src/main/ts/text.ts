// All text the player reads comes through t() from Forge's language files, in Forge's selected language

import type { ComponentChild } from 'preact';
import type { TextKey } from './text.gen';

export type { TextKey };

type Arg = string | number;

let patterns: Record<string, string> | undefined;
let language = 'en-US';
let numbers = new Intl.NumberFormat('en-US');

/** Fetches the page's text from the server. Nothing may be drawn before it arrives. */
export async function loadText(): Promise<void> {
  const answer = await fetch('/text');
  const { lang, text } = await answer.json() as { lang: string; text: Record<string, string> };
  setText(lang, text);
  document.documentElement.lang = lang;
  for (const el of document.querySelectorAll<HTMLElement>('[data-text]')) {
    el.textContent = t(el.dataset.text as TextKey);
  }
}

/** Sets the page's text directly. The tests use this. */
export function setText(lang: string, text: Record<string, string>): void {
  patterns = text;
  language = lang;
  numbers = new Intl.NumberFormat(lang);
}

/** The language the page's text is in, as a BCP 47 tag, for sorting and dates. */
export function textLanguage(): string {
  return language;
}

/** A short date in Forge's language, such as "15 Oct". iso is yyyy-mm-dd, as the server sends a save's date. Without it, today is used. */
export function shortDay(iso?: string): string {
  const [y, m, d] = iso ? iso.split('-').map(Number) : [];
  return (iso ? new Date(y, m - 1, d) : new Date()).toLocaleDateString(language, { day: 'numeric', month: 'short' });
}

/** The text for a key, with {0}, {1}… filled from the arguments as Forge's Localizer fills them. */
export function t(key: TextKey, ...args: Arg[]): string {
  return pieces(key, args).join('');
}

/** Like t(), with the first letter in upper case, for a Forge key written in lower case to sit inside a sentence. */
export function tCap(key: TextKey, ...args: Arg[]): string {
  const text = t(key, ...args);
  return text.charAt(0).toLocaleUpperCase(language) + text.slice(1);
}

/** Like t(), but the arguments can be markup. An element can then go inside a sentence without splitting its words. */
export function tNodes(key: TextKey, ...args: ComponentChild[]): ComponentChild[] {
  return pieces(key, args);
}

function pieces<T>(key: TextKey, args: readonly T[]): (string | T)[] {
  if (!patterns) throw new Error(`The page's text is not loaded yet, so ${key} cannot be shown`);
  const pattern = patterns[key];
  return pattern === undefined ? [key] : format(pattern, args);
}

/** Splits a pattern at its arguments. It follows java.text.MessageFormat's quoting rules, which Forge's language files use. */
export function format<T>(pattern: string, args: readonly T[]): (string | T)[] {
  const out: (string | T)[] = [];
  let text = '';
  let quoted = false;
  for (let i = 0; i < pattern.length; i++) {
    const c = pattern[i];
    if (c === '\'') {
      if (pattern[i + 1] === '\'') {
        text += '\'';
        i++;
      } else {
        quoted = !quoted;
      }
    } else if (c === '{' && !quoted) {
      const end = pattern.indexOf('}', i);
      const arg = args[Number(pattern.slice(i + 1, end))];
      if (typeof arg === 'number') {
        text += numbers.format(arg);
      } else if (typeof arg === 'string' || arg === undefined || arg === null) {
        text += arg ?? '';
      } else {
        if (text) out.push(text);
        text = '';
        out.push(arg);
      }
      i = end;
    } else {
      text += c;
    }
  }
  if (text || !out.length) out.push(text);
  return out;
}

/** A number with its sign, as a card's modifiers are written: +2 or −1. */
export const signed = (n: number): string => (n < 0 ? `−${-n}` : `+${n}`);
