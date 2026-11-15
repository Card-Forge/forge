// The game menu behind the prompt's ⋯ button. It keeps the auto-pass settings one click from the prompt

import type { ComponentChildren } from 'preact';
import { useEffect, useLayoutEffect, useRef, useState } from 'preact/hooks';
import { OnOff, OptionsDialog, PlaymatChoice, Row, SectionHeading } from '../options';
import { SETTINGS, setting, type SettingDef } from '../settings';
import { DevItems } from './devmenu';
import type { Actions } from '../actions';
import type { AutoDecision } from '../protocol';
import { deref, type Model } from '../model';
import { t, type TextKey } from '../text';
import { useClosing } from '../closing';
import { changeUi, ui } from '../ui';
import { Backdrop, Confirm } from '../dialog';
import { ICONS, MenuIcon, Icon } from '../icons';
import { useHoverIntent, useTwoPress } from '../hooks';
import { isPortrait } from '../form';

/** Starts recording a macro, or stops the recording. */
export function recordMacro(actions: Actions): void {
  actions.macroRecord();
  changeUi(u => { u.macro = u.macro === 'recording' ? 'recorded' : 'recording'; });
}

/** Asks the player to confirm before conceding, as desktop does. */
export function ConcedeQuestion({ actions }: { actions: Actions }) {
  const close = () => changeUi(u => { u.concedeAsk = false; });
  return <Confirm title={t('lblConcedeTitle')} text={t('lblConcedeCurrentGame')} back={close} backLabel={t('lblCancel')}
    go={() => { close(); actions.concede(); }} goLabel={t('lblConcede')} danger cls="concede-ask" />;
}

/** The game menu. `dev` opens it at the dev items, for the shortcut that shows the dev panel. A desktop shows them beside the menu, and a phone in its place. */
export function GameMenu({ model, actions, close, open, dev: atDev }: {
  model: Model; actions: Actions; close: () => void; open: (dialog: 'stops' | 'decisions' | 'devSetup') => void; dev?: boolean;
}) {
  const phone = isPortrait();
  const [dev, setDev] = useState(!!atDev && phone);
  const [devBeside, setDevBeside] = useState(!!atDev && !phone);
  const hover = useHoverIntent();
  const [mats, setMats] = useState(false);
  const menu = useRef<HTMLDivElement>(null);
  const { closing, shut, gone } = useClosing(close);
  // The menu goes above the button that opened it, with right edges aligned, because the prompt is in the bottom-right corner
  useLayoutEffect(() => {
    const button = document.querySelector('#match .tools .more')?.getBoundingClientRect();
    const el = menu.current;
    if (!button || !el) return;
    el.style.right = `${Math.max(8, innerWidth - button.right)}px`;
    el.style.bottom = `${innerHeight - button.top + 6}px`;
  }, []);
  const offer = model.drawOffer;
  return (
    <Backdrop cls={closing ? 'anchored closing' : 'anchored'} onAnimationEnd={gone} dismiss={shut}>
      {/* Pointing at another row closes the dev items beside the menu, after a pause, so a pointer crossing rows on its way to them keeps them open */}
      <div ref={menu} class={dev ? 'card-menu game-menu dev' : 'card-menu game-menu'} role="menu" aria-label={dev ? t('lblDevMode') : t('lblGame')}
        onMouseOver={e => {
          if ((e.target as Element).closest('.dev-entry')) hover.stay();
          else if (devBeside) hover.later(() => setDevBeside(false));
        }}>
        {dev ? <DevItems model={model} actions={actions} back={() => setDev(false)} close={close} setUp={() => open('devSetup')} /> : <>
        <button type="button" role="menuitem" class={mats ? 'card-menu-item on' : 'card-menu-item'} aria-expanded={mats}
          onClick={() => { setMats(!mats); setDevBeside(false); }}><MenuIcon d={ICONS.playmat} />{t('lblWebPlaymat')}<MenuIcon d={ICONS.next} end /></button>
        <div class="card-menu-sep" role="separator" />
        <button type="button" role="menuitem" class="card-menu-item" onClick={() => open('stops')}>
          <MenuIcon d={ICONS.interrupts} />{t('lblYieldSettings')}
        </button>
        <button type="button" role="menuitem" class="card-menu-item" disabled={model.spectating} onClick={() => open('decisions')}><MenuIcon d={ICONS.yields} />{t('lblWebGameMenuDecisions')}</button>
        <button type="button" role="menuitem" class="card-menu-item" disabled={model.spectating} onClick={() => {
          recordMacro(actions);
          close();
        }}><MenuIcon d={ICONS.macro} />{t(ui.macro === 'recording' ? 'lblWebGameMenuMacroStop' : 'lblWebGameMenuMacroRecord')}</button>
        <div class="card-menu-sep" role="separator" />
        {/* A desktop has the speaker beside this menu's button. A phone's tools have no room for it */}
        {phone && <button type="button" role="menuitem" class="card-menu-item" onClick={() => { close(); changeUi(u => { u.volumeOpen = true; }); }}>
          <MenuIcon d={ICONS.volume} />{t('lblWebHeadVolume')}
        </button>}
        <button type="button" role="menuitem" class="card-menu-item" onClick={() => { close(); changeUi(u => { u.optionsOpen = true; }); }}>
          <MenuIcon d={ICONS.cog} />{t('lblWebHeadOptions')}
        </button>
        <div class="card-menu-sep" role="separator" />
        <button type="button" role="menuitem" class="card-menu-item" disabled={!!offer || model.spectating}
          onClick={() => { actions.drawOffer('OFFER'); close(); }}>
          <MenuIcon d={ICONS.draw} />{offer?.mine ? t('lblWebGameMenuDrawOffered') : t('lblWebGameMenuOfferDraw')}
        </button>
        <button type="button" role="menuitem" class="card-menu-item concede" disabled={model.spectating} onClick={() => {
          close();
          changeUi(u => { u.concedeAsk = true; });
        }}>
          <MenuIcon d={ICONS.concede} />{t('lblConcede')}
        </button>
        {/* Only the host gets dev mode. The host's seat runs in the same process as the server, and a guest's does not */}
        {setting('devMode') && model.host && !model.spectating && <>
          <div class="card-menu-sep" role="separator" />
          <button type="button" role="menuitem" aria-haspopup="menu" aria-expanded={devBeside} class={devBeside ? 'card-menu-item dev-entry on' : 'card-menu-item dev-entry'}
            onMouseEnter={phone ? undefined : () => { setDevBeside(true); setMats(false); }}
            onClick={() => { if (phone) setDev(true); else { setDevBeside(true); setMats(false); } }}>
            <MenuIcon d={ICONS.dev} />{t('lblDevMode')}<MenuIcon d={ICONS.next} end />
          </button>
        </>}
        </>}
      </div>
      {mats && !dev && <PlaymatPicker beside={menu} />}
      {devBeside && <DevBeside menu={menu} enter={hover.stay}>
        <DevItems model={model} actions={actions} close={close} setUp={() => open('devSetup')} />
      </DevBeside>}
    </Backdrop>
  );
}

/** The dev items beside the game menu, level with its bottom edge. Their families open further left. */
function DevBeside({ menu, enter, children }: { menu: { current: HTMLDivElement | null }; enter: () => void; children: ComponentChildren }) {
  const panel = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const el = panel.current, beside = menu.current?.getBoundingClientRect();
    if (!el || !beside) return;
    el.style.right = `${innerWidth - beside.left + 8}px`;
    el.style.bottom = `${innerHeight - beside.bottom}px`;
  }, []);
  return <div ref={panel} class="card-menu game-menu dev" role="menu" aria-label={t('lblDevMode')} onMouseEnter={enter}>{children}</div>;
}

/** The playmats beside the game menu. A choice changes the table at once, so the board behind it is the preview. */
function PlaymatPicker({ beside }: { beside: { current: HTMLDivElement | null } }) {
  const panel = useRef<HTMLDivElement>(null);
  // Placed from the button, as the menu is, because this runs before the menu has placed itself
  useLayoutEffect(() => {
    const button = document.querySelector('#match .tools .more')?.getBoundingClientRect();
    const el = panel.current;
    if (!button || !el || !beside.current) return;
    el.style.right = `${Math.max(8, innerWidth - button.right) + beside.current.offsetWidth + 8}px`;
    el.style.bottom = `${innerHeight - button.top + 6}px`;
    // A short window scrolls the panel rather than pushing its top rows off screen
    el.style.maxHeight = `${button.top - 14}px`;
  }, []);
  return (
    <div ref={panel} class="playmat-picker" role="dialog" aria-label={t('lblWebPlaymat')}>
      <h3>{t('lblWebPlaymat')}</h3>
      <PlaymatChoice />
    </div>
  );
}

/** The points where auto-passing stops, listed as the options dialog lists them. */
export function AutoPassStops({ close }: { close: () => void }) {
  return (
    <OptionsDialog title={t('lblYieldSettings')} label={t('lblYieldSettings')} kind="stops-dialog" close={close}
      footer={<span class="hint">{t('lblInterruptSettingsDesc')}</span>}>
      {SETTINGS.filter(def => def.menu === 'stops').flatMap((def, i, all) => [
        ...(def.section !== all[i - 1]?.section ? [<SectionHeading key={`section ${def.section}`} name={def.section} keys={false} />] : []),
        <Row key={def.key} def={def} />,
      ])}
    </OptionsDialog>
  );
}

const YIELD_MODE = SETTINGS.find(def => def.key === 'autoYieldMode') as SettingDef;

const KIND_LABEL: Record<AutoDecision['kind'], TextKey> = {
  yield: 'lblWebGameMenuAlwaysYield', accept: 'lblWebGameMenuAlwaysAccept', decline: 'lblWebGameMenuAlwaysDecline',
};

/** The auto-yields and trigger answers set during play, as desktop's dialog lists them. Each one can be removed. */
export function AutoDecisionsDialog({ model, actions, close }: { model: Model; actions: Actions; close: () => void }) {
  const [forgetting, forgetAll] = useTwoPress();
  const [ordersReset, setOrdersReset] = useState(false);
  const all = model.autoDecisions;
  const entries = all?.entries ?? [];
  // The list is requested on opening, from either the game menu or the options
  useEffect(() => { actions.autoDecisions('list'); }, []);
  return (
    <OptionsDialog title={t('lblAutoYieldsAndTriggers')} kind="decisions-dialog" close={close} footer={<>
      <span class="hint">{t('lblWebGameMenuDecisionsHint')}</span>
      <button class={forgetting ? 'clear-all armed' : 'clear-all'} disabled={!entries.length} onClick={forgetAll(() => actions.autoDecisions('clear'))}>{forgetting ? t('lblWebGameMenuForgetAllAgain') : t('lblWebGameMenuForgetAll')}</button>
    </>}>
      {/* Each mode keeps its own list, so the list is read again after a change */}
      <Row def={YIELD_MODE} onChange={() => actions.autoDecisions('list')} />
      {!all ? <p class="hint">{t('lblWebGameMenuReading')}</p>
        : !entries.length ? <p class="hint">{t('lblWebGameMenuNoneSet')}</p>
          : entries.map(e => (
            <div key={`${e.kind} ${e.key}`} class="decision">
              <span class={`decision-kind ${e.kind}`}>{t(KIND_LABEL[e.kind])}</span>
              <span class="decision-key" title={e.key}>{e.key}</span>
              <button class="decision-forget" title={t('lblWebGameMenuForgetThis')} aria-label={t('lblWebGameMenuForgetX', e.key)}
                onClick={() => actions.autoDecisions('remove', e.key)}>
                <Icon d={ICONS.x} />
              </button>
            </div>
          ))}
      {all && (
        <>
          <div class="setting">
            <div>{t('lblWebGameMenuPauseYields')}</div>
            <OnOff on={all.yieldsOff} label={t('lblWebGameMenuPauseYields')} change={on => actions.autoDecisions('disableYields', undefined, on)} />
          </div>
          <div class="setting">
            <div>{t('lblWebGameMenuPauseTriggers')}</div>
            <OnOff on={all.triggersOff} label={t('lblWebGameMenuPauseTriggers')} change={on => actions.autoDecisions('disableTriggers', undefined, on)} />
          </div>
          {/* The host keeps no list of saved orders to show, so the button only shows that it has been pressed */}
          <div class="setting">
            <div>{t('lblResetSavedAbilityOrders')}</div>
            <button disabled={ordersReset} onClick={() => {
              actions.autoDecisions('clearOrders');
              setOrdersReset(true);
            }}>{ordersReset ? t('lblDone') : t('lblReset')}</button>
          </div>
        </>
      )}
    </OptionsDialog>
  );
}

/** Another player's offer of a draw. The game waits for this player to answer it. */
export function DrawOfferQuestion({ model, actions }: { model: Model; actions: Actions }) {
  const offer = model.drawOffer;
  if (!offer?.waitingOnMe) return null;
  const who = deref(model, offer.offerer)?.Name;
  return (
    <Backdrop>
      <div class="dialog" role="dialog" aria-label={t('lblOfferDrawTitle')}>
        <h3>{who != null ? t('lblDrawOfferedBy', who) : t('lblWebGameMenuOpponentOffersDraw')}</h3>
        <p class="hint">{t('lblWebGameMenuDrawOnlyIfAll')}</p>
        <div class="actions">
          <button onClick={() => actions.drawOffer('DECLINE')}>{t('lblDecline')}</button>
          <button class="primary" onClick={() => actions.drawOffer('ACCEPT')}>{t('lblAccept')}</button>
        </div>
      </div>
    </Backdrop>
  );
}
