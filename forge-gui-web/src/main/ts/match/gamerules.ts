// Game rules used by the board, the hand and the preview. Nothing is drawn here

/** The commander damage that loses the game, and the amount at which the portrait starts to warn of it. */
export const COMMANDER_LETHAL = 21;
export const COMMANDER_WARNING = 15;

/** The five colours by their bit in Forge's MagicColor mask. */
export const COLOUR_BITS: [number, string][] = [[1, 'W'], [2, 'U'], [4, 'B'], [8, 'R'], [16, 'G']];

/** Whether a power or toughness is above or below the card's base value, for example after a pump, a counter or a shrink. */
export const ptShift = (now: number, base: number | undefined): string => (base === undefined || now === base ? '' : now > base ? 'up' : 'down');

/** The frame colour of a card drawn without its image, from a MagicColor mask. It is the single colour, M for several, or L or C for none. */
export function frameColour(colours: number, type: string): string {
  const one = COLOUR_BITS.filter(([bit]) => colours & bit);
  if (one.length > 1) return 'M';
  if (one.length === 1) return one[0][1];
  return /\bLand\b/.test(type) ? 'L' : 'C';
}

/** The mana value of a mana cost. {2}{W} counts as three, {W/U} as one, {2/W} as two and X as zero. */
export function manaValue(cost: string | undefined): number {
  return [...String(cost ?? '').matchAll(/\{([^}]+)\}/g)].reduce((sum, [, shard]) => {
    const digits = shard.split('/').find(part => /^\d+$/.test(part));
    return sum + (digits !== undefined ? Number(digits) : /^[xyz]$/i.test(shard) ? 0 : 1);
  }, 0);
}
