// The stack holds the spells and abilities waiting to resolve. It is drawn as a pile beside the board, or as a chip under the phase strip on a phone

import { reconcile } from './reconcile';
import { lastPicture } from './cards';
import { cardImageSrc, noImageOnError, setImage } from '../images';
import { game, deref, derefAll, me, players, stackPick, stateOf, zone, type Model } from '../model';
import { isPortrait } from '../form';
import { hoverCard, hoverable, inspectCard } from '../preview';
import { FIZZLE_MS, SETTLE_MS, journeys, noteFizzled } from './motion';
import { notePick, stackTargets } from './overlay';
import { hovers, longPress } from '../press';
import { byId, q, make, onRelayout, reducedMotion } from '../dom';
import { changeUi, ui } from '../ui';
import type { Actions } from '../actions';
import { t } from '../text';
import type { CardView, GameEvent, StackItemView } from '../protocol';
import { oncePer } from '../sheet';

// The item that resolves next is at the top of the pile. The other items are offset downward behind it
const STEP_MAX = 42;
const STEP_MIN = 14;
const PUSH_Y = 26;

let actions: Actions | null = null;

/** Your spells being cast, by card key. The card is missing from the browser's game until its cost is paid, so its picture is kept here. motion.ts animates each one to the place drawn for it here. */
const awaiting = new Map<string, { src: string; zoom: string; since: number; name: string }>();

/** The model last drawn. The phone's chip also stores, between draws, how many items it held and the prompt during which it was folded. */
let current: Model | null = null;
let countSeen = 0;
let foldedFor: object | null = null;
const foldsForPick = oncePer<object>();
const opensForPick = oncePer<number>();
const opensForTargets = oncePer<object>();

function fold(folded: boolean): void {
  foldedFor = folded ? current?.prompt ?? null : null;
  changeUi(u => { u.stackCollapsed = folded; });
}

/** Opens the stack if it is folded. The keyboard shortcut that shows the stack calls this. */
export function showStack(): void {
  if (ui.stackCollapsed) fold(false);
}

/** A tap on the board folds the phone's list, so the cards under it can be reached. */
export function foldStack(): void {
  if (isPortrait() && !ui.stackCollapsed) fold(true);
}

export function initStack(actionsFor: Actions): void {
  actions = actionsFor;
}

export function renderStack(model: Model, events: readonly GameEvent[]): void {
  const root = byId('stack');
  if (!root.firstChild) {
    root.innerHTML = '<div class="head"><b></b><span class="count"></span><button class="collapse"></button></div><div class="pile"></div>';
    q(root, '.head b').textContent = t('lblStack');
    q(root, '.collapse').onclick = () => fold(!ui.stackCollapsed);
    // On a phone the whole head is the chip that opens and folds the list
    q(root, '.head').onclick = e => {
      if (isPortrait() && !(e.target as Element).closest('.collapse')) fold(!ui.stackCollapsed);
    };
    onRelayout(() => place(root));
  }
  const items: StackItemView[] = derefAll(model, game(model)?.Stack);
  // A spell's card flies onto the stack. An ability or a trigger has no card move, so it grows into place instead
  const flown = new Set([...awaiting.keys(), ...[...journeys(events)].filter(([, m]) => m.to?.zone === 'Stack').map(([k]) => k)]);
  noteAwaiting(model, items, events);
  if (ui.hoveredStackItem !== null && !items.some(i => i.$key === ui.hoveredStackItem)) {
    ui.hoveredStackItem = null;
  }
  current = model;
  const portrait = isPortrait();
  root.classList.toggle('chip', portrait);
  const count = items.length + awaiting.size;
  if (portrait) {
    // The list opens when a new item arrives and you have priority. It stays folded if you folded it during this same prompt
    if (count > countSeen && model.prompt?.priority && foldedFor !== model.prompt) ui.stackCollapsed = false;
    // The list folds for a prompt answered on the board or from the hand, because the list would cover those cards
    const asked = model.prompt;
    if (asked && !asked.priority && !asked.paying && (asked.selectable.length > 0 || asked.selectablePlayers.length > 0) && foldsForPick(asked)) {
      ui.stackCollapsed = true;
    }
    const top = items[0];
    const source = top ? deref(model, top.SourceCard) as CardView | undefined : undefined;
    // A spell still being paid for has no item yet. Its name comes from the card it was cast from
    const paying = top ? '' : [...awaiting.values()][0]?.name ?? '';
    q(root, '.head b').textContent = top
      ? `${t('lblStack')} \u00b7 ${source ? stateOf(model, source).Name ?? '' : ''} (${deref(model, top.ActivatingPlayer)?.Name ?? ''})`
      : paying ? `${t('lblStack')} \u00b7 ${paying} (${me(model)?.Name ?? ''})` : t('lblStack');
    // The dock is drawn after this and can change height. That moves the phase strip the chip is placed on
    requestAnimationFrame(() => place(root));
    // The list opens over the half with fewer of the top item's targets, so they stay in view. On a tie it opens over the opponent's half
    const targets = top ? stackTargets(model, top) : [];
    const mine = targets.filter(o => o.$key === me(model)?.$key || document.querySelector(`#me .card[data-key="${o.$key}"]`)).length;
    root.classList.toggle('over-them', mine >= targets.length - mine);
  } else {
    q(root, '.head b').textContent = t('lblStack');
  }
  countSeen = count;
  const pick = stackPick(model);
  // A spell is picked on the list, so a folded list opens once for that prompt
  if (opensForPick(pick?.id ?? null)) ui.stackCollapsed = false;
  // A target on the stack is also clicked on the list, so the list opens once while the prompt asks for one
  const offered = model.prompt?.selectableStack ?? [];
  if (opensForTargets(offered.length > 0 ? model.prompt : null)) ui.stackCollapsed = false;
  const collapsed = ui.stackCollapsed;
  showPanel(root, items.length + awaiting.size > 0);
  root.classList.toggle('collapsed', collapsed);
  q(root, '.count').textContent = String(items.length + awaiting.size);
  const collapse = q(root, '.collapse');
  collapse.textContent = t(collapsed ? 'lblShow' : 'lblWebStackHide');
  collapse.title = t(collapsed ? 'lblWebStackShowTitle' : 'lblWebStackHideTitle');
  const pile = q(root, '.pile');
  // A spell awaiting payment is placed at the top, where its item will appear. So paying for it moves nothing
  const entries: (StackItemView | string)[] = [...awaiting.keys(), ...items];
  showLeaving(pile, new Set(entries.map(e => typeof e === 'string' ? `awaiting-${e}` : String(e.$key))), events);
  const arriving: { el: HTMLElement; source: string }[] = [];
  reconcile(pile, entries, e => typeof e === 'string' ? `awaiting-${e}` : e.$key,
    e => {
      if (typeof e === 'string') return createAwaiting(e);
      const el = createItem(model);
      el.dataset.fresh = '1';
      return el;
    }, (el, e) => {
      if (typeof e === 'string') return;
      updateItem(el, model, e);
      if (el.dataset.fresh) {
        delete el.dataset.fresh;
        if (!flown.has(String(e.SourceCard?.ref)) && !reducedMotion()) arriving.push({ el, source: String(e.SourceCard?.ref ?? '') });
      }
      el.classList.toggle('targetable', (pick?.stackKeys ?? []).includes(e.$key) || offered.includes(e.$key));
      el.classList.toggle('highlighted', (model.prompt?.highlighted ?? []).includes(e.$key));
    });
  place(root);
  layout(pile, entries.length);
  // Measured here, after every item has its position in the pile
  for (const { el, source } of arriving) arrive(el, source, collapsed);
  renderStorm(root, game(model)?.StormCount ?? 0, entries.length > 0);
}

/** The fade-out that runs when the last item leaves. It is kept so a render during the fade does not show the panel again or hide it at once. */
let closing: Animation | null = null;

/** Fades the panel in or out by opacity only, because a scale or slide would move the slot a flying spell is aimed at. */
function showPanel(root: HTMLElement, shown: boolean): void {
  const reduced = reducedMotion();
  if (shown) {
    if (closing) {
      closing.cancel();
      closing = null;
    } else if (root.hidden && !reduced) {
      root.hidden = false;
      root.animate([{ opacity: 0 }, { opacity: 1 }], { duration: PANEL_FADE_MS, easing: 'ease-out' });
    }
    root.hidden = false;
    return;
  }
  if (root.hidden || closing) return;
  if (reduced) {
    root.hidden = true;
    return;
  }
  closing = root.animate([{ opacity: 1 }, { opacity: 0 }], { duration: PANEL_FADE_MS, easing: 'ease-in', fill: 'forwards' });
  closing.finished.then(() => {
    root.hidden = true;
    closing?.cancel();
    closing = null;
  }, () => { /* cancelled: an item came back */ });
}

const PANEL_FADE_MS = 150;

/** Shows the count of spells cast this turn. It is under the stack while the stack has items, and otherwise beside the bottom of the prompt. */
function renderStorm(stack: HTMLElement, count: number, stacked: boolean): void {
  let chip = document.getElementById('storm');
  if (!chip) {
    chip = Object.assign(document.createElement('div'), { id: 'storm', title: t('lblWebStackStormTitle') });
    stack.after(chip);
  }
  chip.hidden = count <= 0;
  if (chip.hidden) return;
  chip.textContent = t('lblWebStackStorm', count);
  // Measured against the pile's target height, not its height partway through the transition
  const pile = q(stack, '.pile');
  const going = parseFloat(pile.style.height);
  const easing = pile.offsetParent && Number.isFinite(going) ? going - pile.offsetHeight : 0;
  // Checks whether the stack has items, not whether the panel shows. The panel is still fading out after it empties
  const prompt = document.getElementById('prompt');
  if (!stacked && prompt) {
    const at = prompt.getBoundingClientRect();
    chip.style.top = `${at.bottom - chip.offsetHeight}px`;
    chip.style.right = `${innerWidth - at.left + 8}px`;
    return;
  }
  chip.style.right = '';
  chip.style.top = stack.hidden ? '' : `${stack.getBoundingClientRect().bottom + easing + 8}px`;
}

// The panel starts at the top of the board and ends above the hand
function place(root: HTMLElement): void {
  if (isPortrait()) {
    // The chip covers the phase strip while the stack has items. Its list opens down over your cards or up over the opponent's
    const strip = byId('phase-strip').getBoundingClientRect();
    const above = root.classList.contains('over-them') && !ui.stackCollapsed;
    // The chip's head is taller than the strip so a finger can tap it. So the head is centred on the strip
    const over = Math.max(0, (q(root, '.head').offsetHeight - strip.height) / 2);
    root.style.top = above ? 'auto' : `${Math.round(strip.top - over)}px`;
    root.style.bottom = above ? `${Math.round(innerHeight - strip.bottom - over)}px` : 'auto';
    return;
  }
  root.style.top = root.style.bottom = '';
  const hand = byId('hand').getBoundingClientRect();
  q(root, '.pile').style.setProperty('--stack-room', `${Math.max(120, hand.top - root.getBoundingClientRect().top - 48)}px`);
}

function noteAwaiting(model: Model, items: StackItemView[], events: readonly GameEvent[]): void {
  const onStack = new Set(items.map(i => String(i.SourceCard?.ref)));
  for (const [key, move] of journeys(events)) {
    // As on desktop and mobile, another player's spell shows on the stack only once it is paid for
    const mine = !!move.caster && model.localPlayers.includes(move.caster.ref);
    // Read before the card's old zone is redrawn without it
    const img = document.querySelector<HTMLImageElement>(`.card[data-key="${key}"] img, .zone-tile img[data-key="${key}"]`);
    // The card may already be gone or hidden when its move arrives, so the last picture it had is used
    const picture = img?.getAttribute('src') ? { src: img.getAttribute('src') as string, zoom: img.dataset.zoom ?? '' }
      : lastPicture(Number(key));
    if (move.to?.zone === 'Stack' && mine && !onStack.has(key) && picture) {
      // The name is kept too, for the phone's chip to show
      awaiting.set(key, { ...picture, since: Date.now(), name: lastPicture(Number(key))?.name ?? '' });
    } else {
      awaiting.delete(key);
    }
  }
  for (const [key, spell] of awaiting) {
    // A cancelled cast is undone without an event. The card is then back in a zone the browser can see
    const back = model.objects.get(Number(key)) as CardView | undefined;
    const given = model.prompt?.priority && Date.now() - spell.since > SETTLE_MS;
    // A card revealed earlier in the game can still name Hand while it is cast. So the zone it names must also contain it
    const returned = !!back?.Zone && back.Zone !== 'Stack' && players(model).some(p => zone(model, p, back.Zone!).some(c => c.$key === back.$key));
    if (onStack.has(key) || returned || given) {
      awaiting.delete(key);
    }
  }
}

function createAwaiting(key: string): HTMLElement {
  const spell = awaiting.get(key);
  const el = make('div', 'stack-item awaiting');
  el.innerHTML = '<img alt="" draggable="false"><div class="await"></div>';
  q(el, '.await').textContent = t('lblWebStackAwaitingPayment');
  const img = q<HTMLImageElement>(el, 'img');
  img.src = spell?.src ?? '';
  img.dataset.key = key;
  img.dataset.zoom = spell?.zoom ?? '';
  hoverable(el, img);
  el.addEventListener('pointerdown', e => longPress(e, () => inspectCard(img)));
  return el;
}

function createItem(model: Model): HTMLElement {
  const el = make('div', 'stack-item');
  el.innerHTML = '<img alt="" draggable="false"><div class="frame"></div><div class="caption"><div class="who"></div><div class="desc"></div><div class="targets"></div></div>';
  const img = q<HTMLImageElement>(el, 'img');
  noImageOnError(el, img);
  // When the prompt asks for a spell, it is picked here on the stack, where the spell already is
  el.addEventListener('click', () => {
    const pick = stackPick(model);
    const index = (pick?.stackKeys ?? []).indexOf(Number(el.dataset.key));
    if (pick && index >= 0) {
      actions?.answer(pick.id, [index]);
    } else if ((current?.prompt?.selectableStack ?? []).includes(Number(el.dataset.key))) {
      notePick(Number(el.dataset.key));
      actions?.selectStackItem(Number(el.dataset.key));
    }
  });
  el.addEventListener('pointerenter', e => {
    if (!hovers(e)) return;
    const pile = el.parentElement as HTMLElement;
    // The item is lifted at once, not on the next frame, so the pile follows the pointer as it moves along it
    ui.hoveredStackItem = Number(el.dataset.key);
    layout(pile, pile.childElementCount);
    hoverCard(img);
  });
  el.addEventListener('pointerdown', e => longPress(e, () => inspectCard(img)));
  // The desktop stack menu. It offers auto-yield, always accept or decline your optional trigger, and yield to the stack
  el.addEventListener('contextmenu', e => {
    e.preventDefault();
    const key = Number(el.dataset.key);
    changeUi(u => { u.stackMenuAt = { key, x: e.clientX, y: e.clientY }; });
    actions?.askStackMenu(key);
  });
  el.addEventListener('pointerleave', e => {
    if (!hovers(e)) return;
    const pile = el.parentElement as HTMLElement;
    ui.hoveredStackItem = null;
    layout(pile, pile.childElementCount);
    hoverCard(null);
  });
  return el;
}

/** Animates a new ability or trigger out of its card on the board. If that card is not on the board, the item grows into place. */
function arrive(el: HTMLElement, source: string, collapsed: boolean): void {
  const from = collapsed || !source ? null : document.querySelector(`.battlefield .card[data-key="${source}"]`)?.getBoundingClientRect();
  const to = el.getBoundingClientRect();
  if (!from?.width || !to.width) {
    el.animate([{ opacity: 0, scale: '.85' }, { opacity: 1, scale: '1' }], { duration: 200, easing: 'cubic-bezier(.2, .8, .3, 1)' });
    return;
  }
  const dx = from.left + from.width / 2 - (to.left + to.width / 2), dy = from.top + from.height / 2 - (to.top + to.height / 2);
  el.animate([{ translate: `${dx}px ${dy}px`, scale: String(from.width / to.width), opacity: .4 }, { translate: '0px 0px', scale: '1', opacity: 1 }],
    { duration: 320, easing: 'cubic-bezier(.2, .7, .3, 1)' });
}

/** How long a resolved ability takes to fade out. */
const GONE_MS = 140;

/**
 * Animates the items that leave the stack this frame, before their elements are removed. A resolving spell's card flies away, so only
 * an ability fades. An item that fizzled, or was removed while something else resolved, comes apart in place.
 */
function showLeaving(pile: HTMLElement, staying: Set<string>, events: readonly GameEvent[]): void {
  if (reducedMotion()) return;
  const resolved = events.flatMap(e => e.kind === 'stackResolved' ? [e] : []);
  const somethingResolved = resolved.length > 0;
  for (const el of [...pile.children] as HTMLElement[]) {
    const box = el.getBoundingClientRect();
    if (staying.has(el.dataset.key ?? '') || el.classList.contains('awaiting') || !box.width) continue;
    const source = el.querySelector('img')?.dataset.key ?? '';
    const at = resolved.findIndex(e => String(e.source?.ref ?? '') === source);
    const failed = at < 0 ? somethingResolved : resolved.splice(at, 1)[0].fizzled;
    const ability = !!el.dataset.ability;
    if (!failed && !ability) continue;
    const ghost = el.cloneNode(true) as HTMLElement;
    ghost.classList.remove('top', 'lifted', 'targetable', 'highlighted');
    ghost.style.cssText += `position: fixed; left: ${box.left}px; top: ${box.top}px; width: ${box.width}px; height: ${box.height}px; margin: 0; z-index: 41; pointer-events: none;`;
    document.body.append(ghost);
    if (failed) {
      unweave(ghost, box);
      // The failed spell's card is not animated to its next zone (motion.ts)
      if (!ability && source) noteFizzled(source);
    } else {
      ghost.animate([{ opacity: 1, scale: '1' }, { opacity: 0, scale: '.92' }], { duration: GONE_MS, easing: 'ease-in' })
        .finished.then(() => ghost.remove(), () => ghost.remove());
    }
  }
}

let unwoven = 0;

/**
 * A failed spell disappears in patches, with a thin pale glow along each edge, and a few small dots drift up.
 * An SVG filter keeps the item only where a noise value is above a rising level. It draws the glow in the band just below that level.
 */
function unweave(ghost: HTMLElement, box: DOMRect): void {
  const id = `unweave-${++unwoven}`;
  const cut = (level: number) => `0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  14 0 0 0 ${(-14 * level).toFixed(3)}`;
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('width', '0');
  svg.setAttribute('height', '0');
  svg.style.position = 'absolute';
  svg.innerHTML = `<filter id="${id}" x="-15%" y="-15%" width="130%" height="130%" color-interpolation-filters="sRGB">`
    + `<feTurbulence type="fractalNoise" baseFrequency="0.03" numOctaves="3" seed="${unwoven}" result="noise"/>`
    + `<feColorMatrix class="keep" in="noise" type="matrix" values="${cut(0.2)}" result="keep"/>`
    + `<feColorMatrix class="lit" in="noise" type="matrix" values="${cut(0.17)}" result="lit"/>`
    + '<feComposite in="SourceGraphic" in2="keep" operator="in" result="item"/>'
    + '<feFlood flood-color="#c6d4ff" flood-opacity=".85"/><feComposite in2="lit" operator="in"/><feComposite in2="keep" operator="out"/>'
    + '<feComposite in2="SourceAlpha" operator="in" result="edge"/><feGaussianBlur in="edge" stdDeviation="1.3" result="halo"/>'
    + '<feMerge><feMergeNode in="item"/><feMergeNode in="halo"/><feMergeNode in="edge"/></feMerge></filter>';
  document.body.append(svg);
  const keep = svg.querySelector('.keep') as Element, lit = svg.querySelector('.lit') as Element;
  const began = performance.now();
  const step = (now: number) => {
    const t = now - began;
    if (t >= FIZZLE_MS || !ghost.isConnected) {
      ghost.remove();
      svg.remove();
      return;
    }
    const level = 0.2 + 0.62 * Math.min(1, t / (FIZZLE_MS - 40));
    keep.setAttribute('values', cut(level));
    lit.setAttribute('values', cut(level - 0.028));
    const grey = Math.min(1, t / (FIZZLE_MS / 2));
    ghost.style.filter = `grayscale(${grey}) brightness(${1 - 0.25 * grey}) url(#${id})`;
    requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
  for (let i = 0; i < 10; i++) {
    const mote = make('span', 'unweave-mote');
    mote.style.left = `${box.left + 8 + Math.random() * (box.width - 16)}px`;
    mote.style.top = `${box.top + 20 + Math.random() * (box.height - 30)}px`;
    document.body.append(mote);
    mote.animate([{ opacity: 0, translate: '0 0' }, { opacity: .55, offset: 0.4 }, { opacity: 0, translate: `${Math.random() * 20 - 10}px ${-26 - Math.random() * 34}px` }],
      { duration: 420, delay: 60 + Math.random() * (FIZZLE_MS - 260), easing: 'ease-out', fill: 'backwards' })
      .finished.then(() => mote.remove(), () => mote.remove());
  }
}

function updateItem(el: HTMLElement, model: Model, item: StackItemView): void {
  el.dataset.ability = item.Ability ? '1' : '';
  const source = deref(model, item.SourceCard);
  const state = source ? stateOf(model, source) : {};
  const src = cardImageSrc(model, source);
  const img = q<HTMLImageElement>(el, 'img');
  setImage(img, src);
  el.classList.toggle('noimg', !src);
  img.dataset.key = String(source?.$key ?? '');
  img.dataset.zoom = src;
  q(el, '.frame').textContent = state.Name ?? '';
  q(el, '.who').textContent = deref(model, item.ActivatingPlayer)?.Name ?? '';
  // A spell's image shows what it does. An ability needs its text
  q(el, '.desc').textContent = item.Ability || !src ? (item.Description ?? '') : '';
  const targets = stackTargets(model, item).map(t => t.Name ?? stateOf(model, t).Name ?? '?');
  q(el, '.targets').textContent = targets.length ? `→ ${targets.join(', ')}` : '';
}

// A strip of every item stays visible. A deep stack puts the items closer together
function layout(pile: HTMLElement, n: number): void {
  const items = [...pile.children] as HTMLElement[];
  if (!items.length) {
    return;
  }
  // A phone shows the stack as a numbered list. The offset pile of cards is for desktop only
  if (isPortrait()) {
    pile.style.height = '';
    items.forEach((el, i) => {
      el.style.top = '';
      el.dataset.n = String(i + 1);
    });
    return;
  }
  const h = items.findIndex(el => Number(el.dataset.key) === ui.hoveredStackItem);
  const card = items[0].offsetHeight;
  const room = parseFloat(getComputedStyle(pile).getPropertyValue('--stack-room')) || 400;
  const step = n > 1 ? Math.min(STEP_MAX, Math.max(STEP_MIN, (room - card) / (n - 1))) : 0;
  pile.style.height = `${(n - 1) * step + card}px`;
  items.forEach((el, i) => {
    const push = h < 0 || i === h ? 0 : (i < h ? -PUSH_Y : PUSH_Y);
    el.style.top = `${i * step + push}px`;
    el.style.zIndex = String(h < 0 ? n - i : 200 - Math.abs(i - h) * 10 + (i === h ? 5 : 0));
    el.classList.toggle('top', i === 0);
    el.classList.toggle('lifted', i === h);
  });
}

export function resetStack(): void {
  awaiting.clear();
  countSeen = 0;
  foldedFor = null;
  current = null;
}
