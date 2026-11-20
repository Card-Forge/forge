// The pack dial's geometry, which the dial and the draft's seat list share. Angles are in radians and y grows downwards

/** The angle of seat i of n. Seat 0 is at the bottom, and later seats run anticlockwise on screen, towards the player's right. */
export function seatAngle(i: number, n: number): number {
  return Math.PI / 2 - (i * 2 * Math.PI) / n;
}

/** The seat your next pack comes from. It is the first seat upstream from you that holds a pack, or null when no seat holds one. */
export function nextFrom(depths: number[], direction: 1 | -1): number | null {
  const n = depths.length;
  for (let k = 1; k < n; k++) {
    const seat = (((-direction * k) % n) + n) % n;
    if (depths[seat] > 0) return seat;
  }
  return null;
}
