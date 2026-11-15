// Settings marked server:true are Forge preferences shared with the desktop client. The rest are stored in this browser

import { SHORTCUT_PREFS, codesOfKey, sameShortcut, type KeyBindings } from './keys';
import type { ServerSettings } from './protocol';
import { storeJson, storedJson } from './storage';
import { t, type TextKey } from './text';

const LOCAL_KEY = 'forge.settings';
/** The server keeps a guest's settings only for the session. The browser stores them and sends them again on connecting. */
const GUEST_KEY = 'forge.guestSettings';

/** The playmats a player can show under the board. The last is the bare table, with no picture. */
export const PLAYMATS: { id: string; name: TextKey; image: string | null; thumb: string | null }[] = [
  { id: 'waves', name: 'lblWebPlaymatWaves', image: '/playmats/waves.jpg', thumb: '/playmats/waves-thumb.jpg' },
  { id: 'sunset', name: 'lblWebPlaymatSunset', image: '/playmats/sunset.jpg', thumb: '/playmats/sunset-thumb.jpg' },
  { id: 'azorius', name: 'lblWebPlaymatAzorius', image: '/playmats/azorius.jpg', thumb: '/playmats/azorius-thumb.jpg' },
  { id: 'boros', name: 'lblWebPlaymatBoros', image: '/playmats/boros.jpg', thumb: '/playmats/boros-thumb.jpg' },
  { id: 'dimir', name: 'lblWebPlaymatDimir', image: '/playmats/dimir.jpg', thumb: '/playmats/dimir-thumb.jpg' },
  { id: 'golgari', name: 'lblWebPlaymatGolgari', image: '/playmats/golgari.jpg', thumb: '/playmats/golgari-thumb.jpg' },
  { id: 'gruul', name: 'lblWebPlaymatGruul', image: '/playmats/gruul.jpg', thumb: '/playmats/gruul-thumb.jpg' },
  { id: 'izzet', name: 'lblWebPlaymatIzzet', image: '/playmats/izzet.jpg', thumb: '/playmats/izzet-thumb.jpg' },
  { id: 'orzhov', name: 'lblWebPlaymatOrzhov', image: '/playmats/orzhov.jpg', thumb: '/playmats/orzhov-thumb.jpg' },
  { id: 'rakdos', name: 'lblWebPlaymatRakdos', image: '/playmats/rakdos.jpg', thumb: '/playmats/rakdos-thumb.jpg' },
  { id: 'selesnya', name: 'lblWebPlaymatSelesnya', image: '/playmats/selesnya.jpg', thumb: '/playmats/selesnya-thumb.jpg' },
  { id: 'simic', name: 'lblWebPlaymatSimic', image: '/playmats/simic.jpg', thumb: '/playmats/simic-thumb.jpg' },
  { id: 'table', name: 'lblNone', image: null, thumb: null },
];
type SettingValue = string | number | boolean;

interface SettingBase {
  section: string;
  key: string;
  label: string;
  hint?: string;
  /** The game also reads it. Its preference is in Options and its value is in ServerSettings. */
  server?: boolean;
  /** Also set from the volume control beside the options button. */
  volume?: boolean;
  /** Only the host has it. A guest's browser leaves it out of the options. */
  hostOnly?: boolean;
  /** Also set in a dialog opened from the game menu. */
  menu?: 'stops' | 'decisions' | 'playmat';
  /** Set from another setting's control or from a dialog, so it has no row of its own in the options. */
  inside?: boolean;
  /** Listed in the options only while this returns true. */
  when?: () => boolean;
}

/** A choice shown as a picture. It has a value, a name, a hint line under the name, an SVG drawing, and a test for when it is unavailable. */
export interface Tile {
  value: string;
  label: string;
  hint: string;
  picture: string;
  unavailable?: () => boolean;
}

export type SettingDef = SettingBase & (
  | { type: 'toggle'; def: boolean }
  | { type: 'choice'; options: [string, string][]; def: string }
  | { type: 'tiles'; options: Tile[]; def: string }
  | { type: 'playmat'; def: string }
  /** Not a setting. It is a button that opens a game menu dialog. */
  | { type: 'dialog'; dialog: 'stops' | 'decisions'; def: string }
  | { type: 'slider'; min: number; max: number; def: number; step?: number; unit?: 'seconds' }
  | { type: 'css'; def: string }
  | { type: 'key'; action: keyof KeyBindings; def: string }
  /** One of Forge's shortcut preferences, which desktop also reads (shortcutSettings). */
  | { type: 'shortcut'; def: string }
);

const theirs = (x: number, y: number, width: number) => `<rect class="theirs" x="${x}" y="${y}" width="${width}" height="32" rx="3"/>`;
const YOURS = '<rect class="yours" x="4" y="44" width="96" height="32" rx="3"/>';
/** A table of five or more players always shows one opponent at a time. The other layouts cannot be picked during such a match. */
const tabsOnly = () => Number(document.querySelector<HTMLElement>('#match:not([hidden])')?.dataset.opponents ?? 0) > 3;

/** When a declined yield suggestion is offered again, with the same options as desktop. */
const SCOPES: [string, string][] = [['NEVER', t('lblNever')], ['ALWAYS', t('lblAlways')], ['STACK', t('lblDeclScopeStack')], ['TURN', t('lblDeclScopeTurn')]];

export const SETTINGS: SettingDef[] = [
  { section: t('lblInterruptSettings'), key: 'interruptAttackers', label: t('lblInterruptOnAttackers'), type: 'toggle', server: true, menu: 'stops', inside: true, def: true },
  { section: t('lblInterruptSettings'), key: 'interruptOpponentSpell', label: t('lblInterruptOnOpponentSpell'), type: 'toggle', server: true, menu: 'stops', inside: true, def: true },
  { section: t('lblInterruptSettings'), key: 'interruptTargeting', label: t('lblInterruptOnTargeting'), type: 'toggle', server: true, menu: 'stops', inside: true, def: false },
  { section: t('lblInterruptSettings'), key: 'interruptTriggers', label: t('lblInterruptOnTriggers'), type: 'toggle', server: true, menu: 'stops', inside: true, def: false },
  { section: t('lblInterruptSettings'), key: 'interruptMassRemoval', label: t('lblInterruptOnMassRemoval'), type: 'toggle', server: true, menu: 'stops', inside: true, def: false },
  { section: t('lblInterruptSettings'), key: 'interruptReveal', label: t('lblInterruptOnReveal'), type: 'toggle', server: true, menu: 'stops', inside: true, def: false },
  {
    section: t('lblAutomaticSuggestions'), key: 'suggestStackYield', label: t('lblSuggestStackYield'), type: 'choice', server: true, menu: 'stops', inside: true,
    options: SCOPES, def: 'NEVER',
  },
  {
    section: t('lblAutomaticSuggestions'), key: 'suggestNoActions', label: t('lblSuggestNoActions'), type: 'choice', server: true, menu: 'stops', inside: true,
    options: SCOPES, def: 'NEVER',
  },
  { section: t('lblAutomaticSuggestions'), key: 'suppressAfterYield', label: t('lblSuppressAfterYield'), type: 'toggle', server: true, menu: 'stops', inside: true, def: true },
  { section: t('lblAutomaticSuggestions'), key: 'suppressOwnTurn', label: t('lblSuppressOnOwnTurn'), type: 'toggle', server: true, menu: 'stops', inside: true, def: true },
  {
    section: t('lblDisplay'), key: 'playmat', label: t('lblWebPlaymat'), type: 'playmat', menu: 'playmat', def: 'waves',
  },
  {
    section: t('lblDisplay'), key: 'playmatBrightness', label: t('lblWebPlaymatBrightness'), type: 'choice', menu: 'playmat', inside: true,
    options: [['dark', t('lblWebPlaymatDark')], ['dim', t('lblWebPlaymatDim')], ['light', t('lblWebPlaymatLight')], ['bright', t('lblWebPlaymatBright')]], def: 'dim',
  },
  {
    section: t('lblDisplay'), key: 'boardLayout', label: t('lblMultiplayerFieldLayout'), type: 'tiles', def: 'columns',
    options: [
      { value: 'columns', label: t('lblColumns'), hint: t('lblWebOptionsColumnsHint'), unavailable: tabsOnly,
        picture: `${theirs(4, 4, 30)}${theirs(37, 4, 30)}${theirs(70, 4, 30)}${YOURS}` },
      { value: 'quadrants', label: t('lblWebOptionsQuadrants'), hint: t('lblWebOptionsQuadrantsHint'), unavailable: tabsOnly,
        picture: `${theirs(4, 4, 46)}${theirs(54, 4, 46)}<rect class="yours" x="4" y="44" width="46" height="32" rx="3"/>${theirs(54, 44, 46)}` },
      { value: 'tabs', label: t('lblWebOptionsTabs'), hint: t('lblWebOptionsTabsHint'),
        picture: `<rect class="theirs" x="4" y="12" width="96" height="24" rx="3"/><rect class="tab on" x="4" y="4" width="18" height="9" rx="2"/><rect class="tab" x="25" y="5" width="16" height="6" rx="2"/><rect class="tab" x="44" y="5" width="16" height="6" rx="2"/>${YOURS}` },
    ],
  },
  {
    section: t('lblDisplay'), key: 'motion', label: t('lblWebOptionsMotion'), hint: t('lblWebOptionsMotionHint'), type: 'choice',
    options: [['full', t('lblWebOptionsFull')], ['system', t('lblWebOptionsSystem')], ['reduced', t('lblWebOptionsReduced')]], def: 'full',
  },
  {
    section: t('lblCards'), key: 'previewSize', label: t('lblWebOptionsPreviewSize'), hint: t('lblWebOptionsPreviewSizeHint'), type: 'choice',
    options: [['small', t('lblWebOptionsSmall')], ['medium', t('lblMedium')], ['large', t('lblWebOptionsLarge')]], def: 'medium',
  },
  { section: t('lblCards'), key: 'handSize', label: t('lblHandSize'), type: 'slider', min: 70, max: 130, def: 100 },
  {
    section: t('lblCards'), key: 'handSort', label: t('lblWebOptionsHandSort'), type: 'choice',
    options: [['mana', t('lblCMC')], ['color', t('lblColor')], ['draw', t('lblWebOptionsDrawn')]], def: 'mana',
  },
  {
    section: t('lblCards'), key: 'draftRanking', label: t('lblShowDraftRankingOverlay'), hint: t('nlShowDraftRankingOverlay'),
    type: 'toggle', server: true, def: true,
  },
  {
    // Forge ships only the default set. As on desktop, the choice is shown only after someone adds another set
    section: t('lblWebOptionsSectionSound'), key: 'soundSet', label: t('cbpSoundSets'), type: 'choice', server: true, hostOnly: true,
    when: () => (server.soundSets?.length ?? 0) > 1,
    get options(): [string, string][] {
      return (server.soundSets ?? []).map(s => [s, s]);
    },
    def: 'Default',
  },
  {
    section: t('lblWebOptionsSectionSound'), key: 'soundVolume', label: t('lblWebOptionsSoundVolume'), type: 'slider', server: true, volume: true, min: 0, max: 100, def: 100,
  },
  {
    section: t('lblWebOptionsSectionSound'), key: 'musicVolume', label: t('lblWebOptionsMusicVolume'), type: 'slider', server: true, volume: true, min: 0, max: 100, def: 100,
  },
  {
    section: t('GamePlay'), key: 'pace', label: t('lblWebOptionsPace'), hint: t('lblWebOptionsPaceHint'),
    type: 'slider', min: 0, max: 3000, step: 250, unit: 'seconds', def: 1000,
  },
  {
    section: t('GamePlay'), key: 'logDetail', label: t('cbpGameLogEntryType'), type: 'choice', server: true,
    options: [['LOW', t('lblWebOptionsLow')], ['MEDIUM', t('lblMedium')], ['HIGH', t('lblWebOptionsHigh')]], def: 'MEDIUM',
  },
  { section: t('lblAutoYieldsAndTriggers'), key: 'yieldSettings', label: t('lblYieldSettings'), type: 'dialog', dialog: 'stops', def: '' },
  {
    // The host reads the saved list from the player's own game. A spectator or the menu has no list to show
    section: t('lblAutoYieldsAndTriggers'), key: 'autoDecisions', label: t('lblWebGameMenuDecisions'), type: 'dialog', dialog: 'decisions', def: '',
    when: () => !!document.querySelector('#match:not([hidden])') && !document.querySelector('#prompt.spectating'),
  },
  {
    section: t('lblAutoYieldsAndTriggers'), key: 'autoYieldMode', label: t('cbpAutoDecisionMode'), type: 'choice', server: true, menu: 'decisions',
    hint: t('lblWebOptionsAutoYieldModeHint'),
    options: [['ability', t('lblWebOptionsPerAbility')], ['card', t('lblWebOptionsPerCard')]], def: 'ability',
  },
  {
    section: t('lblWebOptionsSectionTable'), key: 'mulliganRule', label: t('cbpMulliganRule'), type: 'choice', server: true, hostOnly: true,
    get options(): [string, string][] {
      return (server.mulliganRules ?? []).map(r => [r, r]);
    },
    def: 'London',
  },
  {
    section: t('lblWebOptionsSectionTable'), key: 'aiProfile', label: t('cbpAiProfiles'), hint: t('lblWebOptionsAiProfileHint'),
    type: 'choice', server: true, hostOnly: true,
    get options(): [string, string][] {
      return (server.aiProfiles ?? []).map(p => [p, p]);
    },
    def: 'Default',
  },
  {
    section: t('lblWebOptionsSectionTable'), key: 'filteredHands', label: t('cbFilteredHands'), hint: t('lblWebOptionsFilteredHandsHint'),
    type: 'toggle', server: true, hostOnly: true, def: false,
  },
  {
    section: t('lblWebOptionsSectionTable'), key: 'ante', label: t('cbAnte'), type: 'toggle', server: true, hostOnly: true, def: false,
  },
  {
    section: t('lblWebOptionsSectionTable'), key: 'undoMode', label: t('cbUndoMode'), hint: t('lblWebOptionsUndoModeHint'), type: 'choice', server: true, hostOnly: true,
    options: ['Off', 'Single step', 'Free'].map(m => [m, m]), def: 'Free',
  },
  { section: t('lblKeyboardShortcuts'), key: 'keyOk', label: t('lblOK'), action: 'ok', type: 'key', def: ' ' },
  { section: t('lblKeyboardShortcuts'), key: 'keyNextFace', label: t('lblWebOptionsKeyNextFace'), hint: t('lblWebOptionsUnderPointer'), action: 'nextFace', type: 'key', def: 'f' },
  { section: t('lblKeyboardShortcuts'), key: 'keyCardText', label: t('lblWebOptionsKeyCardText'), hint: t('lblWebOptionsUnderPointer'), action: 'cardText', type: 'key', def: 't' },
  {
    section: t('lblAdvanced'), key: 'customCss', label: t('lblWebOptionsCustomCss'), hint: t('lblWebOptionsCustomCssHint'), type: 'css', def: '',
  },
  {
    section: t('lblAdvanced'), key: 'devMode', label: t('lblDevMode'), type: 'toggle', server: true, hostOnly: true, def: false,
    hint: t('lblWebOptionsDevModeHint'),
  },
];

const byKey = new Map(SETTINGS.map(s => [s.key, s]));
let local: Record<string, SettingValue> = {};
let server: Partial<ServerSettings> = {};
let saveOnServer: (key: string, value: string) => void = () => {};
let redraw: () => void = () => {};
/** The host's server settings are Forge preferences, which persist after the server stops. A guest's do not. */
let guest = false;

export function setGuest(value: boolean): void {
  guest = value;
}

export function isGuest(): boolean {
  return guest;
}

export function initSettings(save: (key: string, value: string) => void, schedule: () => void): void {
  saveOnServer = save;
  redraw = schedule;
  // A hand-edited backup can hold any JSON here. Anything but an object could not take new values
  const saved = storedJson<unknown>(LOCAL_KEY, {});
  local = saved && typeof saved === 'object' && !Array.isArray(saved) ? saved as Record<string, SettingValue> : {};
  apply();
}

export function setting(key: string): SettingValue {
  const def = byKey.get(key);
  if (!def) throw new Error(`No setting ${key}`);
  const value = def.server ? (server as Record<string, SettingValue | undefined>)[key] : stored(def, local[key]);
  return value === undefined ? def.def : value;
}

/** The browser may hold a value from an older version or an imported backup, such as a playmat that was removed. Such a value is ignored. */
export function stored(def: SettingDef, value: SettingValue | undefined): SettingValue | undefined {
  switch (def.type) {
    case 'toggle': return typeof value === 'boolean' ? value : undefined;
    case 'choice': return def.options.some(([id]) => id === value) ? value : undefined;
    case 'tiles': return def.options.some(o => o.value === value) ? value : undefined;
    case 'playmat': return PLAYMATS.some(m => m.id === value) ? value : undefined;
    case 'slider': return typeof value === 'number' && Number.isFinite(value) ? Math.min(def.max, Math.max(def.min, value)) : undefined;
    default: return typeof value === 'string' ? value : undefined;
  }
}

// The server sends its preference values on connecting and with the rest of the turn controls
export function onServerSettings(values: ServerSettings | undefined): void {
  server = values ?? {};
  apply();
}

/** Sends the server the settings this guest's browser stored. It runs before any game opens, so the game starts with them. */
export function restoreGuestSettings(): void {
  for (const [key, value] of Object.entries(guestSettings())) {
    if (byKey.get(key)?.server || key.startsWith('SHORTCUT_') || key === AUTO_PASS) {
      saveOnServer(key, String(value));
    }
  }
}

// The match's auto-pass button sets this server setting. It has no row in the options, so its value comes with the match's controls
const AUTO_PASS = 'autoPassNoActions';

/** Keeps a guest's auto-pass choice in its browser, as set() keeps its other server settings. */
export function rememberGuestAutoPass(on: boolean): void {
  if (guest && guestSettings()[AUTO_PASS] !== on) {
    rememberGuest(AUTO_PASS, on);
  }
}

function guestSettings(): Record<string, SettingValue> {
  return storedJson(GUEST_KEY, {});
}

function rememberGuest(key: string, value: SettingValue): void {
  storeJson(GUEST_KEY, { ...guestSettings(), [key]: value });
}

export function set(key: string, value: SettingValue): void {
  const def = byKey.get(key);
  if (!def) throw new Error(`No setting ${key}`);
  if (def.server) {
    (server as Record<string, SettingValue>)[key] = value;
    saveOnServer(key, String(value));
    if (guest) rememberGuest(key, value);
  } else {
    local[key] = value;
    storeJson(LOCAL_KEY, local);
  }
  apply();
  redraw();
}

const KEY_SETTINGS = SETTINGS.filter((d): d is SettingDef & { type: 'key' } => d.type === 'key');

export function boundKeys(): KeyBindings {
  return Object.fromEntries(KEY_SETTINGS.map(d => [d.action, String(setting(d.key))])) as unknown as KeyBindings;
}

/** Resets every key to its default, including the shortcut preferences. */
export function resetKeys(): void {
  for (const s of server.shortcuts ?? []) saveShortcut(s.pref, s.defaultKeys);
  setKeys(Object.fromEntries(KEY_SETTINGS.map(d => [d.action, d.def])) as unknown as KeyBindings);
}

/** Option rows for the shortcut preferences the web has an action for, each with Forge's name and default. */
export function shortcutSettings(): SettingDef[] {
  return (server.shortcuts ?? []).filter(s => SHORTCUT_PREFS.has(s.pref))
    .map(s => ({ section: t('lblKeyboardShortcuts'), key: s.pref, label: s.label, type: 'shortcut', server: true, def: s.defaultKeys }));
}

export function shortcutKeys(): Record<string, string> {
  return Object.fromEntries((server.shortcuts ?? []).map(s => [s.pref, s.keys]));
}

function saveShortcut(pref: string, codes: string): void {
  const s = server.shortcuts?.find(x => x.pref === pref);
  if (!s) return;
  s.keys = codes;
  saveOnServer(pref, codes);
  if (guest) rememberGuest(pref, codes);
}

/** Gives a shortcut preference its keys. Any other shortcut or page key with the same keys is cleared, so a key does one thing. */
export function setShortcut(pref: string, codes: string): void {
  if (codes) {
    for (const s of server.shortcuts ?? []) {
      if (s.pref !== pref && sameShortcut(s.keys, codes)) saveShortcut(s.pref, '');
    }
    const keys = boundKeys();
    for (const d of KEY_SETTINGS) {
      if (sameShortcut(codesOfKey(keys[d.action]), codes)) local[d.key] = '';
    }
    storeJson(LOCAL_KEY, local);
  }
  saveShortcut(pref, codes);
  redraw();
}

/** Saves every key binding at once, so a swap between two actions is saved as one change. */
export function setKeys(keys: KeyBindings): void {
  for (const d of KEY_SETTINGS) {
    local[d.key] = keys[d.action];
    // A desktop shortcut on the same key is cleared
    for (const s of server.shortcuts ?? []) {
      if (sameShortcut(s.keys, codesOfKey(keys[d.action]))) saveShortcut(s.pref, '');
    }
  }
  storeJson(LOCAL_KEY, local);
  redraw();
}

// Copies the hand and preview sizes, highlight colour, playmat, motion choice and custom CSS into CSS. Other settings are read where they are used
function apply(): void {
  const root = document.documentElement;
  // The highlight colour is a desktop preference with no control in the web options
  root.style.setProperty('--playable', server.highlightColor ?? '#66ccff');
  const hand = Number(setting('handSize')) / 100;
  root.style.setProperty('--hand-w', `${Math.round(88 * hand)}px`);
  root.style.setProperty('--hand-h', `${Math.round(123 * hand)}px`);
  root.dataset.preview = String(setting('previewSize'));
  const mat = PLAYMATS.find(m => m.id === setting('playmat'));
  root.style.setProperty('--playmat', mat?.image ? `url("${mat.image}")` : 'none');
  root.dataset.playmat = mat?.image ? String(setting('playmatBrightness')) : '';
  const motion = setting('motion');
  const reduced = motion === 'reduced' || (motion === 'system' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches);
  root.dataset.motion = reduced ? 'reduced' : 'full';
  customStyle().textContent = String(setting('customCss') ?? '');
}

function customStyle(): HTMLStyleElement {
  let style = document.getElementById('custom-css') as HTMLStyleElement | null;
  if (!style) {
    style = document.createElement('style');
    style.id = 'custom-css';
    document.head.append(style);
  }
  return style;
}
