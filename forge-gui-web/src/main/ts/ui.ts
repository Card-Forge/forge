// Page layout state that stays in this browser and is never sent to the server. The match table state is cleared by each new match (resetMatchUi).
// The menu pages' tabs and open panels are kept across matches

import { storeJson, storedJson } from './storage';
import { reducedMotion } from './dom';

/** The card or player under the pointer. src is empty when the viewer may not see the card. at is the hovered element. */
type Hover = { card: number | null; src: string; from?: string; at?: HTMLElement } | { player: number };

/** A picker open over match setup, for one seat. */
export interface Picker {
  kind: 'deck' | 'sleeve' | 'avatar' | 'planes' | 'schemes' | 'vanguard' | 'commander';
  seat: number;
}

/** How an open zone is sorted: in zone order, by name, by card type, or by mana value. */
export type ZoneSort = 'order' | 'name' | 'type' | 'mana';

/** resetMatchUi clears the fields that belong to one match. main.ts clears the fields used by only one layout when the layout changes. */
export interface UiState {
  /** Battlefield piles the player has expanded to show each card, keyed by the pile's signature. */
  openPiles: Set<string>;
  /** Zone panels the player has opened by clicking a tile, as `${playerKey}/${zone}`. */
  openZones: Set<string>;
  /** The open zones are collapsed to a bar so the board is visible. They are still open. */
  zonesMinimised: boolean;
  /** The id of the request collapsed to a bar so the board is visible. It is still open. */
  requestFolded: number | null;
  /** Where the player last clicked a card, where a spell's optional costs then open. */
  cardClickedAt: { x: number; y: number } | null;
  /** Filters the open zone to cards whose name contains this text, in lower case. */
  zoneSearch: string;
  zoneSort: ZoneSort;
  /** Of several zones the game shows for one question, the one whose cards are drawn, as `${playerKey}/${zone}`. Null draws the first with a card to choose. */
  zoneChoice: string | null;
  stackCollapsed: boolean;
  /** The stack item under the pointer. Arrows are drawn to its targets. */
  hoveredStackItem: number | null;
  /** The position of a right-click on a stack item. It is kept until the server answers and the menu opens there. */
  stackMenuAt: { key: number; x: number; y: number } | null;
  stopsOpen: boolean;
  optionsOpen: boolean;
  /** The concede confirmation is open, from the game menu or its shortcut. */
  concedeAsk: boolean;
  /** The game menu behind the prompt's ⋯ button, or a dialog opened from it. */
  gameMenu: 'menu' | 'stops' | 'decisions' | 'dev' | 'devSetup' | null;
  /** The host stores the macro but does not report its state. The page tracks whether one is being recorded or is ready. */
  macro: 'none' | 'recording' | 'recorded';
  /** The volume control beside the options button is open. */
  volumeOpen: boolean;
  /** The menu shows the name and avatar form, opened by the dock's edit button. */
  renaming: boolean;
  /** The play choice whose game kinds the start page shows. It is kept so that going back from a table returns there. */
  menuChoice: 'play' | 'friends' | null;
  picker: Picker | null;
  /** The deck finder opened from the start page with no seat, and the format it lists. A hello does not close it. */
  browse: { format: string } | null;
  /** The open importer, and where it was opened from: a seat's deck finder, the start page, or the editor. */
  importer: { from: 'seat' | 'start' | 'editor'; seat?: number; text?: string; url?: string; sync?: boolean } | null;
  /** The host has chosen to watch the computer play its seat. */
  spectate: boolean;
  hover: Hover | null;
  /** Which face of the hovered card the zoom panel shows. */
  faceIndex: number;
  /** The zoom panel shows the hovered card's rules text instead of its image. Its key, T by default, toggles this. */
  cardText: boolean;
  /** Which side column panels are open. Stored in the browser across sessions. */
  sidePanels: { log: boolean; chat: boolean };
  draftHidden: boolean;
  /** The hovered card or player is pinned open as a sheet, after a long press on a phone. */
  inspect: boolean;
  /** The hand is open as a drawer over the board, on a phone. */
  handOpen: boolean;
  /** For layouts that show one opponent at a time. These hold the shown seat, the turn the player last chose one, and whether the phone's seat list is open. */
  openSeat: number | null;
  seatChosenTurn: number;
  seatsOpen: boolean;
  /** The permanents each opponent had when their seat was last shown, keyed by player. */
  seenOnField: Map<number, Set<number>>;
  /** On a phone, the player whose zones are opened from their bar. The bar itself shows only the counts. */
  zonesFor: number | null;
  /** The phone's menu sheet, and which of its tabs is shown. */
  menuSheet: 'log' | 'chat' | 'players' | null;
}

const SIDE_KEY = 'forge.sidePanels';

export const ui: UiState = {
  openPiles: new Set(),
  openZones: new Set(),
  zonesMinimised: false,
  requestFolded: null,
  cardClickedAt: null,
  zoneSearch: '',
  zoneSort: 'order',
  zoneChoice: null,
  stackCollapsed: false,
  hoveredStackItem: null,
  stackMenuAt: null,
  stopsOpen: false,
  optionsOpen: false,
  concedeAsk: false,
  gameMenu: null,
  macro: 'none',
  volumeOpen: false,
  renaming: false,
  menuChoice: null,
  picker: null,
  browse: null,
  importer: null,
  spectate: false,
  hover: null,
  faceIndex: 0,
  cardText: false,
  sidePanels: { log: true, chat: true, ...storedSidePanels() },
  draftHidden: false,
  inspect: false,
  handOpen: false,
  openSeat: null,
  zonesFor: null,
  seatChosenTurn: 0,
  seatsOpen: false,
  seenOnField: new Map(),
  menuSheet: null,
} as UiState;

let redraw: () => void = () => {};
let drawNow: () => void = () => {};

export function initUi(schedule: () => void, now: () => void): void {
  redraw = schedule;
  drawNow = now;
}

/** Changes the UI state and draws it at once. A view transition shows no frames until the new state is drawn. */
function changeUiNow(change: (state: UiState) => void): void {
  change(ui);
  drawNow();
}

/** How long a transition waits for the new page's data before it animates anyway. */
const READY_WAIT_MS = 500;

/**
 * Changes the UI state inside a view transition. It animates what CSS cannot, such as a change of grid areas or of page.
 * `mark` returns the attributes set on the root while the transition runs. CSS uses them to pick the animation.
 * `ready` says whether the new page has its data. The old page stays until then, so a placeholder never slides in.
 */
export function eased(change: (state: UiState) => void, mark: () => Record<string, string> = () => ({}), ready?: () => boolean): void {
  transition(async () => {
    changeUiNow(change);
    const until = performance.now() + READY_WAIT_MS;
    // The browser holds frames while a transition prepares, so data that arrives meanwhile is drawn here
    while (ready && !ready() && performance.now() < until) {
      await new Promise(next => setTimeout(next, 16));
      drawNow();
    }
  }, () => changeUi(change), mark);
}

/** Like eased, for a change to a component's own state. Preact draws that change shortly after it is made, not at once. */
export function easedLocal(change: () => void, mark: () => Record<string, string>): void {
  transition(async () => {
    change();
    await new Promise(drawn => setTimeout(drawn));
  }, change, mark);
}

/** Runs update inside a view transition with mark's attributes on the root, or runs still when motion is off or unsupported. */
export function transition(update: () => void | Promise<void>, still: () => void, mark: () => Record<string, string>): void {
  if (!document.startViewTransition || reducedMotion()) {
    still();
    return;
  }
  const root = document.documentElement;
  let marks: Record<string, string> = {};
  const running = document.startViewTransition(async () => {
    await update();
    marks = mark();
    Object.assign(root.dataset, marks);
  });
  running.finished.finally(() => { for (const key of Object.keys(marks)) delete root.dataset[key]; });
}

export function changeUi(change: (state: UiState) => void): void {
  change(ui);
  redraw();
}

/** Schedules a redraw for a change outside ui, such as a stored preference or an image that failed to load. */
export function redrawUi(): void {
  redraw();
}

/** Clears the match state for a new match, because its cards are gone. The side column panels and menu state are kept. */
export function resetMatchUi(): void {
  ui.openPiles.clear();
  ui.openZones.clear();
  ui.zonesMinimised = false;
  ui.requestFolded = null;
  ui.cardClickedAt = null;
  ui.zoneSearch = '';
  ui.zoneSort = 'order';
  ui.zoneChoice = null;
  ui.hoveredStackItem = null;
  ui.stackMenuAt = null;
  ui.stopsOpen = false;
  ui.optionsOpen = false;
  ui.concedeAsk = false;
  ui.gameMenu = null;
  ui.macro = 'none';
  ui.volumeOpen = false;
  ui.menuSheet = null;
  ui.picker = null;
  ui.hover = null;
  ui.inspect = false;
  ui.handOpen = false;
  ui.openSeat = null;
  ui.zonesFor = null;
  ui.seatChosenTurn = 0;
  ui.seatsOpen = false;
  ui.seenOnField.clear();
  ui.faceIndex = 0;
}

export function rememberSidePanels(): void {
  storeJson(SIDE_KEY, ui.sidePanels);
}

function storedSidePanels(): Partial<UiState['sidePanels']> {
  return storedJson(SIDE_KEY, {});
}
