// Searches a long list by name, ranked as desktop's ListChooser ranks it. Unlike ListChooser, letters of every script are kept, so translated labels can be found

/** Converts to lower case, strips accents, and keeps only letters, digits and spaces. */
export function normalize(s: string): string {
  return s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^\p{L}\p{N} ]/gu, '');
}

/** The start and end of the typed text in the original name. These are positions before normalizing, which can shorten the name. */
export function matchedSpan(name: string, typed: string): [number, number] | null {
  const text = normalize(typed);
  if (!text) return null;
  let flat = '';
  const from: number[] = [];
  for (let i = 0; i < name.length; i++) {
    for (const c of normalize(name[i])) {
      flat += c;
      from.push(i);
    }
  }
  const at = flat.indexOf(text);
  return at < 0 ? null : [from[at], from[at + text.length - 1] + 1];
}

/** The indexes of the names that match the typed text. Names that start with it come first, shortest first, then names that contain it. */
export function rankByName(names: readonly string[], typed: string): number[] {
  const text = normalize(typed);
  if (!text) return names.map((_, i) => i);
  const startsWith: number[] = [];
  const contains: number[] = [];
  names.forEach((name, i) => {
    const n = normalize(name);
    if (n.startsWith(text)) startsWith.push(i);
    else if (n.includes(text)) contains.push(i);
  });
  startsWith.sort((a, b) => names[a].length - names[b].length);
  return [...startsWith, ...contains];
}
