// Decides which messages are worth pausing on, and whether the page is behind the game. pacer.ts holds each board on screen for its moment

import type { Controls, GameEvent, Prompt, Ref } from '../protocol';
import { offersPicks } from '../model';

/** Public zones. A card that enters or leaves one of them is worth showing. */
const OPEN = new Set(['Battlefield', 'Stack', 'Graveyard', 'Exile', 'Command']);

/** Whether events show something the player should see before the game moves on. That is another player's public action, or a change to a player or a permanent. */
export function worthSeeing(events: GameEvent[], localPlayers: number[]): boolean {
  const mine = (player?: Ref) => !!player && localPlayers.includes(player.ref);
  return events.some(e => {
    switch (e.kind) {
      case 'cardMoved':
        return [e.from, e.to].some(place => place && OPEN.has(place.zone)) && !mine(e.caster ?? e.to?.player ?? e.from?.player);
      case 'attackersDeclared':
        return e.attacks.length > 0 && !mine(e.player);
      case 'blockersDeclared':
        return e.blocks.length > 0 && !mine(e.player);
      case 'stackAdded':
      case 'stackResolved':
        return !mine(e.player);
      case 'cardDamaged':
      case 'playerDamaged':
      case 'lifeChanged':
      case 'playerCounters':
      case 'cardCounters':
      case 'cardAttached':
        return true;
      default:
        return false;
    }
  });
}

/** Whether the game is passing priority for the player until some point. The chip beside the phase pill shows this. */
export function passingFor(controls: Controls | null | undefined): boolean {
  return !!controls && (!!controls.marker || controls.untilEndOfTurn || controls.untilStackEmpty);
}

/**
 * Whether a prompt asks the player for something. If it does, they are looking at the board and their answer must not be delayed.
 * While the game passes priority for them, Cancel only stops the passing. That does not count as a question.
 */
export function asksPlayer(prompt: Prompt, passing = false): boolean {
  return prompt.priority || prompt.ok.enabled || (prompt.cancel.enabled && !passing) || offersPicks(prompt);
}

let behind = false;

/** Whether boards the server has sent are waiting to be shown. The page is then behind the game, and no player is holding the game up. */
export const boardBehind = (): boolean => behind;

export function setBoardBehind(value: boolean): void {
  behind = value;
}

/**
 * The server sends a sound or a log line when its event happens, and then the board that shows the event. This passes them on
 * after that board, so a sound plays when its card moves, even if the board is delayed (by a turn banner or a pause on a play).
 * A message that no board follows within `wait` is passed on by itself.
 */
export function behindItsBoard<T extends { t: string }>(deliver: (msg: T) => void, wait: number): (msg: T) => void {
  let ahead: T[] = [];
  let timer: ReturnType<typeof setTimeout> | undefined;
  const flush = () => {
    clearTimeout(timer);
    timer = undefined;
    const waiting = ahead;
    ahead = [];
    waiting.forEach(deliver);
  };
  return msg => {
    if (msg.t === 'sound' || msg.t === 'log') {
      ahead.push(msg);
      timer ??= setTimeout(flush, wait);
    } else {
      deliver(msg);
      if (msg.t === 'state') flush();
    }
  };
}
