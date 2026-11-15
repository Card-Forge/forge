// The server lists the planar decks, scheme decks and avatars a seat may use. So a computer seat is only offered avatars the computer can play

import { useEffect, useState } from 'preact/hooks';
import { imageUrl } from '../images';
import { normalize } from '../search';
import { t, type TextKey, signed } from '../text';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { ExtraChoice, Seat } from '../protocol';
import { Backdrop, CloseButton } from '../dialog';

type ExtraKind = 'planes' | 'schemes' | 'vanguard';

const SECTION: Record<ExtraKind, string> = { planes: 'Planes', schemes: 'Schemes', vanguard: 'Avatar' };
const TITLE: Record<ExtraKind, TextKey> = { planes: 'lblWebLobbyChoosePlanarDeck', schemes: 'lblWebLobbyChooseSchemeDeck', vanguard: 'lblWebLobbyChooseAvatar' };

export function ExtraPicker({ model, actions, index, seat, kind, close }: {
  model: Model; actions: Actions; index: number; seat: Seat; kind: ExtraKind; close: () => void;
}) {
  const section = SECTION[kind];
  const [query, setQuery] = useState('');
  useEffect(() => actions.askExtraChoices(index, section), [index, section]);
  const listed = model.extraChoices?.section === section && model.extraChoices.index === index ? model.extraChoices.choices : null;
  const use = (choice: ExtraChoice) => {
    actions.setSeatExtra(index, section, choice.key);
    close();
  };
  const shown = (listed ?? []).filter(c => !query || normalize(c.label).includes(normalize(query)));
  return (
    <Backdrop layer="finder-back" dismiss={close}>
      <div class={`finder extra-picker ${kind}`}>
        <header class="finder-head">
          <h2>{t(TITLE[kind])}</h2>
          <span class="seat-note">{t('lblWebLobbyForSeat', seat.name ?? '')}</span>
          <CloseButton onClick={close} />
        </header>
        <div class="extra-body">
          <input class="find" type="search" placeholder={t('lblWebLobbySearchByName')} value={query} onInput={e => setQuery(e.currentTarget.value)} />
          {!listed ? <p class="none">{t('lblWebLobbyReadingChoices')}</p>
            : kind === 'vanguard' ? <AvatarGrid choices={shown} use={use} />
              : <div class="extra-list">{shown.map(c => (
                <button key={c.key} class={`extra-choice${c.problem ? ' illegal' : ''}`} title={c.problem ?? ''} onClick={() => use(c)}>
                  <span class="name">{c.label}</span>
                  <span class="count">{c.count != null ? t('lblWebNCards', c.count) : ''}</span>
                  {c.problem && <span class="legal no">{c.problem}</span>}
                </button>
              ))}</div>}
        </div>
      </div>
    </Backdrop>
  );
}

// Players pick an avatar by its effect and its hand and life modifiers. So each is shown as its card with the modifiers under it
function AvatarGrid({ choices, use }: { choices: ExtraChoice[]; use: (c: ExtraChoice) => void }) {
  return (
    <div class="avatar-grid">
      {choices.map(c => (
        <button key={c.key} class="avatar-choice" title={c.label} onClick={() => use(c)}>
          {c.image ? <img alt="" src={imageUrl(c.image)} loading="lazy" /> : <span class="plain">{c.label}</span>}
          <span class="avatar-name">{c.label}</span>
          {c.hand != null && c.life != null && <span class="mods">{t('lblWebLobbyAvatarMods', signed(c.hand), signed(c.life))}</span>}
        </button>
      ))}
    </div>
  );
}
