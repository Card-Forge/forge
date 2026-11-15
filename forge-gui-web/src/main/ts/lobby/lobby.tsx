// Match setup: the seats, their decks and the table's rules, for a game against the computer or at an online table. The browser joins
// through a client even when it hosts, so your own seat arrives as REMOTE and the page finds it by its "mine" flag

import { useEffect, useState } from 'preact/hooks';
import { changeUi, ui, type Picker } from '../ui';
import { sleeveUrl, avatarUrl } from '../avatarsleeves';
import { AvatarPicker } from '../avatarpicker';
import { rememberAvatar, rememberSleeve } from '../identity';
import { BracketMark, DeckFinder } from '../deck/deckfinder';
import { imageUrl, smallImage } from '../images';
import { ExtraPicker } from './extrapicker';
import { CENTRE, SleevePicker, artUrl, objectPosition } from '../sleeves';
import { AI_ICON, Pips } from '../symbols';
import { EventHead } from './event';
import { Invite, MatchBar, TableHeader, seatsLeaving } from './matchbar';
import { backStep, PhoneHead, SetupHead, WAY_NAMES } from '../header';
import { isPortrait } from '../form';
import { t, tCap, type TextKey } from '../text';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { CommanderChoice, DeckSummary, Format, LobbyTable, Seat, SeatExtra } from '../protocol';
import { Backdrop, Dropdown } from '../dialog';
import { ICONS, Icon } from '../icons';
import { byId } from '../dom';
import { dimIntoMatch } from '../match/matchentry';

export function Lobby({ model, actions }: { model: Model; actions: Actions }) {
  const picker = ui.picker;
  const lobby = model.lobby;
  // A lower player count under the pointer. The seats it would remove are dimmed before anything changes
  const [preview, setPreview] = useState<number | null>(null);
  if (!lobby) {
    return null;
  }
  // The host can remove a seat while its picker is open, so the seat may be gone
  const seat = picker ? lobby.seats[picker.seat] : undefined;
  const close = () => changeUi(u => { u.picker = null; });
  const lim = lobby.limited;
  const format = model.formats.find(f => f.id === lobby.format);
  const leaving = preview === null ? new Set<number>() : seatsLeaving(lobby, preview);
  // A guest only reaches a table through a link, so a guest's table is always a friends table
  const way = lobby.shareable || !lobby.host ? 'friends' : 'play';
  // The host goes back by leaving the table. A guest has no earlier steps to go back to
  const back = (to: 'play' | 'friends' | null) => lobby.host ? () => {
    changeUi(u => { u.menuChoice = to; });
    actions.leaveLobby();
  } : undefined;
  const kind = t(lim ? (lim.kind === 'draft' ? 'lblDraft' : 'lblSealed') : 'lblConstructed');
  const trail = [{ label: t('lblStart'), go: back(null) }, { label: WAY_NAMES[way], go: back(way) }, { label: kind }];
  const title = t(lobby.host ? 'lblWebLobbySetUpGame' : lim?.activeEventId ? 'lblWebLobbyBuildThenPlay'
    : lim?.started ? (lim.kind === 'draft' ? 'lblWebEventDraftIsOn' : 'lblWebEventOpeningPacks') : 'lblWebLobbyHostSettingUp');
  const phoneBack = isPortrait() ? backStep(trail) : undefined;
  return (
    <>
      {phoneBack
        ? <PhoneHead back={phoneBack} title={title} action={lobby.shareable ? <Invite model={model} /> : undefined} />
        : <TableHeader model={model} lobby={lobby} trail={trail} />}
      <div class="lobby-main">
        <SetupHead trail={trail} title={title} />
        {/* A new kind of event needs a fresh setup. The key remounts the event head so its dialog opens again */}
        {lim
          ? <EventHead key={lim.kind} model={model} lobby={lobby} actions={actions} preview={setPreview} start={() => dimIntoMatch(byId('lobby'), () => actions.startMatch(ui.spectate))} />
          : <MatchBar lobby={lobby} formats={model.formats} actions={actions} preview={setPreview} />}
        <div class={`seats${lobby.fixedPairs ? ' by-team' : ''}`} id="seats">
          {/* The key includes who sits there, so an added or newly taken seat mounts again and plays its entry animation (lobby.css) */}
          {lobby.seats.map((s, i) => <Plate key={`${i} ${s.type} ${s.name ?? ''}`} seat={s} index={i} lobby={lobby} format={format} actions={actions} leaving={leaving.has(i)} joinable={model.networked}
            avatarCount={model.looks?.avatarCount ?? 0} sleeveCount={model.looks?.sleeveCount ?? 0}
            choose={kind => changeUi(u => { u.picker = { kind, seat: i }; })} random={() => randomDeck(model, actions, i)} />)}
          {/* This button adds a seat where it will appear. The player count above can also add seats */}
          {lobby.host && !lim && !lobby.fixedPairs && lobby.seats.length < lobby.maxSeats && (
            <button class="add-seat" onClick={() => actions.setPlayerCount(lobby.seats.length + 1)}>
              <Icon d={ICONS.plus} />{t('lblWebLobbyAddSeat')}
            </button>
          )}
        </div>
        {/* An event's Play button is on its rail, which also says what the match waits on. Only the illegal decks warning is shown here */}
        {lim ? lim.activeEventId && <IllegalDecks lobby={lobby} /> : <Verdict lobby={lobby} start={() => dimIntoMatch(byId('lobby'), () => actions.startMatch(ui.spectate))} />}
      </div>
      {seat && picker?.kind === 'deck' && <DeckFinder model={model} actions={actions} seat={{ index: picker.seat, seat }} close={close} />}
      {seat && picker && (picker.kind === 'planes' || picker.kind === 'schemes' || picker.kind === 'vanguard') && (
        <ExtraPicker model={model} actions={actions} index={picker.seat} seat={seat} kind={picker.kind} close={close} />
      )}
      {seat && picker?.kind === 'sleeve' && <SleevePicker model={model} actions={actions} index={picker.seat} seat={seat} close={close} />}
      {seat && picker?.kind === 'commander' && <CommanderPicker actions={actions} index={picker.seat} seat={seat} close={close} />}
      {seat && picker?.kind === 'avatar' && (
        <AvatarPicker title={t('lblWebLobbyChooseAvatarFor', seat.name ?? '')} count={model.looks?.avatarCount ?? 0} urlOf={avatarUrl}
          current={seat.avatar} taken={lobby.seats.filter((_, i) => i !== picker.seat).map(s => s.avatar)} close={chosen => {
            if (chosen !== null) {
              if (seat.mine) rememberAvatar(chosen);
              actions.setSeat(picker.seat, { avatar: chosen });
            }
            close();
          }} />
      )}
    </>
  );
}

// The seat types a netplay lobby can hold. Offline shows only the first two
const KIND: Record<string, TextKey> = { LOCAL: 'lblYou', AI: 'lblAI', OPEN: 'lblWebLobbyKindOpen', REMOTE: 'lblWebLobbyKindRemote' };

const kindName = (type: string): string => (KIND[type] ? tCap(KIND[type]) : type);

const PLAYER_ICON = <Icon d={ICONS.user} />;

/** Who plays a seat the host may change. A chip opens the two choices, each with a line that explains it. */
function SeatKind({ type, toAi, toOpen }: { type: string; toAi: () => void; toOpen: () => void }) {
  const ai = type === 'AI';
  // Choosing the seat's current type only closes the menu, because asking again would give the AI a new name
  const choose = (close: () => void, change: boolean, act: () => void) => () => {
    close();
    if (change) act();
  };
  return (
    <Dropdown cls="popup-anchor" button={(open, toggle) => (
      <button class="kind chip" title={t('lblWebLobbyWhoPlaysSeat')} aria-expanded={open} onClick={toggle}>
        {ai ? AI_ICON : PLAYER_ICON}{kindName(type)}<svg class="chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
      </button>
    )}>
      {close => (
        <div class="popup kind-menu" role="menu">
          <button role="menuitemradio" aria-checked={ai} onClick={choose(close, !ai, toAi)}>{AI_ICON}<span>{t('lblAI')}</span><small>{t('lblWebLobbyAiPlaysSeat')}</small></button>
          <button role="menuitemradio" aria-checked={!ai} onClick={choose(close, ai, toOpen)}>{PLAYER_ICON}<span>{t('lblWebLobbyOpenForPlayer')}</span><small>{t('lblWebLobbyOpenForPlayerHint')}</small></button>
        </div>
      )}
    </Dropdown>
  );
}

/** What each of Forge's own AI profiles does. A profile someone adds to res/ai has no hint. */
const STYLE_HINTS: Record<string, TextKey> = {
  Default: 'lblWebLobbyStyleDefaultHint', Cautious: 'lblWebLobbyStyleCautiousHint',
  Reckless: 'lblWebLobbyStyleRecklessHint', Experimental: 'lblWebLobbyStyleExperimentalHint',
};

/** How a computer seat plays. The host gets a chip to choose another profile. Others see the profile name, except for Default. */
function PlayStyle({ profile, profiles, change }: { profile: string; profiles: string[]; change: ((profile: string) => void) | null }) {
  if (!change) {
    return profile === 'Default' ? null : <span class="kind">{profile}</span>;
  }
  return (
    <Dropdown cls="popup-anchor" button={(open, toggle) => (
      <button class={`kind chip${profile === 'Default' ? ' quiet' : ''}`} title={t('lblWebLobbyPlaystyle')} aria-expanded={open} onClick={toggle}>
        {profile}<svg class="chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
      </button>
    )}>
      {close => (
        <div class="popup kind-menu style-menu" role="menu" aria-label={t('lblWebLobbyPlaystyle')}>
          {profiles.map(p => (
            <button key={p} role="menuitemradio" aria-checked={p === profile} onClick={() => { close(); if (p !== profile) change(p); }}>
              {p === profile ? <Icon d={ICONS.check} /> : <span />}<span>{p}</span>
              {STYLE_HINTS[p] && <small>{t(STYLE_HINTS[p])}</small>}
            </button>
          ))}
        </div>
      )}
    </Dropdown>
  );
}

/** A seat's team, on the top edge of the plate over the sleeve. Whoever may move the seat can open it to pick a team. */
function TeamTab({ team, teams, change }: { team: number; teams: number; change: ((team: number) => void) | null }) {
  const name = (n: number) => t('lblWebLobbyTeamN', n + 1);
  if (!change) return <span class="team-tab-slot"><span class="team-tab">{name(team)}</span></span>;
  return (
    <Dropdown cls="team-tab-slot" button={(open, toggle) => (
      <button class="team-tab" title={t('lblWebLobbyChangeTeam')} aria-expanded={open} onClick={toggle}>
        {name(team)}<svg class="chevron" viewBox="0 0 24 24" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
      </button>
    )}>
      {close => (
        <div class="popup menu-list" role="menu">
          {Array.from({ length: teams }, (_, n) => (
            <button key={n} role="menuitemradio" aria-checked={n === team} onClick={() => { close(); if (n !== team) change(n); }}>{name(n)}</button>
          ))}
        </div>
      )}
    </Dropdown>
  );
}

/** A random number below count that is not current. Returns current when count is 1 or less. */
function another(current: number, count: number): number {
  if (count <= 1) return current;
  const pick = Math.floor(Math.random() * (count - (current >= 0 && current < count ? 1 : 0)));
  return current >= 0 && pick >= current ? pick + 1 : pick;
}

/**
 * Decks a seat may be given at random: any the lobby would accept, up to the table's bracket. A generator builds
 * a new deck each time, so it is used only when there is no built deck.
 * A computer seat never gets a deck with cards the AI will not play, as on desktop.
 */
function randomPool(decks: readonly DeckSummary[], maxBracket = 5, forComputer = false): DeckSummary[] {
  const fit = decks.filter(d => !d.problem && (d.bracket == null || d.bracket <= maxBracket) && !(forComputer && d.aiSkips));
  const built = fit.filter(d => !d.generated);
  return built.length ? built : fit;
}

/** Gives a seat a random legal deck, so a table can fill without opening the chooser for each seat. */
function randomDeck(model: Model, actions: Actions, index: number): void {
  const pool = randomPool(model.decks ?? [], model.lobby?.maxBracket, model.lobby?.seats[index]?.type === 'AI');
  if (pool.length) actions.setSeat(index, { deck: pool[Math.floor(Math.random() * pool.length)].key });
}

function Plate({ seat, index, lobby, format, actions, leaving, joinable, avatarCount, sleeveCount, choose, random }: {
  seat: Seat; index: number; lobby: LobbyTable; format: Format | undefined; actions: Actions; leaving: boolean; joinable: boolean; avatarCount: number; sleeveCount: number;
  choose: (kind: Picker['kind']) => void; random: () => void;
}) {
  // Your own seat is the one the server gave you, whatever type the host has for it
  const mine = seat.mine;
  const waiting = seat.type === 'OPEN';
  // Another player's deck comes from their own catalog, so it has a name here but no key
  const hasDeck = seat.deck != null || seat.deckName != null;
  // Momir Basic and MoJhoSto give every seat its deck at the start, so there is none to choose
  const dealt = !!format && format.deckCards == null;
  // A Limited seat has no deck to choose until its pool is out. Until then it shows whether it is ready
  const lim = lobby.limited;
  const beforePools = !!lim && !lim.activeEventId;
  // When another player is seated, each player presses Ready once their deck is chosen. The computer is always ready
  const pressReady = !lim && seat.type !== 'AI' && !waiting && lobby.seats.some(s => s.type !== 'AI' && s.type !== 'OPEN' && s.mine !== mine);
  // Where others can join, the host can switch a seat between AI and open. Everyone else only sees its type
  const swappable = lobby.host && joinable && !mine && (seat.type === 'AI' || seat.type === 'OPEN');
  // The host may give their own seat to the computer and watch. A match can only be watched from the host's seat
  const watchable = lobby.host && mine && (!lobby.limited || !!lobby.limited.activeEventId);
  // Any seat whose deck this browser chooses can get a random one. An event's decks are its players' own pools
  const randomable = seat.mayEdit && !dealt && !lim && !waiting;
  // Right-click picks another sleeve at random, never the current one. A numbered sleeve replaces any card art
  const randomSleeve = () => {
    actions.setSleeveArt(index, '', CENTRE);
    const sleeve = another(seat.sleeveArt ? -1 : seat.sleeve, sleeveCount);
    if (seat.mine) rememberSleeve(sleeve);
    actions.setSeat(index, { sleeve });
  };
  // No two seats share a portrait, so the pick is from the portraits no seat uses
  const randomAvatar = () => {
    const free = Array.from({ length: avatarCount }, (_, i) => i).filter(i => lobby.seats.every(s => s.avatar !== i));
    if (!free.length) return;
    const avatar = free[Math.floor(Math.random() * free.length)];
    if (seat.mine) rememberAvatar(avatar);
    actions.setSeat(index, { avatar });
  };
  // A deck's own card art takes priority over the numbered sleeve, as it does in a match
  const sleeveSrc = seat.sleeveArt ? artUrl(seat.sleeveArt) : sleeveUrl(seat.sleeve);
  // A commander deck shows its commander, which says more about the deck than its sleeve
  const commander = seat.commander;
  return (
    <div class={`plate${mine ? ' mine' : ''}${waiting ? ' waiting' : ''}${!mine && !waiting && seat.type !== 'AI' ? ' guest' : ''}${seat.sittingOut ? ' sitting-out' : ''}${leaving ? ' leaving' : ''}${seat.team != null ? ' teamed' : ''}`}>
      {seat.team != null && <TeamTab team={seat.team} teams={lobby.teams} change={(lobby.host || mine) && !lobby.fixedPairs ? team => actions.setTeam(index, team) : null} />}
      <div class={hasDeck && seat.mayEdit && !dealt && seat.commanderChoices.length > 1 ? 'sleeve-slot three' : 'sleeve-slot'}
        hidden={beforePools} data-image={commander ?? undefined}>
        <button class={`sleeve${hasDeck || dealt ? '' : ' empty'}${seat.sleeveArt && !commander ? ' card-art' : ''}`} title={dealt ? '' : t('lblWebLobbyChooseDeck')}
          data-label={seat.mayEdit ? t('lblWebLobbyChooseDeck') : (waiting ? '' : t('lblNoDeck'))}
          disabled={!seat.mayEdit || dealt} onClick={() => choose('deck')}
          onContextMenu={e => { if (randomable) { e.preventDefault(); random(); } }}>
          <img alt="" hidden={!hasDeck && !dealt} src={commander ? smallImage(imageUrl(commander)) : hasDeck || dealt ? sleeveSrc : undefined}
            style={commander ? undefined : { objectPosition: objectPosition(seat.sleeveOffset) }} />
          {/* The sleeve has two controls, so hovering over it labels what each part does */}
          {hasDeck && seat.mayEdit && !dealt && <span class="sleeve-deck">{t('lblWebLobbyChangeDeck')}</span>}
        </button>
        {/* When more than one card could lead the deck, a commander button sits between deck and sleeve. All three are the same size */}
        {hasDeck && seat.mayEdit && !dealt && seat.commanderChoices.length > 1 && (
          <button class="sleeve-cmdr" onClick={() => choose('commander')}>{t('lblWebLobbyChangeCommander')}</button>
        )}
        {/* A sleeve belongs to a deck, so it cannot be chosen until there is a deck */}
        <button class="sleeve-style" title={t('lblWebSleevesChooseSleeve')} hidden={!hasDeck || !seat.mayEdit}
          onClick={() => choose('sleeve')}
          // A new sleeve would be hidden under the commander's image
          onContextMenu={e => { if (!commander) { e.preventDefault(); randomSleeve(); } }}>{t('lblWebLobbyChangeSleeve')}</button>
      </div>
      <button class="drop" aria-label={t('lblWebLobbyRemoveSeat')} hidden={mine || !lobby.host || lobby.fixedPairs || lobby.seats.length <= 2}
        onClick={() => actions.removeSeat(index)}><Icon d={ICONS.x} /></button>
      <div class="plate-body">
        <div class="who">
          {/* A seat nobody has taken has no portrait to show */}
          <button class="avatar" title={t('lblWebLobbyChooseAvatar')} hidden={waiting} disabled={!seat.mayEdit}
            onClick={() => choose('avatar')} onContextMenu={e => { if (seat.mayEdit) { e.preventDefault(); randomAvatar(); } }}>
            <img alt="" src={avatarUrl(seat.avatar)} /></button>
          <SeatName seat={seat} rename={name => actions.setSeat(index, { name })} />
          {watchable
            ? <button class="kind" title={t('lblWebLobbyWhoPlaysSeat')} aria-pressed={ui.spectate}
                onClick={() => changeUi(u => { u.spectate = !u.spectate; })}>{t(ui.spectate ? 'lblAI' : KIND.LOCAL)}</button>
            : swappable
              ? <SeatKind type={seat.type} toAi={() => actions.aiSeat(index)} toOpen={() => actions.openSeat(index)} />
              : <span class="kind">{mine ? t(KIND.LOCAL) : kindName(seat.type)}</span>}
          {seat.aiProfile && <PlayStyle profile={seat.aiProfile} profiles={lobby.aiProfiles}
            change={lobby.host && seat.mayEdit ? aiProfile => actions.setSeat(index, { aiProfile }) : null} />}
          {seat.role && <span class={`role ${seat.role}`}>{t(seat.role === 'archenemy' ? 'lblArchenemy' : 'lblWebLobbyHero')}</span>}
          {lobby.host && seat.role === 'hero' && (
            <button class="make-archenemy" onClick={() => actions.setArchenemy(index)}>{t('lblWebLobbyMakeArchenemy')}</button>
          )}
        </div>
        {watchable && ui.spectate && <p class="seat-note">{t('lblWebLobbyComputerPlaysSeat')}</p>}
        {dealt && !waiting && <p class="deck-row fixed">{format?.facts[0]}</p>}
        {beforePools && !waiting && <Ready seat={seat} actions={actions} disabled={lim.started} />}
        {/* Sitting out only matters when the table has more players than a match needs */}
        {lim?.activeEventId && lobby.host && !waiting && lobby.seats.filter(s => s.type !== 'OPEN').length > 2 && (
          <label class="sits-out"><input type="checkbox" checked={seat.sittingOut}
            onChange={e => actions.sitOut(index, e.currentTarget.checked)} /> {t('lblWebLobbySitsOut')}</label>
        )}
        {/* With no deck yet, this row offers a random one. The sleeve beside it already opens the deck chooser */}
        {randomable && !hasDeck && (
          <button class="deck-row random-row" onClick={random}><Dice />{t('lblRandomDeck')}</button>
        )}
        <div class="deck-line" hidden={dealt || beforePools || (!hasDeck && !waiting)}>
          <button class={`deck-row${hasDeck ? '' : ' unset'}`} disabled={!seat.mayEdit} onClick={() => choose('deck')}>
            <span class="pips"><Pips colors={seat.colors} /></span>
            <span class="deck-name" title={seat.deckName ?? ''}>{seat.deckName ?? (waiting ? t('lblWebLobbyWaitingForPlayer') : '')}</span>
            <span class="deck-size">{hasDeck ? String(seat.deckSize) : ''}</span>
            {hasDeck && seat.bracket != null && <BracketMark level={seat.bracket} />}
          </button>
          {randomable && hasDeck && (
            <button class="random-deck" title={t('lblWebLobbyDealAnotherDeck')} aria-label={t('lblRandomDeck')} onClick={random}><Dice /></button>
          )}
        </div>
        <p class="seat-problem" hidden={!seat.problem || !hasDeck}>{seat.problem ?? ''}</p>
        {hasDeck && seat.bracket != null && lobby.maxBracket < 5 && seat.bracket > lobby.maxBracket && (
          <p class="seat-problem bracket">{t('lblWebLobbyAboveTableBracket', lobby.maxBracket)}</p>
        )}
        {pressReady && <Ready seat={seat} actions={actions} disabled={!hasDeck && !dealt} />}
        {seat.planes && <ExtraRow name={t('lblPlanes')} extra={seat.planes} mayEdit={seat.mayEdit} open={() => choose('planes')} />}
        {seat.schemes && <ExtraRow name={t('lblSchemes')} extra={seat.schemes} mayEdit={seat.mayEdit} open={() => choose('schemes')} />}
        {seat.vanguard && <ExtraRow name={t('lblAvatar')} counted={false} extra={seat.vanguard} mayEdit={seat.mayEdit} open={() => choose('vanguard')} />}
      </div>
    </div>
  );
}

/** Your own seat's Ready button, which you press again to undo. Other seats only show whether they are ready. */
function Ready({ seat, actions, disabled }: { seat: Seat; actions: Actions; disabled: boolean }) {
  return seat.mine
    ? <button class={`deck-row ready-toggle${seat.ready ? '' : ' unset'}`} disabled={disabled}
        aria-pressed={seat.ready} title={seat.ready ? t('lblWebLobbyNotReadyAfterAll') : ''}
        onClick={() => actions.ready(!seat.ready)}>{t(seat.ready ? 'lblWebLobbyReadyTick' : 'lblWebLobbyPressWhenReady')}</button>
    : <p class="deck-row fixed">{t(seat.ready ? 'lblReady' : 'lblWebLobbyNotReadyYet')}</p>;
}

function Dice() {
  return (
    <svg class="dice" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="4" /><circle cx="8.5" cy="8.5" r="1.2" />
      <circle cx="15.5" cy="15.5" r="1.2" /><circle cx="12" cy="12" r="1.2" /><circle cx="15.5" cy="8.5" r="1.2" /><circle cx="8.5" cy="15.5" r="1.2" /></svg>
  );
}

/** A planar deck, scheme deck or avatar on a seat. It is a plain row that opens its picker and shows its problem, if any. */
function ExtraRow({ name, counted = true, extra, mayEdit, open }: {
  name: string; counted?: boolean; extra: SeatExtra; mayEdit: boolean; open: () => void;
}) {
  return (
    <button class={`seat-extra${extra.problem ? ' warn' : ''}`} disabled={!mayEdit} onClick={open}>
      <span class="extra-kind">{name}</span>
      <span class="extra-label">{extra.label}</span>
      {extra.detail ? <span class="extra-detail">{extra.detail}</span>
        : extra.count > 0 && counted && <span class="extra-count">{extra.count}</span>}
      {extra.problem && <span class="extra-problem">{extra.problem}</span>}
    </button>
  );
}

// Your own name, and the computer's at a table you host, is edited in place. It is saved when the field loses focus. A right click gives the computer a new one
function SeatName({ seat, rename }: { seat: Seat; rename: (name: string) => void }) {
  // Typing changes the DOM directly, outside Preact. So each edit ends by redrawing the field with the server's name
  const [edits, setEdits] = useState(0);
  const name = seat.name || kindName(seat.type);
  if (!seat.mine && !(seat.mayEdit && seat.type === 'AI')) {
    return <span class="who-name" hidden={!seat.name}>{name}</span>;
  }
  return (
    <span key={edits} class="who-name" contentEditable="plaintext-only" spellcheck={false}
      onContextMenu={e => { if (!seat.mine) { e.preventDefault(); rename(''); } }}
      onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); e.currentTarget.blur(); } }}
      onBlur={e => {
        const typed = (e.currentTarget.textContent ?? '').trim();
        setEdits(n => n + 1);
        if (typed && typed !== name) rename(typed);
      }}>{name}</span>
  );
}

function Verdict({ lobby, start }: { lobby: LobbyTable; start: () => void }) {
  const problems = lobby.problems ?? [];
  const over = lobby.overBracket;
  // The host must allow a deck above the table's bracket. The first press asks and the second starts
  const [asking, setAsking] = useState(false);
  useEffect(() => setAsking(false), [over.join('\n')]);
  const play = () => (over.length && !asking ? setAsking(true) : start());
  // Only the host can start. So a joined client sees what it is waiting for instead of a disabled button
  if (!lobby.host) {
    return (
      <div class="play-row">
        <p class="match-line">{problems.length ? problems[0] : t('lblWebEventWaitingHostStart')}</p>
      </div>
    );
  }
  return (
    <div class="play-row">
      <button id="play" class="primary play" disabled={!lobby.canStart} onClick={play} title={over.length ? '' : t('ttWebEventEnterStarts')}>
        {t(asking ? 'lblWebLobbyPlayAnyway' : 'lblPlay')}
      </button>
      <div class="not-yet" hidden={lobby.canStart}>
        <b>{t('lblWebLobbyNotPlayableYet')}</b>
        <ul>{problems.map(p => <li key={p}>{p}</li>)}</ul>
      </div>
      <IllegalDecks lobby={lobby} />
      {lobby.canStart && over.length > 0 && (
        <div class="not-yet warn">
          <b>{t('lblWebLobbyAboveTableBracket', lobby.maxBracket)}</b>
          <ul>{over.map(p => <li key={p}>{p}</li>)}</ul>
          <p class="hint">{t(asking ? 'lblWebLobbyPlayAnywayHint' : 'lblWebLobbyAskIgnoreBracket')}</p>
        </div>
      )}
    </div>
  );
}

/** Illegal decks do not stop the match. As on desktop, Play asks whether to ignore them. */
function IllegalDecks({ lobby }: { lobby: LobbyTable }) {
  if (!lobby.canStart || !lobby.illegalDecks.length) return null;
  return (
    <div class="not-yet warn">
      <b>{t('lblWebLobbyNotLegal')}</b>
      <ul>{lobby.illegalDecks.map(p => <li key={p}>{p}</li>)}</ul>
      <p class="hint">{t(lobby.legalityEnforced ? 'lblWebLobbyAskIgnoreIllegal' : 'lblWebLobbyLegalityNotEnforced')}</p>
    </div>
  );
}

const CHOICE_KIND: Record<CommanderChoice['kind'], TextKey> = {
  DEFAULT: 'lblWebLobbyCommanderBuiltWith', SUGGESTED: 'lblWebLobbyCommanderSuggested', OTHER: 'lblWebLobbyCommanderOther',
};

/** Picks which card leads a seat's deck at this table, from the cards that can, as desktop's lobby does. */
function CommanderPicker({ actions, index, seat, close }: { actions: Actions; index: number; seat: Seat; close: () => void }) {
  return (
    <Backdrop dismiss={close}>
      <div class="dialog commander-picker" role="dialog" aria-label={t('lblWebLobbySwapCommander')}>
        <h3>{t('lblWebLobbySwapCommander')}</h3>
        <div class="commander-options">
          {seat.commanderChoices.map((c, i) => (
            <button key={c.name} class={c.chosen ? 'commander-option on' : 'commander-option'} aria-pressed={c.chosen}
              onClick={() => { actions.setSeat(index, { commander: i }); close(); }}>
              <img alt="" src={smallImage(imageUrl(c.image))} />
              <b>{c.name}</b>
              <small>{t(CHOICE_KIND[c.kind])}</small>
            </button>
          ))}
        </div>
        <div class="actions"><button onClick={close}>{t('lblCancel')}</button></div>
      </div>
    </Backdrop>
  );
}
