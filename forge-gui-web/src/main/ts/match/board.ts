// Lays out the whole match screen and redraws it from the model. Each piece is drawn by its own module

import { reconcile } from './reconcile';
import { cardTypes } from './thresholds';
import { cardImageSrc, hideOnError, setImage, symbolUrl } from '../images';
import { commanderTax, game, me, players, zone, deref, stackPick, stateOf, isLocal, type Model, offersPicks, takesTurn, sharesTurns, isTeammate } from '../model';
import { fanOut, renderHand, resetHand } from './hand';
import { renderZones, togglePile, zoneTitle } from './zones';
import { renderBattlefield } from './battlefield';
import { watchBlockTap } from './combatdrag';
import { followPointer, hoverPlayer, hoverable, inspectCard, inspectPlayer } from '../preview';
import { foldStack, renderStack, resetStack } from './stack';
import { renderPlanes } from './planes';
import { isArchenemy, renderOngoing, resetSchemes, revealSchemes } from './schemes';
import { renderPhaseBar, stopWaiting } from './phasebar';
import { createCard, forgetPictures, updateCard, type CardClick } from './cards';
import { playerAvatarUrl, playerSleeveUrl, cssUrl, ROBOT_ICON } from '../avatarsleeves';
import { animateCardMoves, noteBoard, resetMotion, showCount } from './motion';
import { boardBehind } from './pauserules';
import { byId, q, playOnce, replay, make, onPressOutside, relayout } from '../dom';
import { setting } from '../settings';
import { logTints } from './log';
import type { Actions } from '../actions';
import type { CardView, GameEvent, PlayerView, ZoneType } from '../protocol';
import { commandKind, renderEmblems } from './command';
import { notePick, resetOverlay } from './overlay';
import { resetStarter, showStarter, starterShowing, wentFirst } from './firstplayer';
import { announceTurn, resetTurnBanner } from './turnbanner';
import { noticeLosses, renderGameOver, resetGameOver } from './gameover';
import { hovers, isTouch, longPress } from '../press';
import { fieldChanged, repeatTap, seatToOpen, tapInspects } from './portrait';
import { isPortrait } from '../form';
import { sheet, swipeDown } from '../sheet';
import { changeUi, ui } from '../ui';
import { t, tCap, type TextKey } from '../text';
import { ICONS, iconSvg } from '../icons';
import { COLOUR_BITS, COMMANDER_LETHAL, COMMANDER_WARNING } from './gamerules';

// The Mana property counts the pool by Forge's mana bit (ManaAtom). The five colours use MagicColor's bits, and colourless has its own bit
const MANA: [number, string][] = [...COLOUR_BITS, [32, 'C']];

const SWORDS = iconSvg(ICONS.swords);

export function renderMatch(model: Model, actions: Actions, events: readonly GameEvent[]): void {
  const g = game(model);
  if (!g) return;
  // Called before anything is redrawn, so each card's move is measured from its old position on screen
  noteBoard();
  showStarter(model, actions, events);
  // The opening hand, the mulligan prompt and the phase pill wait until who goes first has been shown (board.css, motion.ts)
  byId('match').classList.toggle('starting', !g.Turn && starterShowing());
  noticeLosses(model, actions);
  // A prompt that offers cards or players to pick, or a spell to pick on the stack, dims everything else (board.css). Paying a cost does not count
  const p = model.prompt;
  byId('match').classList.toggle('picking', !!stackPick(model) || (!!p && !p.paying && offersPicks(p)));
  // The click position is sent with the click, so an ability list opens on the card, as the desktop menu does
  const select: CardClick = (el, menu, e) => {
    const key = Number(el.dataset.key);
    if (isTouch(e) && !menu) {
      if (repeatTap(key, performance.now())) return;
      // Read at the time of the tap. A card keeps the handler it was created with, long after the prompt of that time has gone
      const card = model.objects.get(key) as CardView | undefined;
      const tapped = { playable: el.classList.contains('playable'), selectable: el.classList.contains('selectable'),
        mine: card?.Controller?.ref === me(model)?.$key, attacking: !!card?.Attacking };
      if (tapInspects(tapped, byId('match').classList.contains('picking'), game(model)?.Phase)) {
        inspectCard(el);
        return;
      }
      // Shown at once, because the server's answer can take a moment. Not shown while newer boards are queued, because the click is not sent then
      if (!boardBehind()) playOnce(el, 'pressed');
    }
    notePick(key);
    foldStack();
    ui.cardClickedAt = e ? { x: e.clientX, y: e.clientY } : null;
    actions.selectCard(key, menu, e?.clientX ?? 0, e?.clientY ?? 0);
    if (!menu && el.closest('#me .group.creatures')) {
      const box = el.getBoundingClientRect();
      watchBlockTap(key, { x: box.left + box.width / 2, y: box.top });
    }
  };
  // An attachment can be on another player's card (an aura on an opponent's creature), so slots are built from every battlefield
  const onField = players(model).flatMap(p => zone(model, p, 'Battlefield'));
  renderStack(model, events);
  renderOpponents(byId('opponent'), model, onField, actions, select);
  renderSeat(byId('me'), model, me(model), onField, actions, select);
  renderOut(model, actions);
  announceTurn(model, g);
  renderPhaseBar(model, g, actions);
  renderPlanes(model, actions);
  revealSchemes(model);
  renderHand(model, me(model), select);
  renderZones(model, actions, select);
  renderGameOver(model, g, actions);
  animateCardMoves(model, events);
}

/** The seat order. It is set once when the game is first drawn, so a card that reverses the turn order does not move any seat. */
let seating: number[] | null = null;

function seated(model: Model): PlayerView[] {
  const everyone = players(model);
  if (!seating) {
    const mine = everyone.findIndex(p => isLocal(model, p));
    const from = Math.max(0, mine);
    seating = [...everyone.slice(from), ...everyone.slice(0, from)].map(p => p.$key);
  }
  for (const p of everyone) {
    if (!seating.includes(p.$key)) seating.push(p.$key);
  }
  const byKey = new Map(everyone.map(p => [p.$key, p]));
  const across = seating.map(k => byKey.get(k)).filter((p): p is PlayerView => !!p && !isLocal(model, p));
  // A teammate sits last, which in quadrants is the quarter beside yours
  return sharesTurns(model) ? [...across.filter(p => !isTeammate(model, p)), ...across.filter(p => isTeammate(model, p))] : across;
}

/**
 * One opponent gets the top half as a full seat. Two or three share it in columns or quarters, unless the tabs layout is chosen. Four or more opponents, and a
 * phone, always get tabs. Teams that share their turns always get quarters, so your teammate sits beside you.
 */
function renderOpponents(root: HTMLElement, model: Model, onField: CardView[], actions: Actions, select: CardClick): void {
  const across = seated(model);
  const many = across.length > 1;
  const pairs = sharesTurns(model);
  const tabbed = many && (isPortrait() || across.length > 3 || (!pairs && setting('boardLayout') === 'tabs'));
  const quads = many && !tabbed && (pairs || setting('boardLayout') === 'quadrants');
  const match = byId('match');
  match.classList.toggle('many', many);
  match.classList.toggle('quads', quads);
  match.dataset.opponents = String(across.length);
  if (root.classList.contains('many') !== many) {
    root.replaceChildren();
    delete root.dataset.player;
    root.classList.toggle('many', many);
    root.classList.toggle('seat', !many);
  }
  if (!many) {
    renderSeat(root, model, across[0], onField, actions, select);
    renderSeatTabs(root, model, [], []);
    return;
  }
  // Each opponent's seat uses the colour of their name in the log
  const tints = logTints(players(model).map(p => ({ name: p.Name ?? '', local: isLocal(model, p) })));
  reconcile(root, across, p => p.$key,
    () => {
      const el = make('section', 'seat compact');
      return el;
    },
    (el, p) => {
      // A tabbed seat on desktop has the top half to itself. So it uses the two-player layout, with cards at full size
      // In quarters your teammate's seat is drawn like yours
      const mate = quads && isTeammate(model, p);
      el.classList.toggle('compact', (!tabbed && !mate) || isPortrait());
      el.classList.toggle('mate', mate);
      el.style.setProperty('--tint', tints.find(t => t.name === p.Name)?.colour ?? 'var(--muted)');
      renderSeat(el, model, p, onField, actions, select);
    });
  renderSeatTabs(root, model, tabbed ? across : [], tints);
}

/** The seat shown last. When the open seat changes, the new seat slides in and the board is laid out again. */
let shownSeat: number | null = null;

/** One opponent's seat shows at a time. Each opponent has a tab that shows whose turn it is and whether their board changed while hidden. A phone lists the tabs in a sheet that its players button opens. */
function renderSeatTabs(root: HTMLElement, model: Model, across: PlayerView[], tints: { name: string; colour: string }[]): void {
  const match = byId('match');
  let tabs = document.getElementById('seat-tabs');
  let button = document.getElementById('seat-switch');
  if (!tabs || !button) {
    tabs = Object.assign(document.createElement('div'), { id: 'seat-tabs' });
    tabs.setAttribute('role', 'tablist');
    tabs.setAttribute('aria-label', t('lblWebSetupOpponents'));
    swipeDown(tabs, () => changeUi(u => { u.seatsOpen = false; }));
    root.before(tabs);
    button = Object.assign(document.createElement('button'), { id: 'seat-switch', title: t('lblWebSetupOpponents'), innerHTML: PLAYERS_ICON });
    button.onclick = () => changeUi(u => { u.seatsOpen = !u.seatsOpen; });
    match.append(button);
  }
  const tabbed = across.length >= 2;
  const listed = tabbed && isPortrait();
  tabs.hidden = !tabbed || (listed && !ui.seatsOpen);
  tabs.classList.toggle('sheet', listed);
  button.hidden = !listed;
  sheet('seats', listed && ui.seatsOpen, () => changeUi(u => { u.seatsOpen = false; }));
  root.classList.toggle('tabbed', tabbed);
  match.classList.toggle('tabbed', tabbed);
  if (!tabbed) {
    if (shownSeat !== null) {
      for (const seat of root.querySelectorAll<HTMLElement>(':scope > .seat')) seat.hidden = false;
      shownSeat = null;
    }
    return;
  }
  const g = game(model);
  const p = model.prompt;
  let seats: Map<number, number> | null = null;
  const seatOf = (key: number) => {
    if (!seats) {
      seats = new Map();
      for (const card of document.querySelectorAll<HTMLElement>('#opponent .card[data-key]')) {
        const key = Number(card.dataset.key);
        if (!seats.has(key)) seats.set(key, Number(card.closest<HTMLElement>('.seat')?.dataset.player));
      }
    }
    return seats.get(key) ?? NaN;
  };
  const asked = new Set([...(p?.selectable ?? []).map(r => seatOf(r.ref)), ...(p?.highlighted ?? []).map(seatOf), ...(p?.selectablePlayers ?? []).map(r => r.ref)]
    .filter(k => across.some(o => o.$key === k)));
  const open = seatToOpen({ open: ui.openSeat, seats: across.map(o => o.$key), active: g?.PlayerTurn?.ref ?? null, turn: g?.Turn ?? 0, chosenTurn: ui.seatChosenTurn });
  ui.openSeat = open;
  const tintOf = (o: PlayerView) => tints.find(x => x.name === o.Name)?.colour ?? 'var(--muted)';
  match.style.setProperty('--open-tint', tintOf(across.find(o => o.$key === open) ?? across[0]));
  let changedOutOfSight = false;
  reconcile<PlayerView, HTMLButtonElement>(tabs, across, o => o.$key,
    () => {
      const el = make('button', 'seat-tab');
      el.setAttribute('role', 'tab');
      el.innerHTML = `<span class="pic"><img alt="" draggable="false"><i class="dot" aria-hidden="true"></i><span class="skull-mark">${SKULL}</span></span><span class="who"></span><span class="note"></span><b class="life"></b>`;
      // A tab only shows a seat. To pick the player, the portrait inside the seat is clicked
      el.onclick = () => changeUi(u => {
        u.openSeat = Number(el.dataset.key);
        u.seatChosenTurn = Number(el.dataset.turn);
        u.seatsOpen = false;
      });
      return el;
    },
    (el, o) => {
      const shown = o.$key === open;
      const field = zone(model, o, 'Battlefield').map(c => c.$key);
      if (shown) ui.seenOnField.set(o.$key, new Set(field));
      const changed = !shown && fieldChanged(ui.seenOnField.get(o.$key), field);
      changedOutOfSight ||= changed;
      const turn = takesTurn(o, g?.PlayerTurn);
      el.dataset.turn = String(g?.Turn ?? 0);
      el.title = o.Name ?? '';
      el.classList.toggle('on', shown);
      el.setAttribute('aria-selected', String(shown));
      el.classList.toggle('turn', turn);
      el.classList.toggle('out', !!o.HasLost);
      el.classList.toggle('changed', changed);
      el.classList.toggle('asked', !shown && asked.has(o.$key));
      el.style.setProperty('--tint', tintOf(o));
      setImage(q<HTMLImageElement>(el, 'img'), playerAvatarUrl(o));
      q(el, '.life').textContent = String(o.Life ?? 0);
      q(el, '.who').textContent = o.Name ?? '';
      q(el, '.note').textContent = o.HasLost ? t('lblWebBoardOutOfGame') : turn ? t('lblWebSeatTheirTurn') : '';
    });
  // The players button carries the marks of every hidden seat
  button.classList.toggle('changed', changedOutOfSight);
  button.classList.toggle('asked', [...asked].some(k => k !== open));
  for (const seat of root.querySelectorAll<HTMLElement>(':scope > .seat')) seat.hidden = Number(seat.dataset.player) !== open;
  if (shownSeat !== open) {
    // The seat slides in from the side its tab is on, so the change looks like a step along the row of tabs
    const order = across.map(o => o.$key);
    const seat = root.querySelector<HTMLElement>(`:scope > .seat[data-player="${open}"]`);
    if (seat && shownSeat !== null) {
      const later = order.indexOf(open as number) > order.indexOf(shownSeat);
      replay(seat, later ? 'from-right' : 'from-left', 'from-right', 'from-left');
      // The classes stop the cards resizing, so they go when the slide itself ends, not another animation in the seat
      seat.addEventListener('animationend', function slid(e) {
        if (!e.animationName.startsWith('seat-from-')) return;
        seat.removeEventListener('animationend', slid);
        seat.classList.remove('from-right', 'from-left');
      });
    }
    shownSeat = open;
    // A seat that was hidden had no size to fit its cards to
    relayout();
  }
}

const PLAYERS_ICON = `${iconSvg(ICONS.users)}<i class="dot" aria-hidden="true"></i>`;

/** Whether the banner for a player out of a game that continues was shown. It shows once, and offers a leave button only in a local game. */
let outSaid = false;

function renderOut(model: Model, actions: Actions): void {
  const mine = me(model);
  const out = !!mine?.HasLost && !model.gameOver;
  let banner = document.getElementById('out-banner');
  if (!out) {
    banner?.remove();
    if (!mine?.HasLost) outSaid = false;
    return;
  }
  if (banner || outSaid) return;
  outSaid = true;
  banner = document.createElement('div');
  banner.id = 'out-banner';
  banner.setAttribute('role', 'status');
  banner.innerHTML = `<span class="skull">${SKULL}</span><div><b></b><p></p></div>`
    + '<button class="watch"></button>' + (model.networked ? '' : '<button class="leave primary"></button>');
  q(banner, 'b').textContent = t('lblWebBoardYoureOut');
  q(banner, 'p').textContent = t('lblWebBoardGameGoesOn');
  q(banner, '.watch').textContent = t('lblWebBoardKeepWatching');
  q(banner, '.watch').onclick = () => banner?.remove();
  const leave = banner.querySelector<HTMLButtonElement>('.leave');
  if (leave) {
    leave.textContent = t('lblWebBoardLeaveMatch');
    leave.onclick = () => {
      actions.quitMatch();
      actions.leave();
    };
  }
  byId('match').append(banner);
}

const SKULL = iconSvg(ICONS.skull);

function renderSeat(root: HTMLElement, model: Model, player: PlayerView | undefined, onField: CardView[], actions: Actions, select: CardClick): void {
  if (!player) {
    root.replaceChildren();
    return;
  }
  if (!root.firstChild) {
    root.innerHTML = `
      <div class="player">
        <div class="player-id">
        <div class="avatar"><img class="portrait" alt="" draggable="false"><span class="initial"></span><span class="skull-mark">${SKULL}</span><span class="ai-badge">${ROBOT_ICON}</span><span class="cmdr-arc" hidden><svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="48"/></svg></span><span class="cmdr-chip" hidden>${SWORDS}<b></b></span><span class="life"></span></div>
        <div class="name"><span class="who"></span><span class="role-tag" hidden></span></div>
        <button class="hand-fan" hidden><span class="backs"><i></i><i></i><i></i></span><span class="hand-count"></span></button>
        </div>
        <button class="zones-pill"></button>
        <div class="zone-tiles"></div>
        <div class="player-extra">
        <div class="player-counters"></div>
        <div class="emblems"></div>
        <div class="schemes-ongoing"></div>
        <div class="mana" hidden><span class="mana-label"></span><div class="mana-chips"></div></div>
        </div>
      </div>
      <div class="battlefield">
        <div class="row paired"><div class="group lands"></div><div class="group support"></div></div>
        <div class="row together"><div class="group creatures"></div><div class="group far"></div></div>
      </div>`;
    q(root, '.skull-mark').title = t('lblWebBoardOutOfGame');
    q(root, '.ai-badge').title = t('lblWebBoardComputerPlayer');
    q(root, '.role-tag').textContent = t('lblArchenemy');
    q(root, '.mana-label').textContent = t('lblWebBoardFloatingMana');
    const avatarEl = q(root, '.avatar');
    // The text of the mark that board.css draws on the portrait of the player who goes first
    avatarEl.dataset.firstLabel = t('lblWebBoardFirst');
    avatarEl.onclick = () => {
      notePick(Number(root.dataset.player));
      actions.selectPlayer(Number(root.dataset.player));
    };
    avatarEl.addEventListener('pointerenter', e => {
      if (!hovers(e)) return;
      followPointer(e);
      hoverPlayer(Number(root.dataset.player));
    });
    avatarEl.addEventListener('pointermove', e => { if (hovers(e)) followPointer(e); });
    avatarEl.addEventListener('pointerleave', e => { if (hovers(e)) hoverPlayer(null); });
    avatarEl.addEventListener('pointerdown', e => longPress(e, () => inspectPlayer(Number(root.dataset.player))));
    q(root, '.hand-fan').onclick = () => togglePile(Number(root.dataset.player), 'Hand');
    const pill = q(root, '.zones-pill');
    pill.innerHTML = PILL_ZONES.map(([zoneName, icon]) => `<span data-zone="${zoneName}"><svg viewBox="0 0 24 24" aria-hidden="true">${icon}</svg><b></b></span>`).join('');
    pill.onclick = () => changeUi(u => { u.zonesFor = u.zonesFor === Number(root.dataset.player) ? null : Number(root.dataset.player); });
    // Your player line is beside your hand. So the hand starts where the line ends, however wide its zones make it
    if (root.id === 'me') {
      new ResizeObserver(() => byId('match').style.setProperty('--line-w', `${q(root, '.player').offsetWidth + 32}px`))
        .observe(q(root, '.player'));
    }
  }
  root.dataset.player = String(player.$key);
  const avatar = q(root, '.avatar');
  q(root, '.initial').textContent = (player.Name ?? '?').slice(0, 1).toUpperCase();
  const portrait = q<HTMLImageElement>(root, '.portrait');
  if (setImage(portrait, playerAvatarUrl(player))) {
    portrait.hidden = true;
    portrait.onload = () => { portrait.hidden = false; };
  }
  avatar.classList.toggle('ai', !!player.IsAI);
  root.style.setProperty('--sleeve', cssUrl(playerSleeveUrl(player)));
  showLife(q(root, '.life'), avatar, player.Life ?? 0, isLocal(model, player));
  q(root, '.name .who').textContent = player.Name ?? '';
  q(root, '.name').title = player.Name ?? '';
  // A player out of the game keeps their seat. The seat is greyed and a skull is drawn over the portrait
  root.classList.toggle('out', !!player.HasLost);
  root.classList.toggle('turn', takesTurn(player, game(model)?.PlayerTurn));
  avatar.classList.toggle('highlighted', (model.prompt?.highlighted ?? []).includes(player.$key));
  avatar.classList.toggle('selectable', (model.prompt?.selectablePlayers ?? []).some(r => r.ref === player.$key));
  avatar.classList.toggle('active', takesTurn(player, game(model)?.PlayerTurn));
  // The player who went first stays marked until turn 1 is over, for anyone who missed the reveal
  avatar.classList.toggle('first', wentFirst(player.$key) && (game(model)?.Turn ?? 0) <= 1);
  // A player who has lost shows an empty portrait frame. The portrait has broken out of it, or breaks out now
  avatar.classList.toggle('lost', !!player.HasLost);
  renderHandFan(q(root, '.hand-fan'), model, player);
  renderZoneTiles(q(root, '.zone-tiles'), model, player, select);
  renderZonesPill(root, model, player);
  renderManaPool(q(root, '.mana'), player, isLocal(model, player), actions);
  showCommanderDamage(avatar, model, player);
  reconcile(q(root, '.player-counters'), Object.entries(player.Counters ?? {}), ([name]) => name,
    () => make('span', 'player-counter'),
    (el, [name, n]) => { el.textContent = `${name.toLowerCase()} ${n}`; });
  renderEmblems(q(root, '.emblems'), model, player, zone(model, player, 'Command'), select);
  q(root, '.role-tag').hidden = !isArchenemy(model, player);
  renderOngoing(q(root, '.schemes-ongoing'), model, player);
  // The hand is drawn first, so the cards above it are sized to the space it leaves
  renderTeammateHand(root, model, root.classList.contains('mate') ? zone(model, player, 'Hand') : []);
  renderBattlefield(root, model, player.$key, zone(model, player, 'Battlefield'), onField, select);
}

/** Your teammate's hand lies face up and fanned along the bottom of their seat, beside their portrait. Its cards are there to look at, so a click does nothing. */
function renderTeammateHand(seat: HTMLElement, model: Model, cards: CardView[]): void {
  let row = seat.querySelector<HTMLElement>(':scope > .mate-hand');
  if (!row && !cards.length) return;
  if (!row) {
    row = make('div', 'mate-hand');
    seat.append(row);
  }
  row.hidden = !cards.length;
  reconcile(row, cards, c => c.$key, () => createCard(() => {}), (el, c) => updateCard(el, model, c));
  row.style.paddingLeft = `${q(seat, '.player').offsetWidth + 32}px`;
  fanOut(row);
}

/** The arc and chip on the portrait show the most damage any one commander has dealt this player, not the total. */
function showCommanderDamage(avatar: HTMLElement, model: Model, player: PlayerView): void {
  const hits = (player.CommanderDamage ?? []).filter(h => h.value > 0);
  const top = hits.reduce((best, h) => (h.value > best.value ? h : best), { card: { ref: -1 }, value: 0 });
  const arc = q(avatar, '.cmdr-arc');
  const chip = q(avatar, '.cmdr-chip');
  arc.hidden = chip.hidden = top.value === 0;
  if (top.value === 0) return;
  const near = top.value >= COMMANDER_WARNING;
  arc.classList.toggle('near', near);
  chip.classList.toggle('near', near);
  const circle = q(arc, 'circle');
  const around = 2 * Math.PI * 48;
  circle.setAttribute('stroke-dasharray', `${around * Math.min(top.value, COMMANDER_LETHAL) / COMMANDER_LETHAL} ${around}`);
  q(chip, 'b').textContent = String(top.value);
  const commander = deref(model, top.card);
  const name = (commander ? stateOf(model, commander).Name : undefined) ?? t('lblWebBoardACommander');
  chip.title = t('lblWebBoardCommanderDamageFrom', top.value, COMMANDER_LETHAL, name);
}

// Mana in the pool is lost when the step ends, so it is shown separately from the seat's lasting details
function renderManaPool(root: HTMLElement, player: PlayerView, own: boolean, actions: Actions): void {
  const pool = MANA.filter(([bit]) => player.Mana?.[bit]);
  root.hidden = pool.length === 0;
  reconcile<[number, string], HTMLButtonElement>(q(root, '.mana-chips'), pool, ([bit]) => bit,
    ([bit, sym]) => {
      const el = make('button', 'mana-chip');
      el.innerHTML = '<img class="sym" alt="" draggable="false"><span class="amount"></span>';
      const img = q<HTMLImageElement>(el, 'img');
      img.src = symbolUrl(sym);
      img.alt = sym;
      el.onclick = () => actions.useMana(bit);
      return el;
    },
    (el, [bit, sym]) => {
      const amount = player.Mana?.[bit] ?? 0;
      q(el, '.amount').textContent = String(amount);
      el.disabled = !own;
      const name = t(MANA_NAMES[sym]);
      el.title = own ? t('lblWebBoardManaFloatingOwn', amount, name) : t('lblWebBoardManaFloating', amount, name);
    });
}

const MANA_NAMES: Record<string, TextKey> = {
  W: 'lblWhite', U: 'lblBlue', B: 'lblBlack', R: 'lblRed',
  G: 'lblGreen', C: 'lblColorless',
};

// A hand is drawn as a few fanned card backs with its count. A click opens it in a window, for your own hand too
function renderHandFan(el: HTMLElement, model: Model, player: PlayerView): void {
  const count = zone(model, player, 'Hand').length;
  el.hidden = false;
  el.dataset.count = String(Math.min(count, 3));
  q(el, '.hand-count').textContent = String(count);
  // Your own hand is also shown along the bottom, but a big hand is easier to read in a window
  el.title = isLocal(model, player)
    ? t(count === 1 ? 'lblWebBoardOwnHandOneCard' : 'lblWebBoardOwnHandCards', count)
    : t(count === 1 ? 'lblWebBoardHandOneCard' : 'lblWebBoardHandCards', count);
}

// Small shapes drift down on a graveyard tile and circle on an exile tile, so the two piles are easy to tell apart
const AMBIENT = '<span class="ambient" aria-hidden="true">' + '<i></i>'.repeat(6) + '</span>';

/** Extra zones that some variants and cards add. Each is shown only while it has cards. */
const EXTRA_ZONES: [ZoneType, TextKey][] = [['PlanarDeck', 'lblPlanes'], ['SchemeDeck', 'lblSchemes'],
  ['AttractionDeck', 'lblAttractions'], ['ContraptionDeck', 'lblContraptions'], ['Junkyard', 'lblJunkyardZone'],
  ['Sideboard', 'lblSideboard'], ['Ante', 'lblAnteZone'], ['StickerSheets', 'lblStickerSheets']];
const FACE_DOWN_DECKS: ReadonlySet<ZoneType> = new Set(['PlanarDeck', 'SchemeDeck', 'AttractionDeck', 'ContraptionDeck']);

function graveyardTitle(count: number, types: string[]): string {
  const cards = t(count === 1 ? 'lblWebOneCard' : 'lblWebNCards', count);
  if (!types.length) return t('lblWebBoardGraveyardTitle', cards);
  const kinds = t(types.length === 1 ? 'lblWebBoardOneCardType' : 'lblWebBoardCardTypes', types.length);
  return t('lblWebBoardGraveyardTitleTypes', cards, kinds, types.join(', '));
}

// Drawn on the same 24-unit grid as the other icons. The icons are a fan of cards for the hand, a deck, a headstone, and a ring for exile
const PILL_ZONES: [ZoneType, string][] = [
  ['Hand', '<rect x="9" y="6" width="9" height="13" rx="1.5" transform="rotate(14 13.5 12.5)"/><rect x="5.5" y="6" width="9" height="13" rx="1.5" transform="rotate(-10 10 12.5)"/>'],
  ['Library', '<rect x="5" y="7" width="11" height="14" rx="1.6"/><path d="M8.5 4h8.3a1.7 1.7 0 0 1 1.7 1.7V17"/>'],
  ['Graveyard', '<path d="M7 21V10a5 5 0 0 1 10 0v11"/><path d="M4.5 21h15M12 9.5v5M10 11.5h4"/>'],
  ['Exile', '<circle cx="12" cy="12" r="7.5"/><circle cx="12" cy="12" r="2.2"/>'],
];
let pillWired = false;

/** A phone has no room to show every zone tile. So a player's bar shows the counts, and a tap opens the tiles over the board. */
function renderZonesPill(root: HTMLElement, model: Model, player: PlayerView): void {
  if (!pillWired) {
    pillWired = true;
    // Closed by a tap anywhere else, and by the tap that picks a tile. Listening for click lets that tap reach the tile first
    onPressOutside('.zones-pill', () => { if (ui.zonesFor !== null) changeUi(u => { u.zonesFor = null; }); }, 'click');
  }
  const pill = q(root, '.zones-pill');
  for (const span of pill.querySelectorAll<HTMLElement>('span')) {
    q(span, 'b').textContent = String(zone(model, player, span.dataset.zone as ZoneType).length);
  }
  // Your own hand is shown along the bottom, so only another player's hand is counted here
  q(pill, '[data-zone="Hand"]').hidden = isLocal(model, player);
  const tiles = q(root, '.zone-tiles');
  // Lit when a commander in its tiles can be cast. Marked when it has more tiles than the pill shows
  pill.classList.toggle('lit', !!tiles.querySelector('.zone-tile.selectable, .zone-tile[data-cast="yes"]'));
  pill.classList.toggle('more', tiles.childElementCount > PILL_ZONES.length - 1);
  const open = isPortrait() && ui.zonesFor === player.$key;
  root.classList.toggle('zones-open', open);
  pill.setAttribute('aria-expanded', String(open));
  pill.setAttribute('aria-label', PILL_ZONES.filter(([z]) => z !== 'Hand' || !isLocal(model, player)).map(([z]) => `${zoneTitle(z)} ${zone(model, player, z).length}`).join(', '));
}

function renderZoneTiles(root: HTMLElement, model: Model, player: PlayerView, select: CardClick): void {
  // A player with a commander has a Command tile for the whole game. It is empty while the commander is in another zone
  const commanded = (player.Commander ?? []).some(r => r);
  const zones: ZoneType[] = [...(commanded ? ['Command' as ZoneType] : []), 'Library', 'Graveyard', 'Exile',
    ...EXTRA_ZONES.map(([z]) => z).filter(z => zone(model, player, z).length > 0)];
  reconcile<ZoneType, HTMLButtonElement>(root, zones, z => z,
    zoneName => {
      const el = make('button', 'zone-tile');
      el.dataset.zone = zoneName;
      el.innerHTML = '<img alt="" draggable="false">' + (zoneName === 'Library' ? '' : AMBIENT)
        + '<span class="zone-name"></span><span class="zone-count"></span>';
      const extra = EXTRA_ZONES.find(([z]) => z === zoneName)?.[1];
      q(el, '.zone-name').textContent = extra ? tCap(extra) : zoneTitle(zoneName);
      // A planar, scheme, attraction or contraption deck is face down, so only its count is shown
      el.classList.toggle('hidden-deck', FACE_DOWN_DECKS.has(zoneName));
      const img = q<HTMLImageElement>(el, 'img');
      hideOnError(img);
      el.onclick = () => {
        // A single commander is cast straight from its tile. With partners, the click opens the zone to choose one
        if (zoneName === 'Command' && el.dataset.cast) select(q(el, 'img'), false);
        else if (!el.classList.contains('hidden-deck')) togglePile(Number(el.closest<HTMLElement>('.seat')?.dataset.player), zoneName);
      };
      // The hover data is on the image, because the render finds the tile again by the tile's own data-key
      hoverable(el, img);
      return el;
    },
    (el, zoneName) => {
      if (zoneName === 'Command') {
        updateCommandTile(el, model, player);
        return;
      }
      const cards = zone(model, player, zoneName);
      // A graveyard or exile list's newest card is last; a library's top card is first and replaces the sleeve while it may be seen
      const top = zoneName === 'Graveyard' || zoneName === 'Exile' ? cards[cards.length - 1] : zoneName === 'Library' ? cards[0] : undefined;
      const src = cardImageSrc(model, top);
      const img = q<HTMLImageElement>(el, 'img');
      setImage(img, src);
      img.hidden = !src;
      img.dataset.key = String(top?.$key ?? '');
      img.dataset.zoom = src;
      el.classList.toggle('back', zoneName === 'Library' && cards.length > 0 && !src);
      el.classList.toggle('empty', cards.length === 0);
      // A card still animating to this zone is not counted until it arrives
      el.dataset.count = String(cards.length);
      showCount(el);
      // The card types in a graveyard matter to delirium and similar cards, so the tooltip lists them
      if (zoneName === 'Graveyard') {
        el.title = graveyardTitle(cards.length, cardTypes(cards.map(c => stateOf(model, c).Type ?? '')));
      }
    });
}

/** The Command tile's badge shows the commander's tax. When partners share the tile, it shows the number of cards. */
function updateCommandTile(el: HTMLElement, model: Model, player: PlayerView): void {
  const cards = castFromCommand(model, zone(model, player, 'Command'));
  const top = cards[0];
  const src = cardImageSrc(model, top);
  const img = q<HTMLImageElement>(el, 'img');
  setImage(img, src);
  img.hidden = !src;
  img.dataset.key = String(top?.$key ?? '');
  img.dataset.zoom = src;
  // The render finds the tile again by its own data-key, so the card to cast is marked with a separate attribute
  el.dataset.cast = cards.length === 1 ? 'yes' : '';
  el.classList.toggle('empty', cards.length === 0);
  el.classList.toggle('selectable', cards.some(c => (model.prompt?.selectable ?? []).some(r => r.ref === c.$key)));
  const tax = top ? commanderTax(player, top) : 0;
  q(el, '.zone-count').textContent = cards.length > 1 ? String(cards.length) : tax > 0 ? t('lblWebBoardTax', tax) : '';
  el.title = cards.map(c => {
    const more = commanderTax(player, c);
    const name = stateOf(model, c).Name ?? '';
    return more > 0 ? t('lblWebBoardCostsMoreHere', name, more) : name;
  }).join('\n');
}

/** The command zone cards that are cast from it. Avatars and reminders such as the monarch are left out. */
function castFromCommand(model: Model, cards: CardView[]): CardView[] {
  return cards.filter(c => ['commander', 'signature'].includes(commandKind(c, stateOf(model, c))));
}

/** Clears the state kept per game. A new game then announces its first turn even when the same player starts it. */
export function resetTable(): void {
  resetTurnBanner();
  resetMotion();
  stopWaiting();
  forgetPictures();
  resetStarter();
  seating = null;
  shownSeat = null;
  outSaid = false;
  document.getElementById('out-banner')?.remove();
  resetSchemes();
  resetStack();
  resetHand();
  resetOverlay();
  resetGameOver();
  // The seats' markup outlives the game, so the next game's starting life would show as a change
  for (const life of document.querySelectorAll<HTMLElement>('#match .avatar .life')) delete life.dataset.life;
}

// A life change is easy to miss as a number. So the amount floats up from the avatar and the life total animates
function showLife(el: HTMLElement, avatar: HTMLElement, life: number, local: boolean): void {
  const before = el.dataset.life === undefined ? life : Number(el.dataset.life);
  el.dataset.life = String(life);
  el.textContent = String(life);
  const change = life - before;
  // A seat in a closed tab is not drawn. Its animations would not run, so their pieces would wait and all play when the tab opens
  if (!change || avatar.closest('[hidden]')) {
    return;
  }
  const hurt = change < 0;
  playOnce(el, hurt ? 'hurt' : 'healed', 'hurt', 'healed');
  const float = make('span', `life-change ${hurt ? 'hurt' : 'healed'}`);
  float.textContent = `${hurt ? '' : '+'}${change}`;
  avatar.append(float);
  float.addEventListener('animationend', () => float.remove());
  if (hurt) {
    if (local) takeHit(-change);
    else hitAvatar(avatar, -change);
  }
}

// Damage to another player shakes their portrait and adds a glow to it. A red flash also spreads from the screen edges near it
function hitAvatar(avatar: HTMLElement, amount: number): void {
  const at = avatar.getBoundingClientRect();
  const wash = make('div', `hit-flash near${amount >= 5 ? ' hard' : ''}`);
  wash.style.setProperty('--at', `${at.left + at.width / 2}px ${at.top + at.height / 2}px`);
  document.body.append(wash);
  wash.addEventListener('animationend', () => wash.remove());
  replay(avatar, amount >= 5 ? 'hit-hard' : 'hit', 'hit', 'hit-hard');
  const settled = (e: AnimationEvent) => {
    if (e.target !== avatar) return;
    avatar.classList.remove('hit', 'hit-hard');
    avatar.removeEventListener('animationend', settled);
  };
  avatar.addEventListener('animationend', settled);
  const glow = make('span', 'avatar-hit');
  avatar.append(glow);
  glow.addEventListener('animationend', () => glow.remove());
}

// Damage to your own life shakes the board and flashes red in from the edges, so it cannot be missed
function takeHit(amount: number): void {
  const match = byId('match');
  replay(match, amount >= 5 ? 'hit-hard' : 'hit', 'hit', 'hit-hard');
  // animationend events from cards inside the board also reach this listener. So only the board's own animation ends the shake
  const settled = (e: AnimationEvent) => {
    if (e.target !== match) return;
    match.classList.remove('hit', 'hit-hard');
    match.removeEventListener('animationend', settled);
  };
  match.addEventListener('animationend', settled);
  const flash = make('div', amount >= 5 ? 'hit-flash hard' : 'hit-flash');
  document.body.append(flash);
  flash.addEventListener('animationend', () => flash.remove());
}

/** Daybound and nightbound cards transform without a prompt. So at night the whole table changes, as well as the medallion that shows day or night. */
export function renderSky(model: Model): void {
  const sky = byId('sky');
  const time = model.controls?.dayTime;
  sky.hidden = !time;
  byId('match').classList.toggle('night', time === 'Night');
  if (!time) {
    return;
  }
  if (!sky.firstChild) {
    sky.innerHTML = '<span class="medal" aria-hidden="true"><span class="flip"><i class="face sun"></i><i class="face moon"></i></span></span><span class="when"></span>';
  }
  sky.dataset.time = time;
  q(sky, '.when').textContent = time === 'Night' ? t('lblNight') : t('lblDay');
}
