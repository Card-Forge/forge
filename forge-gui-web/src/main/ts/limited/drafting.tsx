// The draft screen. A click selects a card and a second click or Enter picks it, because a pick cannot be undone

import { useEffect, useLayoutEffect, useRef, useState } from 'preact/hooks';
import { Dial } from './packdial';
import { nextFrom } from './dial';
import { imageUrl } from '../images';
import { cancelFlight, land, lift } from '../flight';
import { avatarUrl } from '../avatarsleeves';
import { rememberedAvatar } from '../identity';
import { SymbolText } from '../symbols';
import { CARD_TYPES, colourHeading, mvGroup } from '../deck/decklist';
import { changeUi } from '../ui';
import { HeadControls, PageHeader } from '../header';
import { hoverCard, inspectCard, isPinned } from '../preview';
import { t, tCap, tNodes, shortDay, type TextKey } from '../text';
import type { ComponentChildren } from 'preact';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { DraftCard, DraftLogLine, DraftSeat, DraftState } from '../protocol';
import { make, reducedMotion } from '../dom';
import { isPortrait } from '../form';
import { setting } from '../settings';
import { Backdrop, Confirm, Dropdown } from '../dialog';
import { Select } from '../select';

type GroupBy = 'colour' | 'type' | 'mv' | 'pick';
type PackSort = 'rarity' | 'colour' | 'mv' | 'rank';
type Drag = { from: 'pack' | 'pick'; index: number };
/** A pick and its index in pick order. The server refers to a pick by this index. */
type Held = { card: DraftCard; index: number };

const DRAG_TYPE = 'application/x-forge-draft';
const CLOCK_LOW_MS = 15_000;

const PASS_MS = 300;
const ARRIVE_MS = 400;
const COLUMN_STAGGER_MS = 22;

/** When the last pack started to leave. The next pack waits on this so the two packs do not cross. */
let passedAt = 0;

/** The side of the screen a pack passes to: -1 for left, 1 for right. */
const passSide = (direction: number): -1 | 1 => (direction < 0 ? -1 : 1);

/** Each pack card's column index, counted from one side of the grid. This lets a pack move across one column at a time. */
function columnsFrom(slots: HTMLElement[], edge: -1 | 1): number[] {
  const xs = slots.map(s => s.offsetLeft);
  const columns = [...new Set(xs)].sort((a, b) => edge * (b - a));
  return xs.map(x => columns.indexOf(x));
}

/** Slides the rest of the pack off towards the next player. The real cards stay hidden until a new pack replaces them, or for two seconds if none comes. */
function passPack(picked: number, side: -1 | 1): void {
  const grid = document.querySelector<HTMLElement>('.draft-pack .cat-grid');
  const slots = [...grid?.querySelectorAll<HTMLElement>('.draft-slot') ?? []].filter((_, i) => i !== picked);
  if (!grid || !slots.length) return;
  const reduced = reducedMotion();
  const box = grid.getBoundingClientRect();
  // The copies go in a layer clipped to the pack, with the same classes, so they are drawn the same way as the cards
  const layer = make('div', 'draft-pack pack-leaving');
  Object.assign(layer.style, { left: `${box.left}px`, top: `${box.top}px`, width: `${box.width}px`, height: `${box.height}px` });
  const inner = make('div', 'cat-grid');
  layer.append(inner);
  const columns = columnsFrom(slots, side);
  const slides = slots.map((slot, i) => {
    const at = slot.getBoundingClientRect();
    const copy = slot.cloneNode(true) as HTMLElement;
    Object.assign(copy.style, { position: 'absolute', left: `${at.left - box.left}px`, top: `${at.top - box.top}px`,
      width: `${at.width}px`, height: `${at.height}px`, margin: '0' });
    inner.append(copy);
    slot.style.visibility = 'hidden';
    return copy.animate(reduced ? [{ opacity: 1 }, { opacity: 0 }]
      : [{ transform: 'none', opacity: 1 }, { transform: `translateX(${side * box.width * 0.8}px)`, opacity: 0 }],
    { duration: reduced ? 160 : PASS_MS, delay: reduced ? 0 : columns[i] * COLUMN_STAGGER_MS, easing: 'cubic-bezier(.4,0,1,1)', fill: 'forwards' });
  });
  document.body.append(layer);
  passedAt = performance.now();
  void Promise.all(slides.map(a => a.finished.catch(() => undefined))).then(() => layer.remove());
  setTimeout(() => slots.forEach(s => { s.style.visibility = ''; }), 2000);
}

/** Slides a new pack in from the side it comes from. The column with the furthest to go starts first. */
function arrive(grid: HTMLElement, from: -1 | 1): void {
  const slots = [...grid.querySelectorAll<HTMLElement>('.draft-slot')];
  const reduced = reducedMotion();
  const columns = columnsFrom(slots, -from as -1 | 1);
  // If the next pack was already waiting, it comes in behind the leaving pack once that pack is mostly gone
  const behind = Math.max(0, passedAt + PASS_MS * 0.6 - performance.now());
  slots.forEach((slot, i) => {
    // A card the new pack shares with the old one in the same place is the same element. The pass hid it, so it is shown again here
    slot.style.visibility = '';
    slot.animate(reduced ? [{ opacity: 0 }, { opacity: 1 }]
      : [{ transform: `translateX(${from * grid.clientWidth * 0.7}px)`, opacity: 0 }, { transform: 'none', opacity: 1 }],
    { duration: reduced ? 200 : ARRIVE_MS, delay: reduced ? behind : behind + columns[i] * COLUMN_STAGGER_MS, easing: 'cubic-bezier(.2,.7,.25,1)', fill: 'backwards' });
  });
}

/** Lifts the pack card about to be picked, so it can fly to where the pick lands. */
function launch(index: number): void {
  lift(document.querySelectorAll('.draft-pack .draft-slot')[index]?.querySelector<HTMLImageElement>('.tile img:not(.sym)'), 'pick');
}

export function Drafting({ model, actions }: { model: Model; actions: Actions }) {
  const state = model.draft;
  const [leaving, setLeaving] = useState(false);
  /** On a phone the picks are a drawer over the pack. Its bar at the bottom of the screen opens it. */
  const [picksOpen, setPicksOpen] = useState(false);
  // An online draft belongs to the table and continues without this browser. So leaving only goes back to the table
  const online = model.screen === 'setup';
  const faces = state ? seatFaces(model, state.seats) : [];
  return (
    <div class="drafting-page">
      <PageHeader class="limited-head">
        <span class="limited-title">{t('lblBoosterDraft')}</span>
        {state && <span class="muted">{t('lblWebDraftProductSeats', state.product, state.seats.length)}</span>}
        <div class="head-right">
          <HeadControls />
          {online
            ? <button onClick={() => changeUi(u => { u.draftHidden = true; })}>{t('lblWebDraftBackToTable')}</button>
            : <button onClick={() => setLeaving(true)}>{t('lblLeaveDraft')}</button>}
        </div>
      </PageHeader>
      {model.error && <p class="limited-error">{model.error}</p>}
      {/* Opening packs can wait on a web site. The leave button stays available while it does */}
      {!state && <p class="muted drafting-wait">{t('lblWebDraftOpeningPacks')}</p>}
      {state && (
        <div class="drafting-shell">
          <Pack state={state} faces={faces} actions={actions} log={online && state.log.length > 0} />
          <Picks state={state} actions={actions} open={picksOpen} toggle={() => setPicksOpen(o => !o)} />
        </div>
      )}
      {picksOpen && isPortrait() && <div class="picks-scrim" onClick={() => setPicksOpen(false)} />}
      {state?.done && <SaveDraft model={model} state={state} actions={actions} />}
      {leaving && <Confirm title={t('lblWebDraftLeaveTheDraftQ')} label={t('lblLeaveDraft')}
        text={t('lblWebDraftPicksNotSaved')}
        back={() => setLeaving(false)} backLabel={t('lblWebDraftKeepDrafting')} go={() => actions.draftDiscard()} goLabel={t('lblLeave')} danger />}
    </div>
  );
}

/** Each seat's portrait. A drafter with no lobby seat gets one from a hash of its name, so it keeps the same portrait all draft. */
function seatFaces(model: Model, seats: DraftSeat[]): string[] {
  const count = model.looks?.avatarCount ?? 0;
  return seats.map((seat, i) => {
    const index = i === 0 ? rememberedAvatar()
      : model.lobby?.seats.find(s => s.name === seat.name)?.avatar ?? (count ? hash(seat.name) % count : -1);
    return index >= 0 ? avatarUrl(index) : '';
  });
}

function hash(text: string): number {
  let h = 0;
  for (const c of text) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  return h;
}

/** The Show table button. It shows the seat passing to you, you, and the seat you pass to. Its panel shows the whole table. */
function TableMenu({ state, faces }: { state: DraftState; faces: string[] }) {
  const n = state.seats.length;
  const direction = state.direction < 0 ? -1 : 1;
  const shown = [(n - direction) % n, 0, (n + direction) % n];
  return (
    <HeadMenu class="table-btn" label={<>
      <span class="faces">{shown.map(i => faces[i] ? <img key={i} alt="" src={faces[i]} /> : null)}</span>{t('lblWebDraftShowTable')}
    </>}>
      {() => <Dial state={state} faces={faces} />}
    </HeadMenu>
  );
}

function HeadMenu({ label, children, class: cls }: { label: ComponentChildren; children: (close: () => void) => ComponentChildren; class?: string }) {
  return (
    <Dropdown cls="table-menu" button={(open, toggle) => (
      <button class={[cls ?? '', 'drop-btn', open ? 'open' : ''].join(' ')} aria-expanded={open} onClick={toggle}>
        {label} <span class="caret" aria-hidden="true">{open ? '▴' : '▾'}</span>
      </button>
    )}>{close => <div class="table-pop">{children(close)}</div>}</Dropdown>
  );
}

/** The time left on the pick clock, counted down from the value in the state when it arrived. */
function useClock(state: DraftState): number {
  const stamp = useRef(Date.now());
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    stamp.current = Date.now();
    setNow(stamp.current);
    if (!state.clockSeconds || !state.clockLeftMillis) return;
    const tick = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(tick);
  }, [state]);
  return state.clockSeconds ? Math.max(0, state.clockLeftMillis - (now - stamp.current)) : 0;
}

function Pack({ state, faces, actions, log }: { state: DraftState; faces: string[]; actions: Actions; log: boolean }) {
  const [selected, setSelected] = useState<number | null>(null);
  const [sort, setSort] = useState<PackSort>('rarity');
  // A new state clears the selection, because its cards are not the ones that were selected
  useEffect(() => setSelected(null), [state.step]);
  const ranked = setting('draftRanking') && state.cards.some(c => c.rank !== undefined);
  const sorts = PACK_SORTS.filter(([id]) => id !== 'rank' || ranked);
  const by = sort === 'rank' && !ranked ? 'rarity' : sort;
  // The cards are drawn in this order, but the server still names each card by its place in the pack
  const order = packOrder(state.cards, by);
  const pick = (index: number) => {
    // The card under the pointer is about to leave with the pack, so its preview is closed now
    hoverCard(null);
    launch(order.indexOf(index));
    passPack(order.indexOf(index), passSide(state.direction));
    actions.draftPick(state.step, index, false);
  };
  // A new pack slides in from the player it came from. That is the side opposite the one packs pass to
  const grid = useRef<HTMLDivElement>(null);
  const shown = useRef('');
  const cards = state.cards.map(c => c.image).join('|');
  useLayoutEffect(() => {
    if (cards === shown.current) return;
    shown.current = cards;
    if (grid.current && state.cards.length) arrive(grid.current, -passSide(state.direction) as -1 | 1);
  }, [cards]);
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && selected !== null && !(e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) pick(selected);
    };
    addEventListener('keydown', onKey);
    return () => removeEventListener('keydown', onKey);
  }, [selected, state.step, by]);
  const left = useClock(state);
  const n = state.seats.length;
  const direction = (state.direction < 0 ? -1 : 1) as 1 | -1;
  const depths = state.seats.map(s => s.packs);
  const next = nextFrom(depths, direction);
  const busiest = depths.reduce((best, d, i) => (d > depths[best] ? i : best), 0);
  const to = (n + direction) % n;
  const seat = (i: number) => <>{faces[i] && <img alt="" src={faces[i]} />}<strong>{state.seats[i].name}</strong></>;
  return (
    <section class="draft-panel draft-pack">
      <div class="draft-panel-head pick-line">
        <b class="pick-title">{t('lblWebDraftPackPickOf', state.pack, state.pick, state.packSize)}</b>
        {left > 0 && <span class={left < CLOCK_LOW_MS ? 'clock low' : 'clock'} title={t('ttWebDraftTimeLeft')}>{clockText(left)}</span>}
        {left > 0 && <span class={left < CLOCK_LOW_MS ? 'clock-bar low' : 'clock-bar'} style={{ width: `${(left / (state.clockSeconds * 1000)) * 100}%` }} />}
      </div>
      <div class="draft-panel-head tool-line">
        <span class="chip">{tNodes(direction > 0 ? 'lblWebDraftPassingRightTo' : 'lblWebDraftPassingLeftTo', seat(to))}</span>
        {next !== null && <span class="chip more">{tNodes('lblWebDraftNextFrom', seat(next))}</span>}
        {depths[busiest] > 2 && <span class="chip more">{busiest === 0 ? t('lblWebDraftYouHoldingPacks', depths[busiest]) : t('lblWebDraftSeatHoldingPacks', state.seats[busiest].name, depths[busiest])}</span>}
        <span class="head-end">
          <Select<PackSort> cls="sort-by" label={t('lblSort')} value={by} change={setSort} choices={sorts.map(([id, name]) => ({ value: id, label: t('lblWebSortBy', t(name)) }))} />
          {log && <HeadMenu label={t('lblWebDraftLog')}>{() => <DraftLog lines={state.log} />}</HeadMenu>}
          <TableMenu state={state} faces={faces} />
        </span>
      </div>
      <div class="cat-grid" data-list ref={grid}>
        {order.map(i => { const card = state.cards[i]; return (
          <div key={`${card.image}-${i}`} class={i === selected ? 'slot draft-slot chosen' : 'slot draft-slot'} data-image={card.image} {...textData(card)}
            data-act={t('lblWebDraftPick')} onClick={e => {
              // A card a third of a phone's screen wide is too small to read. So on a phone a tap opens its preview, and the preview's Pick button picks it
              if (isPinned(e.currentTarget)) pick(i);
              else if (isPortrait()) inspectCard(e.currentTarget);
              else if (i === selected) pick(i);
              else setSelected(i);
            }}>
            <button class="tile" draggable onDragStart={e => startDrag(e, { from: 'pack', index: i })}>
              <span class="tile-name">{card.name}</span>
              <CardFace card={card} />
              {card.rank !== undefined && setting('draftRanking') && <RankShield rank={card.rank} />}
            </button>
            {i === selected && <span class="confirm">{t('lblWebDraftPickConfirm')}</span>}
          </div>
        ); })}
        {state.cards.length === 0 && !state.done && <p class="muted">{t('lblDraftOverlayWaitingForPack')}</p>}
      </div>
    </section>
  );
}

const PACK_SORTS: [PackSort, TextKey][] = [['rarity', 'lblRarity'], ['colour', 'lblColor'], ['mv', 'lblCMC'],
  ['rank', 'lblDraftRanking']];
const RARITY: Record<string, number> = { M: 4, S: 3, R: 3, U: 2, C: 1 };

/** Mono-coloured cards in WUBRG order, then multicoloured, then colourless. */
function colourRank(colors: string): number {
  return colors === 'C' ? 6 : colors.length > 1 ? 5 : 'WUBRG'.indexOf(colors);
}

/** The pack's indexes in the order shown. Rarity is desktop's default: rarest first, then colour, then name. */
function packOrder(cards: DraftCard[], by: PackSort): number[] {
  const name = (a: DraftCard, b: DraftCard) => a.name.localeCompare(b.name);
  const colour = (a: DraftCard, b: DraftCard) => colourRank(a.colors) - colourRank(b.colors);
  const compare: Record<PackSort, (a: DraftCard, b: DraftCard) => number> = {
    rarity: (a, b) => (RARITY[b.rarity] ?? 0) - (RARITY[a.rarity] ?? 0) || colour(a, b) || name(a, b),
    colour: (a, b) => colour(a, b) || a.mv - b.mv || name(a, b),
    mv: (a, b) => a.mv - b.mv || colour(a, b) || name(a, b),
    rank: (a, b) => (b.rank ?? 0) - (a.rank ?? 0) || name(a, b),
  };
  return cards.map((_, i) => i).sort((a, b) => compare[by](cards[a], cards[b]));
}

/** A card's text on its pack slot or pick. The preview shows this text when the card has no picture. */
function textData(card: DraftCard) {
  return { 'data-card': card.name, 'data-cost': card.cost, 'data-type': card.type, 'data-text': card.text, 'data-pt': card.pt ?? '' };
}

/** A card's picture. If the picture fails to load, it shows the name, cost, type and rules text in the card's shape. */
function CardFace({ card }: { card: DraftCard }) {
  const [failed, setFailed] = useState(false);
  const src = imageUrl(card.image);
  // A new address is a new card, or the picture arriving after it was missing
  useEffect(() => setFailed(false), [src]);
  if (!failed) {
    return <img loading="lazy" alt="" draggable={false} src={src} onError={() => setFailed(true)} />;
  }
  return (
    <span class="text-face">
      <span class="tf-head"><b>{card.name}</b><span class="cost"><SymbolText text={card.cost} /></span></span>
      <span class="tf-type">{card.type}</span>
      <span class="tf-text"><SymbolText text={card.text} /></span>
      {card.pt && <span class="tf-pt">{card.pt}</span>}
    </span>
  );
}

function clockText(ms: number): string {
  const s = Math.ceil(ms / 1000);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

/** Picks use the browser's own drag and drop, because nothing is checked during the drag. The deck editor's pointer drag (deck/drag.ts) is for drags that need checks. */
function startDrag(e: DragEvent, drag: Drag): void {
  e.dataTransfer?.setData(DRAG_TYPE, JSON.stringify(drag));
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move';
}

/** Desktop's draft ranking overlay: the score on a shield in the middle of the card. The shield's colour tier follows the score. */
function RankShield({ rank }: { rank: number }) {
  const tier = rank >= 90 ? 's' : rank >= 80 ? 'a' : rank >= 60 ? 'b' : rank >= 25 ? 'c' : 'd';
  return (
    <span class={`rank tier-${tier}`} title={t('lblDraftRanking')}>
      <svg viewBox="0 0 34 42" aria-hidden="true">
        <defs>
          <linearGradient id={`rank-${tier}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" class="hi" /><stop offset=".45" class="mid" /><stop offset="1" class="lo" />
          </linearGradient>
        </defs>
        <path class="face" fill={`url(#rank-${tier})`} d="M17 1.5 C21.5 3.8 26.5 4.8 32 4.8 V18 C32 29 25.5 36.5 17 41 C8.5 36.5 2 29 2 18 V4.8 C7.5 4.8 12.5 3.8 17 1.5 Z" />
        <path class="inner" d="M17 4.6 C20.8 6.4 25 7.3 29.2 7.4 V18 C29.2 27.6 23.8 34 17 37.9 C10.2 34 4.8 27.6 4.8 18 V7.4 C9 7.3 13.2 6.4 17 4.6 Z" />
      </svg>
      <b>{rank}</b>
    </span>
  );
}

function Picks({ state, actions, open, toggle }: { state: DraftState; actions: Actions; open: boolean; toggle: () => void }) {
  const [by, setBy] = useState<GroupBy>('type');
  const [cards, setCards] = useState(true);
  const held: Held[] = state.picks.map((card, index) => ({ card, index }));
  const main = held.filter(h => !h.card.sideboard);
  const side = held.filter(h => h.card.sideboard);
  // A card dropped from the pack is picked into that section. A pick dropped on the other section moves there
  const drop = (sideboard: boolean) => (drag: Drag) => {
    // A dragged card is already at its target, so no flight animation runs
    cancelFlight();
    if (drag.from === 'pack') {
      passPack(drag.index, passSide(state.direction));
      actions.draftPick(state.step, drag.index, sideboard);
    }
    else if (state.picks[drag.index]?.sideboard !== sideboard) actions.draftMove(drag.index, sideboard);
  };
  const move = (h: Held) => actions.draftMove(h.index, !h.card.sideboard);
  const panel = useRef<HTMLElement>(null);
  const bar = useRef<HTMLButtonElement>(null);
  const count = useRef(state.picks.length);
  // A pick made by a click in the pack flies here once it is drawn. The newest pick is the last in pick order
  useLayoutEffect(() => {
    if (state.picks.length > count.current) {
      // A closed drawer shows only its bar, so the pick flies to the bar
      if (isPortrait() && !open) land(bar.current, null, bar.current);
      else {
        const pick = panel.current?.querySelector<HTMLElement>(`[data-pick="${state.picks.length - 1}"]`) ?? null;
        land(pick, pick?.closest('.draft-sections'), pick?.closest('.pick-section')?.querySelector('h3'));
      }
    }
    count.current = state.picks.length;
  }, [state.picks.length]);
  return (
    <section class={open ? 'draft-panel draft-picks open' : 'draft-panel draft-picks'} ref={panel}>
      <div class="draft-panel-head">
        {isPortrait()
          ? <button ref={bar} class="picks-toggle" aria-expanded={open} onClick={toggle}>
              {tNodes('lblWebDraftYourPicks', <span class="muted">{state.picks.length}</span>)}<span class="caret" aria-hidden="true">{open ? '▾' : '▴'}</span>
            </button>
          : <b>{tNodes('lblWebDraftYourPicks', <span class="muted">{state.picks.length}</span>)}</b>}
        <span class="seg" role="group" aria-label={t('lblWebDraftShowPicksAs')}>
          <button aria-pressed={cards} onClick={() => setCards(true)}>{t('lblCards')}</button>
          <button aria-pressed={!cards} onClick={() => setCards(false)}>{t('lblWebDraftList')}</button>
        </span>
      </div>
      <div class="draft-group-by">
        <span class="muted">{tCap('lblGroupby')}</span>
        <span class="seg" role="group" aria-label={t('lblWebDraftGroupPicksBy')}>
          {(['type', 'colour', 'mv', 'pick'] as GroupBy[]).map(g => (
            <button key={g} aria-pressed={by === g} onClick={() => setBy(g)}>{t(GROUP_NAMES[g])}</button>
          ))}
        </span>
      </div>
      <div class="draft-sections" data-list>
        <PickSection title={t('lblMainDeck')} held={main} by={by} cards={cards} onDrop={drop(false)} move={move} />
        <PickSection title={t('lblSideboard')} held={side} by={by} cards={cards} onDrop={drop(true)} move={move} />
      </div>
    </section>
  );
}

const GROUP_NAMES: Record<GroupBy, TextKey> = { colour: 'lblColor', type: 'lblType', mv: 'lblCMC', pick: 'lblWebDraftGroupPickOrder' };

function PickSection({ title, held, by, cards, onDrop, move }: {
  title: string; held: Held[]; by: GroupBy; cards: boolean; onDrop: (drag: Drag) => void; move: (h: Held) => void;
}) {
  // On a phone a tap opens the pick's preview. The preview's button then moves the pick, as a double click does
  const tap = (h: Held) => (e: MouseEvent) => {
    const el = e.currentTarget as HTMLElement;
    if (isPinned(el)) move(h);
    else if (isPortrait()) inspectCard(el);
  };
  const pickData = (h: Held) => ({ ...textData(h.card), 'data-act': t(h.card.sideboard ? 'lblWebDraftMoveToMain' : 'lblWebDraftMoveToSideboard') });
  const [over, setOver] = useState(false);
  const accepts = (e: DragEvent) => !!e.dataTransfer?.types.includes(DRAG_TYPE);
  return (
    <div class={over ? 'pick-section over' : 'pick-section'}
      onDragOver={e => { if (accepts(e)) { e.preventDefault(); setOver(true); } }}
      onDragLeave={e => { if (!(e.currentTarget as HTMLElement).contains(e.relatedTarget as Node)) setOver(false); }}
      onDrop={e => {
        setOver(false);
        const raw = e.dataTransfer?.getData(DRAG_TYPE);
        if (!raw) return;
        e.preventDefault();
        onDrop(JSON.parse(raw) as Drag);
      }}>
      <h3>{title} <span class="muted">{held.length}</span></h3>
      {held.length === 0 && <p class="drop-hint">{t(isPortrait() ? 'lblWebDraftTapToMoveHere' : 'lblWebDraftDragCardsHere')}</p>}
      <div class={cards ? 'pick-cols' : 'zone-body cols'}>
        {grouped(held, by).map(([heading, group]) => (
          <div key={heading} class="group">
            <h4>{heading}<span>{group.length}</span></h4>
            {cards
              ? <div class="pick-stack">{group.map(h => (
                  <img key={h.index} alt={h.card.name} title={t('ttWebDraftDoubleClickToMove')} src={imageUrl(h.card.image)}
                    data-image={h.card.image} {...pickData(h)} data-pick={h.index} draggable onDragStart={e => startDrag(e, { from: 'pick', index: h.index })}
                    onClick={tap(h)} onDblClick={() => move(h)} />
                ))}</div>
              : group.map(h => (
                  <div key={h.index} class="ed-line" data-image={h.card.image} {...pickData(h)} data-pick={h.index} draggable
                    title={t('ttWebDraftDoubleClickToMove')} onDragStart={e => startDrag(e, { from: 'pick', index: h.index })} onClick={tap(h)}
                    onDblClick={() => move(h)}>
                    <span class="nm">{h.card.name}</span>
                    <span class="cost"><SymbolText text={h.card.cost} /></span>
                    <span class="muted pk" title={t('ttWebDraftPackPick', h.card.pack, h.card.pick)}>{h.card.pack}·{h.card.pick}</span>
                  </div>
                ))}
          </div>
        ))}
      </div>
    </div>
  );
}

/** Other seats' picks make up most of the log, so they are shown only on request. The newest line comes first. */
function DraftLog({ lines }: { lines: DraftLogLine[] }) {
  const [everyone, setEveryone] = useState(false);
  const shown = lines.filter(l => everyone || l.kind !== 'seat').reverse();
  return (
    <aside class="draft-log" aria-label={t('lblWebDraftLog')}>
      <header>
        <b>{t('lblWebDraftLog')}</b>
        <label><input type="checkbox" checked={everyone} onChange={e => setEveryone(e.currentTarget.checked)} /> {t('lblWebDraftEverySeatsPicks')}</label>
      </header>
      {shown.map((line, i) => <p key={shown.length - i} class={line.kind === 'pack' ? 'head' : ''}>{line.text}</p>)}
    </aside>
  );
}

function grouped(held: Held[], by: GroupBy): [string, Held[]][] {
  if (by === 'pick') return held.length ? [[t('lblWebDraftInPickOrder'), held]] : [];
  const groups = new Map<string, Held[]>();
  // Mana value sorts low to high. The other groupings keep the order in which their first card was picked
  const ordered = by === 'mv' ? [...held].sort((a, b) => a.card.mv - b.card.mv) : held;
  for (const h of ordered) {
    const c = h.card;
    const key = by === 'type' ? typeHeading(c.type) : by === 'mv' ? mvGroup(c.mv) : colourHeading(c.colors);
    groups.set(key, [...(groups.get(key) ?? []), h]);
  }
  return [...groups.entries()];
}

/** The heading of the first of CARD_TYPES in a type line. */
function typeHeading(type: string): string {
  const found = CARD_TYPES.find(([name]) => type.includes(name));
  return t(found ? found[2] : 'lblOther');
}

function SaveDraft({ model, state, actions }: { model: Model; state: DraftState; actions: Actions }) {
  const [name, setName] = useState(() => `${state.product} ${shortDay()}`);
  const [discarding, setDiscarding] = useState(false);
  const taken = model.nameTaken !== null && model.nameTaken === name.trim();
  return (
    <Backdrop>
      <div class="dialog draft-save">
        <h3>{t('lblWebDraftComplete')}</h3>
        <p class="hint">{t('lblWebDraftCardsDrafted', state.picks.length)}</p>
        <form onSubmit={e => { e.preventDefault(); if (name.trim()) actions.draftSave(name.trim(), false); }}>
          <input type="text" value={name} aria-label={t('lblWebDraftName')} onInput={e => setName(e.currentTarget.value)} />
        </form>
        {taken && <p class="hint">{tNodes('lblWebDraftNameTaken', <b>{name.trim()}</b>)}</p>}
        <div class="actions">
          {discarding
            ? <>
                <span class="muted">{t('lblWebDraftThrowAway')}</span>
                <button onClick={() => setDiscarding(false)}>{t('lblWebDraftKeepThem')}</button>
                <button class="danger" onClick={() => actions.draftDiscard()}>{t('lblWebDraftDiscard')}</button>
              </>
            : <>
                <button onClick={() => setDiscarding(true)}>{t('lblWebDraftDiscard')}</button>
                {taken
                  ? <button class="danger" onClick={() => actions.draftSave(name.trim(), true)}>{t('lblWebDraftReplaceIt')}</button>
                  : <button class="primary" disabled={!name.trim()} onClick={() => actions.draftSave(name.trim(), false)}>{t('lblSave')}</button>}
              </>}
        </div>
      </div>
    </Backdrop>
  );
}
