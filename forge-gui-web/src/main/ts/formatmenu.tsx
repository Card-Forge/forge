// The Format menu's list: every game and legality format under its group. The lobby, the deck finder and the deck editor offer the same entries

import type { ComponentChildren } from 'preact';
import { useState } from 'preact/hooks';
import { t, type TextKey } from './text';
import type { Format } from './protocol';

/** A game's deck size in a few words: "60+ cards", "100 cards", or "no deck" when the deck is dealt. */
function deckMark(format: Format): string {
  return format.deckCards == null ? t('lblNoDeck') : t('lblWebNCards', `${format.deckCards}${format.deckAtLeast ? '+' : ''}`);
}

const CONSTRUCTED_ICON = <svg viewBox="0 0 16 16"><rect x="3" y="2" width="9" height="12" rx="1.5" /><path d="M5 4.5v10.5h8V5" opacity=".6" /></svg>;

const GROUP_ICONS: Record<string, ComponentChildren> = {
  Constructed: CONSTRUCTED_ICON, Sanctioned: CONSTRUCTED_ICON, Casual: CONSTRUCTED_ICON, Custom: CONSTRUCTED_ICON,
  Commander: <svg viewBox="0 0 16 16"><path d="M2 12 L3 5 L6 8 L8 3 L10 8 L13 5 L14 12 Z" /></svg>,
  Other: <svg viewBox="0 0 16 16"><rect x="2.5" y="2.5" width="11" height="11" rx="2.5" /><circle cx="5.7" cy="5.7" r="1.3" fill="currentColor" stroke="none" />
    <circle cx="8" cy="8" r="1.3" fill="currentColor" stroke="none" /><circle cx="10.3" cy="10.3" r="1.3" fill="currentColor" stroke="none" /></svg>,
  Limited: <svg viewBox="0 0 16 16"><path d="M4 2h8l1 3v9H3V5z" /><path d="M3 5h10" /></svg>,
};

const NONE_ICON = <svg viewBox="0 0 16 16"><path d="M4 4l8 8M12 4l-8 8" /></svg>;

/** The name of each family of games. The server names a format's family with these keys. */
export const GROUP_NAMES: Record<string, TextKey> = {
  Constructed: 'lblConstructed', Sanctioned: 'lblSanctioned', Casual: 'lblCasual', Custom: 'lblCustom',
  Commander: 'lblCommander', Other: 'lblOther', Limited: 'lblLimited',
};

/** A legality format, which the server plays as Constructed with that format as its card pool. */
export function isCardPool(f: Format): boolean {
  return f.group === 'Sanctioned' || f.group === 'Casual' || f.group === 'Custom';
}

/** Formats under their group, in the order the server lists them. */
export function groupsOf(formats: readonly Format[]): [string, Format[]][] {
  const groups = new Map<string, Format[]>();
  for (const f of formats) groups.set(f.group, [...(groups.get(f.group) ?? []), f]);
  return [...groups];
}

/** The groups of formats as a list of picks. With explain, a card beside the list explains the chosen format or the one last under the pointer. none adds a last pick for no format. */
export function FormatMenu({ groups, chosen, pick, blocked, explain, none }: {
  groups: [string, Format[]][]; chosen: Format | undefined; pick: (f: Format) => void; blocked?: (f: Format) => boolean; explain?: boolean;
  none?: { label: string; on: boolean; pick: () => void };
}) {
  const [pointed, setPointed] = useState<Format | null>(null);
  const shown = pointed ?? chosen ?? null;
  const choices = (
    <div class="game-choices">
      {groups.map(([group, formats]) => (
        <div key={group} class="game-group">
          <span class="game-group-name">{GROUP_NAMES[group] ? t(GROUP_NAMES[group]) : group}</span>
          {formats.map(f => (
            <button key={f.id} class="game-choice" aria-pressed={f === chosen} disabled={blocked?.(f)}
              onPointerEnter={e => { if (explain && e.pointerType === 'mouse') setPointed(f); }} onFocus={() => explain && setPointed(f)}
              onClick={() => { setPointed(null); pick(f); }}>
              <i class="game-icon" aria-hidden="true">{GROUP_ICONS[f.group]}</i>
              <span class="game-name">{f.name}</span><span class="game-size">{deckMark(f)}</span>
            </button>
          ))}
        </div>
      ))}
      {none && (
        <div class="game-group">
          <span class="game-group-name">{t('lblNone')}</span>
          <button class="game-choice" aria-pressed={none.on} onClick={() => { setPointed(null); none.pick(); }}>
            <i class="game-icon" aria-hidden="true">{NONE_ICON}</i>
            <span class="game-name">{none.label}</span>
          </button>
        </div>
      )}
    </div>
  );
  if (!explain) return choices;
  return (
    <div class="game-menu-list">
      {choices}
      {shown && (
        <div class="game-card">
          <h5>{shown.name}</h5>
          <p class="desc">{shown.desc}</p>
          <div class="facts">{shown.facts.map(x => <span key={x} class="fact">{x}</span>)}</div>
          {shown.play && <p class="format-play"><b>{t('lblWebMatchBarInMatch')}</b> {shown.play}</p>}
        </div>
      )}
    </div>
  );
}
