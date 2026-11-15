// The controller. It is the only place that talks to the server, and it draws the model once per frame

import { connect } from './net';
import { createModel, applyState, cardMenu, concedingEndsGame, isLocal, oldestRequest, players, stackPick } from './model';
import { asked, createActions, type Actions } from './actions';
import { changeUi, initUi, resetMatchUi, transition, ui } from './ui';
import { initForm, isPortrait } from './form';
import { initPress } from './press';
import { initSheets, matchHistory } from './sheet';
import { rememberName, rememberedAvatar, rememberedName, rememberedSleeve } from './identity';
import { renderScreens, pageUnder, screenOf, slideWay } from './screens';
import { renderMatch, renderSky, resetTable } from './match/board';
import { arriveAtMatch, dimIntoMatch } from './match/matchentry';
import { renderPrompt, flash, pressPromptButton } from './match/prompt';
import { initSide, renderSide, showLog } from './match/side';
import { initDetail, nextFace, renderDetail } from './preview';
import { initOverlay, drawOverlay } from './match/overlay';
import { afterCombatDrags, initCombatDrag, renderCombatDrag } from './match/combatdrag';
import { boundKeys, initSettings, onServerSettings, rememberGuestAutoPass, restoreGuestSettings, setGuest, setting, shortcutKeys } from './settings';
import { behindItsBoard, passingFor } from './match/pauserules';
import { setVersion } from './links';
import { goBack } from './header';
import { applyAudioSettings, playMusic, playSound } from './audio';
import { rememberStops, restoreStops } from './match/stopmemory';
import { byId, isTyping, relayout, saveText } from './dom';
import { pictureArrived } from './images';
import { initNotices, showNotice } from './notices';
import { forgetWakeLock, keepAwake } from './wakelock';
import { deleteDeviceDeck, listDeviceDecks, putDeviceDeck } from './deck/devicedecks';
import type { Send, ServerMessage } from './protocol';

const model = createModel();
let scheduled = false;
// A guest's settings are kept in its server session, so the browser sends the settings it stored to each session it is given.
// A seat handed over moves the browser to another session on the same connection
let restoredFor: number | null = null;
// A sound or log line arrives before its board update. It is applied after that board, or alone after 300 ms if no board follows (pauserules.ts)
const send = connect(behindItsBoard(msg => pacer.receive(msg), 300), online => {
  byId('banner').hidden = online;
  // The server replays the chat on every connection, so the browser clears it when the connection drops
  if (!online) {
    restoredFor = null;
    sentDeviceDecks = false;
    model.chat = [];
  }
});

// Otherwise a script error is only visible in the player's own browser. The first few are enough to tell what broke
let reported = 0;
const report = (text: string): void => {
  if (reported++ < 5) send({ t: 'pageError', text: text.slice(0, 1500) });
};
window.addEventListener('error', e => report(String((e.error as Error | undefined)?.stack ?? e.message)));
window.addEventListener('unhandledrejection', e => report(String((e.reason as Error | undefined)?.stack ?? e.reason)));

const wire = createActions(sendNow);
const actions = {
  ...wire,
  // The engine thread waits for the answer, so the question is cleared as soon as it is answered
  answerHostChoice: (id, value) => {
    model.hostChoice = null;
    wire.answerHostChoice(id, value);
    schedule();
  },
} as Actions;

// A change made inside a view transition must draw at once, so it does not start a slide of its own
initUi(schedule, draw);
initForm(() => {
  // Panels that only one layout draws are closed when the layout changes
  ui.menuSheet = null;
  ui.handOpen = false;
  ui.stopsOpen = false;
  ui.zonesFor = null;
  relayout();
  schedule();
});
initNotices(model, schedule);
initPress();
initSettings(actions.setSetting, () => {
  applyAudioSettings();
  schedule();
});

function apply(msg: ServerMessage): void {
  switch (msg.t) {
    case 'hello':
      model.host = msg.host;
      setGuest(!model.host);
      setVersion(msg.version);
      // This runs before any game opens, so the game starts with these settings instead of being corrected later
      if (!model.host && restoredFor !== msg.session) {
        restoredFor = msg.session;
        restoreGuestSettings();
        restoreStops(actions.setStops);
      }
      model.canClaimHost = msg.canClaimHost;
      model.hostPassword = msg.hostPassword;
      onServerSettings(msg.settings);
      applyAudioSettings();
      model.screen = msg.screen;
      model.eventPool = msg.eventPool ?? null;
      model.sealedPools = msg.sealedPools;
      model.draftPools = msg.draftPools;
      // A gauntlet result belongs to the game before it. Every new match starts with a hello, so the result is cleared here
      model.limitedResult = null;
      model.eventKind = msg.eventKind ?? null;
      model.drafting = msg.drafting;
      if (!msg.drafting) {
        model.draft = null;
        ui.draftHidden = false;
      }
      // A picker belongs to the setup table it was opened over
      if (model.screen !== 'setup') ui.picker = null;
      model.spectating = msg.spectating;
      model.playerName = msg.playerName ?? '';
      model.nameSent = false;
      if (model.playerName) {
        rememberName(model.playerName);
      } else if (!model.canClaimHost) {
        // While the host seat is free, the name prompt is shown instead, because that prompt offers the seat
        offerRememberedName();
      }
      model.looks = { avatarCount: msg.avatarCount, sleeveCount: msg.sleeveCount };
      model.savedSleeveArt = msg.sleeveArt ?? [];
      model.error = null;
      // A new lobby has its own address. Returning to the same lobby keeps the address
      if (msg.screen !== 'setup') {
        model.addresses = null;
        askedAddresses = false;
      }
      model.networked = msg.networked;
      // The server replays open requests after every hello
      model.requests.clear();
      if (msg.screen !== 'playing') {
        model.objects.clear();
        model.gameOver = false;
        model.matchScore = [];
        // Nothing draws the match after it is left, so a card still waiting for payment would stay drawn over the menu
        resetTable();
        send({ t: 'decks' });
      }
      if (!model.host && !sentDeviceDecks) {
        sentDeviceDecks = true;
        void listDeviceDecks().then(decks => actions.deviceDecks(decks));
      }
      break;
    case 'hostChoice': model.hostChoice = msg; break;
    case 'error':
      // A stored name can belong to someone else on this server. It is forgotten so the player is asked for another
      if (model.nameSent) {
        model.nameSent = false;
        rememberName(null);
      }
      model.error = msg.message;
      break;
    case 'notice': showNotice(msg); break;
  }
  schedule();
}

// After a server restart or on a first visit, the server does not know this browser. The browser sends the name it last used
function offerRememberedName(): void {
  const name = rememberedName();
  if (name) {
    model.nameSent = true;
    send({ t: 'setName', name, avatar: rememberedAvatar(), sleeve: rememberedSleeve() });
  }
}

// Otherwise a file dropped on the page would open in its place and end the session
for (const kind of ['dragover', 'drop']) {
  addEventListener(kind, e => { if ((e as DragEvent).dataTransfer?.types.includes('Files')) e.preventDefault(); });
}

function schedule(): void {
  if (scheduled) return;
  scheduled = true;
  requestAnimationFrame(() => { scheduled = false; render(); });
}

/** The page last drawn under the editor. */
let shownUnder = '';
/** A slide to another page has started but not yet drawn. Its own draw shows every change made meanwhile. */
let sliding = false;

function render(): void {
  if (sliding) return;
  const way = slideWay(shownUnder, pageUnder(model));
  if (!way) {
    draw();
    return;
  }
  sliding = true;
  const slid = () => {
    sliding = false;
    draw();
  };
  transition(slid, slid, () => ({ screen: way }));
}

function draw(): void {
  // This frame shows the events since the last frame, each once
  const events = model.events;
  model.events = [];
  const page = screenOf(model);
  const under = pageUnder(model);
  // The page keeps its scroll position from before the match. Without this, a new table would open scrolled halfway down
  if (shownUnder === 'match' && under !== 'match') window.scrollTo(0, 0);
  const enteringMatch = under === 'match' && shownUnder !== 'match';
  shownUnder = under;
  byId('editor').hidden = page !== 'editor';
  for (const id of ['menu', 'lobby', 'limited', 'drafting', 'match']) {
    const el = byId(id);
    // A phone's editor hides the page under it, and the page that started a match stays drawn until the board is on screen (match/matchentry.ts)
    el.hidden = (id !== (under === 'name' ? 'menu' : under) || (page === 'editor' && isPortrait())) && !el.classList.contains('to-match');
    // On a desktop the page under the editor stays visible but cannot be used
    el.inert = page === 'editor';
  }
  matchHistory(page === 'match');
  void keepAwake(page === 'match' && !model.gameOver);
  renderScreens(model, actions);
  // No music on the name page, because the player's volume settings are not known yet
  playMusic(page === 'name' ? null : page !== 'match' ? 'menu' : model.gameOver ? null : 'match');
  if (model.screen !== 'playing') {
    // Every page's cards use the same preview, so it is drawn when no match is running too
    renderDetail(model);
    return;
  }
  renderSide(model);
  renderSky(model);
  renderMatch(model, actions, events);
  arriveAtMatch(enteringMatch);
  renderPrompt(model, actions);
  renderDetail(model);
  renderCombatDrag(model);
  drawOverlay(model);
}
