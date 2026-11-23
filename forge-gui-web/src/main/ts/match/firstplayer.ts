// Who goes first. This covers the coin toss, the choice made by the toss's winner or the last game's loser, and the reveal over the board

import { isLocal, me, players, type Model } from '../model';
import { coinInAir, dropCoin, readyCoin, resetCoin, tossCoin } from './coin';
import { playerAvatarUrl } from '../avatarsleeves';
import { byId, make, replay } from '../dom';
import { leave } from '../closing';
import { redrawUi } from '../ui';
import { keyName } from '../keys';
import { boundKeys } from '../settings';
import { t } from '../text';
import type { Actions } from '../actions';
import type { GameEvent, PlayerView } from '../protocol';

/** The player the game chose to take the first turn. It is kept from when that is decided until the next game. */
let firstPlayer: number | null = null;
/** The game has said who starts, but the page has not shown it yet. Showing it waits for the coin. */
let firstDue = false;
/** Whether this viewer was asked who starts, because they won the toss or lost the last game. */
let choseStarter = false;
/** The first game's toss has been shown. */
let tossed = false;

/** Tracks the toss and the game's start through these events, and shows or asks who goes first. */
export function showStarter(model: Model, actions: Actions, events: readonly GameEvent[]): void {
  for (const e of events) {
    if (e.kind === 'coinTossed') toss(model, model.localPlayers.includes(e.winner.ref));
    if (e.kind === 'gameStarted') {
      firstPlayer = e.first.ref;
      firstDue = true;
    }
  }
  const choice = model.prompt?.starterChoice;
  // The winner is asked who starts when the toss is made. That prompt can reach the page before the toss's own event
  if (choice === 'toss') toss(model, true);
  // Who goes first is shown or asked once the coin has landed
  if (!coinInAir()) {
    if (firstDue) {
      firstDue = false;
      // The player who chose the starter already knows who starts
      if (!choseStarter) revealFirst(model, firstPlayer);
    }
    if (choice && !choseStarter) {
      choseStarter = true;
      chooseFirst(model, actions, choice);
    }
  }
  // Close the question once it is answered, by a key or by the board's own portraits, or if the game was conceded before anyone chose
  if (!choice || model.gameOver) {
    // A panel already closing from its own button is left to finish, because a second leave removes it at once
    leave(document.querySelector('#coin-toss.asking:not(.closing)'));
    if (!coinInAir()) dropCoin();
  }
}

/** Whether the player goes first and the reveal has already shown it. */
export function wentFirst(key: number): boolean {
  return firstPlayer === key && !firstDue;
}

/** Whether who goes first is still being shown. A phone's hand drawer waits for it before it opens for the mulligan (hand.ts). */
export function starterShowing(): boolean {
  return coinInAir() || firstDue || !!document.getElementById('first-reveal');
}

export function resetStarter(): void {
  firstPlayer = null;
  firstDue = false;
  tossed = false;
  // A reveal from a match left mid-way never ends, because a hidden page does not run its animation
  document.getElementById('first-reveal')?.remove();
  resetCoin();
  readyCoin();
  choseStarter = false;
}

/** Shows the toss once, only to a player at the table. It is shown on the toss event or the winner's prompt, whichever arrives first. */
function toss(model: Model, heads: boolean): void {
  const name = me(model)?.Name;
  if (tossed || name === undefined) return;
  tossed = true;
  // The page is redrawn when the coin has rested. That is when who goes first is shown or asked
  tossCoin(heads, t(heads ? 'lblYouHaveWonTheCoinToss' : 'lblYouLostTheCoinToss', name), redrawUi);
}

/**
 * Asks who goes first under the coin, which shrinks to make room. After a lost game there is no coin, so the question opens alone.
 * With two players the answers are the prompt's Play and Draw, moved here from the prompt. With more players each face picks that player.
 */
function chooseFirst(model: Model, actions: Actions, choice: string): void {
  const everyone = players(model);
  const two = everyone.length === 2;
  let root = document.querySelector<HTMLElement>('#coin-toss.rested');
  if (!root) {
    root = make('div', 'rested landed');
    root.id = 'coin-toss';
    const said = make('p', '');
    said.textContent = t(choice === 'toss' ? 'lblYouHaveWonTheCoinToss' : 'lblYouLostTheLastGame', me(model)?.Name ?? '');
    root.append(said);
    byId('match').append(root);
  }
  const asked = root;
  asked.onclick = null;
  asked.setAttribute('role', 'dialog');
  // The chosen answer dims as a prompt button does. The panel's exit waits for it (board.css)
  const answer = (button: HTMLElement, send: () => void) => {
    replay(button, 'pressed');
    send();
    leave(asked);
  };
  const ask = make('div', 'first-ask');
  const body = make('div', '');
  const question = make('h3', '');
  question.textContent = t(two ? 'lblWouldYouLiketoPlayorDraw' : 'lblWebBoardWhoGoesFirst');
  body.append(question, two ? playOrDraw(model, b => answer(b, () => actions.ok()), b => answer(b, () => actions.cancel()))
    : pickPlayer(everyone, (key, b) => answer(b, () => actions.selectPlayer(key))));
  ask.append(body);
  asked.append(ask);
  // Laid out closed first, so that opening it is animated
  void asked.offsetHeight;
  asked.classList.add('asking');
}

/** The prompt's two answers, in the prompt's style and with its keys. */
function playOrDraw(model: Model, play: (button: HTMLElement) => void, draw: (button: HTMLElement) => void): HTMLElement {
  const row = make('div', 'first-answers');
  const p = model.prompt;
  for (const [spec, cls, key, send] of [[p?.ok, 'primary ok', keyName(boundKeys().ok), play], [p?.cancel, 'cancel', 'Esc', draw]] as const) {
    const button = make('button', cls);
    button.type = 'button';
    button.textContent = spec?.label ?? '';
    const kbd = make('kbd', '');
    kbd.textContent = key;
    button.append(kbd);
    button.onclick = () => send(button);
    row.append(button);
  }
  return row;
}

/** A face for each player in turn order. Pointing at one numbers every seat by the turn it would play. */
function pickPlayer(everyone: readonly PlayerView[], pick: (key: number, seat: HTMLElement) => void): HTMLElement {
  const row = make('div', 'first-seats');
  const seats = everyone.map((p, i) => {
    const seat = make('button', 'first-seat');
    seat.type = 'button';
    const name = make('span', 'reveal-name');
    name.textContent = p.Name ?? '';
    seat.append(face(p), name, make('i', 'turn'));
    seat.onclick = () => pick(p.$key, seat);
    seat.onpointerenter = seat.onfocus = () => number(i);
    return seat;
  });
  const number = (from: number) => {
    row.classList.toggle('pointing', from >= 0);
    seats.forEach((seat, i) => {
      seat.classList.toggle('lit', i === from);
      seat.lastElementChild!.textContent = from < 0 ? '' : String((i - from + seats.length) % seats.length + 1);
    });
  };
  row.onpointerleave = () => number(-1);
  row.addEventListener('focusout', e => { if (!row.contains(e.relatedTarget as Node | null)) number(-1); });
  row.append(...seats);
  return row;
}

/** The player's avatar, or the first letter of their name when they have none. */
function face(p: PlayerView): HTMLElement {
  const url = playerAvatarUrl(p);
  const picture = document.createElement(url ? 'img' : 'span');
  picture.className = 'reveal-avatar';
  if (picture instanceof HTMLImageElement) {
    picture.alt = '';
    picture.src = url;
  } else {
    picture.textContent = (p.Name ?? '?').slice(0, 1).toUpperCase();
  }
  return picture;
}

/** Shows who goes first over the board, to everyone who did not choose it. */
function revealFirst(model: Model, first: number | null): void {
  const everyone = players(model);
  const starter = everyone.find(p => p.$key === first);
  if (!starter) return;
  document.getElementById('first-reveal')?.remove();
  const reveal = document.createElement('div');
  reveal.id = 'first-reveal';
  reveal.setAttribute('role', 'status');
  const faces = make('div', 'reveal-faces');
  for (const p of everyone) {
    const one = make('div', p.$key === first ? 'reveal-face first' : 'reveal-face');
    const name = make('span', 'reveal-name');
    name.textContent = p.Name ?? '';
    one.append(face(p), name);
    faces.append(one);
  }
  const line = document.createElement('p');
  line.textContent = isLocal(model, starter) ? t('lblWebBoardYouGoFirst') : t('lblWebBoardPlayerGoesFirst', starter.Name ?? '');
  reveal.append(faces, line);
  byId('match').append(reveal);
  reveal.addEventListener('animationend', e => {
    if (e.animationName !== 'first-reveal') return;
    reveal.remove();
    redrawUi();
  });
}
