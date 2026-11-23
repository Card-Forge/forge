// A closing element plays its exit animation first (dialogs.css). It is removed when that animation ends

import { useRef, useState } from 'preact/hooks';
import { reducedMotion } from './dom';

/** Longer than any exit animation (dialogs.css). */
const GIVE_UP_MS = 400;

/**
 * A close that waits for the exit animation. The element gets `closing`, and the end of its own animation closes it. An element
 * that stays drawn once closed, such as the chat dock, loses `closing` again.
 */
export function useClosing(close: () => void): { closing: boolean; shut: () => void; gone: (e: AnimationEvent) => void } {
  const [closing, setClosing] = useState(false);
  const giveUp = useRef(0);
  const done = () => {
    clearTimeout(giveUp.current);
    setClosing(false);
    close();
  };
  return {
    closing,
    shut: () => {
      if (reducedMotion()) {
        close();
        return;
      }
      setClosing(true);
      // Close anyway if no animation runs, for example when no style gives it one
      giveUp.current = window.setTimeout(done, GIVE_UP_MS);
    },
    gone: e => { if (closing && e.target === e.currentTarget) done(); },
  };
}

/** The same for a hand-built element. It gets `closing`, and is removed when its animation ends. */
export function leave(el: Element | null): void {
  if (!el) return;
  if (reducedMotion() || el.classList.contains('closing')) {
    el.remove();
    return;
  }
  el.classList.add('closing');
  el.addEventListener('animationend', e => { if (e.target === el) el.remove(); });
  setTimeout(() => el.remove(), GIVE_UP_MS);
}
