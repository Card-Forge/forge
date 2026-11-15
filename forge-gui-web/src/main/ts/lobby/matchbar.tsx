// The bar above the seats. It shows what is played and by how many players, as labelled fields that each open their own control

import type { ComponentChildren } from 'preact';
import { useState } from 'preact/hooks';
import { BackButton, HeadControls, PageHeader, type SetupStep } from '../header';
import { isPortrait } from '../form';
import { ICONS, Icon } from '../icons';
import { t } from '../text';
import type { Actions } from '../actions';
import type { Model } from '../model';
import { Dropdown } from '../dialog';
import { FormatMenu, groupsOf, isCardPool } from '../formatmenu';
import type { Address, Format, LobbyTable } from '../protocol';

/** name is the button's accessible name, for a label with no words. cls replaces the button's classes. */
function Popup({ label, name, cls, disabled, children, wide, onOpen }: {
  label: ComponentChildren; name?: string; cls?: string; disabled?: boolean; children: (close: () => void) => ComponentChildren; wide?: boolean; onOpen?: () => void;
}) {
  return (
    <Dropdown cls="popup-anchor" button={(open, toggle) => (
      <button class={cls ?? 'field-value menu-button'} title={name} aria-label={name} aria-expanded={open} disabled={disabled} onClick={() => {
        if (!open) onOpen?.();
        toggle();
      }}>{label}</button>
    )}>{close => <div class={wide ? 'popup wide' : 'popup'} role="dialog">{children(close)}</div>}</Dropdown>
  );
}

function Field({ name, grow, children }: { name: string; grow?: boolean; children: ComponentChildren }) {
  return (
    <div class={grow ? 'field grow' : 'field'}>
      <span class="field-name">{name}</span>
      {children}
    </div>
  );
}

/** Draft and Sealed in the same shape the server uses for a format, so the menu's card shows them the same way. */
function limitedFormats(): (Format & { kind: 'draft' | 'sealed' })[] {
  return [
    { id: 'Draft', kind: 'draft', name: t('lblDraft'), group: 'Limited', desc: t('lblWebMatchBarDescDraft'),
      facts: [t('lblWebMatchBarFactPicks'), t('lblWebFactLife', 20)], play: t('lblWebMatchBarPlayDraft'), deckCards: 40, deckAtLeast: false },
    { id: 'Sealed', kind: 'sealed', name: t('lblSealed'), group: 'Limited', desc: t('lblWebMatchBarDescSealed'),
      facts: [t('lblWebMatchBarFactPool'), t('lblWebFactLife', 20)], play: t('lblWebMatchBarPlaySealed'), deckCards: 40, deckAtLeast: false },
  ];
}

/** The table's Format menu. Only a table offers Draft and Sealed, and only the host may change it. */
export function GameMenu({ lobby, formats, actions }: { lobby: LobbyTable; formats: Format[]; actions: Actions }) {
  const lim = lobby.limited;
  const LIMITED = limitedFormats();
  const pool = lobby.format === 'Constructed' ? lobby.cardPool : null;
  const chosen: Format | undefined = lim ? LIMITED.find(l => l.kind === lim.kind)
    : formats.find(f => (pool ? isCardPool(f) && f.id === pool : !isCardPool(f) && f.id === lobby.format));
  // A new kind of event must wait until the started one is over. Other formats must wait until the draft ends
  const drafting = lim?.phase === 'DRAFTING' && !lim.activeEventId;
  const limitedOffered = lobby.shareable || !!lim;
  const pick = (f: Format) => {
    const kind = LIMITED.find(l => l.id === f.id)?.kind;
    if (kind) {
      actions.setLimited(kind);
    } else {
      if (lim) actions.setLimited(null);
      if (isCardPool(f)) {
        if (lobby.format !== 'Constructed') actions.setFormat('Constructed');
        actions.setCardPool(f.id);
      } else {
        actions.setFormat(f.id);
        if (f.id === 'Constructed' && pool) actions.setCardPool(null);
      }
    }
  };
  const blocked = (f: Format) => (LIMITED.some(l => l.id === f.id) ? !!lim?.started : drafting);
  const groups: [string, Format[]][] = [...groupsOf(formats), ...(limitedOffered ? [['Limited', LIMITED] as [string, Format[]]] : [])];
  return (
    <Popup label={chosen?.name ?? lobby.format} disabled={!lobby.host} wide>
      {close => <FormatMenu groups={groups} chosen={chosen} blocked={blocked} explain pick={f => { pick(f); close(); }} />}
    </Popup>
  );
}

/** Why a casual variant cannot be switched on, or null. Momir Basic and MoJhoSto bring their own avatars. */
function variantBlocked(lobby: LobbyTable, formats: Format[], id: string): string | null {
  const group = formats.find(f => f.id === lobby.format)?.group;
  return id === 'Vanguard' && group === 'Other' ? t('lblWebMatchBarVanguardOff') : null;
}

function VariantsMenu({ lobby, formats, actions }: { lobby: LobbyTable; formats: Format[]; actions: Actions }) {
  const on = lobby.casualVariants.filter(v => lobby.variantsOn.includes(v.id));
  return (
    <Popup label={on.length ? on.map(v => v.name).join(', ') : <span class="muted">{t('lblNone')}</span>} disabled={!lobby.host}>
      {() => (
        <div class="menu-list kind-menu variant-menu" role="menu" aria-label={t('lblVariants')}>
          {lobby.casualVariants.map(v => {
            const blocked = variantBlocked(lobby, formats, v.id);
            const checked = lobby.variantsOn.includes(v.id);
            return (
              <button key={v.id} role="menuitemcheckbox" aria-checked={checked} disabled={!!blocked} onClick={() => actions.setVariant(v.id, !checked)}>
                {checked ? <Icon d={ICONS.check} /> : <span />}<span>{v.name}</span>
                <small>{blocked ?? v.desc}</small>
              </button>
            );
          })}
        </div>
      )}
    </Popup>
  );
}

/** The seats a lower count removes, in the server's order: open seats, then computers, from the end. A person's seat is never removed. */
export function seatsLeaving(lobby: LobbyTable, count: number): Set<number> {
  const out = new Set<number>();
  let extra = lobby.seats.length - count;
  for (const type of ['OPEN', 'AI']) {
    for (let i = lobby.seats.length - 1; i >= 0 && extra > 0; i--) {
      if (!lobby.seats[i].mine && lobby.seats[i].type === type && !out.has(i)) {
        out.add(i);
        extra--;
      }
    }
  }
  return out;
}

function fewestSeats(lobby: LobbyTable): number {
  return Math.max(2, lobby.seats.filter(s => s.mine || s.type === 'LOCAL' || s.type === 'REMOTE').length);
}

export function PlayerCount({ lobby, actions, preview }: { lobby: LobbyTable; actions: Actions; preview: (count: number | null) => void }) {
  const fewest = fewestSeats(lobby);
  const counts = Array.from({ length: lobby.maxSeats - 1 }, (_, i) => i + 2);
  const drafting = lobby.limited?.phase === 'DRAFTING' && !lobby.limited.activeEventId;
  return (
    <span class="count" role="group" aria-label={t('lblPlayers')} onPointerLeave={() => preview(null)}>
      {counts.map(n => (
        <button key={n} aria-pressed={n === lobby.seats.length} disabled={!lobby.host || drafting || n < fewest || (lobby.fixedPairs && n !== lobby.seats.length)}
          title={n < fewest ? t('lblWebMatchBarFewerSeats') : ''}
          onPointerEnter={() => preview(n < lobby.seats.length ? n : null)}
          onClick={() => { preview(null); actions.setPlayerCount(n); }}>{n}</button>
      ))}
    </span>
  );
}

/** The highest Commander bracket the table plays at, stored in the host's own preferences. 5 means any. Only the host may change it. */
function MaxBracket({ lobby, actions }: { lobby: LobbyTable; actions: Actions }) {
  return (
    <span class="count" role="group" aria-label={t('lblBracket')}>
      {[1, 2, 3, 4, 5].map(n => (
        <button key={n} aria-pressed={n === lobby.maxBracket} disabled={!lobby.host} title={n < 5 ? t('lblWebMatchBarBracketTip', n) : ''}
          onClick={() => actions.setMaxBracket(n)}>{n < 5 ? n : t('lblAny')}</button>
      ))}
    </span>
  );
}

/** Free-for-all, or the seats split into teams. A guest can only read it. Only the host has choices to pick from. */
function Teams({ lobby, actions }: { lobby: LobbyTable; actions: Actions }) {
  const name = (n: number) => (n ? t('lblWebMatchBarNTeams', n) : t('lblWebMatchBarFreeForAll'));
  return (
    <Popup label={name(lobby.teams)} disabled={!lobby.teamChoices.length}>
      {close => (
        <div class="menu-list" role="menu">
          {[0, ...lobby.teamChoices].map(n => (
            <button key={n} role="menuitemradio" aria-checked={n === lobby.teams} onClick={() => { close(); actions.setTeams(n); }}>{name(n)}</button>
          ))}
        </div>
      )}
    </Popup>
  );
}

const MATCH_LENGTHS = [[1, 'lblWebMatchBarBestOfOne'], [3, 'lblWebMatchBarBestOfThree'], [5, 'lblWebMatchBarBestOfFive']] as const;

/** Best-of-one, three or five, stored in the host's own preferences. Only the host may change it. */
function MatchLength({ lobby, actions }: { lobby: LobbyTable; actions: Actions }) {
  return (
    <span class="count" role="group" aria-label={t('lblMatch')}>
      {MATCH_LENGTHS.map(([n, name]) => (
        <button key={n} aria-pressed={n === lobby.gamesPerMatch} disabled={!lobby.host} title={t(name)}
          onClick={() => actions.setMatchLength(n)}>{t('lblWebMatchBarBestOfShort', n)}</button>
      ))}
    </span>
  );
}

/** The field names follow the tournament rules. A format says which cards are allowed. A match is the games played between decks. */
export function MatchBar({ lobby, formats, actions, preview }: {
  lobby: LobbyTable; formats: Format[]; actions: Actions; preview: (count: number | null) => void;
}) {
  return (
    <div class="match-bar">
      <div class="fields">
        <Field name={t('lblFormat')}><GameMenu lobby={lobby} formats={formats} actions={actions} /></Field>
        <Field name={t('lblPlayers')}><PlayerCount lobby={lobby} actions={actions} preview={preview} /></Field>
        {lobby.format === 'Commander' && <Field name={t('lblBracket')}><MaxBracket lobby={lobby} actions={actions} /></Field>}
        <Field name={t('lblMatch')}><MatchLength lobby={lobby} actions={actions} /></Field>
        {(lobby.teamChoices.length > 0 || lobby.teams > 0) && <Field name={t('lblWebMatchBarTeams')}><Teams lobby={lobby} actions={actions} /></Field>}
        <Field name={t('lblVariants')} grow><VariantsMenu lobby={lobby} formats={formats} actions={actions} /></Field>
      </div>
    </div>
  );
}

/** The button that shows the addresses others join the table at. On a phone it is an icon in the header. */
export function Invite({ model }: { model: Model }) {
  const phone = isPortrait();
  return (
    <Popup label={phone ? <Icon d={ICONS.link} /> : <><Icon d={ICONS.link} />{t('lblWebMatchBarInvite')}</>} name={phone ? t('lblWebMatchBarInvite') : undefined}
      cls={phone ? 'field-value menu-button invite-button' : 'outline invite-button'}>
      {() => (
        <div class="invite">
          <b>{t('lblWebMatchBarOthersJoinAt')}</b>
          <Addresses list={model.addresses ?? []} />
        </div>
      )}
    </Popup>
  );
}

/** How others join, the sound and the options. The dock shows who is here. */
export function TableHeader({ model, lobby, trail }: { model: Model; lobby: LobbyTable; trail: SetupStep[] }) {
  const invite = lobby.shareable ? <Invite model={model} /> : undefined;
  return (
    <PageHeader brand action={invite}>
      <div class="head-right">
        {invite}
        <HeadControls />
        <BackButton trail={trail} />
      </div>
    </PageHeader>
  );
}

function Addresses({ list }: { list: Address[] }) {
  const [copied, setCopied] = useState<string | null>(null);
  if (!list.length) {
    return <p class="muted">{t('lblWebMatchBarWorkingOutAddress')}</p>;
  }
  return <>{list.map(a => (
    <button key={a.url} class="share-row" onClick={() => navigator.clipboard?.writeText(a.url).then(() => setCopied(a.url), () => {})}>
      <span class="share-label">{a.label}</span><code class="share-url">{a.url}</code>
      <span class="share-copy">{t(copied === a.url ? 'lblWebMatchBarCopied' : 'lblCopy')}</span>
    </button>
  ))}</>;
}
