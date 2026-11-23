// The importer's fixes edit the pasted text directly. Most keep every line in place so the line numbers of other problems stay correct

// A card line has an optional count, the name, then any suffix a site adds, such as a set code, a number or a foil mark
const CARD_LINE = /^(\s*(?:\d+x?\s+)?)(.+?)(\s*(?:[[(].*|\*F\*.*)?)$/;

function lines(text: string): string[] {
  return text.split('\n');
}

/** Rewrites one line's card name, keeping its count and anything after the name. */
export function useName(text: string, line: number, name: string): string {
  const all = lines(text);
  const match = CARD_LINE.exec(all[line] ?? '');
  if (match) all[line] = `${match[1]}${name}${match[3]}`;
  return all.join('\n');
}

/** Turns one line into a comment, which the importer ignores. */
export function leaveOut(text: string, line: number): string {
  const all = lines(text);
  if (all[line] !== undefined) all[line] = `// ${all[line]}`;
  return all.join('\n');
}

/** Moves the line holding a card under the list's Commander heading, adding the heading at the top when there is none. */
export function makeCommander(text: string, cardName: string): string {
  const all = lines(text);
  const at = all.findIndex(l => CARD_LINE.exec(l)?.[2].trim().toLowerCase() === cardName.toLowerCase());
  if (at < 0) return text;
  const [moved] = all.splice(at, 1);
  const heading = all.findIndex(l => /^\s*commander:?\s*$/i.test(l));
  if (heading < 0) {
    return ['Commander', moved, ...all].join('\n');
  }
  let end = heading + 1;
  while (end < all.length && /^\s*\d/.test(all[end])) end++;
  all.splice(end, 0, moved);
  return all.join('\n');
}
