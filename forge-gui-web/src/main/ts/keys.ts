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
