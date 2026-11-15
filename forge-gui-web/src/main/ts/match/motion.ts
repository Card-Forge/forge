// Animates each cardMoved event. A card moves from where it was before the board was redrawn to where it is now

import { hoverable } from '../preview';
import type { CardMoved, GameEvent, Place, CardView, PlayerView } from '../protocol';
import { cssUrl, playerSleeveUrl } from '../avatarsleeves';
import { echoSound } from '../audio';
import { unmake } from './leaving';
import type { Model } from '../model';
import { make, reducedMotion, easeOut } from '../dom';
import { cardImageSrc } from '../images';
import { lastPicture, TAPPED_SCALE } from './cards';
import { coinInAir } from './coin';

/** Where a card was drawn and its old element, so a trip can still run after the element has left the page. */
interface Snapshot {
  rect: DOMRect;
  ghost: HTMLElement | null;
  /** Its centre in the layout, before any transform. A tilt or an attacker's step forward does not change it. */
  laid?: { x: number; y: number };
  /** Its size in the layout, before any transform, so a copy of a tapped card is rotated as the card was. */
  size?: { w: number; h: number };
  /** How far a slide or flight in progress has moved it from its resting place. */
  drift?: { x: number; y: number };
  /** Whether it was tapped. It is stored because the ghost is the live element and keeps changing after the snapshot. */
  tapped?: boolean;
}

/** A spell waiting for its cost to be paid, and its flight to the place where it waits. */
interface Waiting {
  rect: DOMRect;
  ghost: HTMLElement | null;
  since: number;
  /** Where the flight to the waiting place started. It is used when the spell is paid for before the flight ends. */
  from?: DOMRect;
}

/** Every card drawn just before the board was redrawn, by key. This covers the hand, board, stack, open zones and zone tile tops. */
const lastSeen = new Map<string, Snapshot>();
/** Spells on the stack that wait for their cost, by card key, with the place each flies to. It holds any player's spells. stack.ts draws your own in their place. */
const waiting = new Map<string, Waiting>();
const fromHint = new Map<string, DOMRect>();
const intoHint = new Map<string, DOMRect>();

/** Where a card will rest when its flight ends. It is stored because a flight's transform changes what the element measures. */
const resting = new Map<string, DOMRect>();

/** Whether a card is in the middle of a move. An endless animation, such as a flyer's hover, does not count. */
function moving(el: HTMLElement): boolean {
  return el.getAnimations().some(a => a.effect?.getTiming().iterations !== Infinity);
}

function restingRect(el: HTMLElement): DOMRect {
  const key = el.dataset.key as string;
  const held = resting.get(key);
  if (held && moving(el)) {
    return held;
  }
  resting.delete(key);
  return el.getBoundingClientRect();
}

/** The usual time for a card's flight between places, such as a play, a draw, a discard or a paid spell's landing. */
const FLIGHT_MS = 500;
/** How long a card put onto the battlefield from the hand takes, such as a land or a permanent that resolves at once. */
const PLAY_MS = 260;
/** How long a move within the battlefield takes, such as a tapped land moving from one pile to another. */
const SHIFT_MS = 340;
/** Cards that move together (a deal, a mulligan, a discard) start this far apart, so the eye can follow each one. */
const STAGGER_MS = 110;
/** A mulligan moves one hand out and another hand in. So each card's flight and the gap between cards are shorter. */
const SWAP_MS = 350;
const SWAP_STAGGER_MS = 70;
/** How long a spell can wait after no cost is being paid for it. After this its stack item is not coming. The stack's placeholder uses the same limit. */
export const SETTLE_MS = 900;
const POP_MS = 220;
const EASE = 'cubic-bezier(.2,.7,.3,1)';
/** The id of a card's own flight animation, so a second flight replaces the first instead of adding to it. */
const FLIGHT = 'flight';

/** The cards of a pile that was just spread out all start from where the pile was. */
export function spreadFrom(keys: string[], rect: DOMRect): void {
  for (const key of keys) {
    fromHint.set(key, rect);
  }
}

/** The cards of a pile being merged back all end where its top card is. */
export function mergeInto(keys: string[], rect: DOMRect): void {
  for (const key of keys) {
    intoHint.set(key, rect);
  }
}

/** Set for a new table. Its cards reuse the old table's keys, so the old positions say nothing about where they come from. */
let fresh = false;
/** The opening hand's cards and where each is dealt from. They are dealt once who goes first has been shown. */
let heldDeal: { key: string; start: DOMRect }[] = [];

export function resetMotion(): void {
  for (const held of waiting.values()) {
    held.ghost?.remove();
  }
  waiting.clear();
  heldDeal = [];
  resting.clear();
  fromHint.clear();
  intoHint.clear();
  fresh = true;
}

/** Records where every card is just before the board is redrawn, because the board can also move between frames without an event. */
export function noteBoard(): void {
  if (fresh) {
    fresh = false;
    lastSeen.clear();
    ownPlace.clear();
    covered = new Set();
    lastShape = '';
    lastHand = '';
    return;
  }
  notePiles();
  note();
}

/** Animates what happened since the last frame. It runs after the board is drawn, so both ends of each move can be measured. */
export function animateCardMoves(model: Model, events: readonly GameEvent[]): void {
  const dealt: { el: HTMLElement; start: DOMRect }[] = [];
  const leavingHand: { was: Snapshot; target: DOMRect | null; tile?: boolean; key?: string; onto?: HTMLElement | null }[] = [];
  const trips = journeys(events);
  // A card put back into the zone it is shown in only changed place there, so it slides. An example is a looked-at card put back on the library
  for (const [key, move] of trips) {
    const sameZone = move.from?.zone === move.to?.zone && move.from?.player?.ref === move.to?.player?.ref && !move.via;
    if (sameZone && lastSeen.get(key)?.ghost?.closest('#zones') && cardElement(key)?.closest('#zones')) trips.delete(key);
  }
  // Hand icons that already swell this frame, so several cards drawn at once swell an icon only once
  const swelled = new Set<HTMLElement>();
  // The zone tiles that cards land on this frame, with the number of cards and the time the first one arrives
  const landings = new Map<HTMLElement, { cards: number; at: number }>();
  let fromLibrary = 0;
  /** How long until the last permanent being unmade in place is gone, so its row does not close up before then. */
  let unmaking = 0;
  for (const [key, move] of trips) {
    const seen = lastSeen.get(key);
    const start = waiting.get(key)?.rect ?? seen?.rect ?? placeRect(move.from);
    // A spell on the stack with no item yet is being paid for. It flies to the slot the stack keeps for it
    const slot = move.to?.zone === 'Stack' ? awaitingSlot(key) : null;
    if (slot) {
      const to = restingRect(slot);
      waiting.set(key, { rect: to, ghost: null, since: Date.now(), from: start ?? undefined });
      if (start) {
        fly(slot, start, FLIGHT_MS, 0);
      }
      continue;
    }
    // A stack item has its source card's key. So only a card that moved to the stack is looked for there, not a card whose ability is on it
    const el = move.to?.zone === 'Stack' ? elementFor(key) : cardElement(key) ?? pileTopFor(key);
    if (move.to?.zone === 'Stack' && !el) {
      if (start) {
        hold(key, start, seen ?? null);
      }
      continue;
    }
    // A card that moved away and straight back (a mulligan's shuffle and redraw) makes both trips
    if (el && move.via && seen?.ghost) {
      const away = tileImageRect(key) ?? placeRect(move.via);
      if (move.from?.zone === 'Hand') leavingHand.push({ was: seen, target: away });
      else if (away) sendTo(seen, away, 0.25);
      const back = away ?? start;
      if (back) {
        if (move.via.zone === 'Library' && move.to?.zone === 'Hand') dealt.push({ el, start: back });
        // A card that leaves and returns in one update flies out first and then back
        else fly(el, back, FLIGHT_MS, away ? FLIGHT_MS : 0);
      }
      continue;
    }
    if (el) {
      const drawn = move.from?.zone === 'Library' && move.to?.zone === 'Hand';
      // A card put straight down from the hand, such as a land, has a short trip, so its flight is quicker
      const ms = move.from?.zone === 'Hand' && move.to?.zone === 'Battlefield' ? PLAY_MS : FLIGHT_MS;
      land(key);
      // Flying a pile's top element would move the whole pile, so a copy of the joining card flies instead
      const pile = el.closest<HTMLElement>('.slot[data-members]');
      if (start && pile && (pile.dataset.members ?? '').split(',').length > 1) {
        const to = restingRect(el);
        openPile(el);
        sendTo({ ...(seen ?? { ghost: el, size: { w: el.offsetWidth, h: el.offsetHeight } }), rect: start }, to, 1, 0, el, ms);
        holdCount(el, ms);
        continue;
      }
      if (start) {
        if (drawn) dealt.push({ el, start });
        else fly(el, start, ms, 0);
      } else if (pile && (pile.dataset.members ?? '').split(',').length > 1) {
        // A token joins a pile of the same tokens. A copy grows onto the pile while the pile opens
        const to = restingRect(el);
        openPile(el);
        const copy = place(el, to, { w: el.offsetWidth, h: el.offsetHeight });
        pop(copy);
        copy.getAnimations()[0]?.finished.then(() => copy.remove(), () => copy.remove());
        holdCount(el, POP_MS);
      } else {
        // A token, or any other card that appears from nowhere, grows into place instead of appearing at once
        pop(el);
      }
      continue;
    }
    // A place not drawn card by card (a library, a graveyard tile, an opponent's hand) has no element, so a copy makes the trip
    let ghost = waiting.get(key)?.ghost ?? seen?.ghost ?? null;
    let was = waiting.get(key)?.ghost ? { rect: waiting.get(key)!.rect, ghost, size: seen?.size } : seen;
    // Another player's hand is only an icon. So a card drawn into it makes no trip, and the icon swells once instead
    const intoFan = move.from?.zone === 'Library' && move.to?.zone === 'Hand' ? handFanOf(move.to) : null;
    if (!ghost && intoFan) {
      if (!swelled.has(intoFan)) {
        swelled.add(intoFan);
        intoFan.animate([{ scale: '1' }, { scale: '1.28', offset: 0.35 }, { scale: '1' }], { duration: 480, easing: EASE });
      }
      land(key);
      continue;
    }
    // The tile the card lands on swells when the card arrives, or at once when no flight to it is shown
    const onto = landedTile(model, key, move.to);
    let arrives = 0;
    if (!ghost && start && move.from?.zone === 'Library') {
      // The library shows no card to copy, so a card back in the owner's sleeve flies instead. Several cards start staggered
      const target = tileImageRect(key) ?? placeRect(move.to);
      if (target) {
        arrives = FLIGHT_MS + fromLibrary * STAGGER_MS;
        if (tileImageRect(key)) holdTile(key, fromLibrary * STAGGER_MS);
        sendTo({ rect: start, ghost: cardBack(model, key) }, target, 1, fromLibrary++ * STAGGER_MS);
      }
    }
    if (fizzled.has(key)) {
      // A spell that failed breaks apart on the stack (stack.ts), so nothing flies. Its tile waits and then shows it
      if (tileImageRect(key)) holdTile(key, FIZZLE_MS - FLIGHT_MS);
      arrives = FIZZLE_MS;
    } else if (!ghost && start && move.from?.zone !== 'Library' && (tileImageRect(key) ?? placeRect(move.to))) {
      // A spell on the stack, a tile's top card and a card in a hand drawn as an icon have no element to copy, so one is made
      ghost = standIn(model, key);
      const h = start.height, w = Math.min(start.width, h * CARD_ASPECT);
      was = { rect: start, ghost, size: { w, h } };
    }
    if (ghost && start && was) {
      const tile = tileImageRect(key);
      const target = tile ?? placeRect(move.to);
      if (move.from?.zone === 'Hand') {
        leavingHand.push({ was: { ...was, rect: start }, target, tile: !!tile, key, onto });
        land(key);
        continue;
      } else {
        // A destroyed or exiled permanent is unmade in place. The tile's picture of it stays hidden until it is gone
        const leaving = move.from?.zone === 'Battlefield' && (move.to?.zone === 'Graveyard' || move.to?.zone === 'Exile')
          ? unmake(ghost, start, was.size, !!was.tapped, move.to.zone) : 0;
        // A card landing on a zone tile ends on the tile's picture, which stays hidden until the card arrives
        if (tile) holdTile(key, leaving ? leaving - FLIGHT_MS : 0);
        unmaking = Math.max(unmaking, leaving);
        if (!leaving) sendTo({ ...was, rect: start }, target ?? start, tile ? 1 : target ? 0.25 : 0);
        arrives = leaving || FLIGHT_MS;
      }
    }
    swell(onto, arrives, landings);
    land(key);
  }
  if (document.getElementById('match')?.classList.contains('starting')) {
    // The opening hand is drawn while who goes first is shown, so its cards wait hidden (board.css). Their art is decoded meanwhile
    for (const { el, start } of dealt.splice(0)) {
      heldDeal.push({ key: el.dataset.key ?? '', start });
      el.querySelector<HTMLImageElement>(':scope > img')?.decode().catch(() => {});
    }
  } else {
    for (const { key, start } of heldDeal.splice(0)) {
      const el = cardElement(key);
      if (el?.closest('#hand, .mate-hand')) dealt.push({ el, start });
    }
  }
  // In a mulligan the old hand leaves before the first new card starts. Both hands move faster
  const swap = dealt.length > 0 && leavingHand.length > 0;
  const ms = swap ? SWAP_MS : FLIGHT_MS;
  const apart = swap ? SWAP_STAGGER_MS : STAGGER_MS;
  const afterLeaving = swap ? (leavingHand.length - 1) * apart + ms : 0;
  // Cards drawn together land from left to right, in the order the hand's sort puts them
  dealt.sort((a, b) => restingRect(a.el).left - restingRect(b.el).left).forEach(({ el, start }, i) =>
    fly(el, start, ms, afterLeaving + i * apart));
  // The host's draw sound plays as the first card starts. Each later card plays its own sound when it starts
  echoSound('draw', dealt.slice(swap ? 0 : 1).map((_, i) => afterLeaving + (swap ? i : i + 1) * apart));
  // Cards leaving the hand together (a mulligan, a discard) go one after another from the right, the way a deal arrives
  leavingHand.sort((a, b) => b.was.rect.left - a.was.rect.left).forEach(({ was, target, tile, key, onto }, i) => {
    swell(onto, i * apart + ms, landings);
    if (tile) holdTile(key, i * apart);
    sendTo(was, target ?? was.rect, tile ? 1 : target ? 0.25 : 0, i * apart, undefined, ms);
  });
  holdCounts(landings);
  fizzled.clear();
  // A library shuffled twice in one update riffles once
  for (const player of new Set(events.flatMap(e => e.kind === 'shuffled' ? [e.player.ref] : []))) riffle(player);
  // A cancelled cast that flew back from beside the stack has travelled, so it does not also fly out of its zone's tile
  const travelled = new Set([...trips.keys(), ...settleWaiting(!!model.prompt?.paying)]);
  arriveElsewhere(travelled);
  shiftBoard(travelled, unmaking);
  shiftHand(travelled);
  shiftZones(travelled);
  layOutPiles();
}

/** Slides cards that changed place on the battlefield without a game event. It waits until any card being unmade there is gone. */
function shiftBoard(travelled: Set<string>, wait: number): void {
  // Only a change in what the rows hold slides cards. Otherwise a resize would make every card on the table slide
  if (boardShape() === lastShape) {
    return;
  }
  // A card folded into a pile slides onto the pile's top card. The pile is drawn in its old shape and then opens
  const joining: { was: Snapshot; top: HTMLElement }[] = [];
  const newcomers = new Set<string>();
  for (const { keys, top } of pileSlots()) {
    for (const key of keys) {
      const was = lastSeen.get(key);
      if (was && ownPlace.has(key) && key !== top.dataset.key && !travelled.has(key) && !intoHint.has(key)) {
        joining.push({ was, top });
      }
    }
    // When the pile's top card is the one that just joined, the pile stays in place and a copy of that card flies in
    const topKey = top.dataset.key as string;
    const topWas = lastSeen.get(topKey);
    // Compare with a card that was already in the pile. A card arriving this frame started from somewhere else
    const stayed = keys.filter(k => k !== topKey && !travelled.has(k)).map(k => lastSeen.get(k)).find(s => s);
    if (topWas && stayed && !travelled.has(topKey) && !intoHint.has(topKey) && apart(topWas.rect, stayed.rect)) {
      joining.push({ was: topWas, top });
      newcomers.add(topKey);
      // Its own transition (a tap) would play at the pile while the copy is still flying, so it is finished at once
      for (const a of top.getAnimations()) {
        if (a instanceof CSSTransition) a.finish();
      }
    }
  }
  // Measure before the piles are reset to their old shape. This is where each top card will be once its pile has opened
  const tops = joining.map(({ top }) => top.getBoundingClientRect());
  for (const { top } of joining) openPile(top);
  slide(document.querySelectorAll<HTMLElement>(BOARD_CARDS), new Set([...travelled, ...newcomers]), SHIFT_MS, wait);
  joining.forEach(({ was, top }, i) => sendTo(was, tops[i], 1, 0, top, SHIFT_MS));
}

function apart(a: DOMRect, b: DOMRect): boolean {
  return Math.hypot(a.left + a.width / 2 - b.left - b.width / 2, a.top + a.height / 2 - b.top - b.height / 2) > 4;
}

/** Cards left in an open zone, such as after one is picked in a search, slide into the gap instead of jumping. */
function shiftZones(travelled: Set<string>): void {
  if (zonesShape() === lastZones) {
    return;
  }
  slide(document.querySelectorAll<HTMLElement>(ZONE_CARDS), travelled);
}

/** Cards staying in the hand slide to their new places when the hand's order changes around them. */
function shiftHand(travelled: Set<string>): void {
  if (handShape() === lastHand) {
    return;
  }
  slide(document.querySelectorAll<HTMLElement>(HAND_CARDS), travelled);
}

function slide(cards: Iterable<HTMLElement>, travelled: Set<string>, duration = FLIGHT_MS, delay = 0): void {
  for (const el of cards) {
    const key = el.dataset.key as string;
    const was = lastSeen.get(key);
    if (!was?.laid || travelled.has(key) || fromHint.has(key)) {
      continue;
    }
    // Compare layout positions, because a transform in progress (an attacker stepping forward) is not a move
    const laid = laidCentre(el);
    // A card redrawn as a new element starts where the old element was shown mid-slide, not at its resting place
    const carried = was.ghost !== el ? was.drift : undefined;
    const dx = was.laid.x + (carried?.x ?? 0) - laid.x;
    const dy = was.laid.y + (carried?.y ?? 0) - laid.y;
    if (Math.abs(dx) + Math.abs(dy) > 2) {
      // During the slide the card measures near its old place, so its resting place is stored for any later flight
      resting.set(key, restingRect(el));
      // Animate translate, because animating transform would override the transition that rotates a card as it taps
      el.animate([{ translate: `${dx}px ${dy}px` }, { translate: '0px 0px' }],
        { duration, delay, easing: EASE, composite: 'add', fill: 'backwards' });
    }
    if (was.ghost && was.ghost !== el && el.closest('.battlefield')) {
      morph(el, was.tapped ?? was.ghost.classList.contains('tapped'), was.size);
    }
  }
}

/** How long a redrawn card takes to rotate or resize from its old element's look. It is a little longer than a tap's rotation. */
const MORPH_MS = 220;

/** Starts a card redrawn as a new element from the old element's rotation and size. The new element has no earlier state to transition from. */
function morph(el: HTMLElement, wasTapped: boolean, oldSize: { w: number; h: number } | undefined): void {
  const isTapped = el.classList.contains('tapped');
  const turn = (wasTapped ? 90 : 0) - (isTapped ? 90 : 0);
  const size = (oldSize?.w && el.offsetWidth ? oldSize.w / el.offsetWidth : 1) * (wasTapped ? TAPPED_SCALE : 1) / (isTapped ? TAPPED_SCALE : 1);
  if (!turn && Math.abs(size - 1) < 0.02) return;
  el.animate([{ rotate: `${turn}deg`, scale: String(size) }, { rotate: '0deg', scale: '1' }], { duration: MORPH_MS, easing: EASE });
}

/** A card playable from another zone joins the hand strip without a move event. So it flies out of that zone's tile. */
function arriveElsewhere(travelled: Set<string>): void {
  if (!lastSeen.size) {
    return;
  }
  for (const el of document.querySelectorAll<HTMLElement>('#hand .card.elsewhere[data-key]')) {
    const key = el.dataset.key as string;
    if (travelled.has(key) || lastSeen.get(key)?.ghost?.closest('#hand')) {
      continue;
    }
    const tile = lastSeen.get(key)?.rect
      ?? document.querySelector('#me .zone-tile[data-zone="' + el.dataset.from + '"]')?.getBoundingClientRect();
    if (tile?.width) {
      fly(el, tile, FLIGHT_MS, 0);
    }
  }
}

/** Where the page layout puts an element's centre. Offsets ignore every transform, but a bounding box does not. */
function laidCentre(el: HTMLElement): { x: number; y: number } {
  let x = el.offsetWidth / 2;
  let y = el.offsetHeight / 2;
  for (let at: HTMLElement | null = el; at; at = at.offsetParent as HTMLElement | null) {
    x += at.offsetLeft - (at.offsetParent?.scrollLeft ?? 0);
    y += at.offsetTop - (at.offsetParent?.scrollTop ?? 0);
  }
  return { x, y };
}

/** A card's trip this frame: where it started, where it ended, and the zone where it turned back if it came back. */
type Journey = CardMoved & { via?: Place };

/** Each card's first origin and last destination this frame. It also has the zone where the card turned back if it left and came back. */
export function journeys(events: readonly GameEvent[]): Map<string, Journey> {
  const out = new Map<string, Journey>();
  for (const e of events) {
    if (e.kind !== 'cardMoved') {
      continue;
    }
    const key = String(e.card.ref);
    const earlier = out.get(key);
    if (!earlier) {
      out.set(key, e);
      continue;
    }
    const home = earlier.from?.zone === e.to?.zone && earlier.from?.player?.ref === e.to?.player?.ref;
    out.set(key, { ...e, from: earlier.from, via: home ? earlier.to ?? earlier.via : undefined });
  }
  return out;
}

// Forge undoes a cancelled cast without an event. So a waiting card that is drawn in a zone again flies back from beside the stack.
// Returns the cards that flew back.
function settleWaiting(paying: boolean): string[] {
  const flown: string[] = [];
  for (const [key, held] of waiting) {
    const arrived = stackItemFor(key) ?? cardElement(key);
    if (arrived) {
      const from = whereNow(held);
      land(key);
      fly(arrived, from, FLIGHT_MS, 0);
      flown.push(key);
    } else if (!awaitingSlot(key) && !paying && Date.now() - held.since > SETTLE_MS) {
      land(key);
    }
  }
  return flown;
}

/** Where a waiting spell is drawn now. While it is still flying, this is short of its waiting place. */
function whereNow(held: Waiting): DOMRect {
  if (held.ghost?.isConnected) {
    return held.ghost.getBoundingClientRect();
  }
  const t = Math.min(1, (Date.now() - held.since) / FLIGHT_MS);
  if (!held.from || t >= 1) {
    return held.rect;
  }
  // The flight eases out, so it covers most of the distance early
  const p = easeOut(t);
  const mix = (a: number, b: number) => a + (b - a) * p;
  return new DOMRect(mix(held.from.left, held.rect.left), mix(held.from.top, held.rect.top),
    mix(held.from.width, held.rect.width), mix(held.from.height, held.rect.height));
}

// The player opens or closes a pile, not the game. So it is animated from the hints stored by spreadFrom and mergeInto
function layOutPiles(): void {
  for (const [key, from] of fromHint) {
    const el = cardElement(key);
    if (el) {
      fly(el, from, FLIGHT_MS, 0);
    }
  }
  for (const [key, into] of intoHint) {
    const was = lastSeen.get(key);
    if (was?.ghost && !cardElement(key)) {
      sendTo(was, into, 1);
    }
  }
  fromHint.clear();
  intoHint.clear();
}

const CARDS = '#me .card[data-key], #opponent .card[data-key], #hand .card[data-key], #zones .card[data-key]';
const BOARD_CARDS = '#me .battlefield .card[data-key], #opponent .battlefield .card[data-key]';
const HAND_CARDS = '#hand .card[data-key]';
const ZONE_CARDS = '#zones .card[data-key]';
const stackItems = () => [...document.querySelectorAll<HTMLElement>('#stack .stack-item:not(.awaiting)')];
const awaitingSlot = (key: string) =>
  document.querySelector<HTMLElement>(`#stack .stack-item.awaiting img[data-key="${key}"]`)?.parentElement ?? null;

export function cardElement(key: string): HTMLElement | null {
  return document.querySelector<HTMLElement>(`#me .card[data-key="${key}"], #opponent .card[data-key="${key}"], `
    + `#hand .card[data-key="${key}"], #zones .card[data-key="${key}"]`);
}

// A stack item is keyed by the item, not the card, so the card's key is read from its picture
function stackItemFor(key: string): HTMLElement | null {
  return stackItems().find(el => el.querySelector('img')?.dataset.key === key) ?? null;
}

function elementFor(key: string): HTMLElement | null {
  return cardElement(key) ?? stackItemFor(key) ?? pileTopFor(key);
}

export function pileTopFor(key: string): HTMLElement | null {
  for (const { keys, top } of pileSlots()) {
    if (keys.includes(key)) {
      return top;
    }
  }
  return null;
}

/** A face-down card in its owner's sleeve. It is used for a card that no one can see. */
function cardBack(model: Model, key: string): HTMLElement {
  const card = model.objects.get(Number(key)) as CardView | undefined;
  const owner = card?.Owner ? model.objects.get(card.Owner.ref) as PlayerView | undefined : undefined;
  const back = make('div', 'card back');
  back.style.setProperty('--sleeve', cssUrl(playerSleeveUrl(owner)));
  return back;
}

/** Opens a pile from its old depth and width while the joining card flies, so nothing moves once it lands (board.css). */
function openPile(top: HTMLElement): void {
  const slot = top.closest<HTMLElement>('.slot');
  if (!slot) return;
  const now = Math.min(3, Math.max(0, (slot.dataset.members ?? '').split(',').length - 1));
  const was = Math.max(0, now - 1);
  if (now === was) return;
  top.dataset.heldDepth = String(was);
  slot.style.setProperty('--held-behind', String(was));
  slot.classList.add('holding');
  // Lay it out once in the old shape, so the width and the new edge animate from there
  void slot.offsetWidth;
  delete top.dataset.heldDepth;
  slot.classList.replace('holding', 'opening');
  top.classList.add('grown');
  setTimeout(() => {
    slot.classList.remove('opening');
    top.classList.remove('grown');
  }, 300);
}

/** Hides a pile's count until the copy joining it arrives, then shows the new count. */
function holdCount(el: HTMLElement, duration: number): void {
  el.querySelector<HTMLElement>('.count')?.animate([{ opacity: 0 }, { opacity: 0, offset: 0.9 }, { opacity: 1 }], { duration, easing: 'linear' });
}

/** Keeps a zone tile's picture of a card hidden until the copy flying to it arrives. */
function holdTile(key: string | undefined, delay: number): void {
  const img = key ? document.querySelector<HTMLElement>(`.zone-tile img[data-key="${key}"]`) : null;
  img?.animate([{ opacity: 0 }, { opacity: 0, offset: 0.92 }, { opacity: 1 }], { duration: FLIGHT_MS + delay, easing: 'linear' });
}

/** The zone tile a card has landed on. A card that went anywhere else has none. A token has none either, because it no longer exists. */
function landedTile(model: Model, key: string, to: Place | undefined): HTMLElement | null {
  const card = model.objects.get(Number(key)) as CardView | undefined;
  if (!to?.player || card?.Zone !== to.zone) {
    return null;
  }
  return document.querySelector<HTMLElement>(`.seat[data-player="${to.player.ref}"] .zone-tile[data-zone="${to.zone}"]`);
}

/** Swells a zone tile once when a card lands on it, however many cards land together. */
function swell(tile: HTMLElement | null | undefined, delay: number, landings: Map<HTMLElement, { cards: number; at: number }>): void {
  if (!tile) {
    return;
  }
  const landing = landings.get(tile);
  if (landing) {
    landing.cards++;
    return;
  }
  landings.set(tile, { cards: 1, at: delay });
  tile.animate([{ scale: '1' }, { scale: '1.12', filter: 'brightness(1.5)', offset: 0.35 }, { scale: '1' }], { duration: 480, delay, easing: EASE });
}

/** Leaves the cards still flying to a tile out of its count until the first card of the batch lands. The tile's picture is held back the same way. */
function holdCounts(landings: Map<HTMLElement, { cards: number; at: number }>): void {
  for (const [tile, { cards, at }] of landings) {
    if (!at || tile.dataset.count === undefined) continue;
    tile.dataset.pending = String(Number(tile.dataset.pending ?? 0) + cards);
    showCount(tile);
    setTimeout(() => {
      tile.dataset.pending = String(Math.max(0, Number(tile.dataset.pending ?? 0) - cards));
      showCount(tile);
    }, at);
  }
}

/** Writes a zone tile's count, minus any cards that have not landed on it yet. The board is redrawn many times during one flight. */
export function showCount(tile: HTMLElement): void {
  const count = tile.querySelector<HTMLElement>('.zone-count');
  if (count) count.textContent = String(Math.max(0, Number(tile.dataset.count ?? 0) - Number(tile.dataset.pending ?? 0)));
}

/** Spells that failed on the stack this frame, by card key (stack.ts). */
const fizzled = new Set<string>();
/** How long a spell that failed takes to break apart on the stack. */
export const FIZZLE_MS = 520;
const CARD_ASPECT = 5 / 7;

export function noteFizzled(key: string): void {
  fizzled.add(key);
}

/** A card element made for a trip when nothing on the page can be copied. It shows the card's picture if the viewer may see it, or else its back. */
function standIn(model: Model, key: string): HTMLElement {
  const card = model.objects.get(Number(key)) as CardView | undefined;
  const src = lastPicture(Number(key))?.src || (card ? cardImageSrc(model, card) : '');
  if (!src) {
    return cardBack(model, key);
  }
  const el = make('div', 'card');
  const img = document.createElement('img');
  img.src = src;
  img.alt = '';
  el.append(img);
  return el;
}

const RIFFLE_MS = 580;

/**
 * Riffles a library's tile when the library is shuffled, as a deck is shuffled on a table. Two halves turn on their top inner
 * corners into a V, the cards drop back one at a time from alternate sides, and the halves are pushed square.
 * The tile clips its contents, so copies of its sleeve move above the board.
 */
function riffle(player: number): void {
  const tile = document.querySelector<HTMLElement>(`.seat[data-player="${player}"] .zone-tile[data-zone="Library"]`);
  const rect = tile?.getBoundingClientRect();
  // The opening shuffle comes while the board fades in and the coin is tossed. Its copies sit above the board, so they would show first
  if (!tile || !rect?.width || reducedMotion() || coinInAir() || boardHidden()) return;
  const sleeve = getComputedStyle(tile).getPropertyValue('--sleeve');
  const cards = 12, open = 150, firstDrop = 190, step = 24, settle = 80;
  const at = (ms: number) => ms / RIFFLE_MS;
  for (let i = 0; i < cards; i++) {
    const side = i % 2 ? 1 : -1;
    const copy = make('div', 'zone-tile back');
    copy.style.cssText = `position: fixed; left: ${rect.left}px; top: ${rect.top}px; width: ${rect.width}px; height: ${rect.height}px;`
      + `z-index: 40; pointer-events: none; box-shadow: 0 1px 2px #0009; transform-origin: ${side < 0 ? '100%' : '0%'} 0%;`;
    copy.style.setProperty('--sleeve', sleeve);
    document.body.append(copy);
    const held = { translate: `${side * 0.34 * rect.width}px ${2 - Math.floor(i / 2) * 0.6}px`, rotate: `${side * 14}deg` };
    const flat = { translate: '0 0', rotate: '0deg' };
    const drop = firstDrop + i * step;
    copy.animate([
      { ...flat, easing: EASE },
      { ...held, offset: at(open) },
      { ...held, offset: at(drop), easing: 'cubic-bezier(.55,0,.75,.35)' },
      { ...flat, offset: at(drop + settle) },
      flat,
    ], { duration: RIFFLE_MS }).finished.then(() => copy.remove(), () => copy.remove());
  }
  tile.animate([{ scale: '1' }, { scale: '1', offset: 0.82 }, { scale: '0.97 1', offset: 0.9 }, { scale: '1' }], { duration: RIFFLE_MS });
}

/** Whether the board is hidden or still fading in as the match comes on screen (matchentry.ts). */
function boardHidden(): boolean {
  return !!document.querySelector('#match:is(.held, .arriving)');
}

// A face-down top card is not drawn on its tile, and its hidden picture has zero size
function tileImageRect(key: string): DOMRect | null {
  const img = document.querySelector(`.zone-tile img[data-key="${key}"]`);
  const rect = img?.getBoundingClientRect();
  return rect?.width ? rect : null;
}

/** The icon that shows another player's hand, if the hand is drawn as an icon. */
function handFanOf(place: Place): HTMLElement | null {
  const seat = place.player ? document.querySelector<HTMLElement>(`.seat[data-player="${place.player.ref}"]`) : null;
  const fan = seat && seat.id !== 'me' ? seat.querySelector<HTMLElement>('.hand-fan') : null;
  return fan && !fan.hidden ? fan : null;
}

/** Roughly where a zone is drawn. It is used for a card that has no element at one end of its trip. */
function placeRect(place: Place | undefined): DOMRect | null {
  if (!place) {
    return null;
  }
  const seat = place.player ? document.querySelector<HTMLElement>(`.seat[data-player="${place.player.ref}"]`) : null;
  const rectOf = (el: Element | null | undefined) => {
    const rect = el?.getBoundingClientRect();
    return rect?.width ? rect : null;
  };
  switch (place.zone) {
    case 'Stack': {
      const stack = document.getElementById('stack');
      return stack && !stack.hidden ? stack.getBoundingClientRect() : null;
    }
    case 'Hand':
      // Your own hand is laid out along the bottom. Every other hand is the fan of card backs under the player's name
      return seat?.id === 'me' ? rectOf(document.getElementById('hand')) : rectOf(seat?.querySelector('.hand-fan'));
    case 'Battlefield':
      return rectOf(seat?.querySelector('.battlefield'));
    case 'Command':
      // A commander has its own tile among the zones. The rest of the command zone is shown by the portrait
      return rectOf(seat?.querySelector('.zone-tile[data-zone="Command"]')) ?? rectOf(seat?.querySelector('.emblems'));
    default:
      return rectOf(seat?.querySelector(`.zone-tile[data-zone="${place.zone}"]`));
  }
}

/** The keys of the cards a pile holds behind its top card. They have no element, but they have not moved. */
let covered = new Set<string>();
/** The cards drawn last frame with their own element, not folded into a pile. */
const ownPlace = new Set<string>();

function notePiles(): void {
  covered = new Set();
  for (const { keys, top } of pileSlots()) {
    for (const key of keys) {
      if (key !== top.dataset.key) {
        covered.add(key);
      }
    }
  }
}

function pileSlots(): { keys: string[]; top: HTMLElement }[] {
  const out: { keys: string[]; top: HTMLElement }[] = [];
  for (const slot of document.querySelectorAll<HTMLElement>('.slot[data-members]')) {
    const top = slot.querySelector<HTMLElement>(':scope > .card:last-child');
    if (top) {
      out.push({ keys: (slot.dataset.members ?? '').split(','), top });
    }
  }
  return out;
}

/** The battlefields' slots in order, each with its cards. The result changes only when what a row holds changes. */
function boardShape(): string {
  // A zone that switches between one line and two moves its cards as much as a card arriving or leaving
  const lines = [...document.querySelectorAll<HTMLElement>('.battlefield .group')].map(g => g.dataset.lines ?? '').join('');
  return lines + '#' + [...document.querySelectorAll<HTMLElement>('#me .battlefield .slot, #opponent .battlefield .slot')]
    .map(slot => `${slot.dataset.members ?? ''}:${[...slot.querySelectorAll<HTMLElement>('.card')].map(c => c.dataset.key).join('+')}`)
    .join('|');
}

const handShape = () => [...document.querySelectorAll<HTMLElement>(HAND_CARDS)].map(c => c.dataset.key).join(',');

let lastShape = '';
let lastHand = '';
let lastZones = '';
const zonesShape = () => [...document.querySelectorAll<HTMLElement>(ZONE_CARDS)].map(c => c.dataset.key).join(',');

/** Where a hovered hand card rests. It is computed from the resting transform (hand.css), because setting that transform would cut the hover rise short. */
function unhovered(el: HTMLElement): DOMRect {
  const style = getComputedStyle(el);
  const [ox, oy] = style.transformOrigin.split(' ').map(parseFloat);
  const w = el.offsetWidth, h = el.offsetHeight;
  // The box that holds the card's corners under a transform, measured from its transform origin
  const reach = (m: DOMMatrix) => {
    const corners = [[0, 0], [w, 0], [0, h], [w, h]].map(([x, y]) => m.transformPoint(new DOMPoint(x - ox, y - oy)));
    const xs = corners.map(p => p.x), ys = corners.map(p => p.y);
    return { left: Math.min(...xs), top: Math.min(...ys), right: Math.max(...xs), bottom: Math.max(...ys) };
  };
  const now = reach(new DOMMatrix(style.transform === 'none' ? undefined : style.transform));
  const rect = el.getBoundingClientRect();
  const drop = parseFloat(el.style.getPropertyValue('--drop')) || 0;
  const tilt = parseFloat(el.style.getPropertyValue('--tilt')) || 0;
  const rest = reach(new DOMMatrix().translate(0, drop).rotate(tilt));
  const x = rect.left - now.left, y = rect.top - now.top;
  return new DOMRect(x + rest.left, y + rest.top, rest.right - rest.left, rest.bottom - rest.top);
}

/** Records where every card is now, for the moves in the next frame. */
function note(): void {
  lastShape = boardShape();
  lastHand = handShape();
  lastZones = zonesShape();
  lastSeen.clear();
  ownPlace.clear();
  for (const el of document.querySelectorAll<HTMLElement>(CARDS)) {
    // Store the element itself. An element that leaves the page is never reused, so it keeps this frame's look for a ghost
    const rest = el.matches('#hand .card:hover') ? unhovered(el) : restingRect(el);
    const now = moving(el) ? el.getBoundingClientRect() : rest;
    lastSeen.set(el.dataset.key as string, {
      rect: rest, ghost: el, laid: laidCentre(el), size: { w: el.offsetWidth, h: el.offsetHeight }, tapped: el.classList.contains('tapped'),
      drift: { x: now.left + now.width / 2 - (rest.left + rest.width / 2), y: now.top + now.height / 2 - (rest.top + rest.height / 2) },
    });
    ownPlace.add(el.dataset.key as string);
  }
  for (const el of stackItems()) {
    const key = el.querySelector('img')?.dataset.key;
    if (key && !lastSeen.has(key)) {
      lastSeen.set(key, { rect: el.getBoundingClientRect(), ghost: null });
    }
  }
  // The top card of a zone tile, such as a commander in its tile, starts from its picture there
  for (const img of document.querySelectorAll<HTMLElement>('.zone-tile img[data-key]:not([hidden])')) {
    const key = img.dataset.key as string;
    const rect = img.getBoundingClientRect();
    if (key && rect.width && !lastSeen.has(key)) {
      lastSeen.set(key, { rect, ghost: null });
    }
  }
  // A copy folded into a pile is at the pile's top card, so if it leaves, it leaves from there
  for (const { keys, top } of pileSlots()) {
    const shown = lastSeen.get(top.dataset.key as string);
    for (const key of keys) {
      if (shown && covered.has(key)) {
        lastSeen.set(key, { ...shown });
      }
    }
  }
}

// A spell waits beside the stack, where it is about to go, instead of over the cards on the table
function waitingSpot(rect: DOMRect): DOMRect {
  const stack = document.getElementById('stack');
  const panel = stack && !stack.hidden ? stack.getBoundingClientRect() : null;
  const board = document.getElementById('me')?.getBoundingClientRect();
  const width = rect.width * 0.8;
  const height = rect.height * 0.8;
  const right = panel ? panel.left : (board?.right ?? innerWidth) - 8;
  const top = panel ? panel.top : (board?.top ?? 80);
  return new DOMRect(right - width - 12, top, width, height);
}

function hold(key: string, from: DOMRect, seen: Snapshot | null): void {
  const spot = waitingSpot(from);
  if (!seen?.ghost) {
    // Nothing is shown while it waits (an opponent's card), but its item still flies in from where the card was
    waiting.set(key, { rect: from, ghost: null, since: Date.now() });
    return;
  }
  const shown = place(seen.ghost, from, seen.size);
  shown.classList.add('paying');
  // The player may still want to read a card waiting to be paid for, so it takes pointer events
  shown.style.pointerEvents = 'auto';
  hoverable(shown, shown.querySelector('img') ?? shown);
  shown.animate([
    { translate: '0px 0px', scale: '1' },
    { translate: shiftTo(shown, spot.width / from.width, spot), scale: String(spot.width / from.width) },
  ], { duration: FLIGHT_MS, easing: EASE, fill: 'forwards' });
  waiting.set(key, { rect: spot, ghost: shown, since: Date.now() });
}

function land(key: string): void {
  waiting.get(key)?.ghost?.remove();
  waiting.delete(key);
}

/** A copy of a card at its layout size, centred where the card was. So a tapped card's copy is rotated, not squeezed. */
function place(from: HTMLElement, rect: DOMRect, size?: { w: number; h: number }): HTMLElement {
  // Copy the element only when a ghost is shown, not for every card on every frame
  const ghost = from.cloneNode(true) as HTMLElement;
  const w = size?.w || rect.width;
  const h = size?.h || rect.height;
  const left = rect.left + rect.width / 2 - w / 2;
  const top = rect.top + rect.height / 2 - h / 2;
  ghost.style.cssText += `position: fixed; left: ${left}px; top: ${top}px; width: ${w}px; height: ${h}px;`
    + 'margin: 0; z-index: 40; pointer-events: none;';
  document.body.append(ghost);
  return ghost;
}

/** The shift that centres a scaled element on a place. It is measured with the scale applied, because cards scale about different origins. */
function shiftTo(el: HTMLElement, scale: number, to: DOMRect): string {
  el.style.scale = String(scale);
  const now = el.getBoundingClientRect();
  el.style.scale = '';
  return `${to.left + to.width / 2 - (now.left + now.width / 2)}px ${to.top + to.height / 2 - (now.top + now.height / 2)}px`;
}

// Scale is a separate property, so the card's own transform (a tap's rotation, a tilt in hand) does not change
function pop(el: HTMLElement): void {
  el.animate([
    { scale: '.7', opacity: 0 },
    { scale: '1', opacity: 1 },
  ], { duration: POP_MS, easing: 'cubic-bezier(.2,.9,.3,1.2)' });
}

function fly(el: HTMLElement, from: DOMRect, duration: number, delay: number): void {
  for (const a of el.getAnimations()) {
    if (a.id === FLIGHT) a.cancel();
  }
  const to = restingRect(el);
  if (!to.width || !from.width) {
    return;
  }
  if (el.dataset.key) {
    resting.set(el.dataset.key, to);
  }
  const scale = from.width / to.width;
  const start = { translate: shiftTo(el, scale, from), scale: String(scale) };
  // A card that waits its turn in a deal is hidden until its flight starts. It then appears at its start point
  const flight = el.animate([
    ...(delay ? [{ ...start, opacity: 0 }, { ...start, opacity: 1, offset: 0.001 }] : [{ ...start, opacity: 1 }]),
    { translate: '0px 0px', scale: '1', opacity: 1 },
  ], { duration, delay, easing: EASE, fill: 'backwards' });
  flight.id = FLIGHT;
  // A ring marks a card at rest, so a card in flight has no ring until it lands (board.css)
  el.classList.add('landing');
  const landed = () => { if (!el.getAnimations().some(a => a.id === FLIGHT)) el.classList.remove('landing'); };
  flight.finished.then(landed, landed);
}

// A copy of the card makes the trip, so the card's own element, if it still has one, does not move
function sendTo(was: Snapshot, target: DOMRect, endOpacity: number, delay = 0, onto?: HTMLElement, duration = FLIGHT_MS): void {
  // A draw or a mulligan can come before the board is on screen. Its copies sit above the board, so they would show first
  if (!was.ghost || boardHidden()) {
    return;
  }
  const ghost = place(was.ghost, was.rect, was.size);
  // A single card makes the trip and looks as it did when it started, even if it was the top of a pile
  ghost.classList.remove('pile');
  if (was.tapped !== undefined) ghost.classList.toggle('tapped', was.tapped);
  // A copy joining a pile that is rotated the other way rotates as it flies, so its scale is measured along the card
  const tapped = (el: HTMLElement) => el.classList.contains('tapped');
  const turn = onto ? Number(tapped(onto)) - Number(tapped(ghost)) : 0;
  const along = (el: HTMLElement) => el.offsetWidth * (tapped(el) ? TAPPED_SCALE : 1);
  const scale = turn && onto ? along(onto) / along(ghost) : target.width / ghost.getBoundingClientRect().width;
  ghost.animate([
    { translate: '0px 0px', scale: '1', rotate: '0deg', opacity: 1 },
    { translate: shiftTo(ghost, scale, target), scale: String(scale), rotate: `${turn * 90}deg`, opacity: endOpacity },
  // A copy joining a pile on the board slows as it lands. A copy going into a tile or a hand speeds up at the end
  ], { duration, delay, easing: onto ? EASE : 'cubic-bezier(.4,0,.8,.4)', fill: 'backwards' }).finished.then(() => ghost.remove(), () => ghost.remove());
}
