// Dev mode. Each cheat asks for what it needs (which card, how much life) through the game's own prompts

import { useEffect, useRef, useState } from 'preact/hooks';
import { OptionsDialog } from '../options';
import type { Actions } from '../actions';
import type { DevAction } from '../protocol';
import { t, tCap, type TextKey } from '../text';
import { players, zone, type Model } from '../model';
import { isPortrait } from '../form';
import { ICONS, MenuIcon } from '../icons';
import { useHoverIntent } from '../hooks';

type Entry = [DevAction, TextKey];

/** The cheats that come in families. Each family is one row of the menu with its cheats in a submenu. */
const FAMILIES: Record<string, { name: TextKey; items: (Entry | 'sep')[] }> = {
  place: { name: 'lblWebDevPlaceCard', items: [['addCardToHand', 'lblHandZone'], ['addCardToBattlefield', 'lblBattlefieldZone'], ['addCardToLibrary', 'lblLibrary'],
    ['addCardToGraveyard', 'lblGraveyard'], ['addCardToExile', 'lblExile'], 'sep', ['addTokenToBattlefield', 'lblWebDevAddToken']] },
  exile: { name: 'lblWebDevExileCards', items: [['exileCardsFromHand', 'lblWebDevExileFromHand'], ['exileCardsFromBattlefield', 'lblWebDevExileFromBattlefield'], 'sep',
    ['removeCardsFromGame', 'lblWebDevRemoveFromGame']] },
  counters: { name: 'lblCounters', items: [['addCountersToPermanent', 'lblWebDevAddCounters'], ['removeCountersFromPermanent', 'lblWebDevRemoveCounters']] },
  tap: { name: 'lblWebDevTapUntap', items: [['tapPermanents', 'lblTapPermanent'], ['untapPermanents', 'lblUntapPermanent']] },
  planechase: { name: 'lblPlanechase', items: [['riggedPlanarRoll', 'lblWebDevRiggedPlanarRoll'], ['planeswalkTo', 'lblWebDevPlaneswalkTo']] },
};
const FAMILY_ICONS: Record<string, string> = { place: ICONS.plus, exile: ICONS.bin, counters: ICONS.yields, tap: ICONS.next, state: ICONS.folder, planechase: ICONS.dev };

/**
 * The cheats, shown in place of the game menu's own items. A family of cheats opens a submenu beside the menu on a desktop,
 * on hover, a click or the right arrow. On a phone it opens as a page of its own with a row back.
 */
export function DevItems({ model, actions, back, close, setUp }: {
  model: Model; actions: Actions; back?: () => void; close: () => void; setUp: () => void;
}) {
  useEffect(() => {
    actions.dev('state');
  }, []);
  const [open, setOpen] = useState<{ family: string; top: number } | null>(null);
  const phone = isPortrait();
  // A pointer crossing other rows on its way into an open family keeps it open. Resting on another row closes it, or opens that row's family
  const hover = useHoverIntent();
  const leave = () => { if (open) hover.later(() => setOpen(null)); };
  const run = (action: DevAction) => {
    actions.dev(action);
    close();
  };
  const toggle = (action: DevAction, label: string, on: boolean | undefined) => (
    <button type="button" role="menuitemcheckbox" aria-checked={!!on} class="card-menu-item" onMouseEnter={leave} onClick={() => actions.dev(action)}>
      {on ? '✓ ' : ''}{label}
    </button>
  );
  const item = (action: DevAction, key: TextKey, icon?: string) => (
    <button key={action} type="button" role="menuitem" class="card-menu-item" onClick={() => run(action)}>{icon && <MenuIcon d={icon} />}{tCap(key)}</button>
  );
  // A Planechase game is one where a player has a planar deck
  const planechase = players(model).some(p => zone(model, p, 'PlanarDeck').length > 0);
  const families = ['place', 'exile', 'counters', 'tap', 'state', ...(planechase ? ['planechase'] : [])];
  const familyName = (family: string): string => tCap(family === 'state' ? 'lblWebDevGameState' : FAMILIES[family].name);
  const subItems = (family: string) => (family === 'state'
    ? <>
        <button type="button" role="menuitem" class="card-menu-item" onClick={setUp}>{t('lblWebDevSetUpState')}</button>
        {item('dumpGameState', 'lblWebDevDumpState')}
      </>
    : FAMILIES[family].items.map((e, i) => (e === 'sep' ? <div key={i} class="card-menu-sep" role="separator" /> : item(e[0], e[1]))));
  const show = (family: string, row: HTMLElement) => setOpen({ family, top: row.offsetTop });
  const familyRow = (family: string) => (
    <button key={family} type="button" role="menuitem" aria-haspopup="menu" aria-expanded={open?.family === family}
      class={open?.family === family ? 'card-menu-item on' : 'card-menu-item'}
      onMouseEnter={phone ? undefined : e => {
        const row = e.currentTarget;
        if (!open || open.family === family) {
          hover.stay();
          show(family, row);
        } else {
          hover.later(() => show(family, row));
        }
      }}
      onClick={e => show(family, e.currentTarget)}
      onKeyDown={e => { if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') { e.preventDefault(); show(family, e.currentTarget); } }}>
      <MenuIcon d={FAMILY_ICONS[family]} />{familyName(family)}<MenuIcon d={ICONS.next} end />
    </button>
  );
  // On a phone an open family replaces the menu, with its name as the way back
  if (phone && open) {
    return (
      <>
        <button type="button" role="menuitem" class="card-menu-item dev-back" onClick={() => setOpen(null)}>‹ {familyName(open.family)}</button>
        {subItems(open.family)}
      </>
    );
  }
  const plain = (action: DevAction, key: TextKey, icon: string) => (
    <button key={action} type="button" role="menuitem" class="card-menu-item" onMouseEnter={leave} onClick={() => run(action)}><MenuIcon d={icon} />{t(key)}</button>
  );
  return (
    <>
      {back && <>
        <button type="button" role="menuitem" class="card-menu-item dev-back" onMouseEnter={leave} onClick={back}>‹ {t('lblDevMode')}</button>
        <div class="card-menu-sep" role="separator" />
      </>}
      {toggle('unlimitedLands', t('lblWebDevUnlimitedLands'), model.devState?.unlimitedLands)}
      {toggle('viewAll', t('lblWebDevViewAll'), model.devState?.viewAll)}
      <div class="card-menu-sep" role="separator" />
      {familyRow('place')}
      {plain('repeatLastAddition', 'lblWebDevRepeatLastAddition', ICONS.undo)}
      {plain('castASpell', 'lblWebDevCastSpell', ICONS.autoPass)}
      {plain('tutorForCard', 'lblWebDevTutor', ICONS.eye)}
      {families.filter(f => f !== 'place' && f !== 'state' && f !== 'planechase').map(familyRow)}
      <div class="card-menu-sep" role="separator" />
      {plain('generateMana', 'lblWebDevAddMana', ICONS.plus)}
      {plain('setPlayerLife', 'lblWebDevSetLife', ICONS.user)}
      {plain('winGame', 'lblWebDevWinGame', ICONS.crown)}
      {familyRow('state')}
      {planechase && familyRow('planechase')}
      {open && !phone && (
        <div class="card-menu dev-sub" role="menu" aria-label={familyName(open.family)} style={{ top: `${open.top - 6}px` }} onMouseEnter={hover.stay}
          onKeyDown={e => { if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') { e.preventDefault(); setOpen(null); } }}>
          {subItems(open.family)}
        </div>
      )}
    </>
  );
}

/** Sets up a game state that is pasted in or read from a file. Desktop's dev mode reads one from its games folder. */
export function DevSetupDialog({ actions, close }: { actions: Actions; close: () => void }) {
  const [text, setText] = useState('');
  const file = useRef<HTMLInputElement>(null);
  return (
    <OptionsDialog title={t('lblWebDevSetUpTitle')} kind="dev-setup" close={close} footer={<>
      <span class="hint">{t('lblWebDevSetUpHint')}</span>
      <button onClick={() => file.current?.click()}>{t('lblWebDevLoadFile')}</button>
      <button class="primary" disabled={!text.trim()} onClick={() => { actions.dev('setupGameState', text); close(); }}>{t('lblWebEventSetUp')}</button>
    </>}>
      <textarea class="dev-state" spellcheck={false} rows={14} value={text} placeholder={'activeplayer=human\nactivephase=MAIN1\nhumanlife=20\nailife=20\nhumanbattlefield=Grizzly Bears;Forest'}
        onInput={e => setText(e.currentTarget.value)} />
      <input ref={file} type="file" accept=".txt,text/plain" hidden onChange={async e => {
        const input = e.currentTarget;
        const chosen = input.files?.[0];
        if (chosen) setText(await chosen.text());
        input.value = '';
      }} />
    </OptionsDialog>
  );
}
