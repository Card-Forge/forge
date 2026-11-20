// A scheme that is set in motion is shown to the whole table for a moment. Ongoing schemes are shown in a row under their archenemy's seat

import { drawNamedTiles } from './planes';
import { cardImageSrc, setImage } from '../images';
import { players, stateOf, zone, type Model } from '../model';
import { byId, q, make, reducedMotion } from '../dom';
import { commandKind } from './command';
import type { CardView, PlayerView } from '../protocol';
import { t } from '../text';

const REVEAL_MS = 3000;

/** Schemes in play now that were not in play at the last check. The first check returns none, so a reload does not replay a reveal. */
function newSchemes(seen: ReadonlySet<number> | null, now: readonly number[]): number[] {
  return seen ? now.filter(k => !seen.has(k)) : [];
}

const schemesOf = (model: Model, player: PlayerView): CardView[] =>
  zone(model, player, 'Command').filter(c => commandKind(c, stateOf(model, c)) === 'scheme');

/** A player with schemes left in the scheme deck or in play is an archenemy. The server does not send the variant itself. */
export function isArchenemy(model: Model, player: PlayerView): boolean {
  return zone(model, player, 'SchemeDeck').length > 0 || schemesOf(model, player).length > 0;
}

/** Draws the ongoing schemes under a seat, in landscape as they are printed. */
export function renderOngoing(root: HTMLElement, model: Model, player: PlayerView): void {
  drawNamedTiles(root, model, schemesOf(model, player), 'scheme');
}

let seen: Set<number> | null = null;

/** Clears the seen schemes, so a new game starts again from its own first check. */
export function resetSchemes(): void {
  seen = null;
}

export function revealSchemes(model: Model): void {
  const all = players(model).flatMap(p => schemesOf(model, p).map(c => ({ card: c, owner: p })));
  const fresh = newSchemes(seen, all.map(s => s.card.$key));
  seen = new Set(all.map(s => s.card.$key));
  if (!fresh.length || reducedMotion()) return;
  const { card, owner } = all.find(s => s.card.$key === fresh[fresh.length - 1])!;
  const reveal = make('div', 'scheme-reveal');
  reveal.innerHTML = '<div class="scheme big"><img alt=""><span class="scheme-name"></span></div><p></p>';
  setImage(q<HTMLImageElement>(reveal, 'img'), cardImageSrc(model, card));
  q(reveal, '.scheme-name').textContent = stateOf(model, card).Name ?? '';
  q(reveal, 'p').textContent = owner.Name != null
    ? t('lblWebSchemesSetsInMotion', owner.Name) : t('lblWebSchemesArchenemySetsInMotion');
  byId('match').append(reveal);
  setTimeout(() => reveal.remove(), REVEAL_MS);
}
