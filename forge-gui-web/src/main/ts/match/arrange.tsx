// The questions that put cards in order: an order, a library to rearrange, and the sideboard between games

import { useEffect, useLayoutEffect, useRef, useState } from 'preact/hooks';
import { SymbolText } from '../symbols';
import { cssUrl, playerSleeveUrl } from '../avatarsleeves';
import { me, stateOf, type Model } from '../model';
import type { ManipulateRequest, OrderRequest, RequestOption, CardView, SideboardRequest } from '../protocol';
import { t } from '../text';
import { make, reducedMotion } from '../dom';
import { isPortrait } from '../form';
import { trackDrag } from '../press';
import { BoardCard, Button, ButtonRow, OkKey, OptionView, offerAnswer, useCardFit, type Answer } from './questionparts';

/** The player's library as a pile in their sleeve. The top and bottom ends of the library are drawn on either side of it. */
function LibraryPile({ model, count }: { model: Model; count: number }) {
  return (
    <div class="lib-pile" style={{ '--sleeve': cssUrl(playerSleeveUrl(me(model))) }} aria-hidden="true">
      <i /><i /><i />
      <span class="lib-count">{count}</span>
      <span class="lib-name">{t('lblLibrary')}</span>
    </div>
  );
}

/** The engine ends a triggered ability's text with the cause of the trigger, in square brackets. */
const SET_OFF_BY = /\s*\[([^\]]*)\]$/;

/** An ability to be put in order. The picture and name of its source card are shown above its text. */
function AbilityRow({ model, opt, text, detail, tag, onClick }: {
  model: Model; opt: RequestOption; text: string; detail: string; tag?: string; onClick: () => void;
}) {
  const source = opt.source ? model.objects.get(opt.source.ref) as CardView | undefined : undefined;
  return (
    <>
      {source && <BoardCard model={model} card={source} />}
      <button class="ability" onClick={onClick}>
        {(source || tag) && <span class="ability-source">{source ? stateOf(model, source).Name : ''}{tag && <span class="order-tag">{tag}</span>}</span>}
        <span><SymbolText text={text} /></span>
        {detail && <span class="ability-detail">{detail}</span>}
      </button>
    </>
  );
}

const CHEVRON = { up: 'M6.5 14.5l5.5-5.5 5.5 5.5', down: 'M6.5 9.5l5.5 5.5 5.5-5.5' };

/** How long a card takes to slide to a new place in the order, and to settle when it is dropped. */
const CARRY_MS = 170;
const CARRY_EASE = 'cubic-bezier(.2,.8,.2,1)';

/** Where an item rests in its row, minus any slide still in progress. */
function resting(el: Element): DOMRect {
  const r = el.getBoundingClientRect();
  const [x = 0, y = 0] = getComputedStyle(el).translate.split(' ').map(v => parseFloat(v) || 0);
  return new DOMRect(r.left - x, r.top - y, r.width, r.height);
}

/**
 * Slides the items that a change moves from their old places to their new ones. Call the returned function just before
 * the change. Items are matched by data-slide, so an item redrawn in another row still slides from its old place.
 */
function useSlide(items: () => HTMLElement[], change: unknown): () => void {
  const stood = useRef<Map<string, DOMRect> | null>(null);
  useLayoutEffect(() => {
    const was = stood.current;
    stood.current = null;
    if (!was || reducedMotion()) return;
    for (const el of items()) {
      const from = was.get(el.dataset.slide ?? '');
      el.getAnimations().forEach(a => a.cancel());
      const to = el.getBoundingClientRect();
      if (!from || el.classList.contains('carried') || (from.left === to.left && from.top === to.top)) continue;
      el.animate([{ translate: `${from.left - to.left}px ${from.top - to.top}px` }, { translate: '0 0' }], { duration: CARRY_MS, easing: CARRY_EASE });
    }
  }, [change]);
  return () => { stood.current = new Map(items().map(el => [el.dataset.slide ?? '', el.getBoundingClientRect()])); };
}

interface Carry {
  /** The row the card is lifted from. The copy is drawn next to it with the row's classes, so the card looks the same as in the row. */
  row: HTMLElement;
  copyClass: string;
  ghost: { current: HTMLElement | null };
  slots: () => Element[];
  /** The card's current element, which the copy settles onto when released. Without it, the copy settles onto the lifted element. */
  home?: () => Element | null;
  /** A card tilts in the direction it moves. A line of words does not tilt, and it keeps its width. */
  words: boolean;
  lifted: () => void;
  over: (slot: number) => void;
  dropped: () => void;
}

/** Drags a card as a copy that follows the pointer. The card moves to the place nearest the pointer and settles there on release. A finger scrolls the dialog, so only a mouse or pen drags. */
function carryCard(e: PointerEvent, c: Carry): void {
  if (e.pointerType === 'touch' || e.button !== 0 || c.ghost.current) return;
  const el = e.currentTarget as HTMLElement;
  const start = { x: e.clientX, y: e.clientY };
  let from: DOMRect | null = null;
  let last = e.clientX;
  let lean = 0;
  const lift = () => {
    from = el.getBoundingClientRect();
    const lifted = make('div', `${c.copyClass} order-ghost`);
    lifted.style.cssText = `left: ${from.left}px; top: ${from.top}px;${c.words ? ` width: ${from.width}px; min-width: 0;` : ''}`;
    // The row's cards are sized to fit the screen, so the copy gets the same size
    const fit = c.row.style.getPropertyValue('--fit');
    if (fit) lifted.style.setProperty('--fit', fit);
    lifted.append(el.cloneNode(true));
    c.row.after(lifted);
    c.ghost.current = lifted;
    c.lifted();
    return true;
  };
  const onMove = (m: PointerEvent) => {
    const dx = m.clientX - start.x;
    const dy = m.clientY - start.y;
    const lifted = c.ghost.current;
    if (!lifted || !from) return;
    // The card tilts in the direction it moves, and turns upright when it stops
    lean += (Math.max(-9, Math.min(9, (m.clientX - last) * 0.9)) - lean) * 0.35;
    last = m.clientX;
    lifted.style.translate = `${dx}px ${dy}px`;
    if (!c.words) lifted.style.rotate = `${lean.toFixed(2)}deg`;
    const x = from.left + from.width / 2 + dx;
    const y = from.top + from.height / 2 + dy;
    const far = (r: DOMRect) => Math.hypot(r.left + r.width / 2 - x, r.top + r.height / 2 - y);
    const slots = c.slots().map(resting);
    let best = 0;
    slots.forEach((s, n) => {
      if (far(s) < far(slots[best])) best = n;
    });
    if (slots.length) c.over(best);
  };
  const onUp = () => {
    const lifted = c.ghost.current;
    if (!lifted || !from) return;
    const done = () => {
      lifted.remove();
      c.ghost.current = null;
      c.dropped();
    };
    const home = c.home ? c.home() : el;
    if (reducedMotion() || !home) {
      done();
      return;
    }
    const to = resting(home);
    lifted.animate([{ translate: lifted.style.translate || '0 0', rotate: lifted.style.rotate || '0deg', scale: getComputedStyle(lifted).scale },
      { translate: `${to.left - from.left}px ${to.top - from.top}px`, rotate: '0deg', scale: '1' }],
    { duration: CARRY_MS, easing: CARRY_EASE, fill: 'forwards' }).finished.then(done, done);
  };
  // trackDrag makes sure the click that ends a drag does not count as a tap on the card
  trackDrag(e, 5, { start: lift, move: onMove, end: onUp });
}

export function Order({ req, model, answer }: { req: OrderRequest; model: Model; answer: Answer }) {
  const [chosen, setChosen] = useState<number[]>(() => [...req.selected]);
  const [remember, setRemember] = useState(false);
  /** The card tapped to be moved, by its index in the options. */
  const [held, setHeld] = useState<number | null>(null);
  const [carried, setCarried] = useState<number | null>(null);
  const row = useRef<HTMLDivElement>(null);
  const order = useRef(chosen);
  order.current = chosen;
  const ghost = useRef<HTMLElement | null>(null);
  // Options that are only words, such as abilities, are listed down the dialog. Cards stay in a row
  const listed = req.options.every(o => !o.card && !o.imageKey && !o.name && !o.player);
  // A phone puts these cards four to a row (portrait.css), because in one row they would be too small to read
  useCardFit(row, listed || isPortrait() ? 0 : chosen.length, { gap: 22, below: 38 });
  const words = req.options.map(o => {
    const found = SET_OFF_BY.exec(o.label ?? '');
    return found ? { text: o.label.slice(0, found.index), detail: found[1] } : { text: o.label ?? '', detail: '' };
  });
  // The cause of each trigger is shown only when the causes differ, so it tells the abilities apart
  const tellsApart = new Set(words.map(w => w.detail)).size > 1;
  const items = () => (row.current ? [...row.current.children] as HTMLElement[] : []);
  const slide = useSlide(items, chosen);
  const move = (from: number, to: number) => {
    slide();
    setChosen(list => {
      const next = [...list];
      next.splice(to, 0, next.splice(from, 1)[0]);
      return next;
    });
  };
  useEffect(() => () => ghost.current?.remove(), []);
  const carry = (e: PointerEvent, i: number) => {
    if (!row.current) return;
    carryCard(e, {
      row: row.current, copyClass: `options ordered${listed ? ' listed' : ''}`, ghost, words: listed,
      slots: items,
      lifted: () => { setHeld(null); setCarried(i); },
      over: best => {
        const at = order.current.indexOf(i);
        if (at >= 0 && best !== at) move(at, best);
      },
      dropped: () => setCarried(null),
    });
  };
  const at = held === null ? -1 : chosen.indexOf(held);
  const step = (by: number) => {
    if (at >= 0 && at + by >= 0 && at + by < chosen.length) move(at, at + by);
  };
  // The arrow keys move the held card along the row, or the held ability up and down its list
  const stepRef = useRef(step);
  stepRef.current = step;
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const by = e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : 0;
      if (!by) return;
      e.preventDefault();
      stepRef.current(by);
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);
  const waiting = req.options.some((_, i) => !chosen.includes(i));
  offerAnswer(req.id, chosen.length >= req.min && chosen.length <= req.max ? () => answer({ indices: chosen, remember }) : null);
  // A card can be removed from the row only when the question allows some to be left out
  const optional = req.min < req.options.length;
  const rememberBox = req.remember
    && <label><input type="checkbox" checked={remember} onChange={e => setRemember(e.currentTarget.checked)} /> {t('lblWebDialogRememberOrder')}</label>;
  // An order with a top and a bottom, such as a library, has a label at both ends. Any other order has a label only on the first item
  const twoEnds = !listed && !!req.bottom;
  const railed = !listed && !optional && !!req.top && !twoEnds;
  const nudge = (by: number, pos: number) => (
    <button class={by < 0 ? 'earlier' : 'later'} disabled={by < 0 ? pos === 0 : pos === chosen.length - 1}
      aria-label={t(by < 0 ? 'lblWebDialogMoveEarlier' : 'lblWebDialogMoveLater')} onClick={() => step(by)}>{by < 0 ? '‹' : '›'}</button>
  );
  const stepButton = (by: number, pos: number) => (
    <button class={by < 0 ? 'earlier' : 'later'} disabled={by < 0 ? pos === 0 : pos === chosen.length - 1}
      aria-label={t(by < 0 ? 'lblWebDialogMoveEarlier' : 'lblWebDialogMoveLater')} onClick={() => step(by)}>
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d={by < 0 ? CHEVRON.up : CHEVRON.down} /></svg>
    </button>
  );
  return (
    <>
      {waiting && <>
        <p class="hint">{t('lblWebDialogPickInOrder')}</p>
        <div class="options">
          {req.options.map((o, i) => chosen.includes(i) ? null
            : <OptionView key={i} model={model} opt={o} onClick={() => setChosen(list => [...list, i])} />)}
        </div>
      </>}
      <div class={listed ? 'options ordered listed' : 'options ordered'} ref={row}>
        {chosen.map((i, pos) => {
          const hold = () => {
            setHeld(h => (h === i ? null : i));
          };
          return (
            <div key={i} data-slide={i} class={`ordered-item${carried === i ? ' carried' : ''}${held === i ? ' selected' : ''}`} onPointerDown={e => carry(e, i)}>
              {listed
                ? <AbilityRow model={model} opt={req.options[i]} text={words[i].text} detail={tellsApart ? words[i].detail : ''}
                    tag={pos === 0 ? req.top : undefined} onClick={hold} />
                : <OptionView model={model} opt={req.options[i]} onClick={hold} />}
              {/* The number is the card's place in the order, which is what the dialog asks for */}
              <span class="order-number">{pos + 1}</span>
              {!listed && !railed && pos === 0 && req.top && <span class="order-end">{req.top}</span>}
              {twoEnds && pos === chosen.length - 1 && pos > 0 && <span class="order-end">{req.bottom}</span>}
              {/* Drawn under every card but shown only for the held card, so holding a card does not move anything else in the dialog */}
              {!listed && <span class="order-nudge">{nudge(-1, pos)}{nudge(1, pos)}</span>}
              {/* The held ability's arrows replace its grip, so they appear where the player is already looking */}
              {listed && (held === i ? <span class="order-step">{stepButton(-1, pos)}{stepButton(1, pos)}</span> : <span class="order-grip" aria-hidden="true" />)}
            </div>
          );
        })}
      </div>
      {railed && <p class="order-rail"><b>{req.top}</b><i /></p>}
      {!listed && rememberBox}
      <ButtonRow>
        <span class="order-move">
          {listed && rememberBox}
          {optional && <button disabled={at < 0} onClick={() => {
            setChosen(list => list.filter(c => c !== held));
            setHeld(null);
          }}>{t('lblRemove')}</button>}
        </span>
        <Button primary disabled={chosen.length < req.min || chosen.length > req.max}
          onClick={() => answer({ indices: chosen, remember })}>{t('lblWebDialogConfirm')} <OkKey /></Button>
      </ButtonRow>
    </>
  );
}

// The host sends the whole library, but only the top cards can move. Cards placed after the rest go to the bottom
type Shelf = 'top' | 'bottom';

/** The top and bottom of the library on either side of the pile. The further a card is from the pile, the nearer it is to that end. */
export function Manipulate({ req, model, answer }: { req: ManipulateRequest; model: Model; answer: Answer }) {
  const all = req.options.map((_, i) => i);
  const rest = all.filter(i => !req.movable.includes(i));
  const [piles, setPiles] = useState(() => ({ top: all.filter(i => req.movable.includes(i)), bottom: [] as number[] }));
  const now = useRef(piles);
  now.current = piles;
  /** The card being dragged, by its index in the options. */
  const [carried, setCarried] = useState<number | null>(null);
  const ghost = useRef<HTMLElement | null>(null);
  const bothEnds = req.toBottom || req.toTop;
  const row = useRef<HTMLDivElement>(null);
  // One row holds each end's cards, or an empty place for an end with none, and the pile between them. A phone stacks the ends instead (portrait.css)
  useCardFit(row, isPortrait() ? 0 : Math.max(piles.top.length, 1) + (bothEnds ? Math.max(piles.bottom.length, 1) : 0) + 1, { gap: 18 });

  // Each end's cards, and the empty place of an end with none, in page order
  const slots = () => (row.current ? [...row.current.querySelectorAll<HTMLElement>('.lib-card')] : []);
  const slide = useSlide(() => slots().filter(el => el.dataset.slide), piles);
  useEffect(() => () => ghost.current?.remove(), []);
  const place = (card: number, shelf: Shelf, at: number) => {
    const p = now.current;
    if (p[shelf][at] === card) return;
    slide();
    setPiles(p => {
      const next = { top: p.top.filter(i => i !== card), bottom: p.bottom.filter(i => i !== card) };
      next[shelf].splice(Math.min(at, next[shelf].length), 0, card);
      return next;
    });
  };
  const carry = (e: PointerEvent, card: number) => {
    if (!row.current) return;
    carryCard(e, {
      row: row.current, copyClass: 'lib-row', ghost, words: false, slots,
      home: () => row.current?.querySelector(`.lib-card[data-slide="${card}"]`) ?? null,
      lifted: () => setCarried(card),
      over: n => {
        const slot = slots()[n];
        if (slot) place(card, slot.dataset.shelf as Shelf, Number(slot.dataset.pos));
      },
      dropped: () => setCarried(null),
    });
  };
  // A card sent to the other end goes next to the pile, which is the middle of the library at that end
  const send = (from: Shelf, pos: number) => setPiles(p => {
    const card = p[from][pos];
    const next = { top: [...p.top], bottom: [...p.bottom] };
    next[from] = next[from].filter((_, k) => k !== pos);
    if (from === 'top') next.bottom = [card, ...next.bottom];
    else next.top = [...next.top, card];
    return next;
  });

  offerAnswer(req.id, () => answer([...piles.top, ...rest, ...piles.bottom]));

  // The top runs from its end to the pile, and the bottom from the pile to its end. So the card at each end is the one furthest out
  const side = (name: Shelf) => (
    <div class={`lib-side ${name}`}>
      <div class="lib-cards">
        {piles[name].map((i, pos) => (
          <div key={i} data-slide={i} data-shelf={name} data-pos={pos} class={`lib-card${carried === i ? ' carried' : ''}`} onPointerDown={e => carry(e, i)}>
            <OptionView model={model} opt={req.options[i]} onClick={bothEnds ? () => send(name, pos) : undefined} />
            {name === 'top' && <span class="order-number">{pos + 1}</span>}
            {name === 'top' && pos === 0 && <span class="order-end">{t('lblClosestToTop')}</span>}
            {name === 'bottom' && pos === piles.bottom.length - 1 && <span class="order-end">{t('lblClosestToBottom')}</span>}
          </div>
        ))}
        {piles[name].length === 0 && <div class="lib-card" data-shelf={name} data-pos={0}><span class="lib-slot" /><span class="order-end">{t(name === 'top' ? 'lblClosestToTop' : 'lblClosestToBottom')}</span></div>}
      </div>
    </div>
  );

  return (
    <div class="manipulate">
      <div class="lib-row" ref={row}>
        {side('top')}
        <LibraryPile model={model} count={rest.length} />
        {bothEnds && side('bottom')}
      </div>
      <p class="hint">{t(bothEnds ? 'lblWebDialogMoveAcross' : 'lblWebDialogReorderHint')}</p>
      <ButtonRow>
        <Button primary onClick={() => answer([...piles.top, ...rest, ...piles.bottom])}>{t('lblWebDialogConfirm')} <OkKey /></Button>
      </ButtonRow>
    </div>
  );
}

// Between games the player moves copies between the main deck and the sideboard. The host asks again if the deck is illegal
export function Sideboard({ req, model, answer }: { req: SideboardRequest; model: Model; answer: Answer }) {
  const [inMain, setInMain] = useState(() => [...req.main]);
  const move = (i: number, delta: number) => setInMain(m => m.map((n, k) => (k === i ? n + delta : n)));
  const columns = useRef<HTMLDivElement>(null);
  const ghost = useRef<HTMLElement | null>(null);
  useEffect(() => () => ghost.current?.remove(), []);
  const lists = () => (columns.current ? [...columns.current.querySelectorAll('.sb-list')] : []);
  // A copy dragged over the other list moves there at once, as a click would. Dragging it back moves it back
  const carry = (e: PointerEvent, i: number, fromMain: boolean) => {
    if (!columns.current) return;
    let inMainNow = fromMain;
    carryCard(e, {
      row: columns.current, copyClass: 'sb-list', ghost, words: true, slots: lists,
      home: () => lists()[inMainNow ? 0 : 1]?.querySelector(`.sb-row[data-entry="${i}"]`) ?? null,
      lifted: () => {},
      over: n => {
        if ((n === 0) === inMainNow) return;
        inMainNow = n === 0;
        move(i, inMainNow ? 1 : -1);
      },
      dropped: () => {},
    });
  };
  const row = (i: number, count: number, arrow: string, delta: number) => (
    <div key={i} class="sb-row" data-entry={i} onPointerDown={e => carry(e, i, delta < 0)}>
      <OptionView model={model} opt={req.entries[i]} onClick={() => move(i, delta)} />
      <span class="sb-name">{req.entries[i].name}</span>
      <span class="sb-count">{`×${count}`}</span>
      <Button onClick={() => move(i, delta)}>{arrow}</Button>
    </div>
  );
  const mainTotal = inMain.reduce((a, b) => a + b, 0);
  const sideTotal = req.entries.reduce((a, e, i) => a + e.total - inMain[i], 0);
  return (
    <>
      <p class="hint">{t('lblWebDialogMoveOneCopy')}</p>
      <div class="sb-columns" ref={columns}>
        <div>
          <h4>{t('lblWebDialogMainDeckCount', mainTotal)}</h4>
          <div class="sb-list">{req.entries.map((_, i) => (inMain[i] > 0 ? row(i, inMain[i], '→', -1) : null))}</div>
        </div>
        <div>
          <h4>{t('lblSideboardNCards', sideTotal)}</h4>
          <div class="sb-list">{req.entries.map((e, i) => (e.total - inMain[i] > 0 ? row(i, e.total - inMain[i], '←', 1) : null))}</div>
        </div>
      </div>
      <ButtonRow>
        <Button onClick={() => setInMain([...req.main])}>{t('lblReset')}</Button>
        <Button primary onClick={() => answer(inMain)}>{t('lblDone')}</Button>
      </ButtonRow>
    </>
  );
}

