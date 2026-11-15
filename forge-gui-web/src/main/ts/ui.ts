// Page layout state that stays in this browser and is never sent to the server. The match table state is cleared by each new match (resetMatchUi).
// The menu pages' tabs and open panels are kept across matches

import { reducedMotion } from './dom';

/** resetMatchUi clears the fields that belong to one match. main.ts clears the fields used by only one layout when the layout changes. */
export interface UiState {
  optionsOpen: boolean;
  /** The volume control beside the options button is open. */
  volumeOpen: boolean;
  /** The play choice whose game kinds the start page shows. It is kept so that going back from a table returns there. */
  menuChoice: 'play' | 'friends' | null;
}

export const ui: UiState = {
  optionsOpen: false,
  volumeOpen: false,
  menuChoice: null,
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
