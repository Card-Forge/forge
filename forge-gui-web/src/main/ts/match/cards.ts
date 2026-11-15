// Builds and updates one card element. Every zone that shows a card uses this, so a card looks the same everywhere

import { combatShown, commanderTax, deref, game, stateOf, type Model } from '../model';
import { hoverable, inspectCard } from '../preview';
import { longPress } from '../press';
import { abilityUrl, cardImageSrc, hideOnError, noImageOnError, setImage, setSymbolText, smallImage } from '../images';
import { playerSleeveUrl, cssUrl } from '../avatarsleeves';
import { reconcile } from './reconcile';
import { q, playOnce, replay, make, reducedMotion } from '../dom';
import type { CardView, KeywordText, PlayerView, Ref } from '../protocol';
import { t } from '../text';
import { drawStickers } from './stickers';
import { ICONS, iconSvg } from '../icons';
import { frameColour, ptShift } from './gamerules';

/** A tapped card is drawn rotated and at this scale (board.css). */
export const TAPPED_SCALE = 0.9;

/** What happens when a card is clicked. On the board it is selected, in a dialog it toggles an option. menu is true for a right-click. */
export type CardClick = (el: HTMLElement, menu: boolean, e?: MouseEvent) => void;

const EYE = iconSvg(ICONS.eye);

export function createCard(onClick: CardClick): HTMLDivElement {
  const el = make('div', 'card');
  el.innerHTML = '<img alt="" draggable="false"><span class="stickers" aria-hidden="true"></span><div class="frame"><b class="name"></b><span class="cost"></span><span class="type"></span></div><i class="chains" aria-hidden="true"></i><i class="seal" aria-hidden="true"></i><span class="pt"><i class="pt-p"></i><i class="pt-t"></i></span><span class="badges"></span><span class="sick">Zz</span><span class="count"></span><span class="cost-badge"></span><span class="seen">' + EYE + '</span><span class="owned-by"></span><span class="kind-tag"></span><span class="phase-tag"></span><span class="barred-mark"></span><span class="corner"><span class="mech"></span><span class="kws"></span></span><span class="blocks"></span><span class="block-tab"></span><span class="combat-tag"></span><i class="halo" aria-hidden="true"></i><span class="haze" aria-hidden="true"></span><i class="pile-edge" aria-hidden="true"></i>';
  q(el, '.sick').title = t('lblWebCardSummoningSick');
  q(el, '.seen').title = t('lblWebCardRevealed');
  q(el, '.block-tab').textContent = t('lblWebCardBlocking');
  noImageOnError(el, q<HTMLImageElement>(el, 'img'));
  el.addEventListener('click', e => onClick(el, false, e));
  // Right-click shows the card's other actions, as on desktop
  el.addEventListener('contextmenu', e => {
    e.preventDefault();
    onClick(el, true, e);
  });
  hoverable(el);
  el.addEventListener('pointerdown', e => longPress(e, () => inspectCard(el)));
  return el;
}

// Every card checks these lists on every render. Each message replaces a whole list, so a Set is cached per list object
const refSets = new WeakMap<readonly (Ref | number)[], Set<number>>();
export const has = (refs: readonly (Ref | number)[] | null | undefined, key: number): boolean => {
  if (!refs) return false;
  let keys = refSets.get(refs);
  if (!keys) {
    keys = new Set(refs.map(r => (typeof r === 'number' ? r : r.ref)));
    refSets.set(refs, keys);
  }
  return keys.has(key);
};

/** The last image shown for each card. It is used for a spell whose card is already hidden when its cast is shown. */
const pictures = new Map<number, { src: string; zoom: string; name: string }>();

export function lastPicture(key: number): { src: string; zoom: string; name: string } | undefined {
  return pictures.get(key);
}

/** Adds a mark as well as the ring to a card picked in a question. So a picked card is shown by shape, not only by colour. */
export function markPicked(el: HTMLElement, picked: boolean): void {
  el.classList.toggle('picked', picked);
  const mark = el.querySelector(':scope > .pick-mark');
  if (picked && !mark) el.append(make('span', 'pick-mark'));
  else if (!picked) mark?.remove();
}

/** A new game reuses card keys, so the images kept from the last game are cleared. */
export function forgetPictures(): void {
  pictures.clear();
}

export function updateCard(el: HTMLElement, model: Model, card: CardView): void {
  const state = stateOf(model, card);
  // A face-down card held in exile under a permanent shows its owner's sleeve, even when the owner may look at it
  const visible = model.visible.has(card.$key) && !(card.Facedown && el.classList.contains('ghost'));
  const selectable = has(model.prompt?.selectable, card.$key);
  const type = visible ? (state.Type ?? '') : '';
  el.classList.toggle('back', !visible);
  const owner = card.Owner ? model.objects.get(card.Owner.ref) : undefined;
  el.style.setProperty('--sleeve', cssUrl(visible ? '' : playerSleeveUrl(owner)));
  el.classList.toggle('tapped', !!card.Tapped);
  el.classList.toggle('flying', visible && !!state.Keywords?.some(k => k.icon === 'IMG_ABILITY_FLYING'));
  // Each flyer starts its hover animation at a different point, so a row of flyers does not move in step
  el.style.setProperty('--hover-at', `${-(card.$key * 1.37 % 4.6).toFixed(2)}s`);
  el.classList.toggle('selectable', selectable);
  el.classList.toggle('playable', has(model.playable?.cards, card.$key));
  el.classList.toggle('auto-tap', has(model.playable?.autoTap, card.$key));
  el.classList.toggle('highlighted', has(model.prompt?.highlighted, card.$key));
  const attacking = !!card.Attacking && combatShown(model);
  el.classList.toggle('attacking', attacking);
  // On a phone, an attacker gets a tag when a tap on your creature will block it. The game may have picked that attacker for you
  const blockThis = attacking && game(model)?.Phase === 'COMBAT_DECLARE_BLOCKERS' && (model.prompt?.highlighted ?? []).includes(card.$key);
  q(el, '.combat-tag').textContent = blockThis ? t('lblWebPortraitBlockingThis') : '';
  el.classList.toggle('blocking', !!card.Blocking);
  el.classList.toggle('phased', !!card.PhasedOut);
  q(el, '.phase-tag').textContent = card.PhasedOut ? t('lblWebCardPhasedOut') : '';
  // The chains animate on and off when the card is detained or released. A newly drawn board shows them without animation
  const detained = !!card.Detained;
  if (el.dataset.detained !== undefined && (el.dataset.detained === '1') !== detained && el.closest('.battlefield') && !reducedMotion()) {
    replay(el, detained ? 'detaining' : 'releasing', 'detaining', 'releasing');
  }
  el.dataset.detained = detained ? '1' : '';
  el.classList.toggle('detained', detained);
  // The board shows a small copy of the image. The preview (data-zoom) uses the full image
  const src = cardImageSrc(model, card);
  const small = visible ? smallImage(src) : '';
  if (setImage(q<HTMLImageElement>(el, 'img'), small)) {
    el.classList.remove('noimg');
  }
  el.dataset.zoom = src;
  if (small) pictures.set(card.$key, { src: small, zoom: src, name: state.Name ?? '' });
  // A battlefield card that changes face (a transform, a morph turned face up) animates to the new face
  const face = card.Facedown ? 'down' : visible ? state.ImageKey ?? '' : '';
  if (el.dataset.face !== undefined && el.dataset.face !== face && el.closest('.battlefield') && !reducedMotion()) {
    q(el, 'img').animate([{ scale: '0 1' }, { scale: '1 1' }], { duration: 220, easing: 'cubic-bezier(.2,.7,.3,1)' });
  }
  el.dataset.face = face;
  el.style.setProperty('--pile-img', small ? cssUrl(small) : 'none');
  drawStickers(q(el, '.stickers'), model, visible && small ? card : undefined);
  q(el, '.name').textContent = visible ? (state.Name ?? '') : '';
  setCost(q(el, '.cost'), visible ? state.ManaCost ?? '' : '');
  // A commander in the command zone costs its tax extra, so the cost badge includes the tax
  const tax = card.Zone === 'Command' ? commanderTax(deref(model, card.Owner) as PlayerView | undefined, card) : 0;
  const badge = q(el, '.cost-badge');
  setCost(badge, visible ? withTax(state.ManaCost ?? '', tax) : '', true);
  // Marks a cost changed from the printed cost by a perpetual effect or a commander's tax
  const perpetual = state.OriginalManaCost !== undefined && (state.ManaCost ?? '') !== state.OriginalManaCost;
  badge.classList.toggle('changed', visible && (tax > 0 || perpetual));
  q(el, '.type').textContent = type;
  el.dataset.frame = visible ? frameColour(state.Colors ?? 0, type) : '';
  const pt = q(el, '.pt');
  const creature = /Creature/.test(type);
  const damage = card.Damage ?? 0;
  const power = creature ? `${state.Power ?? 0}/` : '';
  // A battle is drawn sideways, as it is printed and as every other client draws it
  el.classList.toggle('battle', /Battle/.test(type));
  // Damage is subtracted from the toughness shown, as a player counts it. It is not listed separately
  const toughness = creature ? `${(state.Toughness ?? 0) - damage}`
    : /Planeswalker/.test(type) ? `${state.Loyalty ?? ''}`
    : /Battle/.test(type) ? `${state.Defense ?? ''}` : '';
  // A changed number (a pump, a counter, a loyalty change) is animated so the player notices it
  if (pt.textContent && power + toughness && pt.textContent !== power + toughness) {
    playOnce(pt, 'changed');
  }
  const p = q(el, '.pt-p');
  p.textContent = power;
  p.dataset.shift = creature ? ptShift(state.Power ?? 0, state.BasePower) : '';
  const hurt = q(el, '.pt-t');
  hurt.textContent = toughness;
  // Marked damage counts in the toughness's shift as it does in the number above
  hurt.dataset.shift = creature ? ptShift((state.Toughness ?? 0) - damage, state.BaseToughness) : '';
  hurt.classList.toggle('hurt', creature && damage > 0);
  pt.classList.toggle('on', !!(power + toughness));
  showDamage(el, damage);
  const partner = visible ? deref(model, card.PairedWith) as CardView | undefined : undefined;
  showKeywords(q(el, '.kws'), visible ? state.Keywords : undefined, visible ? card.ShieldCount : undefined,
    partner && t('lblWebCardPaired', stateOf(model, partner).Name ?? ''));
  const mech = visible ? mechanic(card, type) : [];
  const box = q(el, '.mech');
  const html = mech.map(n => (n as HTMLElement).outerHTML).join('');
  if (box.dataset.html !== html) {
    box.dataset.html = html;
    box.replaceChildren(...mech);
  }
  showBlocking(el, card);
  const badges: string[] = [];
  if (card.IsRingBearer) badges.push(t('lblWebCardRingBearer'));
  // The corner already shows a planeswalker's loyalty or a battle's defense, unless the card is also a creature
  const inCorner = creature ? null : /Planeswalker/.test(type) ? 'Loyalty' : /Battle/.test(type) ? 'Defense' : null;
  // The count has a × sign, so "+1/+1 ×2" is not read as a change to the P/T
  for (const [name, n] of Object.entries(card.Counters ?? {})) {
    if (name !== inCorner) badges.push(`${name} ×${n}`);
  }
  q(el, '.badges').textContent = badges.join(' · ');
}

/** Forge has no icon for a soulbond pair, so the page has its own. It is shown with the keywords. */
const PAIRED = 'paired.svg';

/** Shows only keywords the server sends an icon for, so the page needs no keyword table of its own. */
function showKeywords(root: HTMLElement, keywords: KeywordText[] | undefined, shields: number | undefined, paired?: string): void {
  const shown = (keywords ?? []).filter(k => k.icon);
  if (paired) shown.push({ icon: PAIRED, title: paired, reminder: '' });
  reconcile(root, shown, k => k.icon ?? k.title, () => {
    const img = document.createElement('img');
    img.alt = '';
    hideOnError(img);
    return img;
  }, (img, k) => {
    setImage(img as HTMLImageElement, k.icon === PAIRED ? PAIRED : abilityUrl(k.icon ?? ''));
    img.title = k.reminder ? `${k.title} — ${k.reminder}` : k.title;
  });
  // A shield counter is shown with the keywords, because players check it to judge whether removal or a block kills the creature
  const had = root.querySelector('.shield');
  if (!shields) {
    had?.remove();
    return;
  }
  const shield = had ?? root.appendChild(shieldBadge());
  q(shield as HTMLElement, '.n').textContent = String(shields);
  (shield as HTMLElement).title = t(shields === 1 ? 'lblWebCardShieldCounter' : 'lblWebCardShieldCounters', shields);
}

function shieldBadge(): HTMLElement {
  const el = make('span', 'shield');
  el.innerHTML = '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 1.6 13.4 3.4v4.4c0 3.1-2.2 5.6-5.4 6.6-3.2-1-5.4-3.5-5.4-6.6V3.4Z"/></svg><i class="n"></i>';
  return el;
}

/** Adds generic mana to a mana cost, as a tax does. {2}{G} with 2 more is {4}{G}. */
function withTax(cost: string, tax: number): string {
  if (!tax) return cost;
  const generic = /^\{(\d+)\}/.exec(cost);
  return generic ? `{${Number(generic[1]) + tax}}${cost.slice(generic[0].length)}` : `{${tax}}${cost}`;
}

// Every card is updated on every render, so its cost is rebuilt only when it changes
function setCost(el: HTMLElement, text: string, pips = false): void {
  if (el.dataset.text !== text) {
    el.dataset.text = text;
    setSymbolText(el, text);
    // An image cannot show the glow round a changed cost, so each symbol is wrapped in its own span for it
    if (pips) {
      for (const sym of [...el.children]) {
        const pip = make('span', 'pip');
        sym.replaceWith(pip);
        pip.append(sym);
      }
    }
  }
}

/** The Ring's level as a Roman numeral, I to IV. */
export const ringNumeral = (level: number): string => 'I'.repeat(level).replace('IIII', 'IV');

/** A permanent's progress on its set's track, such as class level or sprocket. A card has at most one such track, so one chip shows them all. */
function mechanic(card: CardView, type: string): Node[] {
  const track = (now: number, of: number, text: string) => {
    const pips = make('span', 'pips');
    for (let i = 0; i < of; i++) {
      const pip = make('i', i < now ? 'on' : '');
      pips.append(pip);
    }
    return [pips, label(text)];
  };
  if (card.ClassLevel && /\bClass\b/.test(type)) return track(card.ClassLevel, 3, t('lblWebCardClassLevel', card.ClassLevel));
  if (card.RingLevel) return track(card.RingLevel, 4, t('lblWebCardRingLevel', ringNumeral(card.RingLevel)));
  if (card.CurrentRoom) return [label(card.CurrentRoom)];
  if (card.Sprocket) return track(card.Sprocket, 3, t('lblWebCardSprocket', card.Sprocket));
  if (card.AttractionLights?.length) {
    const lit = new Set(card.AttractionLights);
    const pips = make('span', 'pips');
    for (let i = 1; i <= 6; i++) {
      const pip = make('i', lit.has(i) ? 'lit' : '');
      pips.append(pip);
    }
    return [pips, label(card.AttractionLights.join(' '))];
  }
  // Intensity has no maximum, so there is no track to draw. Only the number is shown
  if (card.Intensity) return [label(t('lblWebCardIntensity', card.Intensity))];
  return [];
}

function label(text: string): HTMLElement {
  const el = make('i', 'mech-text');
  el.textContent = text;
  return el;
}

/** Highlights a creature that must block. A creature that may block more than one attacker shows how many. */
function showBlocking(el: HTMLElement, card: CardView): void {
  const must = (card.MustBlockCards ?? []).filter(r => r).length > 0;
  const extra = card.BlockAny ? '∞' : card.BlockAdditional ? String(card.BlockAdditional + 1) : '';
  el.classList.toggle('must-block', must);
  const blocks = q(el, '.blocks');
  blocks.textContent = extra;
  blocks.title = card.BlockAny ? t('lblWebCardMayBlockAny')
    : card.BlockAdditional ? t('lblWebCardMayBlockCount', card.BlockAdditional + 1) : '';
}

// New damage flashes the card white, shakes it and shows the amount flying off it. So combat can be followed without the log
function showDamage(el: HTMLElement, damage: number): void {
  const before = el.dataset.damage === undefined ? damage : Number(el.dataset.damage);
  el.dataset.damage = String(damage);
  // A card in a closed seat tab is not drawn, so its flash and number would wait and play when the tab opens
  if (damage <= before || el.closest('[hidden]')) {
    return;
  }
  playOnce(el, 'struck');
  // The flash is removed on animationend, so it is added only when the animation will run
  if (!reducedMotion()) {
    const flash = make('span', 'hit-white');
    el.append(flash);
    flash.addEventListener('animationend', () => flash.remove());
  }
  const hit = make('span', 'hit-number');
  hit.textContent = `-${damage - before}`;
  el.append(hit);
  hit.addEventListener('animationend', () => hit.remove());
}

export function setPileCount(el: HTMLElement, count: number, opened: boolean): void {
  el.classList.toggle('pile', count > 1);
  // Up to three copies show behind the top card. A pile of four or more shows four cards, and the badge shows the full count
  el.dataset.depth = String(Math.min(3, Math.max(0, count - 1)));
  const badge = q(el, '.count');
  badge.textContent = opened ? '×' : count > 1 ? `×${count}` : '';
  badge.classList.toggle('opened', opened);
  badge.title = opened ? t('lblWebCardPileGather') : count > 1 ? t('lblWebCardPileSpread') : '';
}
