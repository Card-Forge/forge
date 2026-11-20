// The player's hand along the bottom of the board, fanned out and sorted as the options set

import { commandKind } from './command';
import { reconcile } from './reconcile';
import { createCard, updateCard, type CardClick } from './cards';
import { deref, derefAll, game, isLocal, players, stateOf, zone, type Model } from '../model';
import { isPortrait } from '../form';
import { openedOver, sheet, swipeDown } from '../sheet';
import { changeUi, ui } from '../ui';
import { logTints } from './log';
import { setting } from '../settings';
import { byId, make, q } from '../dom';
import type { CardView, PlayerView, StackItemView, ZoneType } from '../protocol';
import { t, tCap } from '../text';
import { zoneTitle } from './zones';
import { frameColour, manaValue } from './gamerules';
import { starterShowing } from './firstplayer';

// Sorts by colour as Arena does. Each single colour comes in WUBRG order, then multicoloured, colourless, and lands last
function colourRank(colours: number, type: string): number {
  return /\bLand\b/.test(type) ? 7 : 'WUBRGMC'.indexOf(frameColour(colours, type));
}

function handOrder(model: Model, cards: CardView[]): CardView[] {
  const sort = setting('handSort');
  if (sort !== 'mana' && sort !== 'color') {
    return cards;
  }
  return [...cards].sort((a, b) => {
    const sa = stateOf(model, a);
    const sb = stateOf(model, b);
    return (sort === 'color' ? colourRank(sa.Colors ?? 0, sa.Type ?? '') - colourRank(sb.Colors ?? 0, sb.Type ?? '') : 0)
      || manaValue(sa.ManaCost) - manaValue(sb.ManaCost)
      || String(sa.Name ?? '').localeCompare(sb.Name ?? '');
  });
}

/** Cards playable from outside the hand, mapped to the zone each is really in. The engine collects them in its Flashback pseudo-zone. */
function fromElsewhere(model: Model, player: PlayerView | undefined): Map<number, ZoneType> {
  const found = new Map<number, ZoneType>();
  for (const card of zone(model, player, 'Flashback')) {
    if (card.Zone) {
      found.set(card.$key, card.Zone);
    }
  }
  return found;
}

/** Library and hand cards in the Flashback pseudo-zone that a playable list has held. */
let shownOnceListed = new Set<number>();

// The pseudo-zone also holds library and hand cards that are only revealed, and the views do not say which may be played.
// So those wait for the playable list. Cards from the other zones are there because they can be played
function inHand(model: Model, flashback: CardView[]): CardView[] {
  const playable = new Set((model.playable?.cards ?? []).map(r => r.ref));
  const listed = new Set<number>();
  // A new game reuses card keys
  if (!model.playable) shownOnceListed.clear();
  for (const c of flashback) {
    if ((c.Zone === 'Library' || c.Zone === 'Hand') && (playable.has(c.$key) || shownOnceListed.has(c.$key))) {
      listed.add(c.$key);
    }
  }
  shownOnceListed = listed;
  return flashback.filter(c => (c.Zone !== 'Library' && c.Zone !== 'Hand') || listed.has(c.$key));
}

/** The prompt open when the drawer was opened, so only a new prompt closes it. */
const openOver = openedOver();
/** The card last tapped in the drawer. The drawer opens again if that card's play is cancelled. */
let played: number | null = null;
/** The prompt for which the drawer opened by itself. */
let openedFor: object | null = null;

const closeDrawer = () => {
  played = null;
  changeUi(u => { u.handOpen = false; });
};

/** On a phone the hand is a strip of card tops. It opens into a drawer where the cards can be read and played. */
function drawerHead(root: HTMLElement): HTMLElement {
  let head = document.getElementById('hand-head');
  if (!head) {
    head = Object.assign(document.createElement('div'), { id: 'hand-head' });
    head.innerHTML = '<b></b><span class="on-stack"></span><button class="close"></button>';
    q(head, '.close').textContent = t('lblClose');
    q(head, '.close').onclick = closeDrawer;
    root.before(head);
    swipeDown(head, closeDrawer);
    root.addEventListener('click', e => {
      if (!isPortrait()) return;
      if (ui.handOpen) {
        played = Number((e.target as Element).closest<HTMLElement>('.card')?.dataset.key ?? NaN);
        return;
      }
      // The strip is too small to pick a card from, so a tap anywhere on it opens the drawer
      e.stopPropagation();
      played = null;
      changeUi(u => { u.handOpen = true; });
    }, true);
  }
  return head;
}

/** Whether the hand shows as the open drawer this frame. It closes the drawer for a new prompt and opens it again for a cancelled play. */
function drawDrawer(root: HTMLElement, model: Model, held: CardView[], count: number): boolean {
  const head = drawerHead(root);
  const portrait = isPortrait();
  const asked = model.prompt;
  // A prompt answered from the hand, such as a discard or the mulligan before the first turn, needs the hand shown, after who goes first
  const dealt = !game(model)?.Turn && held.length > 0 && !!asked && (asked.ok.enabled || asked.cancel.enabled) && !starterShowing();
  const fromHand = portrait && !!asked && (dealt || (!asked.priority && !asked.paying && asked.selectableMin > 0
    && asked.selectable.some(r => held.some(c => c.$key === r.ref))));
  if (fromHand) {
    if (asked !== openedFor) {
      openedFor = asked;
      ui.handOpen = true;
      // The drawer opened for this prompt. openOver is reset so it records this prompt as the one the drawer opened over
      openOver.note(false, null);
      played = null;
    }
  } else if (openedFor) {
    // A drawer opened for a prompt closes when that prompt ends
    openedFor = null;
    ui.handOpen = false;
  } else if (ui.handOpen && (model.requests.size > 0 || (openOver.seen && asked !== openOver.seen && asked && !asked.priority))) {
    // The tapped card's next prompt is answered on the board, in a menu or in a dialog. So the drawer closes
    ui.handOpen = false;
  } else if (!ui.handOpen && played !== null && asked?.priority) {
    ui.handOpen = portrait && held.some(c => c.$key === played);
    played = null;
  }
  openOver.note(ui.handOpen, asked);
  const open = portrait && ui.handOpen;
  root.classList.toggle('sheet', open);
  root.classList.toggle('strip', portrait && !open);
  head.hidden = !open;
  sheet('hand', open, closeDrawer);
  if (portrait) root.setAttribute('aria-label', t('lblWebPortraitHandTitle', count));
  else root.removeAttribute('aria-label');
  // The drawer scrolls, but the strip must not keep the drawer's scroll position
  if (!open) {
    root.scrollTop = 0;
    return false;
  }
  q(head, 'b').textContent = `${tCap('lblHandZone')} ${count}`;
  // The top of the stack is named here, because the drawer covers the stack
  const top = (derefAll(model, game(model)?.Stack) as StackItemView[])[0];
  const source = top ? deref(model, top.SourceCard) as CardView | undefined : undefined;
  q(head, '.on-stack').textContent = top ? `${source ? stateOf(model, source).Name ?? '' : ''} (${deref(model, top.ActivatingPlayer)?.Name ?? ''})` : '';
  head.style.bottom = `${innerHeight - root.getBoundingClientRect().top - head.offsetHeight}px`;
  return true;
}

export function renderHand(model: Model, player: PlayerView | undefined, select: CardClick): void {
  const root = byId('hand');
  const elsewhere = fromElsewhere(model, player);
  const tints = logTints(players(model).map(p => ({ name: p.Name ?? '', local: isLocal(model, p) })));
  // The planar die has its own button next to the plane, so it is not laid out with the cards
  const others = inHand(model, zone(model, player, 'Flashback').filter(c => commandKind(c, stateOf(model, c)) !== 'dice'));
  // The hand's list can be behind its cards. A card being cast is already on the stack and hidden while the list still holds it
  const held = zone(model, player, 'Hand').filter(c => !c.Zone || c.Zone === 'Hand');
  const cards = [...handOrder(model, others), ...handOrder(model, held)];
  reconcile(root, cards, c => c.$key, () => createCard(select), (el, c) => {
    updateCard(el, model, c);
    const source = elsewhere.get(c.$key);
    el.classList.toggle('elsewhere', source !== undefined);
    // The preview shows the zone's name when the card is hovered
    if (source) {
      el.dataset.from = source;
    } else {
      delete el.dataset.from;
    }
    // Another player's card that you may play shows its owner, so it is not mistaken for one of yours
    const owner = deref(model, c.Owner);
    const foreign = !!owner && !!player && owner.$key !== player.$key;
    el.classList.toggle('foreign', foreign);
    const band = q(el, '.owned-by');
    band.replaceChildren();
    if (foreign) {
      const label = make('small', '');
      label.textContent = source ? zoneTitle(source) : '';
      band.append(label, owner?.Name ?? '');
    }
    if (foreign) el.style.setProperty('--owner-tint', tints.find(t => t.name === owner?.Name)?.colour ?? 'var(--muted)');
    // A card another player may look at has been revealed to them
    el.classList.toggle('revealed', (c.PlayerMayLook ?? []).some(r => !!r && !model.localPlayers.includes(r.ref)));
  });
  if (drawDrawer(root, model, held, cards.length)) {
    return;
  }
  fanOut(root);
}

/** Lays a hand's cards out as a fan that fits the element's width, as your own hand is drawn. */
export function fanOut(root: HTMLElement): void {
  const cards = [...root.children];
  const cardWidth = (root.firstElementChild as HTMLElement | null)?.offsetWidth ?? 0;
  // The hand's padding keeps space clear for the prompt beside it, so that space is not used for cards
  const style = getComputedStyle(root);
  const width = root.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
  // Each card covers a fifth of the one before it, and more when the row would overflow
  const step = cards.length > 1 ? Math.min(-0.2 * cardWidth, (width - 16 - cards.length * cardWidth) / (cards.length - 1)) : 0;
  root.style.setProperty('--step', `${step}px`);
  // At most 3 degrees per card from the middle, 12 at the ends
  const mid = (cards.length - 1) / 2;
  const perCard = mid > 0 ? Math.min(3, 12 / mid) : 0;
  // Each card drops by the amount its tilt would move it if the whole hand rotated around one point below the screen
  const radius = perCard > 0 ? (cardWidth + step) / Math.sin(perCard * Math.PI / 180) : 0;
  cards.forEach((child, i) => {
    const tilt = (i - mid) * perCard;
    const el = child as HTMLElement;
    el.style.setProperty('--tilt', `${tilt}deg`);
    el.style.setProperty('--drop', `${radius * (1 - Math.cos(tilt * Math.PI / 180))}px`);
  });
}

export function resetHand(): void {
  played = null;
  openedFor = null;
  openOver.note(false, null);
}
