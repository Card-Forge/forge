// The URLs of card, mana symbol and ability icon images, and the drawing of text that contains {symbols}

import { stateOf, type Model } from './model';
import type { CardView } from './protocol';
import { make } from './dom';

/** How many times each picture has arrived since the server said it was missing, by its address. */
const arrivals = new Map<string, number>();

/** Adds the picture's arrival count to its address. A new address makes every element that shows the picture load it again. */
const withArrivals = (src: string): string => {
  const n = arrivals.get(src);
  return n ? `${src}&v=${n}` : src;
};

const cardAddress = (key: string): string => `img?key=${encodeURIComponent(key)}`;
const artAddress = (key: string): string => `/sleeveart?key=${encodeURIComponent(key)}`;

export const imageUrl = (key: string): string => withArrivals(cardAddress(key));

/** A card's art alone, cropped by the server, as a card-art sleeve uses it. */
export const artUrl = (key: string): string => withArrivals(artAddress(key));

/** A picture the server answered as missing has been downloaded. The page is drawn again after this, which loads it. */
export function pictureArrived(key: string, art: boolean): void {
  const src = art ? artAddress(key) : cardAddress(key);
  arrivals.set(src, (arrivals.get(src) ?? 0) + 1);
}

/** The image scaled down by the server to a fixed width. A browser that scales the full scan on a moving card makes the text break up. */
export const smallImage = (src: string): string => (src ? `${src}&w=256` : '');

/** Adds Forge's foil mark to the name in a card's image key. The mark tells the server to draw the foil onto the image. */
const foilKey = (key: string): string => key.replace(/^c:[^|$]*/, '$&+');

// Empty for a card the viewer may not see, so the element shows its card back or its frame instead
export function cardImageSrc(model: Model, card: CardView | null | undefined): string {
  if (!card || !model.visible.has(card.$key)) return '';
  const state = stateOf(model, card);
  // A face-down card has its own image, which shows why it is face down, such as a morph, a manifest or a foretold card
  const key = card.Facedown && card.FacedownImageKey ? card.FacedownImageKey : state.ImageKey;
  if (!key) return '';
  // Same foil rule as the server's Foil.key. The paper card is foil, or the match made this card foil at random
  return imageUrl(card.PaperFoil || state.FoilIndex ? foilKey(key) : key);
}

// Setting the same src again restarts the request, so it is skipped. Returns whether the image changed
export function setImage(img: HTMLImageElement, src: string): boolean {
  if ((img.getAttribute('src') ?? '') === src) return false;
  if (src) img.src = src;
  else img.removeAttribute('src');
  return true;
}

// A card with no image still shows its frame, so the class goes on the container and not on the image
export function noImageOnError(container: HTMLElement, img: HTMLImageElement): void {
  img.addEventListener('error', () => container.classList.add('noimg'));
  // The picture can arrive later under a new address
  img.addEventListener('load', () => container.classList.remove('noimg'));
}

// Symbols such as {2}{B} and {T} are drawn from the skin's icon sheet. A symbol with no image on the sheet stays as text
export function setSymbolText(el: HTMLElement, text: string | null | undefined): void {
  // The prompt and preview set their text on every frame, so unchanged text keeps its nodes
  if (el.dataset.symbolText === (text ?? '')) return;
  el.dataset.symbolText = text ?? '';
  el.replaceChildren();
  appendSymbolText(el, text ?? '');
}

export function appendSymbolText(el: HTMLElement, text: string): void {
  for (const part of symbolParts(text)) {
    if (part.symbol) {
      const img = make('img', 'sym');
      img.alt = part.text;
      img.src = symbolUrl(part.symbol);
      el.append(img);
    } else {
      const span = document.createElement('span');
      span.textContent = part.text;
      el.append(span);
    }
  }
}

/** Text split into plain runs and {symbols}. symbol is the symbol's name for a symbol, and null for plain text. */
export function symbolParts(text: string): { text: string; symbol: string | null }[] {
  return String(text).split(/(\{[^}]{1,6}\})/).filter(part => part).map(part => ({
    text: part,
    symbol: part.length > 2 && part.startsWith('{') && part.endsWith('}') ? part.slice(1, -1) : null,
  }));
}

// A hybrid or Phyrexian shard is named without its slash, to match the skin's icon sheet keys
export const symbolUrl = (symbol: string): string => `mana?s=${encodeURIComponent(symbol.replace(/\//g, ''))}`;

/** A keyword's icon, cut from the skin's ability sheet. The server names the icon (FSkinProp.iconFromKeyword). */
export const abilityUrl = (icon: string): string => `ability?k=${encodeURIComponent(icon)}`;

export function hideOnError(img: HTMLImageElement): void {
  img.addEventListener('error', hideBroken);
}

/** An error handler that hides the image, so a missing image leaves an empty space and not a broken-image icon. */
export const hideBroken = (e: Event): void => {
  const img = e.currentTarget as HTMLImageElement;
  img.hidden = true;
  // The picture can arrive later under a new address
  img.addEventListener('load', () => { img.hidden = false; }, { once: true });
};
