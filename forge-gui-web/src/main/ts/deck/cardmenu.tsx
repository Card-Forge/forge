// A card's menu in the editor. It offers every move a drag can make, so no action needs dragging

import { useEffect, useMemo, useRef, useState } from 'preact/hooks';
import type { ComponentChildren } from 'preact';
import { useDismiss } from '../hooks';
import { imageUrl } from '../images';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { DeckSection, EditorCard, EditorState } from '../protocol';
import type { Zone } from './drag';
import { t, type TextKey } from '../text';
import { addCard, preferPrinting, preferredPrinting } from './preferred';
import { Backdrop } from '../dialog';
import { Select } from '../select';

/** The art styles the printing picker filters by, as on desktop. Each entry is a section name in the edition file and its label. */
const ART_STYLES: [string, TextKey][] = [
  ['', 'lblArtStyleAll'], ['cards', 'lblArtStyleStandard'], ['borderless', 'lblArtStyleBorderless'], ['full art', 'lblArtStyleFullArt'],
  ['showcase', 'lblArtStyleShowcase'], ['extended art', 'lblArtStyleExtendedArt'], ['retro frame', 'lblArtStyleRetroFrame'], ['promo', 'lblArtStylePromo'],
];

const IN_SECTION: Record<DeckSection, TextKey> = {
  Main: 'lblWebCardMenuInMain', Sideboard: 'lblWebCardMenuInSideboard', Commander: 'lblWebCardMenuInCommander',
  Avatar: 'lblWebCardMenuInAvatar', Planes: 'lblWebCardMenuInPlanes', Schemes: 'lblWebCardMenuInSchemes',
  Conspiracy: 'lblWebCardMenuInConspiracy', Dungeon: 'lblWebCardMenuInDungeon', Attractions: 'lblWebCardMenuInAttractions',
  Contraptions: 'lblWebCardMenuInContraptions', Stickers: 'lblWebCardMenuInStickers',
};

export interface MenuAt {
  name: string;
  from: Zone;
  x: number;
  y: number;
}

function cardIn(state: EditorState, zone: Zone | DeckSection, name: string): EditorCard | undefined {
  const cards = zone === 'Main' ? state.main.flatMap(g => g.cards) : zone === 'Sideboard' ? state.sideboard
    : zone === 'Commander' ? state.commanders : [];
  return cards.find(c => c.name === name);
}

export function CardMenu({ at, state, actions, close, printings }: {
  at: MenuAt; state: EditorState; actions: Actions; close: () => void; printings: (zone: DeckSection | null) => void;
}) {
  const root = useRef<HTMLDivElement>(null);
  useDismiss(true, root, close);
  const shell = (items: ComponentChildren) => (
    <div ref={root} class="deck-menu card-menu" role="menu" style={{ left: `${at.x}px`, top: `${at.y}px` }}>
      <span class="menu-cap">{at.name}</span>
      {items}
    </div>
  );
  const act = (run: () => void) => () => {
    run();
    close();
  };
  const inDeck = at.from === 'catalogue' ? undefined : cardIn(state, at.from, at.name);
  const zone = at.from === 'catalogue' ? null : at.from;
  const other: DeckSection = zone === 'Main' ? 'Sideboard' : 'Main';
  // A pool's cards only move between the pool and the deck. There is no separate sideboard and no printing to choose
  if (state.limited) {
    return shell(<>
        <button role="menuitem" onClick={act(() => actions.edit({ op: 'add', name: at.name, to: 'Main', count: 1 }))}>{t('lblWebCardMenuAddToDeck')}</button>
        {zone !== null && <>
          <button role="menuitem" onClick={act(() => actions.edit({ op: 'remove', name: at.name, from: zone, count: 1 }))}>{t('lblWebCardMenuReturnOne')}</button>
          {inDeck && inDeck.count > 1 && (
            <button role="menuitem" onClick={act(() => actions.edit({ op: 'remove', name: at.name, from: zone, count: inDeck.count }))}>
              {t('lblWebCardMenuReturnAll', inDeck.count)}
            </button>
          )}
        </>}
        {state.commanderZone && zone !== 'Commander' && (
          <button role="menuitem" onClick={act(() => actions.edit({ op: 'commander', name: at.name, from: zone ?? undefined, count: 1 }))}>
            {t('lblWebCardMenuMakeCommander')}
          </button>
        )}
      </>);
  }
  return shell(<>
      {zone === null && <>
        <button role="menuitem" onClick={act(() => addCard(actions, state, at.name, 'Main'))}>{t('lblWebCardMenuAddToMain')}</button>
        <button role="menuitem" onClick={act(() => addCard(actions, state, at.name, 'Sideboard'))}>{t('lblWebCardMenuAddToSideboard')}</button>
      </>}
      {zone !== null && zone !== 'Commander' && <>
        <button role="menuitem" onClick={act(() => actions.edit({ op: 'add', name: at.name, to: zone, count: 1 }))}>{t('lblWebCardMenuAddOneMore')}</button>
        <button role="menuitem" onClick={act(() => actions.edit({ op: 'move', name: at.name, from: zone, to: other, count: 1 }))}>
          {t(other === 'Main' ? 'lblWebCardMenuMoveOneToMain' : 'lblWebCardMenuMoveOneToSideboard')}
        </button>
        {inDeck && inDeck.count > 1 && (
          <button role="menuitem" onClick={act(() => actions.edit({ op: 'move', name: at.name, from: zone, to: other, count: inDeck.count }))}>
            {t('lblWebCardMenuMoveAll', inDeck.count)}
          </button>
        )}
      </>}
      <hr />
      {zone !== 'Commander' && (
        <button role="menuitem" disabled={!state.commanderZone} title={state.commanderZone ? undefined : t('lblWebCardMenuOnlyCommanderFormats')}
          onClick={act(() => actions.edit({ op: 'commander', name: at.name, from: zone ?? undefined, count: 1 }))}>
          {t('lblWebCardMenuMakeCommander')}
        </button>
      )}
      {/* For a card in the deck this changes the printing of its copies. For a catalogue card it sets the printing this browser prefers. */}
      <button role="menuitem" onClick={act(() => printings(zone))}>{t(zone ? 'lblChangePrinting' : 'lblWebCardMenuPreferredPrinting')}</button>
      {zone !== null && inDeck && (
        <button role="menuitem" onClick={act(() => actions.edit({ op: 'foil', name: at.name, from: zone, count: 1 }))}>
          {t(inDeck.foil ? 'lblWebCardMenuRemoveFoil' : 'lblWebCardMenuMakeFoil')}
        </button>
      )}
      {zone !== null && <>
        <hr />
        <button role="menuitem" onClick={act(() => actions.edit({ op: 'remove', name: at.name, from: zone, count: 1 }))}>{t('lblWebCardMenuRemoveOne')}</button>
        {inDeck && inDeck.count > 1 && (
          <button role="menuitem" onClick={act(() => actions.edit({ op: 'remove', name: at.name, from: zone, count: inDeck.count }))}>
            {t('lblWebCardMenuRemoveAll', inDeck.count)}
          </button>
        )}
      </>}
    </>);
}

/** Shows every printing as a picture, filtered by set or style as in desktop's dialog. Steppers move copies between printings, so the total never changes. Each step is saved at once, so there is no Cancel. */
export function PrintingPicker({ name, zone, model, state, actions, close }: {
  name: string; zone: DeckSection | null; model: Model; state: EditorState; actions: Actions; close: () => void;
}) {
  useEffect(() => {
    actions.askPrintings(name, state.cardPool ?? null);
  }, [name]);
  const card = zone ? cardIn(state, zone, name) : undefined;
  const preferred = preferredPrinting(name);
  const list = model.printings?.name === name ? model.printings.list : [];
  const counts = new Map((card?.split ?? []).map(p => [p.key, p.count]));
  const send = (next: Map<string, number>) => actions.edit({
    op: 'printings', name, from: zone ?? undefined, count: card?.count ?? 0,
    printings: [...next].filter(([, n]) => n > 0).map(([key, count]) => ({ name: key, count })),
  });
  // Adding a copy of this printing takes one from the printing with the most copies. Removing one gives it to that printing
  const shift = (key: string, by: 1 | -1) => {
    const next = new Map(counts);
    const others = list.map(p => p.key).filter(k => k !== key && !list.find(p => p.key === k)?.problem);
    const donor = by > 0 ? others.filter(k => (next.get(k) ?? 0) > 0).sort((a, b) => (next.get(b) ?? 0) - (next.get(a) ?? 0))[0]
      : others.sort((a, b) => (next.get(b) ?? 0) - (next.get(a) ?? 0))[0];
    if (!donor) return;
    next.set(key, (next.get(key) ?? 0) + by);
    next.set(donor, (next.get(donor) ?? 0) - by);
    send(next);
  };
  // Held and preferred printings are listed first, as desktop lists the current one first. The order is fixed when the list arrives
  const ordered = useMemo(() => [...list].sort((x, y) => Number(counts.has(y.key) || y.key === preferred) - Number(counts.has(x.key) || x.key === preferred)), [list]);
  const [query, setQuery] = useState('');
  const [style, setStyle] = useState('');
  const wanted = query.trim().toLowerCase();
  const shown = ordered.filter(p => (!wanted || p.edition.toLowerCase().includes(wanted) || p.setName.toLowerCase().includes(wanted))
    && (!style || p.style === style));
  return (
    <Backdrop dismiss={close}>
      <div class="dialog picker">
        <h3>{t(zone ? 'lblChangePrintingDialogTitle' : 'lblWebCardMenuPreferredOf', name)}</h3>
        <div class="print-bar">
          <input placeholder={t('lblChangePrintingSearchHint')} aria-label={t('lblChangePrintingSearchHint')} value={query}
            onInput={e => setQuery(e.currentTarget.value)} />
          <Select label={t('lblArtStyleAll')} value={style} change={setStyle} choices={ART_STYLES.map(([section, label]) => ({ value: section, label: t(label) }))} />
        </div>
        <div class="print-grid">
          {!list.length && <p class="hint">{t('lblWebCardMenuReadingPrintings')}</p>}
          {list.length > 0 && !shown.length && <p class="hint">{t('lblChangePrintingNoResults')}</p>}
          {shown.map(p => {
            const n = counts.get(p.key) ?? 0;
            return (
              <div key={p.key} class={`print-cell${(zone ? n : p.key === preferred) ? ' cur' : ''}${p.problem ? ' off' : ''}${zone ? '' : ' choose'}`}
                onClick={zone || p.problem ? undefined : () => preferPrinting(name, p.key)}
                // A double-click on a printing sets every copy to it and closes, as in desktop's dialog. A double-click on a stepper is ignored here
                onDblClick={e => {
                  if (p.problem || (e.target as Element).closest('button')) return;
                  if (zone) send(new Map([[p.key, card?.count ?? 0]]));
                  else preferPrinting(name, p.key);
                  close();
                }}>
                <img loading="lazy" alt="" src={imageUrl(p.key)} />
                <span class="ed"><b>{p.setName}</b>{p.problem ? `⊘ ${p.problem}` : `${p.edition}${p.year ? ` · ${p.year}` : ''}`}</span>
                {zone && <span class="under">
                  <button class="step" disabled={!n} onClick={() => shift(p.key, -1)}>&minus;</button>
                  <span class={n ? 'n' : 'n zero'}>{n}</span>
                  <button class="step" disabled={!!p.problem || n >= (card?.count ?? 0)} onClick={() => shift(p.key, 1)}>+</button>
                </span>}
              </div>
            );
          })}
        </div>
        <div class="actions">
          <p class="hint">{zone ? t(IN_SECTION[zone], card?.count ?? 0) : t('lblWebCardMenuPreferredHint')}</p>
          {!zone && <button disabled={!preferred} onClick={() => preferPrinting(name, null)}>{t('lblWebCardMenuPreferredNone')}</button>}
          <button class="primary" onClick={close}>{t('lblDone')}</button>
        </div>
      </div>
    </Backdrop>
  );
}
