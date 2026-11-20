// Entering a match. The page the match starts from dims and blurs, and stays drawn under the board. The coin toss does not wait, so it rises over that page (coin.ts).
// The board then fades in under the coin once its seats are drawn, so the empty table that is drawn first is never shown

import { byId, reducedMotion, replay } from '../dom';

/** Whether the board is on screen but has not yet played its entry animation. */
let awaiting = false;
let giveUp = 0;

/** Starts the match and dims the page. If the server never starts the match, the page goes back to how it was. */
export function dimIntoMatch(page: HTMLElement, start: () => void): void {
  start();
  if (reducedMotion()) return;
  // The page is fixed where it is scrolled to, so it stays in place when the board is drawn below it
  page.style.setProperty('--scrolled', `${-window.scrollY}px`);
  page.classList.add('to-match');
  clearTimeout(giveUp);
  giveUp = window.setTimeout(() => page.classList.remove('to-match'), 8000);
}

/** Called each time the match is drawn. `entering` is true when it has just come on screen. */
export function arriveAtMatch(entering: boolean): void {
  if (entering) awaiting = !reducedMotion();
  arrive();
}

/** Plays the board's entry once both seats are drawn. Until then the board is hidden. */
function arrive(): void {
  if (!awaiting) return;
  const match = byId('match');
  if (!document.querySelector('#me .player') || !document.querySelector('#opponent .player')) {
    match.classList.add('held');
    return;
  }
  awaiting = false;
  clearTimeout(giveUp);
  match.classList.remove('held');
  replay(match, 'arriving');
  match.addEventListener('animationend', function done(e) {
    if (e.target !== match) return;
    match.removeEventListener('animationend', done);
    match.classList.remove('arriving');
    // The page under the board is hidden once the board covers it. It returns to normal when the match ends
    const page = document.querySelector<HTMLElement>('.to-match');
    page?.classList.remove('to-match');
    if (page) page.hidden = true;
  });
}
