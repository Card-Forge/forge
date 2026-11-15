// The phase bar is a pill on the divider. It shows whose turn it is and the five phases. Clicking it opens the grid of phase stops

import { reconcile } from './reconcile';
import { deref, game, isLocal, me, opponents, players, takesTurn, teamOf, type Model } from '../model';
import { playerAvatarUrl } from '../avatarsleeves';
import { byId, q, playOnce, make, onPressOutside, reducedMotion, easeOut, escapeHtml } from '../dom';
import { changeUi, ui } from '../ui';
import { t, tCap, tNodes, type TextKey } from '../text';
import type { Actions } from '../actions';
import type { GameView, PhaseType, TurnMarker } from '../protocol';
import { isPortrait } from '../form';
import { asksPlayer, boardBehind, passingFor } from './pauserules';
import { sheet, swipeDown } from '../sheet';

function star(): string {
  let d = '';
  for (let i = 0; i < 16; i++) {
    const r = i % 2 ? 4.2 : 9.3;
    const a = Math.PI * i / 8 - Math.PI / 2;
    d += `${i ? 'L' : 'M'}${(12 + r * Math.cos(a)).toFixed(2)} ${(12 + r * Math.sin(a)).toFixed(2)}`;
  }
  return `<path d="${d}Z"/>`;
}

const GLYPHS: Record<string, string> = {
  upkeep: '<path d="M3 19h18"/><path d="M7 19a5 5 0 0 1 10 0"/><path d="M12 8.5v2.5M5.6 11.6l1.7 1.7M18.4 11.6l-1.7 1.7"/>',
  draw: '<rect x="4.5" y="7.5" width="10" height="13" rx="1.8"/><path d="M8.5 4.5h8.2a1.8 1.8 0 0 1 1.8 1.8v10.7"/>',
  main1: '<path d="M12 3.5l8.5 8.5-8.5 8.5L3.5 12z"/><circle cx="12" cy="12" r="1.8" fill="currentColor" stroke="none"/>',
  boc: '<path d="M5 5l14 14M19 5L5 19"/><path d="M14.5 18.5l4-4M5.5 14.5l4 4"/>',
  atk: '<path d="M19.5 4.5L8.5 15.5"/><path d="M19.5 4.5H15M19.5 4.5V9"/><path d="M6 13l5 5M8.5 15.5l-4 4"/>',
  blk: '<path d="M12 3.2l7.5 2.8v5.6c0 4.6-3.1 7.9-7.5 9.4-4.4-1.5-7.5-4.8-7.5-9.4V6z"/>',
  fs: '<path d="M13.5 2.8L5.5 13.2h6l-1 8 8-10.4h-6z"/>',
  dmg: star(),
  eoc: '<path d="M12 3v3M8 6h8"/><path d="M10.5 6v8.5l1.5 2.5 1.5-2.5V6"/><path d="M4.5 20.5h15"/>',
  main2: '<path d="M12 3.5l8.5 8.5-8.5 8.5L3.5 12z"/><circle cx="9.4" cy="12" r="1.55" fill="currentColor" stroke="none"/><circle cx="14.6" cy="12" r="1.55" fill="currentColor" stroke="none"/>',
  end: '<path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9z"/>',
  cleanup: '<path d="M20 12a8 8 0 1 1-2.35-5.65L20 9"/><path d="M20 4.5V9h-4.5"/>',
  skip: '<path d="M4.5 6.5l5.5 5.5-5.5 5.5M11 6.5l5.5 5.5-5.5 5.5"/><path d="M19.5 6v12"/>',
  up: '<path d="M6.5 14.5l5.5-5.5 5.5 5.5"/>',
  down: '<path d="M6.5 9.5l5.5 5.5 5.5-5.5"/>',
  wait: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  stop: '<path d="M6.5 6.5l11 11M17.5 6.5l-11 11"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
};
const glyph = (name: string, size: number): string => `<svg class="glyph" viewBox="0 0 24 24" style="width:${size}px;height:${size}px;stroke-width:${(1.3 * 24 / size).toFixed(2)}">${GLYPHS[name]}</svg>`;

// Each step's PhaseType, glyph, full name and short name. Untap is left out because it cannot have a stop
const STEPS: [PhaseType, string, TextKey, TextKey][] = [
  ['UPKEEP', 'upkeep', 'lblWebPhaseUpkeep', 'lblWebPhaseUpkeep'], ['DRAW', 'draw', 'lblWebPhaseDraw', 'lblWebPhaseDraw'],
  ['MAIN1', 'main1', 'lblWebPhaseMain1', 'lblWebPhaseMain1'],
  ['COMBAT_BEGIN', 'boc', 'lblWebPhaseBeginCombat', 'lblCombatTab'],
  ['COMBAT_DECLARE_ATTACKERS', 'atk', 'lblWebPhaseDeclareAttackers', 'lblAttackers'],
  ['COMBAT_DECLARE_BLOCKERS', 'blk', 'lblWebPhaseDeclareBlockers', 'lblBlockers'],
  ['COMBAT_FIRST_STRIKE_DAMAGE', 'fs', 'lblWebPhaseFirstStrikeDamage', 'lblWebPhaseFirstStrike'],
  ['COMBAT_DAMAGE', 'dmg', 'lblWebPhaseCombatDamage', 'lblDamage'], ['COMBAT_END', 'eoc', 'lblWebPhaseEndOfCombat', 'lblWebPhaseEndCombat'],
  ['MAIN2', 'main2', 'lblWebPhaseMain2', 'lblWebPhaseMain2'], ['END_OF_TURN', 'end', 'lblEndStep', 'lblEndStep'],
  ['CLEANUP', 'cleanup', 'lblWebPhaseCleanup', 'lblWebPhaseCleanup'],
];
// A segment covers more steps than the one its glyph is named for, so its name describes all the steps it covers
interface Phase {
  glyph: string;
  /** Shown under the icon when the phase is not the current one. These are desktop's step codes, with CB for all of combat. */
  code: TextKey;
  name: TextKey;
  /** The heading of this phase's group of steps in the stop grid. */
  group: TextKey;
  steps: number[];
}
const PHASES: Phase[] = [
  { glyph: 'upkeep', code: 'lblWebPhaseCodeUpkeep', name: 'lblWebPhaseUpkeepAndDraw', group: 'lblWebPhaseBeginning', steps: [0, 1] },
  { glyph: 'main1', code: 'lblWebPhaseCodeMain1', name: 'lblWebPhaseMain1', group: 'lblWebPhaseMain1', steps: [2] },
  { glyph: 'boc', code: 'lblWebPhaseCodeCombat', name: 'lblCombatTab', group: 'lblCombatTab', steps: [3, 4, 5, 6, 7, 8] },
  { glyph: 'main2', code: 'lblWebPhaseCodeMain2', name: 'lblWebPhaseMain2', group: 'lblWebPhaseMain2', steps: [9] },
  { glyph: 'end', code: 'lblWebPhaseCodeEnd', name: 'lblWebPhaseEndOfTurn', group: 'lblWebPhaseEnding', steps: [10, 11] },
];
/** Short step names for the grid, under the phase heading. A phase with one step shows no step name. Combat steps drop the word "combat". */
const GRID_NAMES: Partial<Record<PhaseType, TextKey>> = {
  COMBAT_BEGIN: 'lblWebPhaseGridBegin', COMBAT_DECLARE_ATTACKERS: 'lblWebPhaseGridAttack',
  COMBAT_DECLARE_BLOCKERS: 'lblWebPhaseGridBlock', COMBAT_END: 'lblWebPhaseGridEnd',
};
const gridName = (i: number): string =>
  PHASES[phaseOf(i)].steps.length === 1 ? '' : t(GRID_NAMES[STEPS[i][0]] ?? STEPS[i][3]);
const stepIndex = (phase: PhaseType | undefined): number => STEPS.findIndex(s => s[0] === phase);
export const stepName = (phase: PhaseType | undefined): string => t(STEPS[stepIndex(phase)]?.[2] ?? 'lblWebPhaseUntap');
/** A step's full name, or nothing for a phase no step is named for. */
const fullName = (phase: PhaseType): string => {
  const s = STEPS[stepIndex(phase)];
  return s ? t(s[2]) : '';
};
const phaseOf = (step: number): number => PHASES.findIndex(p => p.steps.includes(step));
let wired = false;
let stopYield: (() => void) | null = null;

export function renderPhaseBar(model: Model, g: GameView, actions: Actions): void {
  const open = ui.stopsOpen;
  const root = byId('phase-strip');
  if (!wired) {
    build(root);
    wired = true;
  }
  const active = deref(model, g.PlayerTurn);
  const myTurn = takesTurn(me(model), g.PlayerTurn);
  const theirs = whoseTurns(model);
  const step = stepIndex(g.Phase);
  // Untap (index -1) belongs to the beginning phase
  const phase = step < 0 ? 0 : phaseOf(step);
  const pill = q(root, '.pill');
  // A team that shares its turns is named together, with both portraits
  const team = active ? teamOf(model, active) : [];
  q(pill, '.owner').querySelectorAll<HTMLImageElement>('img').forEach((portrait, i) => {
    const avatar = team[i] ? playerAvatarUrl(team[i]) : '';
    portrait.hidden = !avatar;
    if (avatar) portrait.src = avatar;
  });
  const activeName = team.length > 1 ? t('lblWebPhaseTeamNames', team[0].Name ?? '', team[1].Name ?? '') : active?.Name ?? '';
  swapOwner(q(pill, '.owner'), myTurn ? t(team.length > 1 ? 'lblWebPhaseYourTeamsTurn' : 'lblWebPhaseYourTurn') : activeName);
  // The server sends the engine's English name for the time of day
  const dayTime = model.controls?.dayTime;
  const time = dayTime === 'Day' ? t('lblDay') : dayTime === 'Night' ? t('lblNight') : dayTime;
  q(pill, '.owner .turn').textContent = time ? t('lblWebPhaseTurnAndTime', g.Turn ?? 0, time) : t('lblWebPhaseTurnNumber', g.Turn ?? 0);
  drawTrack(pill, model, step, phase, myTurn, actions);
  slideTo(q(pill, '.track'), phase, step);
  drawWaiting(pill, model);
  drawUntil(pill, model, myTurn, theirs, activeName);
  stopYield = actions.stopYield;
  const opens = myTurn ? 'up' : 'down';
  // This runs on every render, so the markup is replaced only when it changes
  if (pill.dataset.opens !== opens) {
    q(pill, '.caret').innerHTML = glyph(opens, 12);
    pill.dataset.opens = opens;
  }
  pill.classList.toggle('open', open);
  pill.classList.toggle('priority', !!me(model)?.HasPriority);

  const panel = q(root, '.stops');
  panel.hidden = !open;
  panel.classList.toggle('sheet', isPortrait());
  sheet('stops', isPortrait() && open, () => changeUi(u => { u.stopsOpen = false; }));
  // The panel opens on the side away from the active player, so their half of the board stays visible
  panel.classList.toggle('above', myTurn);
  if (open) {
    const grid = stopsGrid(model, step, myTurn, theirs);
    if (panel.dataset.grid !== grid) {
      panel.dataset.grid = grid;
      panel.innerHTML = grid;
      wireGrid(panel, actions);
    }
  }
}

// The pill is built once and updated in place, so the highlight can slide from one phase to the next
function build(root: HTMLElement): void {
  root.innerHTML = `<div class="pill" role="button" tabindex="0" title="${escapeHtml(t('lblWebPhaseStops'))}"></div><div class="stops" hidden></div>`;
  const pill = q(root, '.pill');
  // Every segment has the same markup. Only the current one shows its name and pips, so nothing jumps when the phase changes
  const track = PHASES.map(p => `<span class="phase"><span class="mark">${glyph(p.glyph, 12)}<span class="code">${escapeHtml(t(p.code))}</span></span><span class="label"></span><span class="pips"></span></span>`).join('');
  pill.innerHTML = `<span class="owner"><img alt="" hidden><img class="mate" alt="" hidden><b></b><span class="turn"></span></span>`
    + `<span class="track"><span class="slide" aria-hidden="true"></span>${track}</span>`
    + `<span class="waiting" hidden>${glyph('wait', 11)}<span class="who"></span><b></b></span>`
    + `<span class="until" hidden>${glyph('skip', 12)}<span class="verb"></span><span class="text"></span><span class="stop">${glyph('stop', 10)}</span></span>`
    + '<span class="caret"></span>';
  pill.onclick = () => changeUi(u => { u.stopsOpen = !u.stopsOpen; });
  // Clicking the chip for an active pass stops the pass. It does not open the stops grid
  q(pill, '.until').onclick = e => {
    e.stopPropagation();
    stopYield?.();
  };
  onPressOutside('#phase-strip', () => { if (ui.stopsOpen) changeUi(u => { u.stopsOpen = false; }); });
  swipeDown(q(root, '.stops'), () => changeUi(u => { u.stopsOpen = false; }));
}

function drawTrack(pill: HTMLElement, model: Model, step: number, phase: number, myTurn: boolean, actions: Actions): void {
  const marker = model.controls?.marker;
  pill.querySelectorAll<HTMLElement>('.track .phase').forEach((el, n) => {
    const p = PHASES[n];
    const current = n === phase;
    el.classList.toggle('current', current);
    el.classList.toggle('marked', !current && !!marker && marker.mine === myTurn && p.steps.some(i => STEPS[i][0] === marker.phase));
    // Right-click passes priority until this phase, as on desktop. The marker is set on the segment's first step
    el.title = t('lblWebPhaseSegmentTitle', t(p.name));
    el.oncontextmenu = e => {
      e.preventDefault();
      actions.toggleMarker(STEPS[p.steps[0]][0], myTurn);
    };
    const shown = current && step >= 0 ? STEPS[step][1] : p.glyph;
    if (el.dataset.glyph !== shown) {
      el.dataset.glyph = shown;
      q(el, '.glyph').outerHTML = glyph(shown, 12);
    }
    q(el, '.label').textContent = current ? t(step < 0 ? 'lblWebPhaseUntap' : STEPS[step][3]) : '';
    const pips = q(el, '.pips');
    // A phase that is no longer current keeps its last pips while they animate closed
    if (current) {
      drawPips(pips, p, step, marker, myTurn);
    }
  });
}

/** The pill's animations match its CSS transitions (--pill-ms in board.css) in length and easing. */
const PILL_MS = 240;

/** The brass highlight behind the current phase is one element that slides between segments. A CSS gradient cannot transition from one segment to the next. */
let slideKey = '';
let slideAt = { left: 0, width: 0 };
let sliding = 0;
function slideTo(track: HTMLElement, phase: number, step: number): void {
  const key = `${phase}:${step}`;
  if (key === slideKey) return;
  const first = !slideKey;
  slideKey = key;
  const slide = q(track, '.slide');
  const target = () => {
    const r = track.querySelectorAll<HTMLElement>('.phase')[phase].getBoundingClientRect();
    return { left: r.left - track.getBoundingClientRect().left, width: r.width };
  };
  const place = (r: { left: number; width: number }) => {
    slideAt = r;
    slide.style.transform = `translateX(${r.left}px)`;
    slide.style.width = `${r.width}px`;
  };
  cancelAnimationFrame(sliding);
  const start = performance.now();
  const from = slideAt;
  const frame = () => {
    const elapsed = performance.now() - start;
    const to = target();
    // On the first placement there is no start position, so the highlight only tracks the segment as it widens to fit its name
    const k = first || reducedMotion() ? 1 : easeOut(Math.min(1, elapsed / PILL_MS));
    place({ left: from.left + (to.left - from.left) * k, width: from.width + (to.width - from.width) * k });
    // Run a little past the slide, so the highlight ends at the segment's final width
    if (elapsed < PILL_MS + 60) sliding = requestAnimationFrame(frame);
  };
  frame();
}

/** The new turn owner's name fades in while the chip animates to its new width. This stops the pill jumping sideways. */
function swapOwner(owner: HTMLElement, name: string): void {
  const b = q(owner, 'b');
  if (b.textContent === name) return;
  const before = owner.offsetWidth;
  const had = !!b.textContent;
  b.textContent = name;
  if (!had || reducedMotion()) return;
  const after = owner.offsetWidth;
  const timing = { duration: PILL_MS, easing: 'cubic-bezier(.2,.7,.2,1)' };
  owner.animate([{ width: `${before}px` }, { width: `${after}px` }], timing);
  for (const part of owner.children) part.animate([{ opacity: 0 }, { opacity: 1 }], timing);
}

function drawPips(root: HTMLElement, phase: Phase, step: number, marker: TurnMarker | undefined, myTurn: boolean): void {
  reconcile(root, phase.steps, i => i,
    () => {
      const el = make('span', 'pip');
      return el;
    },
    (el, i) => {
      el.classList.toggle('past', i < step);
      el.classList.toggle('now', i === step);
      el.classList.toggle('marked', !!marker && marker.mine === myTurn && marker.phase === STEPS[i][0]);
    });
}

/** The label for the opponents' row. It names the opponent when there is one, or covers all opponents together. */
interface Theirs {
  turns: string;
  /** The one opponent's name, when there is only one. */
  one: string | null;
}

function whoseTurns(model: Model): Theirs {
  const opponent = opponents(model);
  if (opponent.length === 1) {
    const name = opponent[0].Name ?? '';
    return { turns: t('lblWebPhasePlayersTurns', name), one: name };
  }
  return { turns: t('lblWebPhaseOpponentsTurns'), one: null };
}

// Only one yield runs at a time. The chip names the active yield and where it stops
function drawUntil(pill: HTMLElement, model: Model, myTurn: boolean, theirs: Theirs, activeName: string): void {
  const controls = model.controls;
  const marker = controls?.marker;
  let text = '';
  if (marker) {
    const name = fullName(marker.phase);
    text = marker.mine === myTurn ? t('lblWebPhaseUntilStep', name)
      : marker.mine ? t('lblWebPhaseUntilYourStep', name)
      : theirs.one !== null ? t('lblWebPhaseUntilPlayersStep', theirs.one, name)
      : t('lblWebPhaseUntilOpponentsStep', name);
  } else if (controls?.untilEndOfTurn) {
    text = myTurn ? t('lblWebPhaseUntilEndOfYourTurn') : t('lblWebPhaseUntilEndOfPlayersTurn', activeName);
  } else if (controls?.untilStackEmpty) {
    text = t('lblWebPhaseUntilStackClears');
  }
  const until = q(pill, '.until');
  until.hidden = !text;
  until.title = text ? t('lblWebPhasePassingPriority', text) : '';
  q(until, '.verb').textContent = t('lblWebPhasePassing');
  q(until, '.text').textContent = text;
}

// Who the game is waiting on, and since when. It is another player, or you when another person is at the table
let waitingFor: number | null = null;
let waitingSince = 0;
let waitingTick = 0;
let waitingTimer = 0;
let lastPulse = 0;

/** How long another player must be deciding before the pill says who the game is waiting on. */
const WAIT_SHOWN_AFTER_S = 2;
/** How long you must hold priority before the pill says others are waiting on you. A quick pass never shows it. */
const YOUR_WAIT_SHOWN_AFTER_S = 5;
/** How often the chip pulses while others wait on you. The pulse stays the same strength each time. */
const PULSE_EVERY_S = 20;

export function stopWaiting(): void {
  waitingFor = null;
  clearInterval(waitingTimer);
  waitingTimer = 0;
}

function drawWaiting(pill: HTMLElement, model: Model): void {
  const mine = me(model);
  // The game waits on you when you have priority, a declaration to make or a choice to answer
  const onMe = !!(mine?.HasPriority || (model.prompt && asksPlayer(model.prompt, passingFor(model.controls))) || model.requests.size > 0);
  // Your own wait is shown only when another person is in the game, not against only AI players
  const people = players(model).filter(p => !isLocal(model, p) && !p.IsAI && !p.HasLost);
  // The engine sets HasPriority only after priority has passed once. At the start of a step, use the turn's player, or the only other player
  const turn = players(model).find(p => p.$key === game(model)?.PlayerTurn?.ref);
  const left = opponents(model).filter(p => !p.HasLost);
  const other = players(model).find(p => p.HasPriority && !isLocal(model, p))
    ?? (turn ? (isLocal(model, turn) ? undefined : turn) : left.length === 1 ? left[0] : undefined);
  // A finished game waits on nobody, though its board stays open to look at
  const holder = model.gameOver ? undefined : onMe ? (people.length ? mine : undefined) : other;
  const chip = q(pill, '.waiting');
  chip.hidden = !holder;
  if (!holder) {
    stopWaiting();
    return;
  }
  if (waitingFor !== holder.$key) {
    waitingFor = holder.$key;
    waitingSince = waitingTick = Date.now();
    lastPulse = 0;
  }
  chip.classList.toggle('you', onMe);
  // An element, not a string, so the sentence keeps it as a separate bold node
  const you = document.createElement('strong');
  you.textContent = tCap('lblYou');
  const words = onMe
    ? (people.length === 1 ? tNodes('lblWebPhaseWaitingOnYou', people[0].Name ?? '', you) : tNodes('lblWebPhasePlayersWaitingOnYou', people.length, you))
    : [t('lblWebPhaseWaitingOn', holder.Name ?? '')];
  q(chip, '.who').replaceChildren(...words.map(w => (w instanceof Node ? w : String(w))));
  // A short wait shows no chip. A chip that first appeared at 0s looked stuck
  const show = () => {
    const now = Date.now();
    // While the page shows boards it held back for pacing, nobody is being waited on. That time is not counted
    const behind = boardBehind();
    if (behind) waitingSince += now - waitingTick;
    waitingTick = now;
    const seconds = Math.floor((now - waitingSince) / 1000);
    const yours = chip.classList.contains('you');
    chip.hidden = behind || seconds < (yours ? YOUR_WAIT_SHOWN_AFTER_S : WAIT_SHOWN_AFTER_S);
    q(chip, 'b').textContent = t('lblWebOptionsSeconds', seconds);
    // Your wait pulses once every PULSE_EVERY_S. `replay` restarts the animation by removing and re-adding the class
    if (yours && seconds >= PULSE_EVERY_S && seconds % PULSE_EVERY_S === 0 && lastPulse !== seconds) {
      lastPulse = seconds;
      playOnce(chip, 'pulse');
    }
  };
  show();
  if (!waitingTimer) {
    waitingTimer = setInterval(show, 500);
  }
}

function stopsGrid(model: Model, step: number, myTurn: boolean, theirs: Theirs): string {
  const marker = model.controls?.marker;
  // Your own turns always head the grid, whoever's turn it is now
  const rows = [
    { mine: true, label: escapeHtml(t('lblWebPhaseYourTurns')), stops: new Set(model.controls?.myStops ?? []), now: myTurn },
    { mine: false, label: escapeHtml(theirs.turns), stops: new Set(model.controls?.otherStops ?? []), now: !myTurn },
  ];
  const gap = (i: number) => (i > 0 && phaseOf(i) !== phaseOf(i - 1)) ? '<td class="gap"></td>' : '';
  // Each phase has a bracketed heading that spans its steps
  const groups = '<tr class="groups"><td></td>' + PHASES.map((p, g) =>
    `${g ? '<td class="gap"></td>' : ''}<th class="group" colspan="${p.steps.length}"><span>${escapeHtml(t(p.group))}</span></th>`).join('') + '</tr>';
  const head = '<tr><td></td>' + STEPS.map((s, i) => `${gap(i)}<th class="${i === step ? 'now' : ''}" title="${escapeHtml(t(s[2]))}"><span class="head">${glyph(s[1], 13)}</span><span class="name">${escapeHtml(gridName(i))}</span></th>`).join('') + '</tr>';
  // The row of the player whose turn it is is highlighted. The column of the current step is highlighted the same way
  const body = rows.map(r => `<tr class="${r.now ? 'active' : ''}">` + `<td class="who">${r.label}</td>` + STEPS.map((s, i) => {
    const marked = marker && marker.mine === r.mine && marker.phase === s[0];
    const on = r.stops.has(s[0]);
    const title = t(on ? 'lblWebPhaseCellTitleClear' : 'lblWebPhaseCellTitleStop', t(s[2]), r.mine ? t('lblWebPhaseYourTurns') : theirs.turns);
    const cell = marked ? `<span class="skip">${glyph('skip', 13)}</span>` : `<span class="square ${on ? 'on' : ''} ${r.now && i === step ? 'current' : ''}">${on ? glyph('check', 11) : ''}</span>`;
    return `${gap(i)}<td class="${i === step ? 'now' : ''}"><button class="cell" data-phase="${s[0]}" data-mine="${r.mine}" title="${escapeHtml(title)}">${cell}</button></td>`;
  }).join('') + `<td class="row-tools"><button class="row-tool" data-clear data-mine="${r.mine}">${escapeHtml(t('lblWebOptionsClear'))}</button>`
    + `<button class="row-tool" data-defaults data-mine="${r.mine}">${escapeHtml(t('lblWebPhaseDefaults'))}</button></td></tr>`).join('');
  // Touch has no right-click, so pass-until-phase gets labelled buttons. Only a phone shows them
  const until = `<div class="until-row"><span>${escapeHtml(t('lblWebPortraitPassUntil'))}</span>`
    + PHASES.map(p => `<button data-phase="${STEPS[p.steps[0]][0]}" data-mine="${myTurn}" class="${marker && marker.mine === myTurn && p.steps.some(i => STEPS[i][0] === marker.phase) ? 'on' : ''}">${escapeHtml(t(p.name))}</button>`).join('')
    + '</div>';
  return `<div class="title">${escapeHtml(t('lblWebPhaseStops'))}</div><div class="grid-scroll"><table>${groups}${head}${body}</table></div>${until}`
    + `<div class="hint">${escapeHtml(t('lblWebPhaseStopsHint'))}</div>`;
}

function wireGrid(panel: HTMLElement, actions: Actions): void {
  for (const b of panel.querySelectorAll<HTMLElement>('.cell')) {
    // The cell was drawn from STEPS, so its phase is one of them
    const phase = b.dataset.phase as PhaseType;
    const mine = b.dataset.mine === 'true';
    b.onclick = () => actions.toggleStop(phase, mine);
    b.oncontextmenu = e => {
      e.preventDefault();
      actions.toggleMarker(phase, mine);
    };
  }
  for (const b of panel.querySelectorAll<HTMLElement>('.until-row button')) {
    b.onclick = () => actions.toggleMarker(b.dataset.phase as PhaseType, b.dataset.mine === 'true');
  }
  for (const b of panel.querySelectorAll<HTMLElement>('.row-tool')) {
    const mine = b.dataset.mine === 'true';
    b.onclick = () => actions.setStops(mine, b.dataset.clear !== undefined ? [] : undefined);
  }
}
