// A table's draft or sealed event, run as desktop's lobby runs one. The host sets it up and deals the packs once every seat is ready

import { useState } from 'preact/hooks';
import { StepForm } from '../stepform';
import { draftCombo, draftSentence, draftSteps, pickRuleName, sealedSentence, sealedSteps, type DraftValue, type SealedValue } from '../limited/setup';
import { changeUi, ui } from '../ui';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { LobbyTable, PastEvent } from '../protocol';
import { GameMenu, PlayerCount } from './matchbar';
import { t, tCap, shortDay } from '../text';
import { Backdrop, Confirm, CloseButton } from '../dialog';

/** What the setup dialog shows. 'choose' asks for a new event or an earlier one. The other two show that choice's form. */
type SetupView = 'choose' | 'new' | 'earlier';

/** The event panel at the head of the table. On a table with no event yet, the host's setup dialog opens by itself. */
export function EventHead({ model, lobby, actions, preview, start }: {
  model: Model; lobby: LobbyTable; actions: Actions; preview: (count: number | null) => void; start: () => void;
}) {
  const lim = lobby.limited!;
  const [setting, setSetting] = useState(!lim.product);
  // When earlier events are kept, the host first chooses between a new event and playing an earlier one again
  const firstView = (): SetupView => (lim.pastEvents.length && !lim.product ? 'choose' : 'new');
  const [view, setView] = useState<SetupView>(firstView);
  const [leaving, setLeaving] = useState(false);
  const openSetup = () => { setView(firstView()); setSetting(true); };
  const draft = lim.kind === 'draft';
  const unready = lobby.seats.filter(s => s.type !== 'OPEN' && !s.ready);
  const close = () => setSetting(false);
  const stages = [t('lblWebEventStageReady'), draft ? t('lblDraft') : t('lblWebEventStageOpen'), t('lblWebEventStageBuild'), t('lblPlay')];
  const at = eventStage(lobby);
  const players = lobby.seats.length;
  // Computers fill the pod seats that the table's players leave empty. They draft but do not play
  const ai = draft ? Math.max(0, lim.podSize - players) : 0;
  const slim = lim.started;
  let button = null;
  let why = eventStatus(lobby);
  if (!lim.started) {
    if (lobby.host && lim.product) {
      button = <button class="primary big" disabled={unready.length > 0} onClick={() => actions.eventStart()}>{draft ? t('lblNetworkStartDraft') : t('lblWebEventStageOpen')}</button>;
    }
  } else if (model.drafting && ui.draftHidden) {
    button = <button class="primary big" onClick={() => changeUi(u => { u.draftHidden = false; })}>{t('lblWebEventReturnToDraft')}</button>;
  } else if (lim.activeEventId) {
    const problem = lobby.problems?.[0];
    if (lobby.host) {
      button = <button id="play" class="primary big" disabled={!lobby.canStart} onClick={start} title={t('ttWebEventEnterStarts')}>{t('lblPlay')}</button>;
      why = lobby.canStart ? t('lblWebEventEveryoneHasDeck') : problem ?? why;
    } else {
      why = problem ?? t('lblWebEventWaitingHostStart');
    }
  }
  const facts = draft
    ? t('lblWebEventDraftFacts', players, ai, pickRuleName(lim.pickRule, true),
      lim.timer ? t('lblWebEventSecondsAPick', lim.timer) : t('lblWebEventNoPickTimer'))
    : t('lblWebEventSealedFacts', players);
  return (
    <section class={slim ? 'event-head slim' : 'event-head'} aria-label={draft ? t('lblWebEventTheDraft') : t('lblWebEventTheSealedEvent')}>
      {lobby.host && !lim.started && setting && (
        <Backdrop dismiss={close}>
          <div class="dialog event-setup" role="dialog" aria-label={draft ? t('lblWebEventSetUpTheDraft') : t('lblWebEventSetUpSealedEvent')}>
            <CloseButton onClick={close} />
            {view === 'choose' && lim.pastEvents.length > 0
              ? <SetupChoice draft={draft} count={lim.pastEvents.length} choose={setView} />
              : view === 'earlier'
                ? <PastEvents events={lim.pastEvents} actions={actions} back={() => setView('choose')} />
                : <>
                    {lim.pastEvents.length > 0 && !lim.product && <button class="link setup-back" onClick={() => setView('choose')}>{t('lblWebEventBack')}</button>}
                    {!model.limitedOptions ? <p class="muted">{t('lblWebEventLoading')}</p>
                      : draft ? <DraftForm model={model} lobby={lobby} actions={actions} done={close} />
                      : <SealedForm model={model} actions={actions} done={close} />}
                  </>}
          </div>
        </Backdrop>
      )}
      {leaving && <Confirm title={t('lblWebEventStartNewEvent')} label={t('lblWebEventStartNewEvent')} text={t('lblWebEventNewEventHint')}
        back={() => setLeaving(false)} backLabel={t('lblWebEventKeepThisEvent')}
        go={() => { setLeaving(false); actions.eventNew(); setView('choose'); setSetting(true); }} goLabel={t('lblWebEventNewEvent')} />}
      <div class="eh-main">
        <div class="eh-mode"><GameMenu lobby={lobby} formats={model.formats} actions={actions} /></div>
        <div class="eh-title">
          <span class="event-product">{lim.product ?? t('lblWebEventNotSetUp')}</span>
          {lobby.host && !lim.started && <button class="link" onClick={openSetup}>{lim.product ? t('lblEdit') : t('lblWebEventSetUp')}</button>}
          {/* Here the host leaves a started or replayed event for a new one. Its pools stay in the earlier events */}
          {lobby.host && lim.started && !model.drafting && <button class="link" onClick={() => setLeaving(true)}>{t('lblWebEventNewEvent')}</button>}
          {slim && <span class="eh-facts">{facts}</span>}
        </div>
        {!slim && (
          <div class="eh-fields">
            <div class="eh-field"><span class="field-name">{t('lblPlayers')}</span><PlayerCount lobby={lobby} actions={actions} preview={preview} /></div>
            {draft && lim.product && <>
              <div class="eh-field"><span class="field-name">{t('lblWebEventAiDrafters')}</span>
                <span>{ai ? <>{ai} <span class="muted">{t('lblWebEventToMakePod', lim.podSize)}</span></> : t('lblNone')}</span></div>
              <div class="eh-field"><span class="field-name">{t('lblWebEventPicks')}</span><span>{pickRuleName(lim.pickRule)}</span></div>
              <div class="eh-field"><span class="field-name">{t('lblNetworkPickTimerCaption')}</span><span>{lim.timer ? t('lblWebEventSeconds', lim.timer) : t('lblNone')}</span></div>
            </>}
            {!draft && lim.product && <div class="eh-field"><span class="field-name">{t('lblWebEventPools')}</span><span>{t('lblWebEventEachOpensOwn')}</span></div>}
          </div>
        )}
        {!slim && <div class="eh-art" aria-hidden="true"><i class="pk" /><i class="pk" /><i class="pk" /></div>}
      </div>
      <div class="eh-rail">
        <ol class="eh-track" aria-label={t('lblWebEventWhereEventIs')} style={{ '--at': at }}>
          {stages.map((name, i) => <li key={name} class={i < at ? 'done' : i === at ? 'now' : ''}><i />{name}</li>)}
        </ol>
        <div class="eh-go">
          {button}
          <span class="eh-why">{why}</span>
        </div>
      </div>
    </section>
  );
}

/** The event's stage: 0 ready up, 1 draft or opening, 2 building, 3 playing. It is 3 once every seat that plays has a deck. */
function eventStage(lobby: LobbyTable): number {
  const lim = lobby.limited!;
  if (!lim.started) return 0;
  if (!lim.activeEventId) return 1;
  const playing = lobby.seats.filter(s => s.type !== 'OPEN' && !s.sittingOut);
  return playing.every(s => s.deck != null || s.deckName != null) ? 3 : 2;
}

/** The line under the bar at a Draft or Sealed table. It says who or what the table is waiting on. */
function eventStatus(lobby: LobbyTable): string {
  const lim = lobby.limited!;
  if (!lim.product) return lobby.host ? t('lblWebEventSetUpThenReady') : t('lblWebEventHostSettingUp');
  if (!lim.started) {
    const unready = lobby.seats.filter(s => s.type !== 'OPEN' && !s.ready).map(s => (s.mine ? tCap('lblYou') : s.name ?? t('lblWebEventAPlayer')));
    if (unready.length) return t('lblWebEventWaitingForReady', unready.join(', '));
    return lobby.host ? t('lblWebEventEveryoneReady') : t('lblWebEventEveryoneReadyWaitHost');
  }
  if (!lim.activeEventId) return lim.kind === 'draft' ? t('lblWebEventDraftIsOn') : t('lblWebEventOpeningPacks');
  return t('lblWebEventPoolsOut');
}

function SetupChoice({ draft, count, choose }: { draft: boolean; count: number; choose: (view: SetupView) => void }) {
  return (
    <div class="setup-choice">
      <h3>{draft ? t('lblWebEventDraftAtTable') : t('lblWebEventSealedAtTable')}</h3>
      <div class="tiles">
        <button class="tile-choice" onClick={() => choose('new')}>
          <b>{draft ? t('lblWebEventNewDraft') : t('lblWebEventNewSealedEvent')}</b><span>{t('lblWebEventChoosePacksRules')}</span>
        </button>
        <button class="tile-choice" onClick={() => choose('earlier')}>
          <b>{t('lblWebEventAnEarlierEvent')}</b><span>{t(count === 1 ? 'lblWebEventPlayOneSavedAgain' : 'lblWebEventPlaySavedAgain', count)}</span>
        </button>
      </div>
    </div>
  );
}

/** Turns an event's saved "yyyy-MM-dd HH:mm" date into "26 Sept, 09:46". */
function playedOn(date: string): string {
  return date ? `${shortDay(date.slice(0, 10))}, ${date.slice(11, 16)}` : '';
}

/** The earlier events, newest first. Each can be played again or deleted from here. A delete asks first, in the event's row. */
function PastEvents({ events, actions, back }: { events: PastEvent[]; actions: Actions; back: () => void }) {
  const [deleting, setDeleting] = useState<string | null>(null);
  return (
    <div class="past-events">
      <button class="link setup-back" onClick={back}>{t('lblWebEventBack')}</button>
      <h3>{t('lblWebEventEarlierEvents')}</h3>
      {events.length === 0 && <p class="muted">{t('lblWebEventNoEarlierEvents')}</p>}
      <div class="past-list">
        {events.map(p => (
          <div key={p.id} class="past-row">
            <span class="past-name"><b>{p.product || (p.kind === 'draft' ? t('lblDraft') : t('lblSealed'))}</b>
              <span class="muted">{t(p.kind === 'draft' ? 'lblWebEventPlayedDraft' : 'lblWebEventPlayedSealed', playedOn(p.date))}</span></span>
            {deleting === p.id
              ? <>
                  <span class="past-ask">{t('lblWebEventDeleteItsPools')}</span>
                  <button onClick={() => setDeleting(null)}>{t('lblWebLimitedKeep')}</button>
                  <button class="danger" onClick={() => { setDeleting(null); actions.eventForget(p.id); }}>{t('lblDelete')}</button>
                </>
              : <>
                  <button class="outline" onClick={() => actions.eventHostAgain(p.id)}>{t('lblWebEventPlayAgain')}</button>
                  <button class="past-delete" title={t('ttWebEventDeletePools')} aria-label={t('lblWebEventDeleteNamed', p.product)}
                    onClick={() => setDeleting(p.id)}>&times;</button>
                </>}
          </div>
        ))}
      </div>
    </div>
  );
}

function SealedForm({ model, actions, done }: { model: Model; actions: Actions; done: () => void }) {
  const options = model.limitedOptions!;
  const [value, setValue] = useState<SealedValue>({});
  // The pool is named after its event, as on desktop, so the form does not ask for a name
  const steps = sealedSteps(options).filter(s => s.id !== 'name');
  return (
    <StepForm title={t('lblWebEventSetUpSealed')} steps={steps} value={value} onChange={setValue}
      sentence={v => sealedSentence(options, v)} action={t('lblSave')} submit={() => {
        actions.eventSetup({ product: value.product!, block: value.block, combo: value.combo, edition: value.edition,
          template: value.template, cubeId: value.cubeId, packs: value.packs ?? 0, podSize: 0, timer: 0, grace: 0 });
        done();
      }} />
  );
}

function DraftForm({ model, lobby, actions, done }: { model: Model; lobby: LobbyTable; actions: Actions; done: () => void }) {
  const options = model.limitedOptions!;
  const [value, setValue] = useState<DraftValue>({});
  const seated = lobby.seats.filter(s => s.type !== 'OPEN').length;
  return (
    <StepForm title={t('lblWebEventSetUpTheDraft')} steps={draftSteps(options, { seated })} value={value} onChange={setValue}
      sentence={v => draftSentence(v, true)} action={t('lblSave')} submit={() => {
        actions.eventSetup({ product: value.product!, block: value.block, combo: draftCombo(value), cube: value.cube,
          theme: value.theme, cubeId: value.cubeId, packs: 3, podSize: value.podSize ?? 0, pickRule: value.pickRule,
          timer: value.timer ?? 0, grace: value.grace ?? 0 });
        done();
      }} />
  );
}
