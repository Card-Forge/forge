// The card preview used by every page. It shows a large picture beside the pointer, or a sheet after a long press on a phone.
// An element opts in with data attributes. data-image holds its image key. data-act labels the sheet button that clicks the element.
// data-menu means the element has a right-click menu the sheet can open. data-list marks a container whose cards the sheet arrows step through.
// Match cards opt in through hoverable() instead, and carry data-zoom, data-key and data-from.

import { appendSymbolText, cardImageSrc, imageUrl, setImage, setSymbolText } from './images';
import { showFoil, tiltFoil } from './match/foil';
import { changesOf, plain, sortRulesText, type Block, type Changes, type Line, type SortedText } from './match/rulestext';
import { byId, pointerOn, q, replay, make, placeBeside } from './dom';
import { changeUi, redrawUi, ui } from './ui';
import { keyName } from './keys';
import { boundKeys } from './settings';
import type { Actions } from './actions';
import { deref, stateOf, type Model } from './model';
import { isAiming } from './match/overlay';
import { drawStickers } from './match/stickers';
import { hovers, longPress } from './press';
import { openedOver, sheet, swipeAcross, swipeDown } from './sheet';
import type { CardFace, CardView, KeywordText, PlayerDetail, PlayerView } from './protocol';
import { t, type TextKey } from './text';
import { conditionsOf, type Progress } from './match/thresholds';
import { COMMANDER_LETHAL, COMMANDER_WARNING, frameColour, ptShift } from './match/gamerules';

// The host builds a hovered card's text with CardDetailUtil, as desktop does. The text reaches the model shortly after the hover starts

let actions: Actions | null = null;

export function initDetail(actionsFor: Actions): void {
  actions = actionsFor;
  document.addEventListener('pointerover', e => {
    // A card removed from the page under the pointer never fires pointerout, so its preview would stay open
    if (hoverGone()) hoverCard(null);
    const item = menuItem(e.target);
    // Skip a card that is still animating in, such as a new draft pack card. It moved under the pointer, the pointer did not move onto it
    if (item && hovers(e) && !within(item, e.relatedTarget) && !item.getAnimations().length) hoverCard(item);
  });
  document.addEventListener('pointerout', e => {
    const item = menuItem(e.target);
    if (item && hovers(e) && !within(item, e.relatedTarget)) hoverCard(null);
  });
  document.addEventListener('pointerdown', e => {
    const item = menuItem(e.target);
    if (item) longPress(e, () => inspectCard(item));
  });
  // The preview ignores pointer events, so the foil streaks follow the pointer's position on the hovered card
  document.addEventListener('pointermove', e => {
    const hover = ui.hover;
    if (!hover || !('card' in hover) || !hover.at) return;
    const at = pointerOn(hover.at, e);
    tiltFoil(at.x, at.y);
  });
}

/** A card line or tile in a menu. It holds its image key in data-image and previews like a card on the board. */
const menuItem = (target: EventTarget | null): HTMLElement | null => (target instanceof Element ? target.closest<HTMLElement>('[data-image]') : null);

const within = (el: HTMLElement, other: EventTarget | null): boolean => other instanceof Node && el.contains(other);

/** The image URL an element previews. A match card holds it in data-zoom. A menu item holds an image key in data-image. */
const previewSrc = (el: HTMLElement): string => el.dataset.zoom ?? (el.dataset.image ? imageUrl(el.dataset.image) : '');

const hoverGone = () => {
  const hover = ui.hover;
  if (!hover || !('card' in hover) || !hover.at) return false;
  // A hidden page keeps its items in the DOM with no layout boxes. The lobby does this when its match starts
  return !hover.at.isConnected || (!ui.inspect && hover.at.getClientRects().length === 0);
};

/** Turns the hovered card to its next face. It needs the model because the model holds the face count. */
export function nextFace(model: Model): void {
  const hover = ui.hover;
  const count = hover && 'card' in hover && hover.card !== null ? model.cardDetails.get(hover.card)?.faces.length ?? 0 : 0;
  if (count > 1) {
    changeUi(u => { u.faceIndex = (u.faceIndex + 1) % count; });
  }
}

/** How long the pointer must stay on a card or a player before a preview opens. An open preview moves to the next one at once. */
const HOVER_DELAY_MS = 300;
let opening: ReturnType<typeof setTimeout> | undefined;

/** Shows a preview after the hover delay, or at once if one is already showing. Moving the pointer across the board opens nothing. */
function afterRest(show: () => void): void {
  clearTimeout(opening);
  if (ui.hover) show();
  else opening = setTimeout(show, HOVER_DELAY_MS);
}

// el has data-key and data-zoom, which is empty when the viewer may not see the card. data-from marks a hand card that is in another zone
export function hoverCard(el: HTMLElement | null): void {
  if (ui.inspect) return;
  if (!el || !previewSrc(el)) {
    clearTimeout(opening);
    byId('zoom').classList.remove('settling');
    changeUi(u => { u.hover = null; u.faceIndex = 0; });
    return;
  }
  afterRest(() => {
    if (el.isConnected) showCard(el);
  });
}

function showCard(el: HTMLElement): void {
  const key = Number(el.dataset.key);
  const card = Number.isInteger(key) ? key : null;
  const src = previewSrc(el);
  const from = el.dataset.from;
  changeUi(u => { u.hover = { card, src, from, at: el }; u.faceIndex = 0; });
  if (card !== null) actions?.inspectCard(card);
  // A hand card grows while it rises. A preview placed during the transition would jump, so it stays hidden until the card settles
  const zoom = byId('zoom');
  const rising = (el.closest<HTMLElement>('.card') ?? el).getAnimations().filter(a => a instanceof CSSTransition);
  zoom.classList.toggle('settling', rising.length > 0);
  if (rising.length) {
    Promise.allSettled(rising.map(a => a.finished)).then(() => {
      const hover = ui.hover;
      if (!hover || !('card' in hover) || hover.at !== el) return;
      zoom.classList.remove('settling');
      placeZoom(zoom, el);
    });
  }
}

/** Opens a card's details and pins them open. A long press calls this. */
export function inspectCard(el: HTMLElement): void {
  if (!previewSrc(el) && !el.dataset.key) return;
  clearTimeout(opening);
  showCard(el);
  changeUi(u => { u.inspect = true; });
}

export function inspectPlayer(key: number): void {
  clearTimeout(opening);
  changeUi(u => { u.hover = { player: key }; u.faceIndex = 0; u.inspect = true; });
  actions?.inspectPlayer(key);
}

/** Whether el's preview is pinned open. A tap then counts as the second click that desktop needs, as when picking a draft card. */
export const isPinned = (el: Element): boolean => ui.inspect && !!ui.hover && 'card' in ui.hover && ui.hover.at === el;

function closeInspect(): void {
  changeUi(u => { u.hover = null; u.faceIndex = 0; u.inspect = false; });
}

export function hoverable(el: HTMLElement, target: HTMLElement = el): void {
  // A tap also fires pointer events. Only a mouse opens a hover preview, because a finger has no hover
  el.addEventListener('pointerenter', e => { if (hovers(e)) hoverCard(target); });
  el.addEventListener('pointerleave', e => { if (hovers(e)) hoverCard(null); });
}

/** The pointer position over an avatar. The player's details are drawn beside it and move with it. */
let pointer = { x: 0, y: 0 };

export function followPointer(e: MouseEvent): void {
  pointer = { x: e.clientX, y: e.clientY };
  const hover = ui.hover;
  if (hover && 'player' in hover) placeAtPointer(byId('zoom'));
}

// Hovering an avatar shows desktop's player details (life, counters, hand size, commander damage and tax)
export function hoverPlayer(key: number | null): void {
  if (ui.inspect) return;
  clearTimeout(opening);
  if (key === null) {
    changeUi(u => { u.hover = null; u.faceIndex = 0; });
    return;
  }
  afterRest(() => {
    // The player's details move with the pointer. While a target is chosen they would cover the targeting arrow
    if (isAiming()) return;
    changeUi(u => { u.hover = { player: key }; u.faceIndex = 0; });
    actions?.inspectPlayer(key);
  });
}

export function renderDetail(model: Model): void {
  if (hoverGone() || (ui.hover && 'player' in ui.hover && isAiming() && !ui.inspect)) {
    ui.hover = null;
    byId('zoom').classList.remove('settling');
  }
  // A new prompt that picks from the board needs the board visible, so it closes a pinned sheet
  const p = model.prompt;
  if (ui.inspect && p && p !== pinnedOver.seen && !p.paying && ((p.selectableMin > 0 && p.selectable.length > 0) || p.selectablePlayers.length > 0)) {
    ui.hover = null;
  }
  if (!ui.hover) ui.inspect = false;
  pinnedOver.note(ui.inspect, p);
  drawDetail(model);
  const hover = ui.hover;
  const zoom = byId('zoom');
  const pinned = ui.inspect && !!hover;
  zoom.classList.toggle('sheet', pinned);
  sheet('detail', pinned, closeInspect);
  if (pinned) {
    zoom.classList.remove('placed', 'settling');
    zoom.style.left = zoom.style.top = '';
    drawSheetActions(zoom, model);
  } else if (hover && 'card' in hover && hover.at) {
    placeZoom(zoom, hover.at);
  } else if (hover && 'player' in hover) {
    placeAtPointer(zoom);
  } else {
    zoom.classList.remove('placed');
    zoom.style.left = zoom.style.top = '';
  }
}

const pinnedOver = openedOver();

/** Draws buttons under the card for what a mouse click, a right-click and the keys would do. */
function drawSheetActions(zoom: HTMLElement, model: Model): void {
  let bar = zoom.querySelector<HTMLElement>('.sheet-actions');
  if (!bar) {
    bar = document.createElement('div');
    bar.className = 'sheet-actions';
    bar.innerHTML = '<button class="sheet-prev">‹</button><span class="sheet-at"></span><button class="sheet-next">›</button>'
      + '<button class="sheet-act"></button><button class="sheet-abilities"></button><button class="sheet-flip"></button><button class="sheet-close"></button>';
    zoom.append(bar);
    q(bar, '.sheet-prev').setAttribute('aria-label', t('lblWebPortraitPreviousCard'));
    q(bar, '.sheet-next').setAttribute('aria-label', t('lblWebPortraitNextCard'));
    q(bar, '.sheet-close').textContent = t('lblClose');
    q(bar, '.sheet-close').onclick = closeInspect;
    q(bar, '.sheet-flip').onclick = () => changeUi(u => { u.cardText = !u.cardText; });
    swipeDown(zoom, closeInspect);
    // Swipes count only on the picture, because a swipe on the rules text scrolls it. Swipe up does the card's action, as in mobile's card zoom
    swipeAcross(zoom, '.shot', way => {
      const b = q<HTMLButtonElement>(zoom, way === 'left' ? '.sheet-next' : way === 'right' ? '.sheet-prev' : '.sheet-act');
      if (!b.hidden && !b.disabled) b.click();
    });
  }
  const hover = ui.hover;
  const at = hover && 'card' in hover ? hover.at : undefined;
  const item = at && menuItem(at) === at ? at : null;
  const card = item ? null : at?.closest<HTMLElement>('.card') ?? null;
  const stacked = at?.closest<HTMLElement>('.stack-item:not(.awaiting)') ?? null;
  // The other visible cards in the same zone or list, in page order
  const row = card ? [...card.closest('.battlefield, #hand, .cards')?.querySelectorAll<HTMLElement>('.card[data-key]') ?? []].filter(c => c.offsetParent !== null)
    : item ? listOf(item) : [];
  const shownAt = card ?? item;
  const index = shownAt ? row.indexOf(shownAt) : -1;
  const prev = q<HTMLButtonElement>(bar, '.sheet-prev');
  const next = q<HTMLButtonElement>(bar, '.sheet-next');
  prev.hidden = next.hidden = q(bar, '.sheet-at').hidden = row.length < 2 || index < 0;
  prev.disabled = index <= 0;
  next.disabled = index >= row.length - 1;
  prev.onclick = () => inspectCard(row[index - 1]);
  next.onclick = () => inspectCard(row[index + 1]);
  q(bar, '.sheet-at').textContent = t('lblWebNOfM', index + 1, row.length);
  const key = hover && 'card' in hover ? hover.card : null;
  const playable = !!card?.classList.contains('playable');
  const selectable = !!card?.classList.contains('selectable');
  const act = q<HTMLButtonElement>(bar, '.sheet-act');
  // A menu item's data-act names what a tap on it does. With no data-act, the button is hidden
  act.hidden = item ? !item.dataset.act : key === null || (!playable && !selectable);
  act.textContent = item?.dataset.act ?? t(selectable ? 'lblChoose' : 'lblWebPortraitPlay');
  act.onclick = () => {
    // The button clicks the item while its preview is still pinned, so isPinned is true for that click
    item?.click();
    closeInspect();
    if (!item && key !== null) actions?.selectCard(key, false, 0, 0);
  };
  const more = q<HTMLButtonElement>(bar, '.sheet-abilities');
  more.hidden = item ? item.dataset.menu === undefined : !(stacked || (key !== null && playable));
  more.textContent = t(item ? 'lblMore' : stacked ? 'lblWebPortraitStackOptions' : 'lblAbilities');
  // Each branch opens a menu. A phone draws that menu along the bottom edge
  more.onclick = () => {
    closeInspect();
    if (item) {
      // Opens the item's right-click menu, as the abilities button does for a match card
      item.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true, clientX: innerWidth / 2, clientY: innerHeight / 2 }));
    } else if (stacked) {
      const entry = Number(stacked.dataset.key);
      changeUi(u => { u.stackMenuAt = { key: entry, x: innerWidth / 2, y: innerHeight / 2 }; });
      actions?.askStackMenu(entry);
    } else if (key !== null) {
      actions?.selectCard(key, true, innerWidth / 2, innerHeight / 2);
    }
  };
  const flip = q<HTMLButtonElement>(bar, '.sheet-flip');
  const details = key !== null ? model.cardDetails.get(key) : undefined;
  // A card with no rules text has no text side to flip to. The button stays if the text side is already showing
  const face = details?.faces[ui.faceIndex] ?? details?.faces[0] ?? itemFace(item);
  flip.hidden = !face || !(hover && 'card' in hover && hover.src) || (!face.text && !ui.cardText);
  flip.textContent = t(ui.cardText ? 'lblWebPortraitShowImage' : 'lblWebPortraitShowRules');
  // A tap on the picture turns a card with more than one face to its next face
  q<HTMLElement>(zoom, '.shot').onclick = () => nextFace(model);
}

/** The visible items under the menu item's data-list container, in page order. */
function listOf(item: HTMLElement): HTMLElement[] {
  const list = item.closest('[data-list]');
  return list ? [...list.querySelectorAll<HTMLElement>('[data-image]')].filter(c => c.offsetParent !== null) : [item];
}

/** A menu item's own card text. A draft card carries it so that a card with no picture can still be read. */
function itemFace(el: HTMLElement | null | undefined): CardFace | undefined {
  const text = el?.dataset.text;
  if (!el || text === undefined) return undefined;
  return { name: el.dataset.card, cost: el.dataset.cost ?? '', type: el.dataset.type ?? '', pt: el.dataset.pt || undefined, text, colors: 0 };
}

/** The gap between a card and its preview, and between the pointer and the preview. */
const ZOOM_GAP = 16;

/** Places the preview below and right of the pointer, and keeps it inside the window near the edges. */
function placeAtPointer(zoom: HTMLElement): void {
  if (zoom.hidden) return;
  zoom.classList.add('placed');
  placeBeside(zoom, pointer.x, pointer.y, ZOOM_GAP);
}

/** Puts the preview beside its card, never over it, and inside the board clear of the side column. */
function placeZoom(zoom: HTMLElement, at: HTMLElement): void {
  if (zoom.hidden || !at.isConnected) return;
  const card = at.getBoundingClientRect();
  const side = document.getElementById('side')?.getBoundingClientRect();
  const left = 8;
  const right = side && side.width < window.innerWidth / 2 && side.left > card.right ? side.left - 8 : window.innerWidth - 8;
  const w = zoom.offsetWidth, h = zoom.offsetHeight;
  const after = card.right + ZOOM_GAP, before = card.left - ZOOM_GAP - w;
  let x: number;
  if (after + w <= right) x = after;
  else if (before >= left) x = before;
  else x = right - card.right > card.left - left ? right - w : left;
  // The zone's name sits above the preview, so the preview stays low enough to keep it on screen
  const tab = zoom.querySelector<HTMLElement>('.from-tab');
  const above = tab?.offsetParent ? tab.offsetHeight + 8 : 0;
  const y = Math.min(Math.max(8 + above, card.top + card.height / 2 - h / 2), window.innerHeight - h - 8);
  zoom.classList.add('placed');
  zoom.style.left = `${Math.round(x)}px`;
  zoom.style.top = `${Math.round(y)}px`;
}

/** The hovered card and which side the preview last showed. A change of side on the same card plays the flip animation. */
let shown: { hover: object; text: boolean } | null = null;
/** The last image that failed to load. Its card shows its text instead. */
let brokenSrc = '';

function drawDetail(model: Model): void {
  const zoom = byId('zoom');
  const hover = ui.hover;
  zoom.hidden = !hover;
  if (!hover) return;
  ensureZoom(zoom);
  zoom.querySelector<HTMLElement>('.sheet-actions')?.toggleAttribute('hidden', !ui.inspect);
  q(zoom, '.cmdr-taken').hidden = true;
  if ('player' in hover) {
    drawPlayer(zoom, model.playerDetails.get(hover.player));
    drawCommanderDamage(q(zoom, '.cmdr-taken'), model, hover.player);
    return;
  }
  const d = hover.card !== null ? model.cardDetails.get(hover.card) : undefined;
  const face = d?.faces[ui.faceIndex] ?? d?.faces[0] ?? itemFace(hover.at);
  ensureZoom(zoom);
  const img = q<HTMLImageElement>(zoom, 'img');
  // Read from the element each time, because a picture that arrives after the hover is loaded from a new address
  const src = face?.imageKey ? imageUrl(face.imageKey) : hover.at ? previewSrc(hover.at) : hover.src;
  const menu = hover.at?.dataset.image !== undefined;
  // A card the viewer may not see has no image, so only its text is shown. The same applies to an image that failed to load
  const text = (ui.cardText && (!menu || !!face)) || !src || src === brokenSrc;
  // A menu item with no text of its own shows nothing without its picture
  if (text && menu && !face) {
    zoom.hidden = true;
    return;
  }
  // Switching sides on the same card plays the flip animation. Moving to another card does not
  if (shown?.hover === hover && shown.text !== text) {
    replay(zoom, 'flipping');
  }
  shown = { hover, text };
  zoom.classList.toggle('image-only', !text);
  zoom.classList.toggle('text-card', text);
  img.hidden = text;
  // A foil's image can be loaded without its foil, so a failed load is recorded against the card's own address
  const picture = showFoil(q<HTMLCanvasElement>(zoom, 'canvas'), text ? '' : src);
  if (!text) {
    img.dataset.card = src;
    setImage(img, picture);
  }
  const shot = q(zoom, '.shot');
  shot.classList.toggle('turned', !!face?.turn);
  shot.style.setProperty('--turn', `${face?.turn ?? 0}deg`);
  q(zoom, '.detail').hidden = !face;
  const nowPt = q(zoom, '.now-pt');
  nowPt.hidden = true;
  if (!d || !face) drawStickers(q(zoom, '.stickers'), model, undefined);
  const source = sourceText(model, hover);
  setSource(q(zoom, '.from'), hover.from, source);
  setSource(q(zoom, '.from-tab'), hover.from, source);
  q(zoom, '.hint').textContent = '';
  q(zoom, '.conds').replaceChildren();
  if (!face) return;
  q(zoom, '.name').textContent = face.name ?? '';
  // The log names a card with its id, as in "Floodpits Drowner (51)". Showing the id here tells two copies apart
  q(zoom, '.cid').textContent = hover.card !== null && model.objects.has(hover.card) ? `(${hover.card})` : '';
  setSymbolText(q(zoom, '.cost'), face.cost);
  const detail = q(zoom, '.detail');
  // The frame uses the face's own colours, the same as a card with no picture on the board
  detail.dataset.frame = frameColour(face.colors, face.type ?? '');
  q(zoom, '.type-line').textContent = face.type ?? '';
  const set = q(zoom, '.set');
  set.textContent = face.set ?? '';
  set.dataset.rarity = face.rarity ?? '';
  const card = face === d?.faces[0] && hover.card !== null ? model.objects.get(hover.card) as CardView | undefined : undefined;
  drawStickers(q(zoom, '.stickers'), model, text ? undefined : card);
  const state = card ? stateOf(model, card) : undefined;
  // The picture shows the printed power and toughness, such as ★/1+★. A missing value arrives for a ★ and for a 0, so both count as 0
  if (!text && card?.Zone === 'Hand' && state && /Creature/.test(face.type ?? '')
      && ((state.Power ?? 0) !== (state.BasePower ?? 0) || (state.Toughness ?? 0) !== (state.BaseToughness ?? 0))) {
    nowPt.textContent = `${state.Power ?? 0}/${state.Toughness ?? 0}`;
    nowPt.hidden = false;
  }
  // A token or a face-down card has no printed text to compare its changes against
  const changes = card && state && !card.Token && !card.Facedown
    ? changesOf(state.OracleText ?? '', state.AbilityText ?? '', { ...card.ChangedTypes, ...card.ChangedColorWords }, state.Keywords ?? [])
    : undefined;
  const progress = card ? conditionsOf(model, card) : [];
  if (!text) q(zoom, '.conds').replaceChildren(...progress.map(condPill));
  const sorted = setRulesText(q(zoom, '.text'), q(zoom, '.foot'), face.text ?? '', state?.Keywords ?? [], changes, progress);
  q(zoom, '.kind-chip').textContent = sorted.tag ?? '';
  drawPt(q(zoom, '.pt'), model, hover.card, face, face === d?.faces[0]);
  // The hint shows the player's own keys, because both can be rebound in the options
  const keys = boundKeys();
  const faces = d && d.faces.length > 1 ? t('lblWebDetailNextFace', keyName(keys.nextFace), ui.faceIndex + 1, d.faces.length) : '';
  const flip = src ? t(text ? 'lblWebDetailFlipToImage' : 'lblWebDetailFlipToRules', keyName(keys.cardText)) : '';
  q(zoom, '.hint').textContent = [faces, flip].filter(Boolean).join(' · ');
}

// Each zone has its own text because they do not all take a possessive. It is "your graveyard" but not "your exile"
const YOUR_ZONE: Record<string, TextKey> = {
  Graveyard: 'lblWebDetailYourGraveyard',
  Exile: 'lblExileZone',
  Command: 'lblWebDetailYourCommand',
  Library: 'lblWebDetailYourLibrary',
  Sideboard: 'lblWebDetailYourSideboard',
};
const THEIR_ZONE: Record<string, TextKey> = {
  Graveyard: 'lblWebDetailTheirGraveyard',
  Exile: 'lblWebDetailTheirExile',
  Library: 'lblWebDetailTheirLibrary',
  Hand: 'lblWebDetailTheirHand',
};

/** Names the zone a hand card is really in, and whose it is. The engine also lists cards that are only revealed, so "Playable" needs the card in the playable list. */
function sourceText(model: Model, hover: { card: number | null; from?: string }): string {
  const zone = hover.from;
  if (!zone) return '';
  const card = hover.card !== null ? model.objects.get(hover.card) as CardView | undefined : undefined;
  const owner = card ? deref(model, card.Owner) : undefined;
  const theirs = !!owner && !model.localPlayers.includes(owner.$key);
  const known = (theirs ? THEIR_ZONE : YOUR_ZONE)[zone];
  const where = known ? t(known, owner?.Name ?? '') : zone.toLowerCase();
  const playable = hover.card !== null && (model.playable?.cards ?? []).some(r => r.ref === hover.card);
  return t(playable ? 'lblWebDetailPlayableFrom' : 'lblWebDetailIn', where);
}

/** Shows that a hand card is really in another zone. The glow around the card uses that zone's colour too. */
function setSource(el: HTMLElement, zone: string | undefined, text = ''): void {
  el.textContent = text;
  if (zone) {
    el.dataset.from = zone;
  } else {
    delete el.dataset.from;
  }
}

// Only CardDetailUtil's grey and strike marks are kept. Every other tag is dropped but its text is kept, so card text cannot inject markup
function rulesLines(html: string): Line[] {
  const lines: Line[] = [[]];
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const walk = (node: Node, muted: boolean, struck: boolean) => {
    for (const child of node.childNodes) {
      if (child.nodeType === Node.TEXT_NODE) {
        (child.textContent ?? '').split(/\r?\n/).forEach((text, i) => {
          if (i > 0) lines.push([]);
          if (text) lines[lines.length - 1].push(struck ? { text, muted, struck } : { text, muted });
        });
      } else if (child.nodeName === 'BR') {
        lines.push([]);
      } else if (child instanceof Element) {
        walk(child, muted || /gray|grey/i.test(child.getAttribute('style') ?? ''), struck || /^(STRIKE|S|DEL)$/.test(child.nodeName));
      }
    }
  };
  walk(doc.body, false, false);
  return lines;
}

/** The text last sorted, for the next frame to reuse. */
let lastSorted: SortedText = { blocks: [], chips: [], notes: [] };

/** Lays out a card's rules like its printed text box. Changes the game has made to the card go under it as chips and notes. */
function setRulesText(box: HTMLElement, foot: HTMLElement, html: string, keywords: readonly KeywordText[], changes?: Changes, progress: Progress[] = []): SortedText {
  // The preview is redrawn every frame while a card is hovered, so the text is sorted again only when it changes
  const drawn = html + JSON.stringify(keywords) + JSON.stringify(changes, (_, v) => (v instanceof Set ? [...v] : v)) + JSON.stringify(progress);
  if (box.dataset.html === drawn) return lastSorted;
  box.dataset.html = drawn;
  const sorted = lastSorted = sortRulesText(rulesLines(html), keywords);
  box.replaceChildren(...sorted.blocks.map(b => drawBlock(b, changes, progress)));
  // Lost rules are shown struck through. Lost keywords go with the keywords, and other lost text goes after the rules
  if (changes?.lostKeywords.length) {
    const kws = box.querySelector('.kws') ?? box.insertBefore(make('div', 'kws'), box.firstChild);
    const firstReminder = kws.querySelector('.kwrem');
    for (const lost of changes.lostKeywords) kws.insertBefore(changed(richLine(make('span', 'kw lost'), [{ text: lost, muted: false }]), 'lost'), firstReminder);
  }
  for (const lost of changes?.lostText ?? []) box.append(changed(richLine(make('div', 'lost'), [{ text: lost, muted: false }]), 'lost'));
  if (sorted.notes.length) {
    const notes = make('div', 'notes');
    for (const note of sorted.notes) notes.append(richLine(document.createElement('div'), note));
    box.append(notes);
  }
  foot.replaceChildren(...sorted.chips.map(chip => {
    const el = make('span', `chip ${chip.kind}`);
    el.textContent = chip.text;
    return el;
  }));
  return sorted;
}

function changed(el: HTMLElement, how: 'gained' | 'lost'): HTMLElement {
  const tag = make('i', 'change');
  tag.textContent = how === 'gained' ? t('lblWebDetailGained') : t('lblWebDetailLost');
  el.append(tag);
  return el;
}

/** The longest ability cost, in characters, that fits in a column beside the ability's effect. */
const LONG_COST = 22;

function drawBlock(block: Block, changes: Changes | undefined, progress: Progress[]): HTMLElement {
  switch (block.kind) {
    case 'keywords': {
      const el = make('div', 'kws');
      for (const item of block.items) {
        const kw = richLine(make('span', 'kw'), item);
        el.append(changes?.gained.has(plain(item).toLowerCase()) ? changed(kw, 'gained') : kw);
        const pool = progress.find(p => p.pool !== undefined && p.word.toLowerCase() === plain(item).toLowerCase())?.pool;
        if (pool !== undefined) el.append(chip(t('lblWebCondInGraveyard', pool), false));
      }
      for (const reminder of block.reminders) el.append(richLine(make('span', 'kwrem'), reminder));
      return el;
    }
    case 'ability': {
      // A cost too long to fit beside its effect, such as one that returns a land, goes above the effect and wraps
      const el = make('div', plain(block.cost).length > LONG_COST ? 'ab long' : 'ab');
      el.append(richLine(make('span', block.loyalty ? `ac ${block.loyalty}` : 'ac'), block.cost));
      const effect = make('span', 'ef');
      const word = block.label && progress.find(p => p.word === plain(block.label as Line).trim());
      if (block.label) effect.append(word ? wordChip(word) : richLine(make('b', ''), block.label), ' ');
      el.append(richLine(effect, block.effect, progress));
      return el;
    }
    case 'mode':
      return richLine(make('div', 'mode-option'), block.text, progress);
    case 'text': {
      const el = make('div', '');
      if (block.label) el.append(richLine(make('b', ''), block.label), ' — ');
      return richLine(el, block.text, progress);
    }
  }
}

/** Appends a line's text with its symbols drawn. Text that does not apply is greyed and reminder text is styled apart. An ability word the page counts becomes a chip with its count. */
function richLine<E extends HTMLElement>(el: E, line: Line, progress: Progress[] = []): E {
  const words = progress.filter(p => p.cost === undefined);
  const wordAt = words.length ? new RegExp(`(${words.map(p => p.word).join('|')}) — `) : null;
  for (const run of line) {
    run.text.split(/(\((?:[^()]|\([^()]*\))*\))/).forEach((part, i) => {
      if (!part) return;
      const span = make('span', [run.muted ? 'muted' : '', run.struck ? 'struck' : '', i % 2 ? 'rem' : ''].filter(Boolean).join(' '));
      (wordAt && !(i % 2) ? part.split(wordAt) : [part]).forEach((piece, j) => {
        if (j % 2) {
          span.append(wordChip(words.find(p => p.word === piece) as Progress), ' ');
        } else if (piece) {
          appendSymbolText(span, piece);
        }
      });
      el.append(span);
    });
  }
  return el;
}

const TICK = '<svg class="tick" viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 5 5 9-10"/></svg>';

function chip(text: string, met: boolean): HTMLElement {
  const el = make('span', met ? 'aw met' : 'aw');
  el.innerHTML = met ? TICK : '';
  el.append(text);
  return el;
}

/** How far a condition has got, such as "3/4" or "2 in hand". */
function countText(p: Progress): string {
  if (p.upTo === 'hand') return t('lblWebCondInHand', p.have);
  if (p.upTo === 'life') return t('lblNLife', p.have);
  return `${p.have}/${p.need}`;
}

const wordChip = (p: Progress): HTMLElement => chip(`${p.word} ${countText(p)}`, p.met);

/** The pill under the card's picture. A count also gets one segment per card needed. */
function condPill(p: Progress): HTMLElement {
  const el = make('span', ['cond', p.met ? 'met' : '', p.cost !== undefined ? 'delve' : ''].filter(Boolean).join(' '));
  el.innerHTML = p.met ? TICK : '';
  el.append(p.word);
  if (!p.upTo && p.need > 0 && p.need <= 10 && !p.met) {
    const segs = make('span', 'segs');
    for (let i = 0; i < p.need; i++) segs.append(make('i', i < p.have ? 'on' : ''));
    el.append(segs);
  }
  const num = make('span', 'num');
  if (p.cost !== undefined) {
    num.append(t('lblWebCondAsLowAs'), ' ');
    appendSymbolText(num, p.cost);
  } else {
    num.textContent = countText(p);
  }
  el.append(num);
  return el;
}

/** Draws the P/T, loyalty or defence in the card's corner. Damage is subtracted from toughness and changed numbers are coloured. */
function drawPt(el: HTMLElement, model: Model, key: number | null, face: CardFace, current: boolean): void {
  el.classList.toggle('shield', /Planeswalker/.test(face.type ?? ''));
  const card = current && key !== null ? model.objects.get(key) as CardView | undefined : undefined;
  const state = card ? stateOf(model, card) : undefined;
  if (!card || !state || !/Creature/.test(face.type ?? '')) {
    el.textContent = face.pt ?? '';
    return;
  }
  const power = state.Power ?? 0;
  const toughness = (state.Toughness ?? 0) - (card.Damage ?? 0);
  const p = document.createElement('i');
  p.textContent = String(power);
  p.dataset.shift = ptShift(power, state.BasePower);
  const tough = document.createElement('i');
  tough.textContent = String(toughness);
  tough.dataset.shift = card.Damage ? 'down' : ptShift(toughness, state.BaseToughness);
  el.replaceChildren(p, '/', tough);
}

function ensureZoom(zoom: HTMLElement): void {
  if (zoom.firstChild) return;
  zoom.innerHTML = '<span class="shot"><img alt=""><span class="stickers" aria-hidden="true"></span><span class="now-pt" hidden></span><canvas class="foil" hidden></canvas><span class="from-tab"></span></span><div class="detail"><header><b class="name"></b><span class="cid"></span><span class="cost"></span></header><div class="cmdr-taken" hidden></div><div class="type"><span class="type-line"></span><span class="kind-chip"></span><span class="set"></span></div><div class="from"></div><div class="text"></div><div class="foot"></div><div class="pt"></div></div><div class="conds"></div><div class="hint"></div>';
  const img = q<HTMLImageElement>(zoom, 'img');
  img.addEventListener('error', () => {
    brokenSrc = img.dataset.card ?? '';
    redrawUi();
  });
  // Remove the class after the animation. Otherwise the preview would flip again the next time it is shown
  zoom.addEventListener('animationend', e => {
    if (e.animationName === 'zoom-flip') zoom.classList.remove('flipping');
  });
  // The preview ignores pointer events, so the wheel over the hovered card scrolls rules text that overflows its box
  const box = q(zoom, '.text');
  document.addEventListener('wheel', e => {
    if (zoom.hidden || !zoom.classList.contains('text-card') || box.scrollHeight <= box.clientHeight) return;
    e.preventDefault();
    box.scrollTop += e.deltaMode === WheelEvent.DOM_DELTA_LINE ? e.deltaY * 16 : e.deltaY;
  }, { passive: false });
  // The preview is placed before its image loads and grows when it does. Placing it again keeps it on screen
  img.addEventListener('load', () => {
    const hover = ui.hover;
    if (!ui.inspect && hover && 'card' in hover && hover.at) placeZoom(zoom, hover.at);
  });
}

/** Draws one row per commander that has damaged the player, most damage first. The server leaves desktop's commander damage lines out of the text. */
function drawCommanderDamage(root: HTMLElement, model: Model, key: number): void {
  const player = model.objects.get(key) as PlayerView | undefined;
  const hits = (player?.CommanderDamage ?? []).filter(h => h.value > 0).sort((a, b) => b.value - a.value);
  root.hidden = hits.length === 0;
  root.replaceChildren();
  if (!hits.length) return;
  const title = document.createElement('h6');
  title.textContent = t('lblWebDetailCommanderDamageTaken');
  root.append(title);
  for (const { card, value } of hits) {
    const commander = deref(model, card) as CardView | undefined;
    const row = make('div', value >= COMMANDER_WARNING ? 'cmdr-row near' : 'cmdr-row');
    row.innerHTML = '<span class="art"></span><span class="who"><span class="nm"></span><span class="bar"><i></i></span></span><span class="v"><b></b><small></small></span>';
    const src = cardImageSrc(model, commander);
    if (src) q(row, '.art').style.backgroundImage = `url("${src}")`;
    q(row, '.nm').textContent = (commander ? stateOf(model, commander).Name : undefined) ?? t('lblCommander');
    q(row, '.bar i').style.width = `${Math.min(value, COMMANDER_LETHAL) / COMMANDER_LETHAL * 100}%`;
    q(row, '.v b').textContent = String(value);
    q(row, '.v small').textContent = ` / ${COMMANDER_LETHAL}`;
    root.append(row);
  }
}

function drawPlayer(zoom: HTMLElement, d: PlayerDetail | undefined): void {
  ensureZoom(zoom);
  zoom.classList.remove('image-only', 'text-card');
  q(zoom, 'img').hidden = true;
  showFoil(q<HTMLCanvasElement>(zoom, 'canvas'), '');
  q(zoom, '.detail').hidden = !d;
  setSource(q(zoom, '.from'), undefined);
  setSource(q(zoom, '.from-tab'), undefined);
  q(zoom, '.hint').textContent = '';
  if (!d) return;
  q(zoom, '.name').textContent = d.name ?? '';
  q(zoom, '.cid').textContent = '';
  setSymbolText(q(zoom, '.cost'), '');
  q(zoom, '.type-line').textContent = '';
  q(zoom, '.kind-chip').textContent = '';
  q(zoom, '.set').textContent = '';
  const text = q(zoom, '.text');
  text.textContent = d.lines.join('\n');
  delete text.dataset.html;
  q(zoom, '.foot').replaceChildren();
  q(zoom, '.pt').textContent = '';
}
