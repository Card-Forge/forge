// Matches a container's children to a list by key. Existing elements are updated and moved, new ones are created and missing ones are removed

/** Uses moveBefore where the browser supports it. insertBefore removes the element and adds it back, which ends its transitions. */
function place(parent: HTMLElement, el: HTMLElement, next: ChildNode | null): void {
  const mover = parent as HTMLElement & { moveBefore?: (node: Node, child: Node | null) => void };
  if (el.isConnected && mover.moveBefore) {
    try {
      mover.moveBefore(el, next);
      return;
    } catch {
      // If the browser cannot do the move atomically, use the plain insert
    }
  }
  parent.insertBefore(el, next);
}

// Reuses child elements by key, so a card keeps its element (and its CSS transitions) across renders
export function reconcile<T, E extends HTMLElement = HTMLElement>(
  parent: HTMLElement,
  items: readonly T[],
  keyOf: (item: T) => string | number,
  create: (item: T) => E,
  update: (el: E, item: T) => void,
): void {
  const existing = new Map([...parent.children].map(el => [(el as HTMLElement).dataset.key, el as E]));
  let prev: E | null = null;
  for (const item of items) {
    const key = String(keyOf(item));
    let el = existing.get(key);
    if (el) existing.delete(key);
    else {
      el = create(item);
      el.dataset.key = key;
    }
    update(el, item);
    const next: ChildNode | null = prev ? prev.nextSibling : parent.firstChild;
    if (el !== next) place(parent, el, next);
    prev = el;
  }
  for (const el of existing.values()) el.remove();
}
