// A dragged block or attack sends the same clicks the engine already accepts. Each click waits until the one before it shows on the board

import { byId, q, make, placeBeside } from '../dom';
import { attackTargets, game, me, opponents, stateOf, takesTurn, type Model } from '../model';
import { setDragArrow } from './overlay';
import type { Actions } from '../actions';
import type { CardView, Ref } from '../protocol';
import { t } from '../text';
import { trackDrag } from '../press';

/** How far the pointer moves before a press on a creature is a drag rather than a click. */
const DRAG_PX = 8;
/** How long the drag waits for a click to show on the board before it gives up. */
const STEP_MS = 2000;

type Point = { x: number; y: number };

let current: Model | null = null;
/** The drags not yet shown on the board, chained in order, so a quick second drag waits for the first. */
let queued: Promise<void> = Promise.resolve();
let act: Actions | null = null;
/** The name of the player you last attacked. The name is kept because each game of a match creates its players again. */
let lastDefender: string | null = null;
/** Whether this attack prompt has already been switched to lastDefender, or never needed to be. */
let turned = false;
/** The player this browser asked the prompt to switch to, until the switch shows. */
let turning: number | null = null;

const has = (refs: (Ref | null)[] | null | undefined, key: number) => (refs ?? []).some(r => r?.ref === key);

/** The block prompt marks no cards as selectable. So it is recognised by the step and by who the attack targets. */
export function declaringBlocks(model: Model): boolean {
  const mine = me(model)?.$key;
  return game(model)?.Phase === 'COMBAT_DECLARE_BLOCKERS' && !!model.prompt
    && (game(model)?.CombatView ?? []).some(b => b.defender?.ref === mine);
}

const defenders = (model: Model): number[] => attackTargets(model, me(model));

/** The player, planeswalker or battle the attack prompt is declaring attackers against. The prompt highlights it. */
function currentDefender(model: Model): number | undefined {
  const lit = model.prompt?.highlighted ?? [];
  return defenders(model).find(key => lit.includes(key));
}

/** The attack prompt highlights the defender. The priority prompt later in the same step does not. */
function declaringAttacks(model: Model): boolean {
  return game(model)?.Phase === 'COMBAT_DECLARE_ATTACKERS' && takesTurn(me(model), game(model)?.PlayerTurn)
    && defenders(model).length > 1 && currentDefender(model) !== undefined;
}

/** The engine starts every attack at the first opponent. So once per attack prompt this switches it to the player you attacked last. */
function followDefender(model: Model): void {
  const at = declaringAttacks(model) ? currentDefender(model) : undefined;
  if (at === undefined) {
    turned = false;
    turning = null;
    return;
  }
  if (!turned) {
    turned = true;
    const wanted = opponents(model).find(p => p.Name === lastDefender && !p.HasLost);
    if (wanted && wanted.$key !== at && act) {
      turning = wanted.$key;
      act.selectPlayer(wanted.$key);
      return;
    }
  }
  if (turning !== null && at !== turning) return;
  turning = null;
  const player = opponents(model).find(p => p.$key === at);
  if (player) lastDefender = player.Name ?? null;
}

/** Marks the table while blockers or attackers are being declared, so a touch on your creatures drags instead of scrolling. */
export function renderCombatDrag(model: Model): void {
  current = model;
  followDefender(model);
  const blocks = declaringBlocks(model);
  const attacks = declaringAttacks(model);
  byId('match').classList.toggle('declaring-blocks', blocks);
  byId('match').classList.toggle('declaring-attacks', attacks);
  if (!blocks && !attacks) setDragArrow(null, null);
}

/** Runs `then` once every block dragged so far shows on the board. So an OK pressed straight after a drop includes that block. */
export function afterCombatDrags(then: () => void): void {
  void queued.then(then);
}

export function initCombatDrag(actions: Actions): void {
  act = actions;
  document.addEventListener('pointerdown', e => {
    const el = e.button === 0 ? (e.target as Element).closest<HTMLElement>('#me .battlefield .card') : null;
    const model = current;
    if (!el || !model) return;
    const attack = declaringAttacks(model);
    if (!attack && !declaringBlocks(model)) return;
    const creature = Number(el.dataset.key);
    trackDrag(e, DRAG_PX, {
      start: () => { document.body.classList.add('dragging-block'); },
      move: ev => {
        if (!current) return;
        // Over an attacker, or an opponent to attack, the arrow points at it. Anywhere else it follows the pointer
        const over = attack ? defenderAt(current, ev.clientX, ev.clientY)?.face : attackerAt(current, ev.clientX, ev.clientY);
        setDragArrow(el, over ?? { x: ev.clientX, y: ev.clientY }, attack ? 'attack' : 'block');
      },
      end: (ev, dragged) => {
        if (!dragged) return;
        setDragArrow(null, null);
        document.body.classList.remove('dragging-block');
        if (!current || ev.type === 'pointercancel') return;
        const at = { x: ev.clientX, y: ev.clientY };
        let step: (() => Promise<void>) | null = null;
        if (attack) {
          const defender = defenderAt(current, at.x, at.y);
          if (defender) step = () => declare(actions, creature, defender.key);
        } else {
          const attacker = attackerAt(current, at.x, at.y);
          if (attacker) step = () => block(actions, creature, Number(attacker.dataset.key), at);
        }
        // A failed block or attack must not stop the later ones, or the OK that waits for them
        if (step) queued = queued.then(step).catch(e => console.error(e));
      },
    });
  });
}

function attackerAt(model: Model, x: number, y: number): HTMLElement | null {
  const attackers = (game(model)?.CombatView ?? []).flatMap(b => b.attackers ?? []);
  for (const el of document.elementsFromPoint(x, y)) {
    const card = el.closest<HTMLElement>('#opponent .card');
    if (card && has(attackers, Number(card.dataset.key))) return card;
  }
  return null;
}

/** The planeswalker or battle under the pointer, else the opponent whose seat or portrait is, while they are still in the game. */
function defenderAt(model: Model, x: number, y: number): { key: number; face: HTMLElement } | null {
  const targets = defenders(model);
  for (const el of document.elementsFromPoint(x, y)) {
    const card = el.closest<HTMLElement>('#opponent .card');
    if (card && targets.includes(Number(card.dataset.key))) return { key: Number(card.dataset.key), face: card };
    const seat = el.closest<HTMLElement>('#opponent .seat');
    const player = seat && opponents(model).find(p => p.$key === Number(seat.dataset.player));
    if (seat && player && !player.HasLost) return { key: player.$key, face: q(seat, '.avatar') };
  }
  return null;
}

/** Attacks the defender with the creature, using the clicks a player would make. It clicks the defender first, unless the prompt already targets it. */
async function declare(actions: Actions, attacker: number, defender: number): Promise<void> {
  const attacking = () => (game(current!)?.CombatView ?? []).some(b => b.defender?.ref === defender && has(b.attackers, attacker));
  // A click on a creature already attacking the prompt's defender removes it from the attack, so it is not clicked
  if (!current || attacking()) return;
  if (currentDefender(current) !== defender) {
    if (opponents(current).some(p => p.$key === defender)) actions.selectPlayer(defender);
    else actions.selectCard(defender, false, 0, 0);
    if (!await until(() => currentDefender(current!) === defender)) return;
  }
  actions.selectCard(attacker, false, 0, 0);
  await until(attacking);
}

/** Whether the blocker is blocking the attacker, declared or still being planned. */
function blocking(blocker: number, attacker: number): boolean {
  return (game(current!)?.CombatView ?? []).some(b => has(b.attackers, attacker)
    && (has(b.blockers, blocker) || has(b.plannedBlockers, blocker)));
}

function blocked(blocker: number): number | undefined {
  const band = (game(current!)?.CombatView ?? []).find(b => has(b.blockers, blocker) || has(b.plannedBlockers, blocker));
  return band?.attackers?.find(r => r)?.ref;
}

/** Resolves true once the test holds on the board, or false if it has not within STEP_MS. */
function until(test: () => boolean): Promise<boolean> {
  const start = performance.now();
  return new Promise(done => {
    const check = () => {
      if (current && test()) done(true);
      else if (performance.now() - start > STEP_MS) done(false);
      else setTimeout(check, 30);
    };
    check();
  });
}

/** Makes a drag's block the way the block prompt accepts one, by clicking the attacker and then the blocker. If the blocker already blocks another attacker, that block is removed first. */
async function block(actions: Actions, blocker: number, attacker: number, at: Point): Promise<void> {
  if (!current || blocking(blocker, attacker)) return;
  // The block prompt highlights the attacker being blocked. An attacker that is already highlighted is not clicked again
  const named = (key: number) => !!current?.prompt?.highlighted?.includes(key);
  const choose = async (key: number) => {
    if (named(key)) return;
    actions.selectCard(key, false, 0, 0);
    await until(() => named(key));
  };
  const held = blocked(blocker);
  if (held !== undefined) {
    await choose(held);
    actions.selectCard(blocker, false, 0, 0);
    if (!await until(() => !blocking(blocker, held))) return;
  }
  await choose(attacker);
  actions.selectCard(blocker, false, 0, 0);
  if (!await until(() => blocking(blocker, attacker))) explainRefusal(blocker, attacker, at);
}

/** When a tap sets your creature to block the prompt's attacker and the block is refused, this shows why, as a drag does. */
export function watchBlockTap(blocker: number, at: Point): void {
  const model = current;
  if (!model || !declaringBlocks(model) || model.prompt?.priority) return;
  const attacker = (model.prompt?.highlighted ?? []).find(k => (model.objects.get(k) as CardView | undefined)?.Attacking);
  if (attacker === undefined || blocking(blocker, attacker)) return;
  void until(() => blocking(blocker, attacker)).then(taken => {
    if (!taken) explainRefusal(blocker, attacker, at);
  });
}

/** Shows that a block was refused. The host gives no reason, so none is shown. */
function explainRefusal(blocker: number, attacker: number, at: Point): void {
  const model = current;
  if (!model) return;
  const name = (key: number) => {
    const card = model.objects.get(key) as CardView | undefined;
    return (card && stateOf(model, card).Name) ?? t('lblWebBlockThatCreature');
  };
  showTip(at, t('lblWebBlockCantBlock', name(blocker), name(attacker)));
}

/** How long the note stays next to the pointer, unless the next press removes it sooner. */
const TIP_MS = 3500;

/** Shows a small note where the drag was released, so it appears where the player is already looking. */
function showTip(at: Point, title: string): void {
  document.querySelector('.block-tip')?.remove();
  const tip = make('div', 'block-tip');
  tip.append(Object.assign(document.createElement('b'), { textContent: title }));
  document.body.append(tip);
  placeBeside(tip, at.x, at.y, 14);
  const gone = () => {
    tip.classList.add('leaving');
    tip.addEventListener('animationend', () => tip.remove());
  };
  const timer = setTimeout(gone, TIP_MS);
  document.addEventListener('pointerdown', () => { clearTimeout(timer); tip.remove(); }, { once: true });
}
