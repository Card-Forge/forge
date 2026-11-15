// The bar across the top of every page outside a match. Each page fills the middle and puts its exit button at the end

import type { ComponentChildren } from 'preact';
import { useEffect } from 'preact/hooks';
import { changeUi, redrawUi, ui } from './ui';
import { t } from './text';
import { isPortrait } from './form';
import { Dropdown } from './dialog';
import { ICONS, Icon } from './icons';

/** The Forge wordmark: Forge's anvil, the name over a bar in Magic's five colours, and the beta tag. This is the only place the colours are pure decoration, with no card art nearby and no meaning in the hue. */
export function Wordmark() {
  return (
    <div class="wordmark-block">
      <span class="wordmark" aria-label="Forge">
        {/* Traced from icon-512.png, so it matches the page's window icon */}
        <svg class="anvil" viewBox="0 0 65 56" aria-hidden="true">
          <path fill-rule="evenodd" d="M44.2 0.2 52.7 0.3 52.7 3.3 53.8 5.6 53.8 10.7 53.6 11.6 52.7 13.0 52.6 15.6 44.1 15.5ZM10.3 5.2 11.0 5.2 11.5 6.0 11.3 10.7 11.0 10.9 9.9 10.7 9.9 5.8ZM12.8 5.2 13.5 5.2 14.0 6.0 14.0 9.6 13.7 10.9 12.8 10.9 12.4 10.5 12.4 5.8ZM7.6 5.4 8.6 5.5 8.5 10.8 7.4 10.7 7.4 6.5ZM15.0 5.6 42.0 5.6 42.1 10.1 41.9 10.4 15.0 10.2ZM0.2 23.0 6.0 22.8 16.4 22.9 18.5 26.4 19.9 30.6 20.7 30.8 20.8 29.7 19.7 26.1 17.6 21.6 17.7 21.2 20.2 21.2 22.4 26.4 24.0 33.8 24.8 34.1 25.1 33.8 25.1 33.1 23.5 25.2 21.9 20.5 20.4 18.0 20.5 17.7 64.2 17.8 64.2 21.4 63.7 22.4 32.8 22.3 35.0 22.8 44.7 23.7 61.0 23.8 56.3 28.4 50.6 35.0 48.6 37.9 47.1 40.8 46.4 43.1 46.4 46.5 47.1 48.1 49.7 50.4 52.4 51.3 54.2 51.5 54.5 51.7 54.9 53.9 54.9 55.9 12.5 55.9 12.2 55.5 12.6 51.8 45.4 51.7 39.7 49.8 20.5 49.7 20.4 49.4 22.6 47.4 24.2 45.3 25.5 42.4 25.8 39.9 24.3 39.6 23.6 39.8 23.1 39.4 22.3 37.1 20.2 37.1 17.7 36.6 14.3 35.3 10.5 33.3 5.1 29.4 1.8 25.9Z" />
        </svg>
        <span class="word" aria-hidden="true">Forge<span class="bar"><i /><i /><i /><i /><i /></span></span>
        <span class="beta">{t('lblWebBeta')}</span>
      </span>
    </div>
  );
}

/** brand pages show the phone header on a phone, with the wordmark as its title and action as its one action. Other pages keep this header on a phone. */
export function PageHeader({ class: extra, brand, action, children }: { class?: string; brand?: boolean; action?: ComponentChildren; children?: ComponentChildren }) {
  if (brand && isPortrait()) return <PhoneHead title={<Wordmark />} action={action} />;
  return <header class={extra ? `page-head ${extra}` : 'page-head'}><Wordmark />{children}</header>;
}

/**
 * The header of a page on a phone: back to the previous step, the title and its line, one action of the page's own, the chat and a menu.
 * tabs, when given, are a second row. more adds the page's own rows to the menu, and dev adds dev mode tools at its end.
 */
export function PhoneHead({ back, title, line, action, more, dev, tabs, inert }: {
  back?: SetupStep; title: ComponentChildren; line?: ComponentChildren; action?: ComponentChildren;
  more?: (close: () => void) => ComponentChildren; dev?: ComponentChildren; tabs?: ComponentChildren; inert?: boolean;
}) {
  // The dock draws its folded bar into the chat slot (dock.tsx), so it must draw again once the slot exists
  useEffect(redrawUi, []);
  return (
    <header class="ph" inert={inert}>
      <div class="ph-row">
        {back && <button class="ph-icon ph-back" title={back.label} aria-label={back.label} onClick={back.go}><Icon d={ICONS.back} /></button>}
        <div class="ph-title">
          {typeof title === 'string' ? <h2>{title}</h2> : title}
          {line && <span class="ph-line">{line}</span>}
        </div>
        {action}
        <span class="ph-chat" />
        <Dropdown cls="ph-more" button={(open, toggle) => (
          <button class="ph-icon" title={t('lblWebHeadMore')} aria-label={t('lblWebHeadMore')} aria-expanded={open} onClick={toggle}><Icon d={ICONS.ellipsis} /></button>
        )}>
          {close => (
            <div class="ph-menu" role="menu">
              <button role="menuitem" onClick={() => { close(); changeUi(u => { u.volumeOpen = true; }); }}><Icon d={ICONS.volume} />{t('lblWebHeadVolume')}</button>
              {more?.(close)}
              <button role="menuitem" onClick={() => { close(); changeUi(u => { u.optionsOpen = true; }); }}><Icon d={ICONS.cog} />{t('lblWebHeadOptions')}</button>
              {dev}
            </div>
          )}
        </Dropdown>
      </div>
      {tabs}
    </header>
  );
}

/** The earlier step of the trail on screen, which Escape goes back to. */
let shownBack: SetupStep | null = null;

/** Goes back one step of the trail on screen. It returns false when the page shows no earlier step. */
export function goBack(): boolean {
  if (!shownBack?.go) return false;
  shownBack.go();
  return true;
}

/** The step a phone header's back button returns to: the last earlier step of the trail, if any. */
export const backStep = (trail: SetupStep[]): SetupStep | undefined => trail.findLast(step => step.go);

/** The page's way back at the end of its header: the trail's last earlier step. A trail with no earlier step draws nothing. */
export function BackButton({ trail }: { trail: SetupStep[] }) {
  const step = backStep(trail);
  return step ? <button title={step.label} onClick={step.go}>{t('lblBack')}</button> : null;
}

/** A step of game setup in the trail. go returns to that step, and is left out for the current step. */
export interface SetupStep {
  label: string;
  go?: () => void;
}

/**
 * The trail through game setup, above the page's heading. Each earlier step in the trail is a button that goes back to it.
 * On a phone a trail with an earlier step is drawn by the page's PhoneHead instead, so only the aside and the line under the heading are left here.
 */
export function SetupHead({ trail, title, aside, sub }: { trail: SetupStep[]; title: string; aside?: ComponentChildren; sub?: ComponentChildren }) {
  const back = backStep(trail);
  useEffect(() => {
    if (!back) return;
    shownBack = back;
    return () => { if (shownBack === back) shownBack = null; };
  });
  if (isPortrait() && back) {
    return aside || sub ? <div class="setup-head">{aside && <div class="setup-aside">{aside}</div>}{sub && <p class="setup-sub">{sub}</p>}</div> : null;
  }
  return (
    <div class="setup-head">
      <nav class="crumb" aria-label={t('lblGameSetup')}>
        {trail.map((step, i) => (
          <>
            {i > 0 && <span aria-hidden="true">›</span>}
            {step.go ? <button class="link" onClick={step.go}>{step.label}</button> : <span aria-current={i === trail.length - 1 ? 'step' : undefined}>{step.label}</span>}
          </>
        ))}
      </nav>
      {/* The aside is shown next to the heading. It holds your deck before a match, or the exit button of a form */}
      <div class="setup-title">
        <h2>{title}</h2>
        {aside && <div class="setup-aside">{aside}</div>}
      </div>
      {sub && <p class="setup-sub">{sub}</p>}
    </div>
  );
}

/** The name of each way to play, as shown in the trail. */
export const WAY_NAMES: Record<'play' | 'friends', string> = {
  get play() { return t('lblWebMenuVersusAi'); },
  get friends() { return t('lblWebMenuWithFriends'); },
};

/** The volume and options buttons. Each page places them just before its exit button. */
export function HeadControls() {
  return (
    <>
      <button class="icon-button volume" title={t('lblWebHeadVolume')} aria-label={t('lblWebHeadVolume')} aria-expanded={ui.volumeOpen}
        onClick={() => changeUi(u => { u.volumeOpen = !u.volumeOpen; })}>
        <Icon d={ICONS.volume} />
      </button>
      <button class="icon-button" title={t('lblWebHeadOptions')} aria-label={t('lblWebHeadOptions')} onClick={() => changeUi(u => { u.optionsOpen = true; })}><Icon d={ICONS.cog} /></button>
    </>
  );
}
