// Drags a card between the catalogue and the deck. It uses pointer events so the dragged chip can show what a drop will do

import { copyLimit, countsInDeck } from './catalogue';
import { imageUrl } from '../images';
import type { EditorState } from '../protocol';
import { t, type TextKey } from '../text';
import { make } from '../dom';
import { trackDrag } from '../press';

export type Zone = 'Main' | 'Sideboard' | 'Commander' | 'catalogue';

export interface Carried {
  name: string;
  /** The zone the drag started from. It is catalogue for a card not in the deck yet. */
  from: Zone;
  count: number;
  /** The copy limit the catalogue worked out for the card, which counts its own rules and the copies owned. */
  limit?: number;
}

export interface Verdict {
  zone: Zone;
  accepts: boolean;
  /** The drop breaks a rule of the deck, as opposed to dropping where the card already is. */
  refused?: boolean;
  /** The text the drag chip shows, such as "add 1 to Sideboard", or the reason the drop is refused. */
  verb: string;
}

const THRESHOLD_PX = 4;
// A refusal's text starts with this mark
const REFUSED = '⊘';
const ADD: Record<'Main' | 'Sideboard', TextKey> = { Main: 'lblWebEditorDragAddMain', Sideboard: 'lblWebEditorDragAddSideboard' };
const MOVE: Record<'Main' | 'Sideboard', TextKey> = { Main: 'lblWebEditorDragMoveMain', Sideboard: 'lblWebEditorDragMoveSideboard' };
const REMOVE: Record<Exclude<Zone, 'catalogue'>, TextKey> = {
  Main: 'lblWebEditorDragRemoveMain', Sideboard: 'lblWebEditorDragRemoveSideboard', Commander: 'lblWebEditorDragRemoveCommander',
};
const refused = (key: TextKey, ...args: number[]) => `${REFUSED} ${t(key, ...args)}`;

/** What dropping a dragged card on a zone would do. The server decides whether a card can be the commander. */
export function verdictFor(carried: Carried, zone: Zone, state: EditorState): Verdict {
  const n = carried.count;
  if (zone === carried.from) {
    return { zone, accepts: false, verb: carried.from === 'catalogue' ? t('lblWebEditorDragDropOnSection') : t('lblWebEditorDragAlreadyHere') };
  }
  if (zone === 'catalogue') {
    return { zone, accepts: true, verb: carried.from === 'catalogue' ? '' : t(REMOVE[carried.from], n) };
  }
  if (zone === 'Commander') {
    if (!state.commanderZone) {
      return { zone, accepts: false, refused: true, verb: refused('lblWebCardMenuOnlyCommanderFormats') };
    }
    return { zone, accepts: true, verb: state.commanders.length ? t('lblWebEditorDragReplaceCommander') : t('lblWebEditorDragMakeCommander') };
  }
  if (carried.from !== 'catalogue') {
    return { zone, accepts: true, verb: t(MOVE[zone], n) };
  }
  if (state.limited) {
    const left = state.sideboard.find(c => c.name === carried.name)?.count ?? 0;
    return n > left ? { zone, accepts: false, refused: true, verb: refused('lblWebEditorDragNoneLeft') } : { zone, accepts: true, verb: t(ADD[zone], n) };
  }
  const have = countsInDeck(state).get(carried.name) ?? 0;
  const limit = carried.limit ?? copyLimit(state, carried.name);
  if (have + n > limit) {
    return { zone, accepts: false, refused: true, verb: limit === 1 ? refused('lblWebEditorDragSingleton') : refused('lblWebEditorCopiesAlready', have, limit) };
  }
  return { zone, accepts: true, verb: t(ADD[zone], n) };
}

/** Starts a drag once the pressed pointer moves a few pixels. On release over an accepting zone, it passes that zone's verdict to drop. */
export function startDrag(e: PointerEvent, carried: Carried, image: string, judge: (zone: Zone) => Verdict,
  drop: (v: Verdict) => void): void {
  if (e.button !== 0 || e.pointerType === 'touch') {
    return;
  }
  let chip: HTMLElement | null = null;
  let over: Verdict | null = null;
  const zones = [...document.querySelectorAll<HTMLElement>('[data-zone]')];
  const source = e.currentTarget instanceof HTMLElement ? e.currentTarget : null;

  const start = () => {
    chip = makeChip(carried, image);
    source?.classList.add('dragging');
    for (const z of zones) mark(z, judge(z.dataset.zone as Zone), false);
  };
  const move = (ev: PointerEvent) => {
    if (!chip) return;
    chip.style.left = `${ev.clientX + 12}px`;
    chip.style.top = `${ev.clientY + 12}px`;
    const target = document.elementFromPoint(ev.clientX, ev.clientY)?.closest<HTMLElement>('[data-zone]') ?? null;
    over = target ? judge(target.dataset.zone as Zone) : null;
    for (const z of zones) mark(z, judge(z.dataset.zone as Zone), z === target);
    const verb = chip.querySelector('.verb');
    if (verb) verb.textContent = over ? over.verb : t('lblWebEditorDragDropOnSection');
    chip.classList.toggle('no', !!over && !over.accepts);
    chip.classList.toggle('rm', !!over && over.accepts && over.zone === 'catalogue');
    scrollNearEdge(target, ev.clientY);
  };
  const end = () => {
    if (!chip) return;
    chip.remove();
    source?.classList.remove('dragging');
    for (const z of zones) unmark(z);
    if (over?.accepts) drop(over);
  };
  trackDrag(e, THRESHOLD_PX, { start, move, end });
}

/** Pointer handlers for a draggable card. Its menu opens on right-click or from the More button in its preview. */
export type CardHandlers = (name: string, from: Zone, image: string, count: number, limit?: number) => {
  'data-menu': string;
  onPointerDown: (e: PointerEvent) => void;
  onContextMenu: (e: MouseEvent) => void;
};

function makeChip(carried: Carried, image: string): HTMLElement {
  const chip = make('div', 'drag-chip');
  const art = document.createElement('img');
  art.src = imageUrl(image);
  art.alt = '';
  const words = document.createElement('span');
  const name = document.createElement('b');
  name.textContent = carried.count > 1 ? `${carried.count} × ${carried.name}` : carried.name;
  const verb = make('span', 'verb');
  words.append(name, verb);
  chip.append(art, words);
  document.body.append(chip);
  return chip;
}

function mark(zone: HTMLElement, v: Verdict, under: boolean): void {
  zone.classList.toggle('drop-eligible', v.accepts && !under);
  zone.classList.toggle('drop-over', v.accepts && under);
  zone.classList.toggle('drop-refuse', !!v.refused);
  zone.classList.toggle('drop-remove', v.accepts && v.zone === 'catalogue');
  zone.dataset.dropHint = v.accepts ? (under ? v.verb : t('lblWebEditorDragWouldAccept')) : v.verb;
}

function unmark(zone: HTMLElement): void {
  zone.classList.remove('drop-eligible', 'drop-over', 'drop-refuse', 'drop-remove');
  delete zone.dataset.dropHint;
}

function scrollNearEdge(zone: HTMLElement | null, y: number): void {
  const body = zone?.querySelector<HTMLElement>('.zone-body, .cat-grid, .cat-table');
  if (!body) return;
  const box = body.getBoundingClientRect();
  if (y < box.top + 30) body.scrollTop -= 12;
  else if (y > box.bottom - 30) body.scrollTop += 12;
}
