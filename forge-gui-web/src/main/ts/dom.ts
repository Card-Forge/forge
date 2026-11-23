// Helpers for hand-built markup. Lookups throw with the selector when an element is missing. Also element, pointer, motion, relayout, file-save, easing and escaping helpers

export function q<T extends HTMLElement = HTMLElement>(root: ParentNode, selector: string): T {
  const el = root.querySelector<T>(selector);
  if (!el) throw new Error(`Missing ${selector}`);
  return el;
}

export function byId<T extends HTMLElement = HTMLElement>(id: string): T {
  const el = document.getElementById(id);
  if (!el) throw new Error(`Missing #${id}`);
  return el as T;
}

export function make<K extends keyof HTMLElementTagNameMap>(tag: K, className: string): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag);
  el.className = className;
  return el;
}

/** Whether the player wants less motion. The Motion option decides, and it follows the system setting only if set to do so. */
export const reducedMotion = (): boolean => document.documentElement.dataset.motion === 'reduced';

/** The pointer position over el. x and y each run from -0.5 to 0.5 across it. */
export function pointerOn(el: Element, e: PointerEvent): { x: number; y: number } {
  const box = el.getBoundingClientRect();
  return {
    x: Math.max(-0.5, Math.min(0.5, (e.clientX - box.left) / box.width - 0.5)),
    y: Math.max(-0.5, Math.min(0.5, (e.clientY - box.top) / box.height - 0.5)),
  };
}

/** Restarts a class's CSS animation from the start. It first removes the classes listed in remove. */
export function replay(el: HTMLElement, add: string, ...remove: string[]): void {
  el.classList.remove(add, ...remove);
  void el.offsetWidth;
  el.classList.add(add);
}

/** Plays a class's animation as replay does, then takes the class off when that animation ends. A class left on plays its animation again each time a hidden element is shown. */
export function playOnce(el: HTMLElement, add: string, ...remove: string[]): void {
  el.classList.remove(add, ...remove);
  const before = new Set(el.getAnimations());
  el.classList.add(add);
  const started = el.getAnimations().filter(a => !before.has(a));
  // A cancelled animation was replaced by a newer play of the class, which then owns it
  Promise.all(started.map(a => a.finished)).then(() => el.classList.remove(add), () => {});
}

/** Places el gap pixels below and right of a point. It flips or moves back where it would leave the window. */
export function placeBeside(el: HTMLElement, x: number, y: number, gap: number): void {
  const left = x + gap + el.offsetWidth > innerWidth - 8 ? x - gap - el.offsetWidth : x + gap;
  el.style.left = `${Math.round(Math.max(8, left))}px`;
  el.style.top = `${Math.round(Math.max(8, Math.min(y + gap, innerHeight - el.offsetHeight - 8)))}px`;
}

const relayouts: (() => void)[] = [];

/** Runs place when the window resizes, and when relayout says the board moved without it. */
export function onRelayout(place: () => void): void {
  relayouts.push(place);
  window.addEventListener('resize', place);
}

/** Re-runs every placement that measures the board. Call it when the board changes size but the window does not. */
export function relayout(): void {
  for (const place of relayouts) place();
}

export function saveText(text: string | Blob, fileName: string, type: string): void {
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob([text], { type }));
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(link.href);
}

/**
 * Calls away for every press outside the elements matching inside, so a hand-built panel can close itself. hooks.ts's
 * useDismiss does the same for a component. It listens on pointerdown, or on click if the press must first act on what it hits.
 */
export function onPressOutside(inside: string, away: () => void, on: 'pointerdown' | 'click' = 'pointerdown'): void {
  document.addEventListener(on, e => {
    if (!(e.target instanceof Element && e.target.closest(inside))) away();
  });
}

/** Whether the element takes typed text, so a key or a layout change must leave it alone. */
export function isTyping(el: Element | null): boolean {
  return el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement || (el instanceof HTMLElement && el.isContentEditable);
}

/** A cubic ease-out: quick at first, slowing to a stop at 1. */
export const easeOut = (t: number): number => 1 - (1 - t) ** 3;

const ESCAPES: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

export function escapeHtml(s: string): string {
  return String(s).replace(/[&<>"']/g, c => ESCAPES[c]);
}
