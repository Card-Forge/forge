// How large a question's cards are drawn on the dimmed board

// Cards grow to fill the space they are given, up to about 200px wide. At that size their text is easy to read
const MOST_FIT = 2.3;

export interface CardFit {
  /** The pixels between two cards. */
  gap: number;
  /** The height in pixels of the buttons under each card. */
  below?: number;
  wrap?: boolean;
  /** The smallest scale, below which the cards scroll instead. */
  least?: number;
  /** How many boxes are stacked one above another in the backdrop's height. */
  share?: number;
}

/**
 * How much to enlarge a question's cards so they fill the space given. When wrapping is allowed, each row count is tried.
 * The layout with the biggest cards is used.
 */
function cardScale(count: number, width: number, height: number, card: { w: number; h: number }, how: CardFit): number {
  const { gap, below = 0, wrap = false, least = 1 } = how;
  let best = 0;
  for (let rows = 1; rows <= (wrap ? count : 1); rows++) {
    const cols = Math.ceil(count / rows);
    best = Math.max(best, Math.min((width - gap * (cols - 1)) / cols / card.w, (height - gap * (rows - 1) - below * rows) / rows / card.h));
  }
  return Math.max(least, Math.min(MOST_FIT, best));
}

/** Sets the scale of the cards in a row. They fill the backdrop's space that the rest of the box does not use. */
export function fitCards(row: HTMLElement, box: HTMLElement, back: HTMLElement, count: number, how: CardFit): void {
  const around = getComputedStyle(back);
  const own = getComputedStyle(row);
  const card = { w: parseFloat(own.getPropertyValue('--card-w')), h: parseFloat(own.getPropertyValue('--card-h')) };
  const width = back.clientWidth - parseFloat(around.paddingLeft) - parseFloat(around.paddingRight)
    - parseFloat(own.paddingLeft) - parseFloat(own.paddingRight) - 16;
  // The row's padding holds the labels under its cards, so it counts as used. The rest of the box keeps its height
  const used = box.offsetHeight - row.clientHeight + parseFloat(own.paddingTop) + parseFloat(own.paddingBottom);
  const room = back.clientHeight - parseFloat(around.paddingTop) - parseFloat(around.paddingBottom);
  const height = room / (how.share ?? 1) - used - 16;
  row.style.setProperty('--fit', cardScale(count, width, height, card, how).toFixed(2));
}
