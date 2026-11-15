// The zones panel, for a graveyard, exile or other zone opened over the board.
// It folds to a bar instead of closing. So the player can look at the board during a prompt and come back to the same place.

import { h, render } from 'preact';
import { reconcile } from './reconcile';
import { createCard, has, markPicked, updateCard, type CardClick } from './cards';
import { fitCards } from './fit';
import { isLocal, stateOf, zone, type Model } from '../model';
import { byId, q, make, reducedMotion } from '../dom';
import { setImage, setSymbolText } from '../images';
import { playerAvatarUrl } from '../avatarsleeves';
import { sourceName, splitQuestion } from './asking';
import { changeUi, ui, type ZoneSort } from '../ui';
import { normalize, rankByName } from '../search';
import { isPortrait } from '../form';
import { sheet } from '../sheet';
import type { Actions } from '../actions';
import { keyName } from '../keys';
import { boundKeys } from '../settings';
import type { CardView, PlayerView, PromptButton, ZoneType } from '../protocol';
import { t, tCap, type TextKey } from '../text';
import { Select } from '../select';
import { manaValue } from './gamerules';

// One open zone. It is either a zone the game asks to show or a pile the player opened by clicking
interface Panel {
  player: number;
  zone: ZoneType;
  /** True when the game is showing it. Then the game closes it, not a Close button. */
  forced: boolean;
}

/** The prompt that OK answered while the game showed zones. */
let answered: Model['prompt'] = null;

/** Hides the zones the game shows until the next prompt. The host sends their closing with that prompt, which can wait behind another player's play. */
export function closeShownZones(model: Model): void {
  if (model.zones.length && model.prompt?.ok?.enabled) answered = model.prompt;
}

/** The zones the game shows, unless OK has already answered the prompt they belong to. */
export function shownZones(model: Model): Model['zones'] {
  return answered && model.prompt === answered ? [] : model.zones;
}

const SORTS: [ZoneSort, TextKey][] = [['order', 'lblWebZoneSortOrder'], ['name', 'lblWebZoneSortName'], ['type', 'lblWebZoneSortType'],
  ['mana', 'lblWebZoneSortManaValue']];

const ZONE_NAMES: Record<ZoneType, TextKey> = {
  Hand: 'lblHandZone', Library: 'lblLibrary', Graveyard: 'lblGraveyard', Battlefield: 'lblBattlefieldZone',
  Exile: 'lblExile', Flashback: 'lblWebZoneFlashback', Command: 'lblCommandZone', Stack: 'lblStack',
  Sideboard: 'lblSideboard', Ante: 'lblAnteZone', Merged: 'lblWebZoneMerged', SchemeDeck: 'lblSchemeDeck',
  PlanarDeck: 'lblPlanarDeck', AttractionDeck: 'lblAttractionDeckZone', Junkyard: 'lblJunkyardZone',
  ContraptionDeck: 'lblContraptionDeckZone', Subgame: 'lblSubgameZone', ExtraHand: 'lblWebZoneExtraHand',
  StickerSheets: 'lblStickerSheets', None: 'lblNone',
};

export function zoneTitle(zoneName: ZoneType): string {
  const key = ZONE_NAMES[zoneName];
  return key ? tCap(key) : zoneName;
}

export function togglePile(playerKey: number, zoneName: ZoneType): void {
  const k = `${playerKey}/${zoneName}`;
  changeUi(u => {
    if (!u.openZones.delete(k)) u.openZones.add(k);
  });
}

export function renderZones(model: Model, actions: Actions, select: CardClick): void {
  const panels = new Map<string, Panel>();
  const shown = shownZones(model);
  for (const z of shown) panels.set(`${z.player.ref}/${z.zone}`, { player: z.player.ref, zone: z.zone, forced: true });
  // Several zones the game shows for one question, such as every graveyard for a target, are chips. Only the chosen zone's cards are drawn
  const chips = shown.length > 1 ? [...panels.keys()] : [];
  if (!chips.length) ui.zoneChoice = null;
  const chosen = chips.length ? chosenZone(model, chips) : null;
  for (const k of ui.openZones) {
    if (panels.has(k)) continue;
    const [player, zoneName] = k.split('/');
    panels.set(k, { player: Number(player), zone: zoneName as ZoneType, forced: false });
  }
  const root = byId('zones');
  const open = panels.size > 0;
  // With no zone open, nothing is folded either. So the folded state is cleared when the last zone closes
  if (!open && ui.zonesMinimised) {
    changeUi(u => { u.zonesMinimised = false; });
  }
  root.hidden = !open;
  // Back closes the zones the player opened. The game closes the zones it showed
  sheet('zones', isPortrait() && open && ui.openZones.size > 0, () => changeUi(u => { u.openZones.clear(); }));
  root.classList.toggle('minimised', ui.zonesMinimised);
  // A zone the game shows is a question about its cards, laid out on the board like other questions. A zone opened only to look at stays a window
  root.classList.toggle('asking', [...panels.values()].some(p => p.forced));
  if (!open) {
    root.replaceChildren();
    return;
  }
  if (ui.zonesMinimised) {
    drawBar(root, model, [...panels.values()]);
    return;
  }
  // Add the shelf again only when it is missing. Removing an open dialog from the page resets its scroll and loses the search box's focus
  const box = shelf(root);
  if (root.firstElementChild !== box || root.childElementCount !== 1) root.replaceChildren(box);
  const drawn = [...panels.entries()].filter(([k, p]) => !p.forced || !chosen || k === chosen);
  reconcile(q(root, '.zone-shelf'), drawn, ([k]) => k, createPanel,
    (el, [, p]) => updatePanel(el, model, actions, p, select, chips, chosen));
  for (const el of box.querySelectorAll<HTMLElement>('.zone-panel:not([data-opened])')) growFromPile(el);
  fitZones();
}

let fitting = false;

/** Sizes the cards of zones the game asks about to fill the space the board leaves, as for a question's cards. */
function fitZones(): void {
  if (!fitting) {
    fitting = true;
    window.addEventListener('resize', fitZones);
  }
  const root = byId('zones');
  const asked = [...root.querySelectorAll<HTMLElement>('.zone-panel.asking')];
  for (const el of asked) {
    const row = q(el, '.cards');
    // A long zone does not shrink its cards below the size used in a pile opened to look at. It scrolls instead
    fitCards(row, el, root, row.querySelectorAll(':scope > :not(.back:not(.selectable))').length, { gap: 14, wrap: true, least: 1.45, share: asked.length });
  }
}

/** A pile opened to look at animates out of its pile on the board. Others use the normal dialog animation: a zone the game shows, any zone on a phone, or one with no pile on the board. */
function growFromPile(el: HTMLElement): void {
  const [player, zoneName] = (el.dataset.key ?? '').split('/');
  const pile = isPortrait() || el.classList.contains('asking') ? null
    : document.querySelector(`.seat[data-player="${player}"] ${zoneName === 'Hand' ? '.hand-fan' : `.zone-tile[data-zone="${zoneName}"]`}`)?.getBoundingClientRect();
  if (!pile?.width) {
    el.dataset.opened = 'plain';
    return;
  }
  const at = el.getBoundingClientRect();
  el.style.transformOrigin = `${pile.left + pile.width / 2 - at.left}px ${pile.top + pile.height / 2 - at.top}px`;
  el.dataset.opened = 'pile';
}

/** A window that opened out of a pile animates back into it before it closes. */
function putAway(el: HTMLElement, close: () => void): void {
  if (el.dataset.opened !== 'pile' || reducedMotion()) {
    close();
    return;
  }
  el.animate([{}, { opacity: 1, offset: .5 }, { opacity: 0, scale: .06 }], { duration: 170, easing: 'cubic-bezier(.5, 0, .8, .3)', fill: 'forwards' })
    .onfinish = close;
}

/** The single container for the dialogs. It is kept across renders so the reconciled panels are not lost. */
function shelf(root: HTMLElement): HTMLElement {
  const known = root.querySelector<HTMLElement>('.zone-shelf');
  if (known) {
    return known;
  }
  const el = make('div', 'zone-shelf');
  return el;
}

function createPanel(): HTMLElement {
  const el = make('section', 'zone-panel');
  el.innerHTML = '<p class="zone-ask"><span class="ask-source"></span><span class="ask-sep" aria-hidden="true">·</span><span class="ask-text"></span></p><div class="zone-chips" role="group"></div><header><b class="zone-who"></b><span class="zone-count"></span><span class="zone-gap"></span>'
    + '<input class="zone-find" type="search">'
    + '<span class="zone-sort"></span>'
    + '<button class="zone-fold"></button></header>'
    + '<div class="cards"></div>'
    + '<footer><span class="zone-hint"></span><button class="zone-done primary"></button>'
    + '<button class="zone-answer ok primary"><span class="label"></span><kbd>Space</kbd></button></footer>';
  const find = q<HTMLInputElement>(el, '.zone-find');
  find.placeholder = t('lblWebZoneSearch');
  find.setAttribute('aria-label', t('lblWebZoneSearch'));
  q(el, '.zone-fold').textContent = t('lblWebZoneShowBoard');
  q(el, '.zone-done').textContent = t('lblDone');
  return el;
}

/** chips are the keys of the zones shown together for one question, and chosen is the one drawn. Both are empty for a single zone. */
function updatePanel(el: HTMLElement, model: Model, actions: Actions, p: Panel, select: CardClick, chips: string[], chosen: string | null): void {
  const player = model.objects.get(p.player);
  const cards = shown(model, zone(model, player, p.zone));
  const among = p.forced && chips.length > 1;
  renderChips(q(el, '.zone-chips'), model, among ? chips : [], chosen);
  // Among several zones the chip already shows the avatar, so the line names the player in words
  q(el, '.zone-who').textContent = `${among && player && isLocal(model, player) ? tCap('lblYou') : player?.Name ?? ''} \u00b7 ${zoneTitle(p.zone)}`;
  q(el, '.zone-count').textContent = t(cards.length === 1 ? 'lblWebOneCard' : 'lblWebNCards', cards.length);
  const find = q<HTMLInputElement>(el, '.zone-find');
  if (find.value !== ui.zoneSearch) {
    find.value = ui.zoneSearch;
  }
  find.oninput = () => changeUi(u => { u.zoneSearch = find.value; });
  // A search orders the zone by how well each name matches, so the sort is disabled until the search is cleared
  render(h(Select<ZoneSort>, { label: t('lblSort'), prefix: t('lblSort'), value: ui.zoneSort, disabled: !!normalize(ui.zoneSearch),
    change: sort => changeUi(u => { u.zoneSort = sort; }), choices: SORTS.map(([id, name]) => ({ value: id, label: t(name) })) }), q(el, '.zone-sort'));
  const fold = q(el, '.zone-fold');
  fold.onclick = () => changeUi(u => { u.zonesMinimised = true; });
  // A pile the player opened already has Done to close it
  fold.hidden = !p.forced;
  const asking = p.forced;
  el.classList.toggle('asking', asking);
  // On the board the fold button goes beside the answer, as in a question. A phone answers from the dock, so it stays in the header
  const beside = asking && !isPortrait();
  const home = beside ? q(el, 'footer') : q(el, 'header');
  if (fold.parentElement !== home) {
    if (beside) home.insertBefore(fold, q(el, '.zone-answer.ok'));
    else home.append(fold);
  }
  // The game opens and closes a zone it shows, so that zone has no Done button
  const done = q<HTMLButtonElement>(el, '.zone-done');
  done.hidden = p.forced;
  done.onclick = () => {
    // A second press during the close animation would open it again
    done.disabled = true;
    // It closes rather than toggles, because the window can be closed another way, such as by Escape, before the animation ends
    putAway(el, () => changeUi(u => { u.openZones.delete(`${p.player}/${p.zone}`); }));
  };
  // A zone the game shows has the prompt's question and its OK button. It leaves out the other button, which is the way out of priority
  const prompt = p.forced ? model.prompt : null;
  const question = p.forced ? prompt?.message || t('lblWebZoneWaitingOnYou') : '';
  q(el, '.zone-hint').textContent = question;
  // On the board the question is one line: the source card, then the question, as in a dialog
  const asked = splitQuestion(question);
  const source = sourceName(model, asked, prompt?.card);
  q(el, '.zone-ask .ask-source').textContent = source;
  q(el, '.zone-ask .ask-sep').hidden = !source;
  setSymbolText(q(el, '.zone-ask .ask-text'), asked.ask);
  answer(q(el, '.zone-answer.ok'), prompt?.ok, () => actions.ok());
  q(el, '.zone-answer.ok kbd').textContent = keyName(boundKeys().ok);
  reconcile(q(el, '.cards'), cards, c => c.$key, () => createCard(select), (c, card) => {
    updateCard(c, model, card);
    markPicked(c, asking && c.classList.contains('highlighted'));
  });
}

/** The chosen zone, or else the first zone with a card that can be chosen. */
function chosenZone(model: Model, keys: string[]): string {
  if (ui.zoneChoice && keys.includes(ui.zoneChoice)) return ui.zoneChoice;
  return keys.find(k => chipCounts(model, k).can > 0) ?? keys[0];
}

/** A zone's player, and how many of its cards can be chosen and are chosen. */
function chipCounts(model: Model, key: string): { player: PlayerView | undefined; zoneName: ZoneType; can: number; got: number } {
  const [ref, zoneName] = key.split('/') as [string, ZoneType];
  const player = model.objects.get(Number(ref));
  const cards = zone(model, player, zoneName);
  return { player, zoneName, can: cards.filter(c => has(model.prompt?.selectable, c.$key)).length,
    got: cards.filter(c => has(model.prompt?.highlighted, c.$key)).length };
}

/** One chip per zone: the player's avatar and name, and a count of the cards that can be chosen there. Once some are chosen, it counts those in a light badge. */
function renderChips(box: HTMLElement, model: Model, keys: string[], chosen: string | null): void {
  box.hidden = !keys.length;
  // Zones of more than one kind, such as a graveyard and an exile, name the zone on each chip
  const kinds = new Set(keys.map(k => k.split('/')[1])).size;
  reconcile(box, keys, k => k, () => {
    const chip = make('button', 'zone-chip');
    chip.innerHTML = '<img alt="" draggable="false"><b></b><em></em><i></i>';
    return chip;
  }, (chip, k) => {
    const { player, zoneName, can, got } = chipCounts(model, k);
    const name = kinds > 1 ? `${player?.Name ?? ''} · ${zoneTitle(zoneName)}` : player?.Name ?? '';
    if (player) setImage(q<HTMLImageElement>(chip, 'img'), playerAvatarUrl(player));
    q(chip, 'b').textContent = name;
    const you = q(chip, 'em');
    you.textContent = tCap('lblYou');
    you.hidden = !player || !isLocal(model, player);
    const badge = q(chip, 'i');
    badge.textContent = String(got || can);
    badge.hidden = !got && !can;
    badge.classList.toggle('got', got > 0);
    // A player with nothing to choose keeps a chip, faded, so the chips stay in seat order
    chip.classList.toggle('none', !can && !got);
    chip.setAttribute('aria-pressed', String(k === chosen));
    chip.onclick = () => changeUi(u => { u.zoneChoice = k; });
  });
  // The arrow keys move between the chips, as between tabs
  box.onkeydown = e => {
    const by = e.key === 'ArrowLeft' ? -1 : e.key === 'ArrowRight' ? 1 : 0;
    const all = [...box.children] as HTMLElement[];
    const at = all.indexOf(document.activeElement as HTMLElement);
    if (!by || at < 0) return;
    e.preventDefault();
    const next = all[(at + by + all.length) % all.length];
    next.focus();
    next.click();
  };
}

function answer(el: HTMLElement, button: PromptButton | undefined, run: () => void): void {
  el.hidden = !button?.enabled;
  q(el, '.label').textContent = button?.label ?? '';
  el.onclick = run;
}

/** The zone's cards in dialog order. While searching, the best match is first. Otherwise they follow the chosen sort. */
function shown(model: Model, cards: CardView[]): CardView[] {
  const name = (c: CardView) => stateOf(model, c).Name ?? '';
  if (normalize(ui.zoneSearch)) {
    return rankByName(cards.map(name), ui.zoneSearch).map(i => cards[i]);
  }
  const kept = [...cards];
  if (ui.zoneSort === 'name') {
    return kept.sort((a, b) => name(a).localeCompare(name(b)));
  }
  if (ui.zoneSort === 'type') {
    const type = (c: CardView) => stateOf(model, c).Type ?? '';
    return kept.sort((a, b) => type(a).localeCompare(type(b)) || name(a).localeCompare(name(b)));
  }
  if (ui.zoneSort === 'mana') {
    const value = (c: CardView) => manaValue(stateOf(model, c).ManaCost);
    return kept.sort((a, b) => value(a) - value(b) || name(a).localeCompare(name(b)));
  }
  return kept;
}

/** The folded bar. It shows what is open and a button to unfold it. */
function drawBar(root: HTMLElement, model: Model, panels: Panel[]): void {
  const first = panels[0];
  const player = model.objects.get(first.player);
  const count = panels.length > 1 ? t('lblWebZoneNZones', panels.length) : t('lblWebNCards', zone(model, player, first.zone).length);
  // Kept across renders, so a render between press and release does not lose the click
  let bar = root.querySelector<HTMLElement>(':scope > .zone-bar');
  if (!bar) {
    bar = document.createElement('section');
    bar.className = 'zone-bar';
    bar.innerHTML = '<span class="zone-dot" aria-hidden="true"></span><b></b><span class="zone-count"></span>'
      + '<button class="zone-unfold primary"></button>'
      + '<button class="zone-shut">×</button>';
    q(bar, '.zone-unfold').textContent = t('lblWebZoneShowCards');
    q(bar, '.zone-shut').setAttribute('aria-label', t('lblWebZoneClose'));
  }
  q(bar, 'b').textContent = panels.length > 1 ? t('lblWebZoneOpenZones') : `${player?.Name ?? ''} \u00b7 ${zoneTitle(first.zone)}`;
  q(bar, '.zone-count').textContent = count;
  q(bar, '.zone-unfold').onclick = () => changeUi(u => { u.zonesMinimised = false; });
  // Folding is not closing. So the close button still closes only the zones the player opened
  const shut = q(bar, '.zone-shut');
  shut.hidden = panels.every(p => p.forced);
  shut.onclick = () => changeUi(u => {
    u.zonesMinimised = false;
    for (const p of panels) {
      if (!p.forced) u.openZones.delete(`${p.player}/${p.zone}`);
    }
  });
  if (root.firstElementChild !== bar || root.childElementCount !== 1) root.replaceChildren(bar);
}
