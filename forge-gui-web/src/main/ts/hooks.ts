// Small pieces of page behaviour that several pages share

import { useEffect, useLayoutEffect, useRef, useState } from 'preact/hooks';
import type { RefObject } from 'preact';

/** The value after it has stopped changing for ms. A long list costs a request per row, so a search waits for a pause in typing. */
export function useDebounced<T>(value: T, ms = 200): T {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), ms);
    return () => clearTimeout(timer);
  }, [value]);
  return settled;
}

/**
 * Closes a popup on Escape, or on a press outside its root element. A popup inside a dialog passes inDialog: it then hears the
 * press and the key before the dialog does, and Escape closes only the popup.
 */
export function useDismiss(open: boolean, root: RefObject<HTMLElement>, close: () => void, inDialog = false): void {
  useEffect(() => {
    if (!open) return;
    const outside = (e: PointerEvent) => { if (!root.current?.contains(e.target as Node)) close(); };
    const escape = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      if (inDialog) e.stopPropagation();
      close();
    };
    document.addEventListener('pointerdown', outside, inDialog);
    document.addEventListener('keydown', escape, inDialog);
    return () => {
      document.removeEventListener('pointerdown', outside, inDialog);
      document.removeEventListener('keydown', escape, inDialog);
    };
  }, [open]);
}

/** A two-press button for actions that cannot be undone. The first press arms it and changes its label. The second press runs act. */
export function useTwoPress(): [armed: boolean, press: (act: () => void) => () => void] {
  const [armed, setArmed] = useState(false);
  return [armed, act => () => {
    if (!armed) {
      setArmed(true);
      return;
    }
    act();
    setArmed(false);
  }];
}

/**
 * Where a menu placed against the window goes, so that no panel around its button can clip it. It opens under the anchor, or above
 * when there is no room below, and stays on screen. It is null while closed and until the menu is measured. again lists what changes the menu's size.
 */
export function usePlacedMenu(open: boolean, anchor: RefObject<HTMLElement>, menu: RefObject<HTMLElement>, again: unknown[] = []): { top: number; left: number; width: number } | null {
  const [at, setAt] = useState<{ top: number; left: number; width: number } | null>(null);
  useLayoutEffect(() => {
    if (!open) {
      setAt(null);
      return;
    }
    const place = () => {
      const box = anchor.current?.getBoundingClientRect();
      if (!box) return;
      const width = menu.current?.offsetWidth ?? 0;
      const height = menu.current?.offsetHeight ?? 0;
      const below = box.bottom + 6;
      const top = below + height > window.innerHeight - 8 && box.top - 6 - height >= 8 ? box.top - 6 - height : below;
      setAt({ top, left: Math.max(8, Math.min(box.left, window.innerWidth - width - 8)), width: box.width });
    };
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open, ...again]);
  return at;
}

/**
 * Delays a submenu change while the pointer crosses other rows of a menu on its way into the submenu. later runs an action after
 * a short pause, and stay cancels it when the pointer reaches the row that opened the submenu, or the submenu itself.
 */
export function useHoverIntent(ms = 300): { later: (action: () => void) => void; stay: () => void } {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const stay = () => {
    if (timer.current !== null) clearTimeout(timer.current);
    timer.current = null;
  };
  useEffect(() => stay, []);
  return {
    later: action => {
      stay();
      timer.current = setTimeout(() => {
        timer.current = null;
        action();
      }, ms);
    },
    stay,
  };
}
