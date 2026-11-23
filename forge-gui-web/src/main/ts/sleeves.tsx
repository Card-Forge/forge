// The sleeve picker. A card-art sleeve belongs to the deck, not the player. Choosing one writes it to the deck, so every Forge client uses it

import { useEffect, useRef, useState } from 'preact/hooks';
import { sleeveUrl } from './avatarsleeves';
import { rememberSleeve } from './identity';
import { imageUrl, artUrl } from './images';
import { t } from './text';
import type { Actions } from './actions';
import type { Model } from './model';
import type { Printing, Seat } from './protocol';
import { CloseButton } from './dialog';
import { ICONS, Icon } from './icons';
import { useDebounced } from './hooks';

const SEARCH_DEBOUNCE_MS = 250;
export const CENTRE = 500;

/** Converts the stored 0-1000 crop offset to a CSS position. It moves the art along whichever axis overflows. */
export const objectPosition = (value: number | undefined): string => `${(value ?? CENTRE) / 10}% ${(value ?? CENTRE) / 10}%`;
export { artUrl };

export function SleevePicker({ model, actions, index, seat, close }: {
  model: Model; actions: Actions; index: number; seat: Seat; close: () => void;
}) {
  // Choosing a card is a separate step, so the card picker replaces the sleeve grid instead of appearing above it
  const [picking, setPicking] = useState(false);
  const choose = (key: string, offset: number) => {
    actions.setSleeveArt(index, key, offset);
    close();
  };
  return (
    <div class="sleeves-back">
      <div class={picking ? 'sleeves picking' : 'sleeves'}>
        <header class="sleeves-head">
          <h2>{t(picking ? 'lblChooseaCard' : 'lblWebSleevesChooseSleeve')}</h2>
          <CloseButton onClick={close} />
        </header>
        <div class="sleeves-body">
          {/* Every card-art choice is written to the seat's deck. Without a deck the choice would be silently dropped */}
          <section class={seat.deck ? 'saved' : 'saved no-deck'}>
            <h3>{t('lblWebSleevesCardArt')}</h3>
            <div class="art-grid">
              {model.savedSleeveArt.map(art => (
                <span key={art.key} class="art-slot">
                  <button class="art-tile" aria-pressed={art.key === seat.sleeveArt} onClick={() => choose(art.key, art.offset)}>
                    <img alt="" src={artUrl(art.key)} style={{ objectPosition: objectPosition(art.offset) }} />
                  </button>
                  {/* The saved sleeves belong to the host, so only the host can remove one */}
                  {model.host && (
                    <button class="drop" title={t('lblWebSleevesRemoveArt')} aria-label={t('lblWebSleevesRemoveArt')}
                      onClick={() => actions.forgetSleeveArt(art.key)}><Icon d={ICONS.x} /></button>
                  )}
                </span>
              ))}
              <button class="art-tile add" title={t('lblWebSleevesPickCard')} onClick={() => setPicking(true)}>+</button>
            </div>
            <p class="no-deck-why" hidden={!!seat.deck}>{t('lblWebSleevesNeedDeck')}</p>
          </section>
          <section class="numbered">
            <h3>{t('lblWebSleevesSleeves')}</h3>
            <div class="sleeve-grid">
              {Array.from({ length: model.looks?.sleeveCount ?? 0 }, (_, i) => (
                <button key={i} class="sleeve-tile" aria-pressed={!seat.sleeveArt && i === seat.sleeve} onClick={() => {
                  // Clear the deck's card art so it no longer overrides the numbered sleeve, then set this sleeve
                  actions.setSleeveArt(index, '', CENTRE);
                  actions.setSeat(index, { sleeve: i });
                  if (seat.mine) rememberSleeve(i);
                  close();
                }}><img alt="" src={sleeveUrl(i)} /></button>
              ))}
            </div>
          </section>
          {picking && <ArtPicker model={model} actions={actions} back={() => setPicking(false)} use={choose} />}
        </div>
      </div>
    </div>
  );
}

function ArtPicker({ model, actions, back, use }: {
  model: Model; actions: Actions; back: () => void; use: (key: string, offset: number) => void;
}) {
  const [typed, setTyped] = useState<string | null>(null);
  const [name, setName] = useState<string | null>(null);
  const [picked, setPicked] = useState<Printing | null>(null);
  const [offset, setOffset] = useState(CENTRE);
  const find = useRef<HTMLInputElement>(null);
  useEffect(() => {
    find.current?.focus();
  }, []);
  const searched = useDebounced(typed, SEARCH_DEBOUNCE_MS);
  useEffect(() => {
    if (searched !== null) actions.searchCards(searched);
  }, [searched]);
  // Use only the printings of the chosen name. A reply for an earlier name belongs to a different card
  const printings = name !== null && model.printings?.name === name ? model.printings.list : [];
  const pick = (print: Printing) => {
    setPicked(print);
    // Each newly chosen card starts centred. Dragging moves the crop
    setOffset(CENTRE);
  };
  useEffect(() => {
    if (!picked && printings.length) pick(printings[0]);
  }, [printings]);
  return (
    <section class="art-picker">
      <button class="back-to-sleeves" onClick={back}>&larr; {t('lblWebSleevesBackToSleeves')}</button>
      <div class="names">
        <input ref={find} class="card-find" type="search" placeholder={t('lblWebSleevesSearchCard')} autocomplete="off"
          onInput={e => setTyped(e.currentTarget.value)} />
        <div class="name-list">
          {typed !== null && model.cardNames.map(n => (
            <button key={n} class="dk-cardname" aria-pressed={n === name} onClick={() => {
              setName(n);
              setPicked(null);
              actions.askPrintings(n);
            }}>{n}</button>
          ))}
        </div>
      </div>
      <div class="prints">
        {printings.map(p => (
          <button key={p.key} class="print" aria-pressed={picked?.key === p.key} onClick={() => pick(p)}>
            <img alt="" src={imageUrl(p.key)} /><span>{p.edition}</span>
          </button>
        ))}
      </div>
      <div class="preview">
        <p class="preview-title">{t('lblSleevePreview')}</p>
        <Crop art={picked} offset={offset} setOffset={setOffset} />
        <p class="dk-hint">{t('lblWebSleevesDragCrop')}</p>
        <button class="primary use-art" disabled={!picked} onClick={() => picked && use(picked.key, offset)}>{t('lblWebSleevesUseArt')}</button>
      </div>
    </section>
  );
}

// The art is cover-cropped, so it overflows on one axis only. Dragging moves the crop along that axis
function Crop({ art, offset, setOffset }: { art: Printing | null; offset: number; setOffset: (offset: number) => void }) {
  const drag = useRef({ x: 0, y: 0, from: CENTRE });
  return (
    <div class="preview-frame"
      onPointerDown={e => {
        if (!art) return;
        drag.current = { x: e.clientX, y: e.clientY, from: offset };
        e.currentTarget.setPointerCapture(e.pointerId);
      }}
      onPointerMove={e => {
        const frame = e.currentTarget;
        if (!art || !frame.hasPointerCapture(e.pointerId)) return;
        // Dragging the art with the cursor moves the crop window the other way
        const { x, y, from } = drag.current;
        const travel = Math.max(frame.clientWidth, frame.clientHeight) || 1;
        const moved = Math.abs(e.clientX - x) > Math.abs(e.clientY - y) ? e.clientX - x : e.clientY - y;
        setOffset(Math.max(0, Math.min(1000, Math.round(from - (moved * 1000) / travel))));
      }}
      onPointerUp={e => e.currentTarget.releasePointerCapture(e.pointerId)}>
      {/* The image is not draggable. Otherwise the browser's own image drag takes the pointer and the crop never moves */}
      <img alt="" draggable={false} src={art ? artUrl(art.key) : undefined} style={{ objectPosition: objectPosition(offset) }} />
    </div>
  );
}
