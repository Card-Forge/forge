// A copy of a card is taken as it looked when chosen. It flies to its new place once that place has been drawn

import { reducedMotion } from './dom';

const FLY_MS = 380;

/** The lifted card copy, its start position, and a key that names the move. It is kept until the target is drawn. */
let inFlight: { face: HTMLElement; from: DOMRect; key: string } | null = null;

/** Copies a card's image at its current position. A card that shows only text, or has no loaded image, does not fly. */
export function lift(img: HTMLImageElement | null | undefined, key: string): void {
  inFlight = !reducedMotion() && img?.complete && img.naturalWidth
    ? { face: img.cloneNode() as HTMLElement, from: img.getBoundingClientRect(), key } : null;
}

/** Lifts a card from a line with no image, such as a table row. The card image starts at the line's start, at card size. */
export function liftFromLine(src: string, line: DOMRect, key: string): void {
  if (reducedMotion()) {
    inFlight = null;
    return;
  }
  const face = new Image();
  face.src = src;
  face.alt = '';
  // An image that is still loading flies as a dark card, so something is visible
  face.style.background = '#1c1f26';
  const height = Math.max(line.height * 1.6, 56);
  const width = height * 63 / 88;
  inFlight = { face, from: new DOMRect(line.left, line.top + (line.height - height) / 2, width, height), key };
}

/** The key that names the current flight's move, or null if no card is lifted. */
export function flyingFor(): string | null {
  return inFlight?.key ?? null;
}

/** Discards a lifted card that will not fly, such as one a drag has already moved. */
export function cancelFlight(): void {
  inFlight = null;
}

/** Flies the lifted card to place. If place is scrolled out of view, the card flies to the list heading and fades out. */
export function land(place: HTMLElement | null, view: Element | null | undefined, heading: Element | null | undefined): void {
  const flight = inFlight;
  inFlight = null;
  if (!flight || !place) return;
  const box = view?.getBoundingClientRect();
  let to = place.getBoundingClientRect();
  const hidden = !box || to.bottom < box.top || to.top > box.bottom;
  if (hidden) to = heading?.getBoundingClientRect() ?? to;
  const { face, from } = flight;
  Object.assign(face.style, { position: 'fixed', left: `${from.left}px`, top: `${from.top}px`, width: `${from.width}px`,
    height: `${from.height}px`, margin: '0', zIndex: '60', pointerEvents: 'none', transformOrigin: '0 0', borderRadius: '6px',
    boxShadow: '0 12px 28px #000b' });
  document.body.append(face);
  // The card scales to fit its target, so it shrinks into a list line
  const scale = Math.min(to.width / from.width, to.height / from.height);
  const dy = to.top - from.top + (to.height - from.height * scale) / 2;
  if (!hidden) place.style.visibility = 'hidden';
  const glide = face.animate([
    { transform: 'none', opacity: 1 },
    { transform: `translate(${to.left - from.left}px, ${dy}px) scale(${scale})`, opacity: hidden ? 0 : 1 },
  ], { duration: FLY_MS, easing: 'cubic-bezier(.3,.1,.2,1)' });
  glide.onfinish = glide.oncancel = () => {
    face.remove();
    place.style.visibility = '';
  };
}
