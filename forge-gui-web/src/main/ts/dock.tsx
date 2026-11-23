// The list of people on this server and their chat. It is a panel at the bottom edge before a match and a widget under the log during one

import { useEffect, useRef, useState } from 'preact/hooks';
import { createPortal } from 'preact/compat';
import { avatarUrl } from './avatarsleeves';
import { AvatarPicker } from './avatarpicker';
import { rememberAvatar, rememberedSleeve } from './identity';
import { logTints } from './match/log';
import { players, type ChatEntry, type Model } from './model';
import type { Actions } from './actions';
import type { Person } from './protocol';
import { t, type TextKey } from './text';
import { Dropdown } from './dialog';
import { useClosing } from './closing';
import { isPortrait } from './form';

/** The roster text for what each person is doing. */
const DOING: Record<string, TextKey> = {
  waiting: 'lblWebDockWaiting', joining: 'lblWebDockJoining', table: 'lblWebDockAtTheTable', playing: 'lblWebDockPlaying',
  watching: 'lblWebDockWatching',
};

function Crown() {
  return (
    <svg class="crown" role="img" aria-label={t('lblHost')} viewBox="0 0 24 24">
      <path d="M4.5 18h15M5 7.2l4.2 3.4L12 5.2l2.8 5.4L19 7.2l-1.1 8.3H6.1z" />
    </svg>
  );
}

/** When rename is given, your own row gets an edit button. Only the menu page passes it, so the name can only change there. */
export function Dock({ model, actions, rename }: { model: Model; actions: Actions; rename?: () => void }) {
  const [open, setOpen] = useState(false);
  const { closing, shut, gone } = useClosing(() => setOpen(false));
  const [listed, setListed] = useState(false);
  const [text, setText] = useState('');
  const [picking, setPicking] = useState(false);
  const log = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (log.current) log.current.scrollTop = log.current.scrollHeight;
  }, [model.chat.length, open]);
  // The last line the player could have read. It is stored as the entry, not an index, so a chat replayed after a reconnect is counted from its start
  const lastSeen = useRef<ChatEntry | undefined>(model.chat[model.chat.length - 1]);
  if (open || model.screen === 'playing') {
    lastSeen.current = model.chat[model.chat.length - 1];
  }
  // The names present when the dock was last pressed. Anyone who arrived since is marked until the next press
  const known = useRef<Set<string> | null>(null);
  const [, redraw] = useState(0);

  const people = model.presence;
  if (!people.length) {
    return null;
  }
  known.current ??= new Set(people.map(p => p.name));
  const arrived = people.filter(p => !known.current!.has(p.name) && p.name !== model.playerName);
  const arrivedBadge = arrived.length > 0 && (
    <span key={arrived.length} class="dock-new">{t('lblWebDockJoined', arrived.length === 1 ? arrived[0].name : arrived.length)}</span>
  );
  const sawArrivals = () => {
    if (arrived.length) {
      known.current = new Set(people.map(p => p.name));
      redraw(n => n + 1);
    }
  };
  const inMatch = model.screen === 'playing';
  // Only the host can reassign a disconnected seat, so the host sees the full roster without opening it
  const mayReseat = model.host && people.some(p => p.disconnected);
  // In a match the roster shows only a summary until opened, because the board already names every seated player
  const showList = !inMatch || listed || mayReseat;
  const waiting = people.filter(p => !p.host && !p.disconnected && (p.doing === 'waiting' || p.doing === 'joining'));
  const host = people.find(p => p.host);
  const watching = people.filter(p => p.doing === 'watching').length;
  const seated = people.filter(p => p.doing !== 'watching');
  const tints = chatTints(model);
  if (!inMatch && !open) {
    const unread = unreadSince(model.chat, lastSeen.current, model.playerName);
    const bar = (
      <button class={unread || arrived.length ? 'dock folded unread' : 'dock folded'} onClick={() => setOpen(true)}>
        <span class="faces" aria-hidden="true">
          {people.slice(0, 3).map(p => <img key={p.name} alt="" src={avatarUrl(p.avatar)} />)}
        </span>
        <span class="dock-count">{t('lblWebDockHere', people.length)}</span>
        {/* Keyed on the count, so each new line creates a new badge and its pulse animation plays again */}
        {unread > 0 && <span key={unread} class="dock-new">{t('lblWebDockNew', unread > 9 ? '9+' : unread)}</span>}
        {arrivedBadge}
      </button>
    );
    // A phone header has a slot for the folded dock (header.tsx). The pages are drawn before the dock, so the slot is in place
    const slot = isPortrait() ? document.querySelector('main:not([hidden]) .ph-chat') : null;
    return slot ? createPortal(bar, slot) : bar;
  }
  // Before a match, pressing your own avatar in the roster opens this picker. During a match the seat's avatar is fixed
  const picker = picking && (
    <AvatarPicker title={t('lblWebMenuChooseYourAvatar')} count={model.looks?.avatarCount ?? 0} urlOf={avatarUrl}
      current={people.find(p => p.name === model.playerName)?.avatar ?? 0} close={chosen => {
        setPicking(false);
        if (chosen !== null) {
          rememberAvatar(chosen);
          actions.setName(model.playerName, chosen, rememberedSleeve());
        }
      }} />
  );
  return (<>
    <section class={inMatch ? 'dock open in-match' : closing ? 'dock open closing' : 'dock open'} aria-label={t('lblWebDockWhoIsHere')}
      onPointerDown={sawArrivals}
      // The panel closes only after its slide-down animation ends, so the bar replaces it without a jump
      onAnimationEnd={gone}>
      {inMatch ? (
        <button class="dock-head" aria-expanded={listed} onClick={() => setListed(!listed)}>
          <span class="faces" aria-hidden="true">
            {seated.slice(0, 4).map(p => <img key={p.name} alt="" src={avatarUrl(p.avatar)} />)}
          </span>
          <span class="dock-sum">
            {host ? t('lblWebDockHosts', host.name) : t('lblWebDockAtThisTable')}{watching ? ` · ${t('lblWebDockNumWatching', watching)}` : ''}
          </span>
          {arrivedBadge}
          <Chevron up={!listed} />
        </button>
      ) : (
        <button class="dock-head" aria-expanded onClick={shut}>
          <span class="live" aria-hidden="true" />
          <b>{t('lblWebDockOnThisServer')}</b>
          <span class="dock-count">{people.length}</span>
          {arrivedBadge}
          <Chevron up={false} />
        </button>
      )}
      {showList && (
        <ul class="roster">
          {people.map(p => (
            <li key={p.name} class={`${p.doing === 'watching' || p.disconnected ? 'watching' : ''}${arrived.includes(p) ? ' arrived' : ''}`}>
              {!inMatch && p.name === model.playerName
                ? <button class="face-change" title={t('lblWebMenuSelectAvatar')} aria-label={t('lblWebMenuSelectAvatar')} onClick={() => setPicking(true)}>
                    <img class="face-small" alt="" src={avatarUrl(p.avatar)} /></button>
                : <img class="face-small" alt="" src={avatarUrl(p.avatar)} />}
              <span class="who">{p.name}</span>
              {p.host && <Crown />}
              {rename && p.name === model.playerName && (
                <button class="rename" title={t('lblWebDockRename')} aria-label={t('lblWebDockRename')} onClick={rename}>
                  <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4.5 19.5h5L20 9a2.6 2.6 0 0 0-3.7-3.7L5.8 15.8z" /></svg>
                </button>
              )}
              <span class="spacer" />
              {mayReseat && p.disconnected && (waiting.length > 0 || !model.drafting)
                ? <SeatMenu name={p.name} waiting={waiting} inMatch={inMatch} mayDrop={!model.drafting} actions={actions} />
                : <span class="doing">{p.disconnected ? t('lblWebDockDisconnected') : DOING[p.doing] ? t(DOING[p.doing]) : p.doing}</span>}
            </li>
          ))}
        </ul>
      )}
      <div class="dock-log" ref={log}>
        {model.chat.map((line, i) => (
          !line.from
            ? <p key={i} class="said note">{line.text}</p>
            : <p key={i} class="said"><img class="face-small" alt="" src={avatarUrl(faceOf(people, line.from))} /><span><b style={{ color: tints.get(line.from) }}>{line.from}</b> {line.text}</span></p>
        ))}
      </div>
      <form class="dock-say" onSubmit={e => {
        e.preventDefault();
        const said = text.trim();
        if (said) {
          actions.say(said);
          setText('');
        }
      }}>
        <input value={text} maxLength={240} placeholder={t('lblWebDockSay')} aria-label={t('lblWebDockSay')}
          onInput={e => setText(e.currentTarget.value)} />
      </form>
    </section>
    {picker}
  </>);
}

/** The host's options for a seat whose player has disconnected. The host can give it to someone waiting or drop the player. */
function SeatMenu({ name, waiting, inMatch, mayDrop, actions }: {
  name: string; waiting: readonly Person[]; inMatch: boolean; mayDrop: boolean; actions: Actions;
}) {
  return (
    <Dropdown cls="menu-anchor" block button={(open, toggle) => (
      <button class="seat-gone" aria-expanded={open} onClick={toggle}>{t('lblWebDockDisconnected')}<Chevron up={open} /></button>
    )}>
      {() => (
        <div class="deck-menu" role="menu">
          <span class="menu-cap">{name}</span>
          {waiting.map(w => (
            <button key={w.name} role="menuitem" onClick={() => actions.giveSeat(name, w.name)}>{t('lblWebDockGiveSeatTo', w.name)}</button>
          ))}
          {/* A drafter's seat belongs to the draft, so it cannot be opened */}
          {mayDrop && <button role="menuitem" onClick={() => actions.dropPlayer(name)}>{t(inMatch ? 'lblWebDockSeatToAi' : 'lblWebDockOpenSeat')}</button>}
        </div>
      )}
    </Dropdown>
  );
}

function Chevron({ up }: { up: boolean }) {
  return (
    <svg class="chev" aria-hidden="true" viewBox="0 0 24 24">
      <path d={up ? 'M6.5 14.5l5.5-5.5 5.5 5.5' : 'M6.5 9.5l5.5 5.5 5.5-5.5'} />
    </svg>
  );
}

/** Counts lines from other people after the last line the player could have read. Server notes and lines from before the player arrived are left out. */
function unreadSince(chat: readonly ChatEntry[], lastSeen: ChatEntry | undefined, me: string): number {
  const from = lastSeen ? chat.lastIndexOf(lastSeen) + 1 : 0;
  return chat.slice(from).filter(l => l.from && !l.earlier && l.from !== me).length;
}

/** The colour of each name. Players get the log's colours in seat order, then everyone watching gets the next ones. */
function chatTints(model: Model): Map<string, string> {
  const names = new Set([...players(model).map(p => p.Name ?? ''), ...model.presence.map(p => p.name)]);
  return new Map(logTints([...names].map(name => ({ name, local: name === model.playerName }))).map(n => [n.name, n.colour]));
}

/** The avatar shown beside a chat line. A person who has since left gets the first avatar instead of none. */
function faceOf(people: readonly Person[], name: string): number {
  return people.find(p => p.name === name)?.avatar ?? 0;
}
