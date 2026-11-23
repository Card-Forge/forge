// Draws the arrows over the board. They point from attackers to what they attack, from blockers to what they block, and from spells to their targets

import { attackTargets, combatShown, game, derefAll, players, zone, type Model, offersPicks } from '../model';
import { byId, onRelayout, reducedMotion, easeOut } from '../dom';
import { cardElement, pileTopFor } from './motion';
import { ui } from '../ui';
import { isPortrait } from '../form';
import { tagsFor } from './portrait';
import type { CardView, Ref, Refs, StackItemView, TrackedObject } from '../protocol';
import { shownZones } from './zones';

// Attack, block and target arrows differ in brightness as well as hue, so a player with red-green colour blindness can still tell them apart
interface ArrowKind {
  sheath: string;
  rim: string;
  glow: string;
  core: string;
  /** A planned block, or a block a card forces, is drawn fainter than a declared block. */
  alpha: number;
}

const ATTACK = { sheath: '#e8321f', rim: '#ff6a4c', glow: '#ff2a14', core: '#fff4ee' };
const BLOCK = { sheath: '#2f7df0', rim: '#6fb0ff', glow: '#2a8cff', core: '#eef6ff' };
const TARGET = { sheath: '#f0b81f', rim: '#ffd95a', glow: '#ffc21a', core: '#fffbea' };

const KINDS: Record<'attack' | 'block' | 'plannedBlock' | 'target' | 'mustBlock', ArrowKind> = {
  attack: { ...ATTACK, alpha: 1 },
  block: { ...BLOCK, alpha: 1 },
  plannedBlock: { ...BLOCK, alpha: 0.55 },
  target: { ...TARGET, alpha: 1 },
  mustBlock: { ...BLOCK, alpha: 0.4 },
};

/** The arrow was designed for a smaller card size. This is how much larger it is drawn over the board's cards. */
const SCALE = 1.4;
/** The distance from the tip of the head back to the point where the body joins it, in the head's design units. */
const NECK = 14;

interface Point {
  x: number;
  y: number;
}

let settleFrame = 0;
/** How long after a redraw the overlay keeps repainting, even if no card animation has started yet. */
const SETTLE_START_MS = 100;
let drawn: Model | null = null;
let repaintQueued = false;

export function initOverlay(schedule: () => void): void {
  onRelayout(schedule);
  // Scrolling the log or a zone panel moves the cards the arrows point at. So only the arrows are redrawn, at most once a frame
  document.addEventListener('scroll', repaintSoon, true);
  document.addEventListener('pointermove', e => {
    if (e.pointerType !== 'mouse') return;
    pointer = { x: e.clientX, y: e.clientY };
    if (aiming) repaintSoon();
  });
}

function repaintSoon(): void {
  if (repaintQueued || !drawn) return;
  repaintQueued = true;
  requestAnimationFrame(() => {
    repaintQueued = false;
    if (drawn) paint(drawn);
  });
}

export function drawOverlay(model: Model): void {
  drawn = model;
  paint(model);
  // A card animation starts a frame after the change that causes it. So the first frames repaint even if nothing is moving yet
  cancelAnimationFrame(settleFrame);
  const since = performance.now();
  const settle = () => {
    if (drawn !== model) return;
    paint(model);
    if (performance.now() - since < SETTLE_START_MS || boardMoving() || growing) settleFrame = requestAnimationFrame(settle);
  };
  settleFrame = requestAnimationFrame(settle);
}

/** Whether a card on the board is still animating to a new position. Animations that repeat forever, such as a pulsing glow, are ignored. */
function boardMoving(): boolean {
  return document.getAnimations().some(a => {
    const target = (a.effect as KeyframeEffect | null)?.target;
    return a.playState === 'running' && a.effect?.getTiming().iterations !== Infinity
      && target instanceof Element && !!target.closest('#match .card, #match .slot, #match .seat, #match .group');
  });
}

function paint(model: Model): void {
  growing = false;
  drawnNow = new Map();
  glowNext = null;
  paintArrows(model);
  born = drawnNow;
  // paintArrows sets glowNext, but the compiler cannot see that from here
  const next = glowNext as typeof glowing;
  if (next?.el !== glowing?.el || next?.colour !== glowing?.colour) {
    glowing?.el.classList.remove('aimed');
    if (next) {
      next.el.classList.add('aimed');
      next.el.style.setProperty('--aimed', next.colour);
    }
    glowing = next;
  }
}

/** The element under an aimed or dragged arrow glows in the arrow's colour. It is the one a click or drop picks. */
let glowing: { el: HTMLElement; colour: string } | null = null;
let glowNext: { el: HTMLElement; colour: string } | null = null;
const glow = (el: HTMLElement, kind: ArrowKind) => { glowNext = { el, colour: kind.glow }; };

function paintArrows(model: Model): void {
  const canvas = byId<HTMLCanvasElement>('overlay');
  const ratio = window.devicePixelRatio || 1;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  if (canvas.width !== innerWidth * ratio || canvas.height !== innerHeight * ratio) {
    canvas.width = innerWidth * ratio;
    canvas.height = innerHeight * ratio;
    canvas.style.width = `${innerWidth}px`;
    canvas.style.height = `${innerHeight}px`;
  } else {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  const g = game(model);
  if (!g || byId('match').hidden) {
    placeCharges(new Set());
    return;
  }
  const atFace = atLoneFace(model);
  // In the combat damage step the chevrons are removed. The attackers stay marked
  const landed = g.Phase === 'COMBAT_DAMAGE';
  placeCharges(landed ? new Set() : chargingAtPlayer(model), landed);
  drawDrag(ctx);
  // A phone marks targets with numbered tags. An arrow from the stack would cross the whole board and cover what it points at
  const phone = isPortrait();
  if (phone) paintTags(model);
  else {
    clearTags();
    drawAim(ctx, model);
  }
  drawPair(ctx, model);
  for (const band of !combatShown(model) ? [] : g.CombatView ?? []) {
    const attackers = present(band.attackers);
    // A blocked attacker shows only its block arrows, not its arrow to the defender
    const blocked = present(band.blockers).length > 0 || present(band.plannedBlockers).length > 0;
    attackers.forEach(attacker => {
      if (!phone && !atFace.has(attacker.ref) && !blocked) {
        ribbon(elementFor(attacker.ref), elementFor(band.defender?.ref), KINDS.attack);
      }
      for (const blocker of present(band.blockers)) {
        ribbon(elementFor(blocker.ref), elementFor(attacker.ref), KINDS.block);
      }
      for (const blocker of present(band.plannedBlockers)) {
        ribbon(elementFor(blocker.ref), elementFor(attacker.ref), KINDS.plannedBlock);
      }
    });
  }
  // Draws an arrow from a creature to each attacker it must block. Without it, an illegal block is refused and nothing on screen says why
  if (g.Phase === 'COMBAT_DECLARE_BLOCKERS') {
    for (const card of players(model).flatMap(p => zone(model, p, 'Battlefield'))) {
      present(card.MustBlockCards).forEach(attacker => ribbon(elementFor(card.$key), elementFor(attacker.ref), KINDS.mustBlock));
    }
  }
  // Another player's spell or ability always shows its arrows, because you did not see it aimed. Your own items show arrows only under the pointer
  for (const item of phone ? [] : derefAll(model, g.Stack) as StackItemView[]) {
    const theirs = !!item.ActivatingPlayer && !model.localPlayers.includes(item.ActivatingPlayer.ref);
    if (theirs || item.$key === ui.hoveredStackItem) {
      const from = document.querySelector<HTMLElement>(`.stack-item[data-key="${item.$key}"]`);
      stackTargets(model, item).forEach(target => ribbon(from, elementFor(target.$key), KINDS.target));
    }
  }
  drawQueued(ctx);
}

const PAIR = '#6fd3c7';
let bonded: HTMLElement[] = [];

/** A soulbond pair gets rings and a joining line only while the pointer is on one of the two. So no line crosses the board otherwise. */
function drawPair(ctx: CanvasRenderingContext2D, model: Model): void {
  const hover = ui.hover;
  const card = hover && 'card' in hover && hover.card !== null ? model.objects.get(hover.card) as CardView | undefined : undefined;
  const pair = [elementFor(card?.$key), elementFor(card?.PairedWith?.ref)].filter((el): el is HTMLElement => !!el);
  const now = pair.length === 2 ? pair : [];
  bonded.filter(el => !now.includes(el)).forEach(el => el.classList.remove('bonded'));
  now.forEach(el => {
    el.classList.add('bonded');
    el.style.setProperty('--bonded', PAIR);
  });
  bonded = now;
  if (!now.length) return;
  const [a, b] = now.map(el => {
    const r = el.getBoundingClientRect();
    return { x: r.left + r.width / 2, y: r.top - 4 };
  });
  ctx.save();
  ctx.beginPath();
  ctx.moveTo(a.x, a.y);
  ctx.quadraticCurveTo((a.x + b.x) / 2, Math.min(a.y, b.y) - Math.max(40, Math.abs(b.x - a.x) * 0.28), b.x, b.y);
  ctx.setLineDash([8, 6]);
  ctx.lineCap = 'round';
  ctx.lineWidth = 3;
  ctx.strokeStyle = PAIR;
  ctx.stroke();
  ctx.restore();
}

function clearTags(): void {
  for (const el of document.querySelectorAll('.target-tags')) el.remove();
}

/** Each target shows the number of every stack item that targets it. The top of the stack is 1. */
function paintTags(model: Model): void {
  const items = derefAll(model, game(model)?.Stack) as StackItemView[];
  const tags = tagsFor(items.map(i => ({ key: i.$key, targets: stackTargets(model, i).map(o => o.$key) })));
  const want = new Map<HTMLElement, number[]>();
  for (const [key, numbers] of tags) {
    const el = elementFor(key);
    // A card in a hidden opponent seat is tagged on that opponent's tab. On a phone with the player list closed, it is tagged on the players button
    const tab = document.querySelector<HTMLElement>(`.seat-tab[data-key="${el?.closest<HTMLElement>('.seat')?.dataset.player ?? key}"]`);
    const shown = el && el.offsetParent !== null ? el : tab && tab.offsetParent === null ? document.getElementById('seat-switch') : tab;
    if (shown) want.set(shown, [...(want.get(shown) ?? []), ...numbers]);
  }
  for (const old of document.querySelectorAll<HTMLElement>('.target-tags')) {
    if (!want.has(old.parentElement as HTMLElement)) old.remove();
  }
  for (const [el, numbers] of want) {
    const text = [...new Set(numbers)].sort((a, b) => a - b).join(',');
    let box = el.querySelector<HTMLElement>(':scope > .target-tags');
    if (box?.dataset.tags === text) continue;
    if (!box) {
      box = document.createElement('span');
      box.className = 'target-tags';
      el.append(box);
    }
    box.dataset.tags = text;
    box.replaceChildren(...text.split(',').map(n => Object.assign(document.createElement('b'), { className: n === '1' ? 'target-tag top' : 'target-tag', textContent: n })));
  }
}

const present = (refs: Refs | null | undefined): Ref[] => (refs ?? []).filter((r): r is Ref => !!r);

/** A block or attack being dragged. The arrow runs from the creature to what it would block or attack, or to the pointer. */
let drag: { from: HTMLElement; to: HTMLElement | Point; kind: 'block' | 'attack' } | null = null;

export function setDragArrow(from: HTMLElement | null, to: HTMLElement | Point | null, kind: 'block' | 'attack' = 'block'): void {
  const next = from && to ? { from, to, kind } : null;
  if (!next && !drag) return;
  drag = next;
  if (drawn) paint(drawn);
}

function drawDrag(ctx: CanvasRenderingContext2D): void {
  if (!drag) return;
  const kind = KINDS[drag.kind];
  if (drag.to instanceof HTMLElement) {
    ribbon(drag.from, drag.to, kind, false);
    glow(drag.to, kind);
  } else {
    arrow(ctx, edge(drag.from, drag.to, 2), drag.to, kind);
  }
}

const onStack = (key: string): HTMLElement | null =>
  document.querySelector<HTMLElement>(`#stack .stack-item img[data-key="${key}"]`)?.closest<HTMLElement>('.stack-item') ?? null;

let pointer: Point | null = null;
/** The card being aimed and the targets picked for it. The payment prompt that follows no longer includes them. */
let lastAim: { key: string; targets: number[] } | null = null;

/** Stores a clicked target. A spell with one target goes straight to payment, so the prompt never shows the target as picked. */
export function notePick(key: number): void {
  const p = drawn?.prompt;
  const offered = !!p && (p.selectable.some(r => r.ref === key) || p.selectablePlayers.some(r => r.ref === key)
    || p.selectableStack.includes(key));
  if (aiming && lastAim && offered && !lastAim.targets.includes(key)) lastAim.targets.push(key);
}
/** Whether the last paint drew the aim arrow. Mouse moves repaint only while it is drawn. */
let aiming = false;

export function isAiming(): boolean {
  return aiming;
}
/** The target under the aim arrow. It is kept until the pointer leaves its box, so small moves over it do not drop the arrow. */
let aimedAt: HTMLElement | null = null;
/** How far outside that target's box the pointer can move before the arrow leaves the target. */
const LET_GO = 4;

/** Draws the aim arrow while targets are chosen. A target choice is a prompt that names a card and also offers picks. */
function drawAim(ctx: CanvasRenderingContext2D, model: Model): void {
  const p = model.prompt;
  const aimed = !!p?.card && !p.paying && !p.priority && offersPicks(p)
    && !(shownZones(model).length > 0 && !ui.zonesMinimised);
  const key = String(p?.card?.ref);
  // While its targets are chosen, a spell is already on the stack awaiting payment. An ability's card is on the board
  const from = aimed ? onStack(key) ?? cardElement(key) ?? pileTopFor(key) : null;
  aiming = !!from;
  if (from && p) {
    // Targets clicked for this card are kept until the prompt lists them itself
    const clicked = lastAim?.key === key ? lastAim.targets : [];
    lastAim = { key, targets: [...new Set([...p.highlighted, ...clicked])] };
  } else {
    // The payment prompt no longer names the targets. So the last picked targets are drawn from the awaiting slot, through any prompts before payment
    const slot = lastAim && !p?.priority ? onStack(lastAim.key) : null;
    const waiting = slot?.classList.contains('awaiting') ? slot : null;
    if (waiting && lastAim) {
      const targets = lastAim.targets;
      targets.forEach(k => ribbon(waiting, elementFor(k) ?? onStack(String(k)), KINDS.target));
    } else {
      lastAim = null;
    }
  }
  if (!from || !p) {
    aimedAt = null;
    return;
  }
  p.highlighted.forEach(k => ribbon(from, elementFor(k) ?? onStack(String(k)), KINDS.target));
  if (!pointer) return;
  const r = from.getBoundingClientRect();
  if (within(r, pointer, 0)) return;
  const hit = document.elementFromPoint(pointer.x, pointer.y)?.closest<HTMLElement>('.selectable, .targetable') ?? null;
  const held = aimedAt?.isConnected && aimedAt.matches('.selectable, .targetable') && within(aimedAt.getBoundingClientRect(), pointer, LET_GO);
  const over = hit ?? (held ? aimedAt : null);
  aimedAt = over;
  if (over) {
    // Drawn at full length at once. The arrow already reached the pointer, so only its head moves onto the target
    ribbon(from, over, KINDS.target, false);
    glow(over, KINDS.target);
  } else {
    arrow(ctx, edge(from, pointer, 2), pointer, KINDS.target);
  }
}

// The attack mark's drawing (board.css) is 64 by 54 units. Its chevrons run from 16 below the top to 13.7 above the bottom
const MARK_W = 64;
const MARK_H = 54;
const MARK_TOP = 16;
const MARK_BELOW = 13.7;

/** The chevron over each attacker at a player. It is kept between paints so its halo animation continues instead of restarting. */
const charges = new Map<number, HTMLElement>();

/** Places each chevron from its card's current box on screen, so a tapped card's chevron is over its rotated edge. */
function placeCharges(keys: Set<number>, landed = false): void {
  for (const [key, mark] of charges) {
    if (!keys.has(key)) {
      charges.delete(key);
      retreat(mark, !!elementFor(key), landed);
    }
  }
  for (const key of keys) {
    const card = elementFor(key);
    let mark = charges.get(key);
    if (!card) {
      mark?.remove();
      charges.delete(key);
      continue;
    }
    const arriving = !mark;
    if (!mark) {
      mark = document.createElement('div');
      mark.className = 'charge';
      byId('charges').append(mark);
      charges.set(key, mark);
    }
    const r = card.getBoundingClientRect();
    const width = card.offsetWidth * .66;
    const height = width * MARK_H / MARK_W;
    // The chevrons are placed about an eighth of the card's width clear of it, so the last one is not hidden behind the card
    const sink = height * MARK_BELOW / MARK_H - card.offsetWidth * .12;
    const down = !!card.closest('#opponent');
    // A crowded battlefield scrolls. So the chevron is kept inside the battlefield's box and off the phase pill outside it
    const field = card.closest('.battlefield')?.getBoundingClientRect();
    mark.classList.toggle('down', down);
    mark.style.width = `${width}px`;
    mark.style.left = `${r.left + r.width / 2}px`;
    mark.style.top = `${down
      ? Math.min(r.bottom - sink, (field?.bottom ?? Infinity) - height * (MARK_H - MARK_TOP) / MARK_H)
      : Math.max(r.top + sink, (field?.top ?? -Infinity) + height * (MARK_H - MARK_TOP) / MARK_H)}px`;
    if (arriving && !reducedMotion()) {
      // The chevron moves out from the attacker as it grows and fades in. This is the retreat animation in reverse
      const back = down ? -1 : 1;
      mark.animate([
        { opacity: 0, translate: `0 ${back * height * 0.7}px`, scale: '.55' },
        { opacity: 1, translate: '0 0', scale: '1' },
      ], { duration: ARRIVE_MS, easing: 'cubic-bezier(.2, .8, .3, 1)' });
    }
  }
}

const ARRIVE_MS = 220;

/** The delay after combat damage before the chevrons retreat, so the player sees the hit first. */
const RETREAT_DELAY_MS = 250;
const RETREAT_MS = 300;

/** Animates a chevron back into its card. If the card has left the battlefield, the chevron is removed at once. */
function retreat(mark: HTMLElement, cardStays: boolean, afterHit: boolean): void {
  if (!cardStays || reducedMotion()) {
    mark.remove();
    return;
  }
  // translate and scale are separate properties, so they combine with the transform that places and rotates the mark
  const back = mark.classList.contains('down') ? -1 : 1;
  const run = mark.animate([
    { opacity: 1, translate: '0 0', scale: '1' },
    { opacity: 0, translate: `0 ${back * mark.offsetHeight * 0.7}px`, scale: '.55' },
  ], { duration: RETREAT_MS, delay: afterHit ? RETREAT_DELAY_MS : 0, easing: 'cubic-bezier(.5, 0, .75, 0)', fill: 'forwards' });
  run.finished.then(() => mark.remove(), () => mark.remove());
}

/** When the attacking player can attack only one player and nothing else, maps each attacker to whether it is blocked. */
function atLoneFace(model: Model): Map<number, boolean> {
  const out = new Map<number, boolean>();
  if (!combatShown(model)) return out;
  const g = game(model);
  const targets = attackTargets(model, players(model).find(p => p.$key === g?.PlayerTurn?.ref));
  if (targets.length !== 1) return out;
  for (const band of g?.CombatView ?? []) {
    const blocked = present(band.blockers).length > 0 || present(band.plannedBlockers).length > 0;
    if (band.defender?.ref === targets[0]) present(band.attackers).forEach(a => out.set(a.ref, blocked));
  }
  return out;
}

/** The unblocked attackers when there is only one thing to attack. Only these get a chevron. Any other attack is drawn as an arrow. */
export function chargingAtPlayer(model: Model): Set<number> {
  return new Set([...atLoneFace(model)].filter(([, blocked]) => !blocked).map(([key]) => key));
}

export function stackTargets(model: Model, item: StackItemView): TrackedObject[] {
  const out: TrackedObject[] = [];
  for (let i: StackItemView | undefined = item; i; i = i.SubInstance ? model.objects.get(i.SubInstance.ref) : undefined) {
    out.push(...derefAll(model, i.TargetCards), ...derefAll(model, i.TargetPlayers));
  }
  return out;
}

// A card inside a pile is represented by the pile's top card
function elementFor(key: number | undefined): HTMLElement | null {
  if (key === undefined) return null;
  return document.querySelector<HTMLElement>(`.seat[data-player="${key}"] .avatar`)
    ?? cardElement(String(key)) ?? pileTopFor(String(key))
    ?? document.querySelector<HTMLElement>(`#stack .stack-item[data-key="${key}"]`);
}

function center(el: HTMLElement): Point {
  const r = el.getBoundingClientRect();
  return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
}

function within(r: DOMRect, at: Point, pad: number): boolean {
  return at.x >= r.left - pad && at.x <= r.right + pad && at.y >= r.top - pad && at.y <= r.bottom + pad;
}

// Where the line from the element's middle towards `to` leaves its box
function edge(el: HTMLElement, to: Point, pad: number): Point {
  const r = el.getBoundingClientRect();
  const c = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
  const dx = to.x - c.x;
  const dy = to.y - c.y;
  const len = Math.hypot(dx, dy) || 1;
  const step = Math.min(dx ? (r.width / 2 + pad) / Math.abs(dx / len) : 1e9,
    dy ? (r.height / 2 + pad) / Math.abs(dy / len) : 1e9);
  return { x: c.x + (dx / len) * step, y: c.y + (dy / len) * step };
}

const rgba = (hex: string, a: number) => {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${(n >> 16) & 255},${(n >> 8) & 255},${n & 255},${a})`;
};

/** The arrows of this paint. They are drawn together at the end, so arrows with the same target can be spaced out across it. */
let queued: { from: HTMLElement; to: HTMLElement; kind: ArrowKind; grows: boolean }[] = [];

function ribbon(fromEl: HTMLElement | null, toEl: HTMLElement | null, kind: ArrowKind, grows = true): void {
  if (!fromEl || !toEl || fromEl === toEl) return;
  // The engine can list a block as both declared and planned. The declared one is queued first, so only it is drawn
  if (queued.some(q => q.from === fromEl && q.to === toEl)) return;
  queued.push({ from: fromEl, to: toEl, kind, grows });
}

/** The largest gap between the heads of arrows at one target. Each head stays distinct from the others. */
const HEAD_GAP = 6;

/** Arrows at one target aim at its middle and stop at its edge. They are spaced in the order of their sources, so they do not cross. */
function drawQueued(ctx: CanvasRenderingContext2D): void {
  const byTarget = new Map<HTMLElement, typeof queued>();
  for (const q of queued) byTarget.set(q.to, [...byTarget.get(q.to) ?? [], q]);
  for (const [to, arrows] of byTarget) {
    const from = arrows.map(q => center(q.from));
    const middle = { x: from.reduce((n, p) => n + p.x, 0) / from.length, y: from.reduce((n, p) => n + p.y, 0) / from.length };
    const c = center(to);
    // The direction across the arrows' approach. Each arrow aims a little off the middle, by its source's position along it
    const len = Math.hypot(c.x - middle.x, c.y - middle.y) || 1;
    const across = { x: -(c.y - middle.y) / len, y: (c.x - middle.x) / len };
    const order = arrows.map((_, i) => i)
      .sort((i, j) => (from[i].x - from[j].x) * across.x + (from[i].y - from[j].y) * across.y);
    const gap = Math.min(HEAD_GAP, 28 / arrows.length);
    order.forEach((i, rank) => {
      const q = arrows[i];
      const off = (rank - (arrows.length - 1) / 2) * gap;
      const aim = { x: c.x + across.x * off, y: c.y + across.y * off };
      const a = edge(q.from, aim, 2);
      arrow(ctx, a, aim, q.kind, reach(a, aim, to, 6) * growth(q.from, q.to, q.kind, q.grows));
    });
  }
  queued = [];
}

/** How far along the arrow's curve it first comes within pad of the target. A portrait is tested by its circle, and a card by its box. */
function reach(a: Point, b: Point, to: HTMLElement, pad: number): number {
  const r = to.getBoundingClientRect();
  const c = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
  const round = to.classList.contains('avatar');
  const bend = bendOf(a, b);
  for (let t = 0; t <= 1; t += 0.005) {
    const p = curveAt(a, bend, b, t);
    if (round ? Math.hypot(p.x - c.x, p.y - c.y) <= r.width / 2 + pad : within(r, p, pad)) return t;
  }
  return 1;
}

/** The control point of an arrow's curve. A deeper curve keeps two arrows between the same rows apart, and looks less like a straight line. */
function bendOf(a: Point, b: Point): Point {
  const bow = 0.34;
  return { x: (a.x + b.x) / 2 + (b.y - a.y) * bow, y: (a.y + b.y) / 2 - (b.x - a.x) * bow };
}

/** How long an arrow takes to reach its target when it first appears. */
const GROW_MS = 220;
/** When each arrow was first drawn, keyed by its ends and kind. An arrow not drawn in a paint is removed. */
let born = new Map<string, number>();
let drawnNow = new Map<string, number>();
/** Whether an arrow is still growing, so the overlay keeps painting until it has. */
let growing = false;
const ids = new WeakMap<Element, number>();
let nextId = 0;
const idOf = (el: Element) => ids.get(el) ?? (ids.set(el, ++nextId), nextId);

/** How far an arrow has grown, from 0 when it first appears to 1 when it reaches its target. An arrow drawn at full length is stored as grown, so it stays full if it later becomes a growing arrow. */
function growth(from: HTMLElement, to: HTMLElement, kind: ArrowKind, grows: boolean): number {
  if (reducedMotion()) return 1;
  const key = `${idOf(from)}>${idOf(to)}>${kind.core}`;
  const first = grows ? born.get(key) ?? performance.now() : -Infinity;
  drawnNow.set(key, first);
  const t = Math.min(1, (performance.now() - first) / GROW_MS);
  if (t < 1) growing = true;
  return easeOut(t);
}

function arrow(ctx: CanvasRenderingContext2D, a: Point, full: Point, kind: ArrowKind, grown = 1): void {
  const whole = bendOf(a, full);
  // A growing arrow, or one stopped at its target's edge, is the full curve cut short. So it follows the path it will end on
  const end = grown < 1 ? curveAt(a, whole, full, grown) : full;
  const bend = grown < 1 ? { x: a.x + (whole.x - a.x) * grown, y: a.y + (whole.y - a.y) * grown } : whole;
  // Not drawn while still too short for a head
  if (grown < 1 && Math.hypot(end.x - a.x, end.y - a.y) < NECK * SCALE * 1.5) return;
  // The body stops where the head joins it, so the head does not cover the body's end
  const [bodyBend, bodyEnd] = trim(a, bend, end, NECK * SCALE);
  // The body is thin at the source, widest a little past the middle, and narrow again where the head joins
  const half = (t: number) => (0.9 + 1.7 * Math.sin(Math.PI * Math.pow(t, 1.35))) * SCALE;
  ctx.save();
  ctx.globalAlpha = kind.alpha;
  taper(ctx, a, bodyBend, bodyEnd, half);
  ctx.shadowColor = rgba(kind.glow, 0.6);
  ctx.shadowBlur = 7 * SCALE;
  ctx.fillStyle = kind.sheath;
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.strokeStyle = kind.rim;
  ctx.lineWidth = 0.6;
  ctx.stroke();
  taper(ctx, a, bodyBend, bodyEnd, t => half(t) * 0.28);
  ctx.fillStyle = kind.core;
  ctx.fill();
  head(ctx, end, Math.atan2(end.y - bend.y, end.x - bend.x), kind);
  ctx.restore();
}

/** The point a fraction t along the quadratic curve from a to b, bent towards bend. */
function curveAt(a: Point, bend: Point, b: Point, t: number): Point {
  const u = 1 - t;
  return { x: u * u * a.x + 2 * u * t * bend.x + t * t * b.x, y: u * u * a.y + 2 * u * t * bend.y + t * t * b.y };
}

/** Cuts the quadratic curve short where it comes within `back` pixels of its end. Returns the new control point and end. */
function trim(a: Point, bend: Point, b: Point, back: number): [Point, Point] {
  const at = (t: number): Point => curveAt(a, bend, b, t);
  let t = 1;
  while (t > 0.5 && Math.hypot(at(t).x - b.x, at(t).y - b.y) < back) t -= 0.005;
  return [{ x: a.x + (bend.x - a.x) * t, y: a.y + (bend.y - a.y) * t }, at(t)];
}

// The outline of a quadratic curve given a half-width at each point along it
function taper(ctx: CanvasRenderingContext2D, a: Point, bend: Point, b: Point, half: (t: number) => number): void {
  const steps = 26;
  const side: [Point[], Point[]] = [[], []];
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    const u = 1 - t;
    const { x, y } = curveAt(a, bend, b, t);
    const dx = 2 * u * (bend.x - a.x) + 2 * t * (b.x - bend.x);
    const dy = 2 * u * (bend.y - a.y) + 2 * t * (b.y - bend.y);
    const len = Math.hypot(dx, dy) || 1;
    const w = half(t);
    side[0].push({ x: x - (dy / len) * w, y: y + (dx / len) * w });
    side[1].push({ x: x + (dy / len) * w, y: y - (dx / len) * w });
  }
  ctx.beginPath();
  side[0].forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)));
  for (let i = side[1].length - 1; i >= 0; i--) {
    ctx.lineTo(side[1][i].x, side[1][i].y);
  }
  ctx.closePath();
}

/** Draws the head pointing along its angle with its tip on the point. */
function head(ctx: CanvasRenderingContext2D, at: Point, angle: number, kind: ArrowKind): void {
  ctx.save();
  ctx.translate(at.x, at.y);
  ctx.rotate(angle);
  ctx.scale(SCALE, SCALE);
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.quadraticCurveTo(-9, 2, -21, 9.5);
  ctx.lineTo(-NECK, 0);
  ctx.lineTo(-21, -9.5);
  ctx.quadraticCurveTo(-9, -2, 0, 0);
  ctx.closePath();
  ctx.shadowColor = rgba(kind.glow, 0.53);
  ctx.shadowBlur = 6;
  ctx.fillStyle = shade(kind.sheath, -0.35);
  ctx.fill();
  ctx.shadowColor = 'transparent';
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.quadraticCurveTo(-9, -2, -21, -9.5);
  ctx.lineTo(-NECK, 0);
  ctx.closePath();
  ctx.fillStyle = kind.sheath;
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.quadraticCurveTo(-8, -1.4, -16, -5);
  ctx.lineTo(-12, 0);
  ctx.closePath();
  ctx.globalAlpha *= 0.55;
  ctx.fillStyle = shade(kind.rim, 0.15);
  ctx.fill();
  ctx.globalAlpha /= 0.55;
  ctx.beginPath();
  ctx.moveTo(-0.5, 0);
  ctx.lineTo(-NECK + 0.5, 0);
  ctx.strokeStyle = kind.core;
  ctx.lineWidth = 0.8;
  ctx.stroke();
  ctx.restore();
}

/** A colour darkened (amount below 0) or lightened (above 0) by that fraction of the way to black or white. */
function shade(hex: string, amount: number): string {
  const n = parseInt(hex.slice(1), 16);
  const move = (v: number) => Math.round(amount < 0 ? v * (1 + amount) : v + (255 - v) * amount);
  return `rgb(${move((n >> 16) & 255)},${move((n >> 8) & 255)},${move(n & 255)})`;
}

/** Clears the last game's aim. A new game numbers its cards again from the start and could reuse the old key. */
export function resetOverlay(): void {
  lastAim = null;
}
