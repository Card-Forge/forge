// The first screen. It asks for a name and avatar, then shows the menu to the host and a waiting card to everyone else

import type { ComponentChildren } from 'preact';
import { ForgeLinks } from './links';
import { useEffect, useRef, useState } from 'preact/hooks';
import { AvatarPicker } from './avatarpicker';
import { changeUi, eased, ui } from './ui';
import { avatarUrl } from './avatarsleeves';
import { artUrl } from './images';
import { BackButton, HeadControls, PageHeader, PhoneHead, SetupHead, WAY_NAMES, Wordmark } from './header';
import { isPortrait } from './form';
import type { Actions } from './actions';
import type { Model } from './model';
import { t } from './text';
import { rememberAvatar, rememberedAvatar, rememberedSleeve } from './identity';

/** Matches the server's limit (WebSession.MAX_NAME_LENGTH), so the field stops before the server would refuse the name. */
const MAX_NAME_LENGTH = 24;

export function Menu({ model, actions }: { model: Model; actions: Actions }) {
  const choosing = ui.menuChoice;
  // A step forward slides in from the right, and a step back from the left (dialogs.css)
  const setChoosing = (way: 'play' | 'friends' | null) => {
    eased(u => { u.menuChoice = way; }, () => ({ way: way ? 'on' : 'back' }));
  };
  // A new name from the server means the rename succeeded, so the rename prompt closes
  useEffect(() => changeUi(u => { u.renaming = false; }), [model.playerName]);
  if (ui.renaming) {
    return <NamePrompt model={model} actions={actions} initial={model.playerName} cancel={() => changeUi(u => { u.renaming = false; })} />;
  }
  // A browser that is not the host has no menu. It waits for somebody to open a table
  if (!model.host) {
    return <Waiting model={model} actions={actions} />;
  }
  return (
    <div class="menu-shell">
      {choosing && isPortrait()
        ? <PhoneHead back={{ label: t('lblStart'), go: () => setChoosing(null) }} title={t('lblWebMenuWhatMode')} />
        : (
          <PageHeader brand>
            <div class="head-right">
              <HeadControls />
              {choosing && <BackButton trail={[{ label: t('lblStart'), go: () => setChoosing(null) }]} />}
            </div>
          </PageHeader>
        )}
      <div class="menu-page">
        <ForgeLinks foot />
        {choosing && <Chooser who={choosing} model={model} actions={actions} back={() => setChoosing(null)} />}
        <div class="start-step" hidden={!!choosing}>
          <SetupHead trail={[{ label: t('lblStart') }]} title={t('lblWebMenuHowToPlay')} />
          <div class="modes">
            <Mode id="play" name={t('lblWebMenuVersusAi')} blurb={t('lblWebMenuVersusAiBlurb')} onClick={() => setChoosing('play')} />
            <Mode id="multiplayer" name={t('lblWebMenuWithFriends')} blurb={t('lblWebMenuWithFriendsBlurb')} onClick={() => setChoosing('friends')} />
            <Mode id="editor" name={t('lblWebMenuManageDecks')} blurb={t('lblWebMenuDecksBlurb')}
              onClick={() => {
                changeUi(u => { u.browse = { format: 'Constructed' }; });
                actions.browseFormat('Constructed');
              }} />
          </div>
        </div>
        <p class={model.error ? 'menu-note bad' : 'menu-note'}>{model.error ?? ''}</p>
      </div>
    </div>
  );
}

/** The choice of game kind, shown the same way for AI and friend opponents. Greyed kinds are not available yet. */
function Chooser({ who, model, actions, back }: { who: 'play' | 'friends'; model: Model; actions: Actions; back: () => void }) {
  const computer = who === 'play';
  return (
    <div class="chooser">
      <SetupHead trail={[{ label: t('lblStart'), go: back }, { label: WAY_NAMES[who] }]} title={t('lblWebMenuWhatMode')} />
      <div class="chooser-kinds">
        <Kind id="constructed" name={t('lblConstructed')} onClick={() => actions.openLobby(!computer)}
          blurb={t('lblWebMenuConstructedBlurb')} />
        <Kind id="draft" name={t('lblDraft')} onClick={() => (computer ? actions.limitedOpen('draft', false) : actions.openLimitedTable('draft'))}
          blurb={t('lblWebMenuDraftBlurb')}
          resume={computer && model.draftPools > 0 ? () => actions.limitedOpen('draft', true) : undefined} />
        <Kind id="sealed" name={t('lblSealed')} onClick={() => (computer ? actions.limitedOpen('sealed', false) : actions.openLimitedTable('sealed'))}
          blurb={t('lblWebMenuSealedBlurb')}
          resume={computer && model.sealedPools > 0 ? () => actions.limitedOpen('sealed', true) : undefined} />
      </div>
    </div>
  );
}

// A fanned deck for Constructed. Two packs passing between players for Draft. An opened pack with cards rising out for Sealed
const KIND_ICONS: Record<string, ComponentChildren> = {
  constructed: <><rect x="16" y="11" width="21" height="29" rx="3" /><path d="M12 15v21M8 19v13" /></>,
  draft: <><rect x="7" y="16" width="14" height="22" rx="2" /><rect x="27" y="16" width="14" height="22" rx="2" /><path d="M17 9h13l-3-3M31 45H18l3 3" /></>,
  sealed: <><path d="M13 22v19h22V22" /><path d="M13 22l2.75-2.5 2.75 2.5 2.75-2.5 2.75 2.5 2.75-2.5 2.75 2.5 2.75-2.5 2.75 2.5" />
    <rect x="16" y="6" width="10" height="15" rx="1.5" transform="rotate(-12 21 13.5)" /><rect x="23" y="5" width="10" height="15" rx="1.5" transform="rotate(10 28 12.5)" /></>,
};

// The card art behind each tile, pinned to one printing so the picture does not change
const MENU_ART: Record<string, string> = {
  play: 'c:Mirror Match|C15|1',
  multiplayer: 'c:Secret Rendezvous|SOC|1',
  editor: 'c:Archivist of Oghma|CLB|1',
  constructed: 'c:Jace, the Mind Sculptor|WWK|1',
  draft: 'c:Cogwork Librarian|CNS|1',
  sealed: 'c:Booster Tutor|UND|1',
};

/** A tile's art once it has loaded, as a CSS url. The tile keeps its drawn look until then, and for good when the server has no art. */
function useArt(id: string): string | null {
  const key = MENU_ART[id];
  const [loaded, setLoaded] = useState<string | null>(null);
  // auto marks the download as the page's own idea, so the server skips it when online image fetching is off
  const src = key ? `${artUrl(key)}&auto=1` : '';
  useEffect(() => {
    if (!src) return;
    const img = new Image();
    img.onload = () => setLoaded(`url("${src}")`);
    img.src = src;
    return () => { img.onload = null; };
  }, [src]);
  return loaded;
}

/** A kind of game, drawn as a plaque like the start page's. It holds two buttons because a button cannot contain another. It is greyed when onClick is missing. icon replaces the kind's own icon. */
export function Kind({ id, icon, name, blurb, resume, resumeLabel, onClick }: {
  id: string; icon?: ComponentChildren; name: string; blurb: string; resume?: () => void; resumeLabel?: string; onClick?: () => void;
}) {
  const art = useArt(id);
  const drawn = <svg viewBox="0 0 48 48">{icon ?? KIND_ICONS[id]}</svg>;
  return (
    <div class={`kind-card plaque${art ? ' has-art' : ''}${resume ? ' has-resume' : ''}`} style={art ? { '--art': art } : undefined}>
      <span class="plaque-rim" aria-hidden="true" />
      <span class="plaque-mark" aria-hidden="true">{drawn}</span>
      <button class="kind-main" data-kind={id} disabled={!onClick} onClick={onClick}>
        <span class="mode-art" aria-hidden="true">{drawn}</span>
        <span class="mode-text">
          <span class="mode-name">{name}</span>
          <span class="mode-blurb">{blurb}</span>
        </span>
      </button>
      {resume && (
        <button class="kind-resume" onClick={resume}>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12a9 9 0 1 0 3-6.7" /><path d="M3 4v5h5" /><path d="M10 9l5 3-5 3z" /></svg>
          {resumeLabel ?? t('lblWebMenuResumeLastEvent')}
        </button>
      )}
    </div>
  );
}

// A robot for the computer, two people for friends, a deck for the editor
const MODE_ICONS: Record<string, ComponentChildren> = {
  play: <><rect x="11" y="15" width="26" height="22" rx="4" /><path d="M24 15V9M19 25h2M27 25h2M19 31h10" /><circle cx="24" cy="7" r="2.4" /><path d="M6 22v8M42 22v8" /></>,
  multiplayer: <><circle cx="18" cy="17" r="6" /><path d="M8 38c0-5.5 4.5-10 10-10s10 4.5 10 10" /><circle cx="34" cy="20" r="4.6" /><path d="M28.5 34c1-4 4-6.6 8-6.6 3.2 0 5.5 1.6 5.5 1.6" /></>,
  editor: <><rect x="16" y="11" width="21" height="29" rx="3" /><path d="M12 15v21M8 19v13" /></>,
};

/** An entry on the start page, drawn as a plaque. Its card art covers the drawn pattern once the art has loaded. */
function Mode({ id, name, blurb, onClick }: {
  id: string; name: string; blurb: string; onClick?: () => void;
}) {
  const art = useArt(id);
  return (
    <button class={art ? 'mode plaque has-art' : 'mode plaque'} data-mode={id} disabled={!onClick} onClick={onClick} style={art ? { '--art': art } : undefined}>
      <span class="plaque-rim" aria-hidden="true" />
      <span class="plaque-mark" aria-hidden="true"><svg viewBox="0 0 48 48">{MODE_ICONS[id]}</svg></span>
      <span class="mode-art" aria-hidden="true"><svg viewBox="0 0 48 48">{MODE_ICONS[id]}</svg></span>
      <span class="mode-text">
        <span class="mode-name">{name}</span>
        <span class="mode-blurb">{blurb}</span>
      </span>
    </button>
  );
}

/** Asks for the player's name and avatar. It also offers the host seat while the seat is free. */
export function NamePrompt({ model, actions, initial = '', cancel }: {
  model: Model; actions: Actions; initial?: string; cancel?: () => void;
}) {
  const [value, setValue] = useState(initial);
  const [avatar, setAvatar] = useState(rememberedAvatar);
  const [picking, setPicking] = useState(false);
  const [takeHost, setTakeHost] = useState(true);
  const [password, setPassword] = useState('');
  const input = useRef<HTMLInputElement>(null);
  // A remembered name is already sent, and before the first hello a name sent would join as a guest, so the prompt waits
  const asking = !model.nameSent && !!model.looks;
  useEffect(() => {
    input.current?.focus();
  }, [asking]);
  if (!asking) {
    return <div class="menu-page start"><Wordmark /></div>;
  }
  // A rename does not offer the host seat, because the player may already hold it
  const offerHost = !cancel && model.canClaimHost;
  return (
    <div class="menu-page start">
      <ForgeLinks foot />
      <Wordmark />
      <form class="start-card" onSubmit={e => {
        e.preventDefault();
        const name = value.trim();
        if (!name) {
          return;
        }
        rememberAvatar(avatar);
        // Claim the host seat before sending the name. Otherwise the session does not know it is the host and joins as a guest
        if (offerHost && takeHost) {
          actions.claimHost(password);
        }
        actions.setName(name, avatar, rememberedSleeve());
      }}>
        <div class="start-field">
          <label for="player-name">{t('lblWebMenuCallYou')}</label>
          <div class="name-row">
            <button type="button" class="face" title={t('lblWebMenuSelectAvatar')} aria-label={t('lblWebMenuSelectAvatar')} onClick={() => setPicking(true)}>
              <img alt="" src={avatarUrl(avatar)} />
              <span class="face-edit" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M4.5 19.5h5L20 9a2.6 2.6 0 0 0-3.7-3.7L5.8 15.8z" /></svg>
              </span>
            </button>
            <input id="player-name" ref={input} value={value} maxLength={MAX_NAME_LENGTH} autocomplete="nickname"
              onInput={e => setValue(e.currentTarget.value)} />
          </div>
        </div>
        {offerHost && (
          <label class="host-box">
            <input type="checkbox" checked={takeHost} onChange={e => setTakeHost(e.currentTarget.checked)} />
            <span class="host-text">
              <b>{t('lblWebMenuHostGame')}</b>
              <span>{t('lblWebMenuHostGameBlurb')}</span>
            </span>
          </label>
        )}
        {offerHost && takeHost && model.hostPassword && <HostPassword value={password} set={setPassword} />}
        <div class="start-buttons">
          {cancel && <button type="button" onClick={cancel}>{t('lblCancel')}</button>}
          <button type="submit" class="primary" disabled={!value.trim()}>{t(cancel ? 'lblWebMenuChange' : 'lblContinue')}</button>
        </div>
      </form>
      <p class={model.error ? 'menu-note bad' : 'menu-note'}>{model.error ?? ''}</p>
      {picking && (
        <AvatarPicker title={t('lblWebMenuChooseYourAvatar')} count={model.looks?.avatarCount ?? 0} urlOf={avatarUrl} current={avatar}
          close={chosen => {
            setPicking(false);
            if (chosen !== null) {
              setAvatar(chosen);
            }
          }} />
      )}
    </div>
  );
}

/** The host password, asked for when the server has one and this browser is on another machine. */
function HostPassword({ value, set }: { value: string; set: (value: string) => void }) {
  return <input type="password" class="host-password" placeholder={t('lblWebConsoleHostPassword')} aria-label={t('lblWebConsoleHostPassword')}
    autocomplete="current-password" value={value} onInput={e => set(e.currentTarget.value)} />;
}

/** The page for a browser with no seat. A seat is being taken, the last attempt failed, or nobody has opened a table yet. */
function Waiting({ model, actions }: { model: Model; actions: Actions }) {
  const [password, setPassword] = useState('');
  const failed = model.screen !== 'opening' && !!model.error;
  const state = model.screen === 'opening' ? 'joining' : failed ? 'failed' : 'idle';
  // A match in progress takes no new players, so the guest waits for the host to give up a seat or open the next table
  const midMatch = state === 'idle' && model.presence.some(p => p.host && (p.doing === 'playing' || p.doing === 'watching'));
  return (
    <div class="menu-page">
      <Wordmark />
      <section class={`wait-card ${state}`}>
        <ol class="trail">
          <Step name={t('lblWebMenuStepConnected')} done />
          <Step name={t('lblWebMenuStepNamed')} done={!!model.playerName} />
          <Step name={t('lblWebMenuStepSeated')} state={state} />
        </ol>
        <div class="wait-head">
          <span class="wait-mark" aria-hidden="true" />
          <b>{t(model.screen === 'opening' ? 'lblWebMenuTakingSeat' : failed ? 'lblWebMenuCouldNotTakeSeat' : midMatch ? 'lblWebMenuMatchInProgress' : 'lblWebMenuNoTableYet')}</b>
        </div>
        <p class="wait-body">
          {model.screen === 'opening' ? t('lblWebMenuMakingRoom')
            : failed ? model.error
              : t(midMatch ? 'lblWebMenuSeatedAfterMatch' : model.canClaimHost ? 'lblWebMenuNobodyHosting' : 'lblWebMenuSeatedWhenOpens')}
        </p>
        <div class="wait-buttons">
          {failed && <button onClick={() => actions.join()}>{t('lblTryAgain')}</button>}
          {model.canClaimHost && model.hostPassword && <HostPassword value={password} set={setPassword} />}
          {model.canClaimHost && <button class="primary" onClick={() => actions.claimHost(password)}>{t('lblWebMenuHostTable')}</button>}
        </div>
      </section>
    </div>
  );
}

function Step({ name, done = false, state }: { name: string; done?: boolean; state?: string }) {
  const mark = state === undefined ? (done ? 'done' : 'todo') : state === 'joining' ? 'now' : state === 'failed' ? 'failed' : 'todo';
  return <li class={`step ${mark}`}><span class="dot" aria-hidden="true" />{name}</li>;
}
