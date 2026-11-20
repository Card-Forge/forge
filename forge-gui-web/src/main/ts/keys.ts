// Decides what each key does, checking the top layer first, so a key does exactly one thing. Escape is the exception: a Preact dialog or menu closes on it by itself

import { cardMenu, cardPrompt, oldestRequest, stackPick, type Model } from './model';
import type { UiState } from './ui';

export type KeyCommand =
  | 'closeOptions' | 'closeConcede' | 'closeGameMenu' | 'closeVolume' | 'closeStackMenu' | 'closeStops' | 'closeZones' | 'closePicker' | 'declineHostChoice'
  | 'ok' | 'cancel' | 'nextFace' | 'cardText' | 'startMatch'
  | 'closeCardMenu' | `pickCardMenu${Digit}` | 'castWithoutCost' | 'castWithCost' | 'closeReveal' | 'confirmRequest' | 'foldRequest' | 'unfoldRequest' | 'editorUndo' | 'closeImporter' | 'closeBrowse' | 'back'
  | ShortcutCommand;

/** What each of Forge's shortcut preferences does on the web. Alpha strike is a prompt button, so it is handled with the prompt's keys. */
const SHORTCUT_COMMANDS = {
  SHORTCUT_SHOWSTACK: 'showStack', SHORTCUT_SHOWCONSOLE: 'showLog', SHORTCUT_SHOWDEV: 'showDev', SHORTCUT_UNDO: 'undo',
  SHORTCUT_CONCEDE: 'concede', SHORTCUT_ENDTURN: 'endTurn', SHORTCUT_AUTOYIELD_ALWAYS_YES: 'alwaysYes', SHORTCUT_AUTOYIELD_ALWAYS_NO: 'alwaysNo',
  SHORTCUT_YIELD_OPTIONS: 'yieldOptions', SHORTCUT_YIELD_AUTO_PASS: 'toggleAutoPass', SHORTCUT_MACRO_RECORD: 'macroRecord',
  SHORTCUT_MACRO_REPEAT_ACTIONS: 'macroRepeat', SHORTCUT_SHOWHOTKEYS: 'showShortcuts',
} as const;

type ShortcutCommand = typeof SHORTCUT_COMMANDS[keyof typeof SHORTCUT_COMMANDS];

/** The shortcut preferences the web has an action for. */
export const SHORTCUT_PREFS = new Set([...Object.keys(SHORTCUT_COMMANDS), 'SHORTCUT_ALPHASTRIKE']);

type Digit = '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9';

/** The keys a player can choose. Each is stored in the form keyCommand reads: a lower case letter, or ' ' for Space. */
export interface KeyBindings {
  ok: string;
  nextFace: string;
  cardText: string;
}

// Reserved keys. Escape closes and cancels everywhere. Enter confirms and starts a match. The numbers pick from a card's menu
const KEPT = new Set(['Escape', 'Enter', 'Tab', '1', '2', '3', '4', '5', '6', '7', '8', '9']);

/** Gives an action a new key. An action that had that key gets the old key instead, so no key triggers two actions. */
export function rebind(keys: KeyBindings, action: keyof KeyBindings, key: string): KeyBindings {
  const chosen = key.length === 1 ? key.toLowerCase() : key;
  if (KEPT.has(chosen)) return keys;
  const next = { ...keys, [action]: chosen };
  for (const other of Object.keys(keys) as (keyof KeyBindings)[]) {
    if (other !== action && keys[other] === chosen) next[other] = keys[action];
  }
  return next;
}

export function keyName(key: string): string {
  return key === ' ' ? 'Space' : key.length === 1 ? key.toUpperCase() : key;
}

/**
 * A desktop shortcut, parsed from its preference the way desktop parses it. The preference is Java key codes separated by
 * spaces. 16 is Shift, 17 is Ctrl, and the last other code is the key. Desktop ignores Alt. Null when no key is set.
 */
interface Shortcut {
  code: number;
  ctrl: boolean;
  shift: boolean;
}

export function parseShortcut(codes: string): Shortcut | null {
  const list = codes.split(' ').filter(Boolean).map(Number);
  const code = list.filter(c => c !== 16 && c !== 17).pop();
  return code ? { code, ctrl: list.includes(17), shift: list.includes(16) } : null;
}

/** Whether two shortcuts use the same keys, in any stored order. */
export function sameShortcut(a: string, b: string): boolean {
  const x = parseShortcut(a);
  const y = parseShortcut(b);
  return !!x && !!y && x.code === y.code && x.ctrl === y.ctrl && x.shift === y.shift;
}

/** The desktop key code for a page key that is a letter, a digit or Space. Other keys return an empty string. */
export function codesOfKey(key: string): string {
  if (key === ' ') return '32';
  return /^[a-z0-9]$/.test(key) ? String(key.toUpperCase().charCodeAt(0)) : '';
}

/** A shortcut's name as desktop shows it, with the key names joined by "+". */
export function shortcutName(codes: string): string {
  return codes.split(' ').filter(Boolean).map(Number).map(javaKeyText).join('+');
}

const JAVA_KEY_NAMES: Record<number, string> = {
  8: 'Backspace', 9: 'Tab', 10: 'Enter', 16: 'Shift', 17: 'Ctrl', 18: 'Alt', 27: 'Escape', 32: 'Space',
  37: 'Left', 38: 'Up', 39: 'Right', 40: 'Down', 127: 'Delete',
};

/** A Java key code's name. Letters, digits and function keys share their codes with the browser's keyCode. */
function javaKeyText(code: number): string {
  if ((code >= 48 && code <= 57) || (code >= 65 && code <= 90)) return String.fromCharCode(code);
  if (code >= 112 && code <= 123) return `F${code - 111}`;
  return JAVA_KEY_NAMES[code] ?? `#${code}`;
}

/** Converts a key press to preference codes while a shortcut is being chosen. Alt and Meta are left out, because Forge ignores Alt. */
export function shortcutCodes(e: { keyCode: number; ctrlKey: boolean; shiftKey: boolean }): string {
  return [...(e.ctrlKey ? [17] : []), ...(e.shiftKey ? [16] : []), e.keyCode].join(' ');
}

interface KeyPress {
  key: string;
  /** The browser's keyCode. It equals the Java key code for the keys desktop shortcuts use. */
  code: number;
  ctrl: boolean;
  shift: boolean;
  alt: boolean;
  meta: boolean;
  /** The key was typed into a field. The page then handles only Escape, to close the panel holding the field. */
  typing: boolean;
}

/** Whether a press matches a shortcut exactly, modifiers included, as Swing matches a key stroke. */
function pressed(press: KeyPress, shortcut: Shortcut | null): boolean {
  return !!shortcut && press.code === shortcut.code && press.ctrl === shortcut.ctrl && press.shift === shortcut.shift && !press.alt && !press.meta;
}

/** keys are the page's own bindings. shortcuts are Forge's shortcut preferences, which a match handles as desktop does. */
export function keyCommand(press: KeyPress, model: Model, ui: UiState, keys: KeyBindings, shortcuts: Record<string, string>): KeyCommand | null {
  const shortcut = model.screen === 'playing' ? Object.keys(shortcuts).find(pref => pressed(press, parseShortcut(shortcuts[pref]))) : undefined;
  const modified = press.ctrl || press.alt || press.meta;
  if (modified && !shortcut) {
    // The only other modified key the page handles is undo in the deck editor. The browser would otherwise do nothing with it
    const editing = !!model.editor && model.screen !== 'playing' && !ui.importer;
    return editing && !press.typing && press.key.toLowerCase() === 'z' ? 'editorUndo' : null;
  }
  // A key pressed with a modifier never matches a player's chosen key
  const key = modified ? '' : press.key.length === 1 ? press.key.toLowerCase() : press.key;
  const escape = key === 'Escape';
  const ok = (!!keys.ok && key === keys.ok) || key === 'Enter';
  // A question the host is waiting on is shown over every page. Escape declines it
  if (model.hostChoice) {
    return escape ? 'declineHostChoice' : null;
  }
  // The concede question blocks every other key. Escape closes it without conceding
  if (ui.concedeAsk) {
    return escape ? 'closeConcede' : null;
  }
  // The options can open on any page, and the yield dialog opened from them is drawn on top
  if (ui.gameMenu === 'stops' || (model.screen === 'playing' && ui.gameMenu)) {
    return escape ? 'closeGameMenu' : null;
  }
  if (ui.optionsOpen) {
    return escape ? 'closeOptions' : null;
  }
  if (model.screen !== 'playing') {
    if (ui.importer) {
      return escape ? 'closeImporter' : null;
    }
    if (ui.browse && !model.editor) {
      return escape ? 'closeBrowse' : null;
    }
    if (model.screen === 'setup' && ui.picker) {
      return escape ? 'closePicker' : null;
    }
    // Escape outside a match goes back one step of the page's trail, as its Back button does
    if (escape && !press.typing) {
      return 'back';
    }
    const table = model.lobby;
    return !press.typing && model.screen === 'setup' && key === 'Enter' && table?.host && table.canStart
      // With a deck above the table's bracket, only the Play button starts the match, because it asks first
      && !table.overBracket.length ? 'startMatch' : null;
  }
  if (ui.volumeOpen) {
    return escape ? 'closeVolume' : null;
  }
  if (ui.stackMenuAt) {
    return escape ? 'closeStackMenu' : null;
  }
  if (ui.stopsOpen) {
    return escape ? 'closeStops' : null;
  }
  // Escape closes an open zone window first, so the same press never also undoes the player's last action
  if (escape && ui.openZones.size) {
    return 'closeZones';
  }
  if (press.typing) {
    return null;
  }
  // Turning or reading the card under the pointer answers nothing, so it also works while a request is open
  if (keys.nextFace && key === keys.nextFace) {
    return 'nextFace';
  }
  if (keys.cardText && key === keys.cardText) {
    return 'cardText';
  }
  // In a card's ability menu, Escape closes it. Its items are numbered as on desktop
  const menu = cardMenu(model);
  if (menu) {
    if (escape) return 'closeCardMenu';
    return /^[1-9]$/.test(key) && Number(key) <= menu.options.length ? `pickCardMenu${key as Digit}` : null;
  }
  const request = oldestRequest(model);
  // Optional costs cannot be declined, so Escape does nothing, and a single cost's two items take 1 and 2
  if (request?.kind === 'costs') {
    return request.options.length !== 1 ? null : key === '1' ? 'castWithoutCost' : key === '2' ? 'castWithCost' : null;
  }
  if (request?.kind === 'reveal' && (escape || ok)) {
    return 'closeReveal';
  }
  // A request about cards is answered on the board. OK confirms it. Escape collapses it so the board is visible, and never declines it
  if (request && cardPrompt(request)) {
    if (ui.requestFolded === request.id) return ok ? 'unfoldRequest' : null;
    return ok ? 'confirmRequest' : escape ? 'foldRequest' : null;
  }
  // A request in a dialog is answered in the dialog. The prompt buttons under it get no keys
  const pick = stackPick(model);
  if ((request && !pick) || model.spectating) {
    return null;
  }
  // A pick from the stack is declined with the prompt's Cancel, if declining is allowed
  if (pick) {
    return escape && pick.min === 0 ? 'cancel' : null;
  }
  const prompt = model.prompt;
  if (shortcut === 'SHORTCUT_ALPHASTRIKE') return prompt?.cancel?.enabled && prompt.cancel.kind === 'alphaStrike' ? 'cancel' : null;
  if (ok && prompt?.ok?.enabled) return 'ok';
  // Escape means cancel, so it never triggers the alpha strike button
  if (escape && prompt?.cancel?.enabled && prompt.cancel.kind !== 'alphaStrike') return 'cancel';
  return shortcut && shortcut in SHORTCUT_COMMANDS ? SHORTCUT_COMMANDS[shortcut as keyof typeof SHORTCUT_COMMANDS] : null;
}
