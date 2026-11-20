// A long press with a finger opens what a right-click or a hover opens for a mouse

import { make } from './dom';

const LONG_PRESS_MS = 500;
const MOVE_LIMIT_PX = 8;
/** How long after a finger lifts the browser's own context menu event for that touch can still arrive. */
const AFTER_TOUCH_MS = 700;

let touching = false;
let touchEnded = -Infinity;
let swallowClick = false;

/** Whether the pointer can hover without pressing. A phone has no such pointer, whatever events its browser fires. */
export const hovers = (e: PointerEvent): boolean => e.pointerType === 'mouse' && matchMedia('(any-hover: hover)').matches;

/** Whether a click came from a touch. Most browsers report this on the click. Otherwise a recent touch end decides it. */
export const isTouch = (e: Event | undefined): boolean => {
  const type = (e as PointerEvent | undefined)?.pointerType;
  return type ? type === 'touch' : !!e && performance.now() - touchEnded < AFTER_TOUCH_MS;
};

export function initPress(): void {
  const lifted = (e: PointerEvent) => {
    if (e.pointerType !== 'touch') return;
    touching = false;
    touchEnded = performance.now();
  };
  document.addEventListener('pointerdown', e => {
    if (e.pointerType !== 'touch') return;
    touching = true;
    swallowClick = false;
  }, true);
  document.addEventListener('pointerup', lifted, true);
  document.addEventListener('pointercancel', lifted, true);
  // Android fires its own context menu on a long touch. That would act as a right-click as well as the long press
  document.addEventListener('contextmenu', e => {
    if (e.isTrusted && (touching || performance.now() - touchEnded < AFTER_TOUCH_MS)) {
      e.preventDefault();
      e.stopPropagation();
    }
  }, true);
  // When a long press opened something, the click at its end is swallowed so it does not also click the element
  document.addEventListener('click', e => {
    if (swallowClick) {
      swallowClick = false;
      e.preventDefault();
      e.stopPropagation();
    }
  }, true);
}

/** Calls open after a long press on an element, and fills a ring while it waits. A touch that moves is a scroll or a drag, so it cancels. */
export function longPress(e: PointerEvent, open: (x: number, y: number) => void): void {
  if (e.pointerType !== 'touch') {
    return;
  }
  const x = e.clientX;
  const y = e.clientY;
  const ring = make('div', 'press-ring');
  ring.style.left = `${x - 20}px`;
  ring.style.top = `${y - 20}px`;
  document.body.append(ring);
  const timer = setTimeout(() => {
    cancel();
    swallowClick = true;
    navigator.vibrate?.(10);
    open(x, y);
  }, LONG_PRESS_MS);
  const moved = (ev: PointerEvent) => {
    if (Math.hypot(ev.clientX - x, ev.clientY - y) > MOVE_LIMIT_PX) cancel();
  };
  function cancel(): void {
    clearTimeout(timer);
    ring.remove();
    document.removeEventListener('pointermove', moved);
    document.removeEventListener('pointerup', cancel);
    document.removeEventListener('pointercancel', cancel);
  }
  document.addEventListener('pointermove', moved);
  document.addEventListener('pointerup', cancel);
  document.addEventListener('pointercancel', cancel);
}

/**
 * Tracks a press that becomes a drag once the pointer has moved threshold pixels. start is called then, and can return
 * false to wait for the next move. move is called on each later move, and end when the press lifts or is cancelled. The click
 * that ends a drag is swallowed, so releasing over an element does not also click it.
 */
export function trackDrag(e: PointerEvent, threshold: number, on: {
  start: (ev: PointerEvent) => boolean | void; move: (ev: PointerEvent) => void; end: (ev: PointerEvent, dragged: boolean) => void;
}): void {
  const x0 = e.clientX;
  const y0 = e.clientY;
  let dragging = false;
  const moved = (ev: PointerEvent) => {
    if (!dragging) {
      if (Math.hypot(ev.clientX - x0, ev.clientY - y0) < threshold || on.start(ev) === false) return;
      dragging = true;
    }
    on.move(ev);
  };
  const lifted = (ev: PointerEvent) => {
    document.removeEventListener('pointermove', moved);
    document.removeEventListener('pointerup', lifted);
    document.removeEventListener('pointercancel', lifted);
    if (dragging) {
      // The click fires in the same task as the lift or not at all. Removing the listener after this task keeps later clicks working
      const swallow = (c: Event) => { c.stopPropagation(); c.preventDefault(); };
      document.addEventListener('click', swallow, true);
      setTimeout(() => document.removeEventListener('click', swallow, true));
    }
    on.end(ev, dragging);
  };
  document.addEventListener('pointermove', moved);
  document.addEventListener('pointerup', lifted);
  document.addEventListener('pointercancel', lifted);
}
