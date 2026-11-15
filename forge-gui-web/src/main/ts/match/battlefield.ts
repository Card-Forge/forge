// Draws one player's permanents. It sorts them into rows, piles identical ones, and sizes the cards to fit the board

import { reconcile } from './reconcile';
import { createCard, updateCard, setPileCount, type CardClick, TAPPED_SCALE } from './cards';
import { combatShown, derefAll, game, me, stateOf, type Model } from '../model';
import { declaringBlocks } from './combatdrag';
import { ICONS } from '../icons';
import { mergeInto, spreadFrom } from './motion';
import { chargingAtPlayer, stackTargets } from './overlay';
import { q, make } from '../dom';
import { changeUi, ui } from '../ui';
import type { CardView } from '../protocol';
import { t } from '../text';

// A slot is one place on the battlefield. It holds a card with its attachments under it, or a pile of identical permanents
interface Slot {
  top: CardView;
  members: CardView[];
  attached: CardView[];
  /** The key the player opens or closes a pile by. It is null for a card that never piles. */
  sig: string | null;
}

/** How many cards are drawn behind a slot's top card. These are its attachments, or up to three other copies in a pile. */
const behind = (s: Slot): number => s.attached.length || Math.min(3, s.members.length - 1);

/** Planeswalkers and battles go in the far group at the end of the creature row, because they can be attacked but do not block. */
type Group = 'lands' | 'support' | 'creatures' | 'far';

function groupOf(model: Model, slot: Slot): Group {
  const type = stateOf(model, slot.top).Type ?? '';
  // A land that is also a creature can attack and block, so it goes with the creatures
  if (/Creature/.test(type)) return 'creatures';
  if (/\bLand\b/.test(type)) return 'lands';
  if (/Planeswalker|Battle/.test(type)) return 'far';
  return 'support';
}

export function renderBattlefield(root: HTMLElement, model: Model, player: number, cards: CardView[], onField: CardView[], select: CardClick): void {
  const slots = slotsFor(model, player, cards, onField);
  const of = (group: Group) => slots.filter(s => groupOf(model, s) === group);
  const groups: Record<Group, Slot[]> = {
    // Token creatures are placed after the card creatures, so the row's order is easier to read
    lands: of('lands'), support: of('support'),
    creatures: [...of('creatures').filter(s => !s.top.Token), ...of('creatures').filter(s => s.top.Token)], far: of('far'),
  };
  const charging = chargingAtPlayer(model);
  for (const [name, list] of Object.entries(groups)) {
    reconcile(q(root, `.${name}`), list, s => s.top.$key, createSlot, (el, s) => updateSlot(el, model, s, select, charging));
  }
  const stats = (name: Group): GroupStats => {
    const list = groups[name];
    return {
      el: q(root, `.${name}`),
      slots: list.length,
      steps: list.reduce((n, s) => n + behind(s), 0),
      depth: list.reduce((n, s) => Math.max(n, behind(s)), 0),
      battles: list.filter(s => /Battle/.test(stateOf(model, s.top).Type ?? '')).length,
    };
  };
  fitCards(root, [[[stats('lands')], [stats('support')]], [[stats('creatures')], [stats('far')]]]);
}

/** Below this size the art is too small to be useful. A board that needs smaller cards scrolls instead. */
const MIN_FIT = 0.42;
/** Cards on an empty board start at this size and shrink as the board fills, as in Arena. */
const MAX_FIT = 1.4;
const STEP = 0.02;
/** A zone splits onto a second line only when its cards grow by this much. This stops one card added or removed from switching it back and forth. */
const TWO_LINE_GAIN = 0.08;
const TWO_LINE_BELOW = 0.72;
/** How much smaller the smallest zone may get in a layout that keeps every other zone larger. */
const SMALLEST_SLACK = 0.06;

interface GroupStats {
  el: HTMLElement;
  slots: number;
  /** Cards fanned behind the group's slots, counting both attachments and pile copies. */
  steps: number;
  /** The most cards behind any one slot. */
  depth: number;
  /** Battles. They are drawn sideways, so they are wider than a normal slot. */
  battles: number;
}

/** Groups sized together, with one card size and one line count. fitCards puts each group in a zone of its own. */
type Zone = GroupStats[];
/** The zones side by side in one of a battlefield's two rows. */
type RowZones = Zone[];

/** A zone's size and line count. raw is the size before the minimum is applied. It is below the minimum when the zone overflows. */
interface Sized { fit: number; lines: number; raw: number; }

/** Sizes each zone separately. It measures the seat, not the cards, so the new card size cannot change the measurement. */
function fitCards(root: HTMLElement, rows: RowZones[]): void {
  const field = q(root, '.battlefield');
  const style = getComputedStyle(root);
  const fieldStyle = getComputedStyle(field);
  const px = (from: CSSStyleDeclaration, name: string) => parseFloat(from.getPropertyValue(name)) || 0;
  const h = px(style, '--card-h') || 123;
  const w = px(style, '--card-w') || 88;
  const air = px(style, '--slot-gap');
  const fan = px(fieldStyle, '--fan');
  const glow = px(fieldStyle, '--glow');
  const supportIndent = px(fieldStyle, '--support-indent');
  // A phone sets a minimum card width in pixels. Below it a card cannot be read or tapped
  const minWidth = px(style, '--min-card-w');
  const minFit = minWidth ? Math.max(MIN_FIT, minWidth / w) : MIN_FIT;
  const slotW = h * TAPPED_SCALE + 2 * air;
  // A group's width at card size 1, on one line or split over two
  const groupUnit = (g: GroupStats, lines: number) => {
    const total = g.slots * slotW + g.steps * w * fan + g.battles * (h - w);
    // Two lines need at most half the total width plus the widest slot, because the second line holds what does not fit on the first
    return lines === 1 ? total : total / 2 + slotW + g.depth * w * fan + (g.battles ? h - w : 0);
  };
  // The gap between a group's two lines leaves room for an attacker on the back line to move forward, with its chevron
  const lineGap = (fit: number) => 14 + (h - w * TAPPED_SCALE) / 2 * fit;
  const live = (z: Zone) => z.filter(g => g.slots > 0);
  const rowGap = (r: number) => px(fieldStyle, r === 1 ? '--row-gap-1' : '--row-gap-0') || (r === 1 ? 52 : 40);
  const zoneWidth = (z: Zone, s: Omit<Sized, 'raw'>, gap: number) =>
    live(z).reduce((n, g) => n + groupUnit(g, s.lines) * s.fit, 0) + gap * Math.max(0, live(z).length - 1);
  // Cards behind a card fan out to its left only, so they add width to a row but never height
  const zoneHeight = (s: Sized) => s.lines * h * s.fit + (s.lines - 1) * lineGap(s.fit);

  // Use offsetWidth, so a scrollbar that appears does not reduce the measured width and change the size again
  const width = field.offsetWidth - px(fieldStyle, 'padding-left') - px(fieldStyle, 'padding-right') - 2;
  // In a two-player game the seat's portrait and zones are drawn over the board's left edge (board.css). The rows must fit beside them
  const player = q(root, '.player');
  const block = getComputedStyle(player).position === 'absolute'
    ? Math.max(0, player.getBoundingClientRect().right + 16 - field.getBoundingClientRect().left - px(fieldStyle, 'padding-left')) : 0;
  const room = width - block;
  // A compact seat, like a phone's, shows the player's details in a row above the cards. The seat's padding surrounds both
  const header = root.classList.contains('compact') ? player.offsetHeight + 8 : minWidth ? player.offsetHeight : 0;
  const seatPad = px(style, 'padding-top') + px(style, 'padding-bottom');
  // The space near the phase pill for an attacker to move forward grows with the cards (board.css). So it is computed for each size tried
  const mine = root.id === 'me' || root.classList.contains('mate');
  const fieldPad = (fit: number) => {
    // A phone sets the field's padding in its own styles. It is smaller than the space computed below
    if (minWidth) return px(fieldStyle, 'padding-top') + px(fieldStyle, 'padding-bottom');
    const chevron = w * fit * 0.38;
    return mine ? Math.max(glow + 6, chevron - 2) + glow : glow + Math.max(glow + 20, chevron + 14);
  };
  const seatHeight = root.clientHeight;
  // A teammate's hand lies below their cards (board.css)
  const hand = root.querySelector<HTMLElement>(':scope > .mate-hand')?.offsetHeight ?? 0;
  const footer = hand ? hand + 8 : 0;
  const height = (fit: number) => seatHeight - header - footer - seatPad - fieldPad(fit);

  // A zone's size for the width it is given, on one line or two. It uses two lines only as TWO_LINE_GAIN allows
  const sizeFor = (z: Zone, room: number, cap: number, two: boolean, gap: number): Sized => {
    const g = live(z);
    const inner = gap * Math.max(0, g.length - 1);
    // Sizes are compared before the minimum is applied, so a second line that stops an overflow counts as a gain
    const fitOn = (lines: number) =>
      Math.min(cap, (room - inner) / Math.max(1, g.reduce((n, x) => n + groupUnit(x, lines), 0)));
    const one = fitOn(1);
    const floor = (fit: number) => Math.max(minFit, fit);
    if (!two) return { fit: floor(one), lines: 1, raw: one };
    const split = fitOn(2);
    const was = Number(z[0].el.dataset.lines ?? '1');
    const takeTwo = was === 2 ? split > one : split >= one + TWO_LINE_GAIN && one < TWO_LINE_BELOW;
    return takeTwo ? { fit: floor(split), lines: 2, raw: split } : { fit: floor(one), lines: 1, raw: one };
  };
  // Each zone gets the width it needs, up to an equal share of the row. A binary search finds the share, so the widest zone shrinks first
  const layRow = (r: number, cap: number, two: boolean): Sized[] => {
    const row = rows[r];
    const gap = rowGap(r);
    const zones = row.filter(z => live(z).length);
    const need = zones.map(z => zoneWidth(z, { fit: cap, lines: 1 }, gap));
    // The support group (permanents that are not lands or creatures) is indented from the row's end (board.css). This keeps it clear of the stack panel's edge
    const indent = r === 0 && live(rows[0][1]).length ? supportIndent : 0;
    const gaps = gap * Math.max(0, zones.length - 1) + indent;
    // A zone at the minimum size keeps that width whatever its share. The zones beside it lose the difference
    const least = zones.map(z => zoneWidth(z, { fit: minFit, lines: 1 }, gap));
    const taken = (i: number, share: number) => Math.max(least[i], Math.min(need[i], share));
    const used = (share: number) => zones.reduce((n, _, i) => n + taken(i, share), 0) + gaps;
    let lo = 0;
    let hi = room;
    for (let i = 0; i < 30; i++) {
      const mid = (lo + hi) / 2;
      if (used(mid) <= room) lo = mid;
      else hi = mid;
    }
    // Zones too wide for the row even at the minimum size are sized for the width there is. Their raw size then shows the overflow, which counts against the layout
    const over = Math.min(1, (room - gaps) / least.reduce((n, x) => n + x, 0));
    const sizes = zones.map((z, i) => sizeFor(z, taken(i, lo) * over, cap, two, gap));
    return row.map(z => sizes[zones.indexOf(z)] ?? { fit: cap, lines: 1, raw: cap });
  };
  const rowHeight = (r: number, sizes: Sized[], cap: number) => {
    const row = rows[r];
    // An empty row is still 60% of a card's height (.row's min-height)
    if (!row.some(z => live(z).length)) return 0.6 * h * cap;
    return Math.max(...row.map((z, i) => (live(z).length ? zoneHeight(sizes[i]) : 0)));
  };

  // A phone has less spare space and sets it in --fit-spare
  const spare = px(fieldStyle, '--fit-spare') || 32 + 24;
  // Each layout that fits the height is scored by its smallest raw zone size, before the minimum is applied. So less overflow wins
  const tried: { cap: number; sizes: Sized[][]; smallest: number; split: number }[] = [];
  for (let cap = MAX_FIT; cap >= minFit - 1e-9; cap -= STEP) {
    const rowA = [layRow(0, cap, false), layRow(0, cap, true)];
    const rowB = [layRow(1, cap, false), layRow(1, cap, true)];
    for (const a of rowA) {
      for (const b of rowB) {
        const sizes = [a, b];
        // The spare space (32px above the rows and 24px between them) is needed because a board filled to the pixel scrolls after rounding
        const tall = rowHeight(0, sizes[0], cap) + rowHeight(1, sizes[1], cap) + spare;
        if (tall > height(cap)) continue;
        const flat = sizes.flat();
        tried.push({ cap, sizes, smallest: Math.min(...flat.map(s => s.raw)), split: flat.filter(s => s.lines === 2).length });
      }
    }
  }
  // Among layouts within SMALLEST_SLACK of the best smallest zone, prefer the larger cap, then fewer split zones, then the larger smallest zone
  const top = Math.max(...tried.map(t => t.smallest));
  const best = tried.filter(t => t.smallest >= top - SMALLEST_SLACK)
    .sort((a, b) => b.cap - a.cap || a.split - b.split || b.smallest - a.smallest)[0];
  // When nothing fits at the minimum size, use the minimum size with two lines where allowed. The board scrolls instead of shrinking further
  const chosen = best ?? { cap: minFit, sizes: [layRow(0, minFit, true), layRow(1, minFit, true)] };
  field.classList.toggle('crowded', !best);
  root.style.setProperty('--fit', chosen.cap.toFixed(2));
  let shrinking = false;
  // A row stays centred on the board if that keeps it clear of the portrait. Otherwise it moves right just enough to clear it
  rows.forEach((row, r) => {
    const zones = row.filter(z => live(z).length);
    const indent = r === 0 && live(row[1]).length ? supportIndent : 0;
    const across = zones.reduce((n, z) => n + zoneWidth(z, chosen.sizes[r][row.indexOf(z)], rowGap(r)), 0)
      + rowGap(r) * Math.max(0, zones.length - 1) + indent;
    // On a phone a row too wide for the board starts at its edge and scrolls
    const shift = minWidth ? 0 : Math.min(block, Math.max(0, 2 * block + across - width));
    row[0][0].el.parentElement?.style.setProperty('padding-left', shift ? `${shift}px` : '');
  });
  rows.forEach((row, r) => row.forEach((zone, z) => {
    const s = chosen.sizes[r][z];
    for (const g of zone) {
      const was = parseFloat(g.el.style.getPropertyValue('--fit'));
      if (s.fit < was - 0.001) shrinking = true;
      g.el.style.setProperty('--fit', s.fit.toFixed(3));
      g.el.dataset.lines = String(s.lines);
      g.el.classList.toggle('two', s.lines === 2);
      // The split width is set at full size and scaled in CSS, so it shrinks at the same rate as the cards
      g.el.style.setProperty('--split-w', s.lines === 2 ? `${groupUnit(g, 2)}px` : '');
      // Below this size the keyword icons are too small to tell apart, so the group hides them and shows only the art
      g.el.classList.toggle('cramped', s.fit < TWO_LINE_BELOW);
    }
  }));
  // A card added to a full row overflows it while the size animates down. Overflow is clipped until then so no scrollbar appears
  if (shrinking) {
    field.classList.add('settling');
    clearTimeout(settling.get(field));
    settling.set(field, setTimeout(() => field.classList.remove('settling'), SETTLE_MS));
  }
}

/** A little longer than the --fit transition in board.css. */
const SETTLE_MS = 700;
const settling = new WeakMap<HTMLElement, ReturnType<typeof setTimeout>>();

// An attachment is drawn under the card at the end of its attachment chain, on whichever battlefield that card is
function slotsFor(model: Model, player: number, cards: CardView[], onField: CardView[]): Slot[] {
  const byKey = new Map(onField.map(c => [c.$key, c]));
  const hostOf = (c: CardView) => (c.EntityAttachedTo ? byKey.get(c.EntityAttachedTo.ref) : undefined);
  const rootOf = (c: CardView) => {
    let host = c;
    // No real attachment chain is this long. The limit stops an endless loop if a broken chain has a cycle
    for (let i = 0; i < 10; i++) {
      const next = hostOf(host);
      if (!next) break;
      host = next;
    }
    return host;
  };
  const under = new Map<number, CardView[]>();
  for (const c of onField) {
    if (!hostOf(c)) continue;
    const host = rootOf(c).$key;
    const list = under.get(host) ?? [];
    list.push(c);
    under.set(host, list);
  }
  // A card that a permanent holds in exile, such as an Oblivion Ring's, is drawn under it like an attachment, as a ghost
  for (const holder of onField) {
    // The spell a prepared creature can cast comes first, as on desktop and mobile
    const held = [holder.PreparedSpell, ...(holder.UntilLeavesBattlefield ?? [])]
      .map(r => r && model.objects.get(r.ref) as CardView | undefined)
      .filter((c): c is CardView => c?.Zone === 'Exile');
    if (!held.length) continue;
    const host = rootOf(holder).$key;
    under.set(host, [...(under.get(host) ?? []), ...held]);
  }
  const hosts = cards.filter(c => !hostOf(c));
  const marks = [
    new Set((model.prompt?.selectable ?? []).map(r => r.ref)),
    new Set(model.prompt?.highlighted ?? []),
    new Set((model.playable?.cards ?? []).map(r => r.ref)),
    new Set((model.playable?.autoTap ?? []).map(r => r.ref)),
  ];
  const roles = combatRoles(model);
  // A targeted card leaves its pile and keeps its targeting items, so the next copy can be picked and differently targeted cards stay apart
  const targeted = new Map<number, number[]>();
  for (const item of derefAll(model, game(model)?.Stack)) {
    for (const o of stackTargets(model, item)) targeted.set(o.$key, [...(targeted.get(o.$key) ?? []), item.$key]);
  }
  // A pile the player opened stays open while its cards gain and lose marks, so it is keyed by the cards' lasting look
  const opened = (look: string) => `${player}\u0001${look}`;
  const piles = new Map<string, Slot>();
  const slots: Slot[] = [];
  for (const c of hosts) {
    const attached = under.get(c.$key) ?? [];
    const look = attached.length ? null : signature(model, c);
    const open = look && opened(look);
    const sig = look && [look, ...marks.map(m => m.has(c.$key)), roles.get(c.$key) ?? '', targeted.get(c.$key)?.join('+') ?? ''].join('\u0000');
    // A pile the player has opened shows its cards separately until the player closes it
    const pile = sig && open && !ui.openPiles.has(open) && piles.get(sig);
    if (pile) {
      pile.members.push(c);
      continue;
    }
    const slot: Slot = { top: c, members: [c], attached, sig: open };
    if (sig) piles.set(sig, slot);
    slots.push(slot);
  }
  // An opened pile whose cards have all gone is forgotten, so a later card of the same kind arrives in a pile
  const here = new Set(slots.map(s => s.sig));
  for (const key of ui.openPiles) {
    if (key.startsWith(opened('')) && !here.has(key)) ui.openPiles.delete(key);
  }
  // Cards with the same name stay together, in the order the name first appears. So a pile that splits stays in place
  const firstOf = new Map<string, number>();
  slots.forEach((slot, i) => {
    const name = stateOf(model, slot.top).Name ?? '';
    if (!firstOf.has(name)) firstOf.set(name, i);
  });
  const order = new Map(slots.map((slot, i) => [slot, i]));
  const rank = (slot: Slot) => firstOf.get(stateOf(model, slot.top).Name ?? '') ?? 0;
  // Within a name, untapped cards come first and tapped cards after them. So a land tapped out of its pile moves to the right
  const tapped = (slot: Slot) => Number(!!slot.top.Tapped);
  return slots.sort((a, b) => rank(a) - rank(b) || tapped(a) - tapped(b) || order.get(a)! - order.get(b)!);
}

// Cards with the same signature pile together. So it includes everything a player can see on the card. A property never set gives the same value as one reset to its default.
// What a player can act on (the prompt's marks, combat roles, targets from the stack) is added by slotsFor
export function signature(model: Model, card: CardView): string | null {
  // Face-down cards look the same but can be different cards, so each one keeps its own slot
  if (!model.visible.has(card.$key) || card.Facedown) return null;
  const s = stateOf(model, card);
  // A land played this turn is the same as the lands played before it, so it goes in their pile
  return [s.Name, s.Power ?? null, s.Toughness ?? null, s.Loyalty && s.Loyalty !== '0' ? s.Loyalty : null, !!card.Tapped,
    counters(card.Counters), card.Damage ?? 0, !!card.Attacking, !!card.Blocking, isSick(model, card), !!card.PhasedOut,
    !!card.Detained, !!card.Token, !!card.Cloned, card.EntityAttachedTo?.ref ?? null, !!card.IsRingBearer, card.Stickers ?? null,
    (s.Keywords ?? []).map(k => k.title).sort().join(','), (card.MustBlockCards ?? []).map(r => r?.ref).join(','), card.BlockAdditional ?? 0, !!card.BlockAny,
    card.ClassLevel ?? null, card.CurrentRoom ?? null, card.Sprocket ?? null, (card.AttractionLights ?? []).join(','), card.Intensity ?? null].join('\u0000');
}

/** What each card does in the combat: the attacker it blocks, or for an attacker, what it attacks and who blocks it. Cards with different roles do not pile together. */
function combatRoles(model: Model): Map<number, string> {
  const roles = new Map<number, string>();
  for (const band of game(model)?.CombatView ?? []) {
    const attackers = (band.attackers ?? []).flatMap(r => (r ? [r.ref] : []));
    // Blocks being declared are planned blockers until the declaration is done
    const blockers = [...band.blockers ?? [], ...band.plannedBlockers ?? []].flatMap(r => (r ? [r.ref] : []));
    for (const a of attackers) roles.set(a, `attacks ${band.defender?.ref ?? ''} blocked by ${blockers.join('+')}`);
    for (const b of blockers) roles.set(b, `${roles.get(b) ?? 'blocks'} ${attackers.join('+')}`);
  }
  return roles;
}

/** Counters as they are shown, in a fixed order. No counters, an empty list and zero counts all give the same result. */
function counters(all: Record<string, number> | null | undefined): string {
  return Object.entries(all ?? {}).filter(([, n]) => n > 0).sort(([a], [b]) => a.localeCompare(b))
    .map(([name, n]) => `${name}:${n}`).join(',');
}

// The engine's Sickness flag means only "came under your control this turn", so creature type and haste are checked here
function isSick(model: Model, card: CardView): boolean {
  const state = stateOf(model, card);
  return !!card.Sickness && /Creature/.test(state.Type ?? '') && !state.Keywords?.some(k => k.icon === 'IMG_ABILITY_HASTE');
}

/**
 * The part of combat this player is declaring. While they declare, the host sends the creatures that can still attack or block as the playable cards.
 * A finished declaration empties that list, but its prompt stays up until the next one replaces it. With no playable card, nothing is shown as left out.
 */
function declaring(model: Model): 'attack' | 'block' | null {
  const p = model.prompt;
  if (!p || p.priority || model.spectating || !model.playable?.cards.length) return null;
  if (p.cancel.kind === 'alphaStrike' || p.cancel.kind === 'callBack') return 'attack';
  return declaringBlocks(model) ? 'block' : null;
}

/**
 * Dims a creature of yours that the host left out of the attack or block being declared, and strikes through a sword or shield.
 * A tapped or phased-out creature already looks unable, and a summoning-sick one keeps its Zz. So the strike means an ability or effect.
 * The host sends no reason, so only Defender, which is on the card, is named.
 */
function showBarred(el: HTMLElement, model: Model, card: CardView): void {
  const step = declaring(model);
  const state = stateOf(model, card);
  const barred = !!step && card.Zone === 'Battlefield' && card.Controller?.ref === me(model)?.$key && /Creature/.test(state.Type ?? '')
    && !card.Tapped && !card.PhasedOut && !(model.playable?.cards ?? []).some(r => r.ref === card.$key)
    && (step === 'attack' ? !card.Attacking && !isSick(model, card) : !card.Blocking);
  const why = barred && step === 'attack' ? state.Keywords?.find(k => k.icon === 'IMG_ABILITY_DEFENDER')?.title ?? '' : '';
  const shown = barred ? `${step}:${why}` : '';
  if ((el.dataset.barred ?? '') === shown) return;
  el.dataset.barred = shown;
  el.classList.toggle('barred', barred);
  const mark = q(el, '.barred-mark');
  mark.title = !barred ? '' : t(step === 'attack' ? 'lblWebCardCantAttack' : 'lblWebCardCantBlock');
  mark.innerHTML = !barred ? '' : `<svg viewBox="0 0 24 24" aria-hidden="true"><g>${step === 'attack' ? ICONS.sword : ICONS.shield}</g>`
    + '<path class="under" d="M3 21 21 3"/><path class="strike" d="M3 21 21 3"/></svg>';
  if (why) {
    const tag = make('b', '');
    tag.textContent = why;
    mark.append(tag);
  }
}

function createSlot(): HTMLElement {
  const el = make('div', 'slot');
  return el;
}

function updateSlot(el: HTMLElement, model: Model, slot: Slot, select: CardClick, charging: Set<number>): void {
  // The card the others are attached to comes last, so it is drawn over them
  const cards = [...slot.attached, slot.top];
  reconcile(el, cards, c => c.$key, () => createCard(select), (c, card) => {
    // Set before updateCard, because a face-down ghost is drawn as its sleeve
    c.classList.toggle('ghost', card.Zone === 'Exile');
    updateCard(c, model, card);
    // The engine also flags creatures off the battlefield as sick, so the mark is shown only on battlefield cards
    c.classList.toggle('sickness', card.Zone !== 'Exile' && isSick(model, card));
    // A land among the creatures gets a tag that explains why, because its art still looks like a land
    const type = stateOf(model, card).Type ?? '';
    q(c, '.kind-tag').textContent = /\bLand\b/.test(type) && /Creature/.test(type) ? t('lblWebBoardLandCreature') : '';
    showBarred(c, model, card);
  });
  [...el.children].forEach((c, i) => (c as HTMLElement).style.setProperty('--under', String(i)));
  el.style.setProperty('--behind', String(behind(slot)));
  el.classList.toggle('attacking', !!slot.top.Attacking && combatShown(model));
  el.classList.toggle('charging', charging.has(slot.top.$key));
  const sig = slot.sig;
  const opened = !!sig && ui.openPiles.has(sig);
  const spreadable = opened || (!!sig && slot.members.length > 1);
  const top = el.lastChild as HTMLElement;
  setPileCount(top, slot.members.length, opened);
  const count = q(top, '.count');
  count.onclick = spreadable && sig ? e => {
    e.stopPropagation();
    // The cards animate out of the pile, or back into it, instead of just appearing beside it
    const keys = slot.members.map(c => String(c.$key));
    const from = top.getBoundingClientRect();
    changeUi(u => {
      if (u.openPiles.delete(sig)) {
        mergeInto(keys, from);
      } else {
        u.openPiles.add(sig);
        spreadFrom(keys, from);
      }
    });
  } : null;
  el.dataset.members = slot.members.map(c => c.$key).join(',');
}
