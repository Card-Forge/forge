// The server lists the planar decks, scheme decks and avatars a seat may use. So a computer seat is only offered avatars the computer can play

import { useEffect, useState } from 'preact/hooks';
import { imageUrl, smallImage } from '../images';
import { Icon, ICONS } from '../icons';
import { ArtCrop } from '../deck/planareditor';
import { normalize } from '../search';
import { t, type TextKey, signed } from '../text';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { ExtraChoice, Seat } from '../protocol';
import { Backdrop, CloseButton } from '../dialog';
import { changeUi } from '../ui';

type ExtraKind = 'planes' | 'schemes' | 'vanguard';

const SECTION: Record<ExtraKind, string> = { planes: 'Planes', schemes: 'Schemes', vanguard: 'Avatar' };
const TITLE: Record<ExtraKind, TextKey> = { planes: 'lblPlanarDeck', schemes: 'lblSchemeDeck', vanguard: 'lblWebLobbyChooseAvatar' };
/** The kinds that are chosen as a whole deck. */
type DeckKind = Exclude<ExtraKind, 'vanguard'>;

const GENERATED: Record<DeckKind, TextKey> = { planes: 'lblWebLobbyRandomPlanesHint', schemes: 'lblWebLobbyRandomSchemesHint' };
const IN_DECK: Record<DeckKind, TextKey> = { planes: 'lblWebLobbyPlanesIn', schemes: 'lblWebLobbySchemesIn' };
const SAVED: Record<DeckKind, TextKey> = { planes: 'lblWebLobbySavedPlanarDecks', schemes: 'lblWebLobbySavedSchemeDecks' };
/** A shorter list is seen whole, so it needs no search. */
const SEARCH_FROM = 9;

export function ExtraPicker({ model, actions, index, seat, kind, close }: {
  model: Model; actions: Actions; index: number; seat: Seat; kind: ExtraKind; close: () => void;
}) {
  const section = SECTION[kind];
  useEffect(() => actions.askExtraChoices(index, section), [index, section]);
  const listed = model.extraChoices?.section === section && model.extraChoices.index === index ? model.extraChoices : null;
  const use = (choice: ExtraChoice) => {
    actions.setSeatExtra(index, section, choice.key);
    close();
  };
  return (
    <Backdrop layer="finder-back" dismiss={close}>
      <div class={`finder extra-picker ${kind}`}>
        <header class="finder-head">
          <h2>{t(TITLE[kind])}</h2>
          <span class="seat-note">{t('lblWebLobbyForSeat', seat.name ?? '')}</span>
          <CloseButton onClick={close} />
        </header>
        <div class="extra-body">
          {!listed ? <p class="none">{t('lblWebLobbyReadingChoices')}</p>
            : kind === 'vanguard' ? <AvatarGrid choices={listed.choices} use={use} />
              : <ExtraDecks kind={kind} choices={listed.choices} chosen={listed.chosen} seat={seat} use={use} edit={how => {
                close();
                changeUi(u => { u.planarSeat = index; });
                actions.openEditor({ ...how, seat: index });
              }} />}
        </div>
      </div>
    </Backdrop>
  );
}

function Search({ query, setQuery }: { query: string; setQuery: (q: string) => void }) {
  return <input class="find" type="search" placeholder={t('lblWebLobbySearchByName')} value={query} onInput={e => setQuery(e.currentTarget.value)} />;
}

const matches = (choices: ExtraChoice[], query: string): ExtraChoice[] =>
  choices.filter(c => !query || normalize(c.label).includes(normalize(query)));

/** The ways to get a deck are tiles. The saved decks are listed under them: those in this browser, then the host's. */
function ExtraDecks({ kind, choices, chosen, seat, use, edit }: {
  kind: DeckKind; choices: ExtraChoice[]; chosen?: string; seat: Seat; use: (c: ExtraChoice) => void; edit: (how: { key?: string; newFormat?: string; copy?: boolean }) => void;
}) {
  const [query, setQuery] = useState('');
  const here = choices.filter(c => c.key.startsWith('device:'));
  const stored = choices.filter(c => c.key.startsWith('deck:'));
  const random = choices.find(c => c.key === 'random');
  const tiles = choices.filter(c => c.key === 'own' || c.key === 'generate');
  // A remote player sees the host's decks but may not change them
  const hosts = kind === 'planes' && stored.some(c => !c.editable);
  const rows = (list: ExtraChoice[]) => matches(list, query).map(c => (
    <div key={c.key} class={`extra-row${c.problem ? ' illegal' : ''}${c.key === chosen ? ' on' : ''}`}>
      <button class="extra-choice" aria-pressed={c.key === chosen} onClick={() => use(c)}>
        <span class="thumbs">{c.cards.map(card => <ArtCrop key={card.image} src={smallImage(imageUrl(card.image))} />)}</span>
        <span class="words"><b>{c.label}</b><small class={c.problem ? 'no' : ''}>{c.problem ? t('lblWebLobbyExtraNotLegal', c.problem) : contents(c)}</small></span>
        <span class="count">{t('lblWebNCards', c.count ?? 0)}</span>
      </button>
      {kind === 'planes' && <button class="small" onClick={() => edit({ key: c.key, copy: !c.editable })}>{t(c.editable ? 'lblEdit' : 'lblCopy')}</button>}
    </div>
  ));
  return (
    <>
      <div class="extra-sources">
        {tiles.map(c => (
          <button key={c.key} class="extra-source" aria-pressed={c.key === chosen} onClick={() => use(c)}>
            <span class="pics">{c.cards.map(card => <ArtCrop key={card.image} src={imageUrl(card.image)} />)}</span>
            {c.key === chosen && <span class="tick"><Icon d={ICONS.check} /></span>}
            <span class="words"><b>{c.key === 'own' ? t(IN_DECK[kind], seat.deckName ?? '') : c.label}</b>
              <small class={c.problem ? 'no' : ''}>{c.problem ? t('lblWebLobbyExtraNotLegal', c.problem) : c.key === 'own' ? t('lblWebNCards', c.count ?? 0) : t(GENERATED[kind])}</small></span>
          </button>
        ))}
        {kind === 'planes' && (
          <button class="extra-source new" onClick={() => edit({ newFormat: 'Planechase' })}>
            <span class="plus"><Icon d={ICONS.plus} /></span>
            <span class="words"><b>{t('lblWebLobbyNewPlanarDeck')}</b><small>{t('lblWebLobbyNewPlanarDeckHint')}</small></span>
          </button>
        )}
      </div>
      {here.length + stored.length >= SEARCH_FROM && <Search query={query} setQuery={setQuery} />}
      {here.length > 0 && <><h4>{t('lblWebFinderSourceDevice')}</h4><div class="extra-list">{rows(here)}</div></>}
      {stored.length > 0 && (
        <>
          <h4>{t(hosts ? 'lblWebLobbyHostsPlanarDecks' : SAVED[kind])}
            {random && <button class="small" onClick={() => use(random)}>{t('lblWebLobbyPickAtRandom')}</button>}</h4>
          <div class="extra-list">{rows(stored)}</div>
        </>
      )}
    </>
  );
}

function contents(c: ExtraChoice): string {
  const names = c.cards.map(card => card.name).join(', ');
  const more = (c.count ?? 0) - c.cards.length;
  return more > 0 ? t('lblWebLobbyAndMore', names, more) : names;
}

// Players pick an avatar by its effect and its hand and life modifiers. So each is shown as its card with the modifiers under it
function AvatarGrid({ choices, use }: { choices: ExtraChoice[]; use: (c: ExtraChoice) => void }) {
  const [query, setQuery] = useState('');
  return (
    <>
    <Search query={query} setQuery={setQuery} />
    <div class="avatar-grid">
      {matches(choices, query).map(c => (
        <button key={c.key} class="avatar-choice" title={c.label} onClick={() => use(c)}>
          {c.image ? <img alt="" src={imageUrl(c.image)} loading="lazy" /> : <span class="plain">{c.label}</span>}
          <span class="avatar-name">{c.label}</span>
          {c.hand != null && c.life != null && <span class="mods">{t('lblWebLobbyAvatarMods', signed(c.hand), signed(c.life))}</span>}
        </button>
      ))}
    </div>
    </>
  );
}
