// The column beside the board. It holds the game log and, in a networked game, the chat dock. Each panel collapses to its tab

import { byId, q, relayout } from '../dom';
import { changeUi, eased, rememberSidePanels, ui, type UiState } from '../ui';
import type { Model } from '../model';
import { t } from '../text';
import { isPortrait } from '../form';

const PANELS = ['log', 'chat'] as const;

export function initSide(): void {
  const side = byId('side');
  const menu = Object.assign(document.createElement('button'), { id: 'menu-button', title: t('lblMenu') });
  menu.setAttribute('aria-label', t('lblMenu'));
  menu.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16"/></svg>';
  menu.onclick = () => changeUi(u => { u.menuSheet = u.menuSheet ? null : 'log'; });
  byId('match').append(menu);
  // Every sheet reads the dock's height, and some sheets are drawn outside the board. The height changes with the prompt's text
  let refit = 0;
  new ResizeObserver(() => {
    if (!isPortrait()) return;
    document.documentElement.style.setProperty('--dock-h', `${byId('prompt').offsetHeight}px`);
    // The board's halves use the space the dock leaves. So their cards are fitted again once the dock stops resizing
    clearTimeout(refit);
    refit = window.setTimeout(relayout, 200);
  }).observe(byId('prompt'));
  for (const panel of PANELS) {
    q(side, `.side-toggle[data-panel="${panel}"]`).onclick = () => folding(u => {
      u.sidePanels[panel] = !u.sidePanels[panel];
      rememberSidePanels();
    });
  }
}

/** Opens the log for the shortcut that shows the console. On a phone it opens in the menu sheet, elsewhere in the side column. */
export function showLog(): void {
  if (isPortrait()) {
    changeUi(u => { u.menuSheet = 'log'; });
  } else if (!ui.sidePanels.log) {
    folding(u => {
      u.sidePanels.log = true;
      rememberSidePanels();
    });
  }
}

/** CSS cannot animate a change of grid areas. So the column slides off the right edge as it folds, and back in as it opens (board.css). */
function folding(change: (state: UiState) => void): void {
  const match = byId('match');
  const wasFolded = match.classList.contains('side-folded');
  eased(change, (): Record<string, string> => {
    const folded = match.classList.contains('side-folded');
    return folded === wasFolded ? {} : { side: folded ? 'shut' : 'open' };
  });
}

/** On a phone the prompt is the board's bottom row. So it is moved out of the column, which is not shown on a phone. */
function placePrompt(): void {
  const prompt = byId('prompt');
  const home = isPortrait() ? byId('match') : byId('side');
  if (prompt.parentElement !== home) home.append(prompt);
}

/** Which panels were open when last drawn. The board is told to reflow only when this changes. */
let drawn = '';

// Chat exists only in a game others can join. Its dock is one of the screens (screens.tsx)
export function renderSide(model: Model): void {
  placePrompt();
  const hasChat = model.networked;
  const shown = { log: ui.sidePanels.log, chat: hasChat && ui.sidePanels.chat };
  const state = `${hasChat}/${shown.log}/${shown.chat}`;
  if (state === drawn) {
    return;
  }
  drawn = state;
  const side = byId('side');
  q(side, '#chat-panel').hidden = !hasChat;
  side.dataset.log = shown.log ? 'open' : 'shut';
  side.dataset.chat = shown.chat ? 'open' : 'shut';
  for (const panel of PANELS) q(side, `.side-toggle[data-panel="${panel}"]`).setAttribute('aria-expanded', String(shown[panel]));
  byId('match').classList.toggle('side-folded', !shown.log && !shown.chat);
  // The board changes width. Anything placed by measuring it must be placed again
  relayout();
}
