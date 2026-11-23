// Behaviour shared by every sheet on a phone. Back closes it, and so does a swipe down from its handle

import { byId, q } from './dom';

const open = new Map<string, () => void>();
let inMatch = false;
let leaving = false;
/** History steps back that this module made itself to remove a closed sheet's entry. They are not Back presses by the player. */
let ownSteps = 0;

/** A match adds one history entry of its own. Back with nothing open then asks before it leaves the page. */
export function matchHistory(now: boolean): void {
  // After a reload the match's entry is still in the history
  if (now && !inMatch && !history.state?.match) history.pushState({ match: true }, '');
  if (!now && inMatch) {
    // The entry is removed when the match ends, so the next page has no Back press that does nothing
    open.clear();
    if (history.state?.match) step();
  }
  inMatch = now;
}

/** Steps back in history for this module. The popstate handler must not treat it as the player's Back press. */
function step(): void {
  ownSteps++;
  history.back();
}

export function initSheets(): void {
  const ask = byId('leave-ask');
  // Stay pushes back the entry that Back removed, so the next Back asks again
  q(ask, '.stay').onclick = () => {
    ask.hidden = true;
    if (inMatch && !history.state?.match) history.pushState({ match: true }, '');
  };
  // Leave does what Back would have done without the match's entry
  q(ask, '.leave').onclick = () => {
    ask.hidden = true;
    leaving = true;
    // Back already removed the match's entry, so one more step goes to the page before this one
    history.back();
    setTimeout(() => { leaving = false; }, 500);
  };
  addEventListener('popstate', () => {
    if (ownSteps > 0) {
      ownSteps--;
      return;
    }
    const last = [...open.keys()].pop();
    // A sheet that closed while another sheet was above it leaves a stale entry. Back skips over it
    if (history.state?.sheet && !open.has(history.state.sheet)) {
      step();
    }
    if (last === undefined) {
      // Back just removed the match's own entry. The leave question is shown before the next step leaves the page
      if (inMatch && !leaving && !history.state?.match) ask.hidden = false;
      return;
    }
    const close = open.get(last);
    open.delete(last);
    close?.();
  });
}

/** Called each frame with whether a sheet is open. A sheet closed by any means then also removes its history entry. */
export function sheet(id: string, isOpen: boolean, close: () => void): void {
  if (isOpen && !open.has(id)) {
    open.set(id, close);
    history.pushState({ sheet: id }, '');
  } else if (isOpen) {
    open.set(id, close);
  } else if (open.has(id)) {
    open.delete(id);
    if (history.state?.sheet === id) step();
  }
}

const SWIPE_PX = 60;

/** A swipe down from a sheet's handle closes it. Lower down, the same swipe scrolls the sheet's content. */
export function swipeDown(el: HTMLElement, close: () => void): void {
  // This uses touch events. When a browser treats a drag as a scroll, it cancels the pointer and never reports where it lifted
  let from: number | null = null;
  el.addEventListener('touchstart', e => {
    const y = e.touches[0].clientY;
    from = y - el.getBoundingClientRect().top <= 48 ? y : null;
  }, { passive: true });
  el.addEventListener('touchend', e => {
    if (from !== null && e.changedTouches[0].clientY - from > SWIPE_PX) close();
    from = null;
  });
  el.addEventListener('touchcancel', () => { from = null; });
}

/** Calls swiped for a swipe left, right or up that starts on the part of el matching inside. */
export function swipeAcross(el: HTMLElement, inside: string, swiped: (way: 'left' | 'right' | 'up') => void): void {
  let from: Touch | null = null;
  el.addEventListener('touchstart', e => {
    from = e.target instanceof Element && e.target.closest(inside) ? e.touches[0] : null;
  }, { passive: true });
  el.addEventListener('touchend', e => {
    if (!from) return;
    const dx = e.changedTouches[0].clientX - from.clientX;
    const dy = e.changedTouches[0].clientY - from.clientY;
    from = null;
    if (Math.abs(dx) > SWIPE_PX && Math.abs(dx) > Math.abs(dy)) swiped(dx < 0 ? 'left' : 'right');
    else if (-dy > SWIPE_PX && -dy > Math.abs(dx)) swiped('up');
  });
  el.addEventListener('touchcancel', () => { from = null; });
}

/** Tracks the prompt a sheet opened over, so only a later prompt closes the sheet. note is called once per draw. */
export function openedOver(): { readonly seen: object | null; note(open: boolean, prompt: object | null | undefined): void } {
  // If the sheet opened with no prompt, any later prompt also counts as new
  const none = {};
  let seen: object | null = null;
  return {
    get seen() { return seen; },
    note(open, prompt) { seen = open ? seen ?? prompt ?? none : null; },
  };
}

/** Returns true when given a key that differs from the previous one, so a sheet acts once per prompt and not on every draw. null is no key. */
export function oncePer<T>(): (key: T | null) => boolean {
  let last: T | null = null;
  return key => {
    const fresh = key !== null && key !== last;
    last = key;
    return fresh;
  };
}
