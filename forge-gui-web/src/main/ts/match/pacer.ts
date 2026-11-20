// Delays game messages while the page shows something the player should see first. That is a turn's banner or another player's play

import { announceComing } from './turnbanner';
import { asksPlayer, passingFor, setBoardBehind, worthSeeing } from './pauserules';
import { setting } from '../settings';
import type { Model } from '../model';
import type { ServerMessage } from '../protocol';

/** The message types that carry the game's progress. They are held back and shown in order. Other messages, such as chat or a card's details, are shown when they arrive. */
const HELD_MESSAGES = new Set(['state', 'prompt', 'controls', 'request', 'zones', 'playable', 'sound', 'log', 'gameOver', 'drawOffer', 'flash']);
/** Longer than any banner. It matters only if the banner's own release never comes. */
const HOLD_LIMIT_MS = 4000;
/** The sound of a turn ending, which the host sends with the next turn's first state. */
const TURN_SOUND = 'end_of_turn';

/** Receives the server's messages in order. Game messages are held while a banner or another player's play is on screen. `apply` applies one message to the model. */
export function createPacer(model: Model, apply: (msg: ServerMessage) => void): { receive: (msg: ServerMessage) => void; holding: () => boolean; hurry: () => void } {
  /** Messages that arrived while a new turn's banner shows, or while an earlier board is still being shown. */
  let held: ServerMessage[] | null = null;
  /** The time until which the board on screen stays shown, because it shows another player's play. */
  let showUntil = 0;
  /** Whether the held messages wait for a turn's banner, not for a board that is still being shown. */
  let announcing = false;
  /** The player conceded. Nothing is held until the game ends, so the end of the game shows at once. */
  let hurrying = false;

  function receive(msg: ServerMessage): void {
    if (hurrying) {
      apply(msg);
      if (msg.t === 'gameOver' || msg.t === 'hello') hurrying = false;
    } else if (!held) {
      // Each release applies only to this hold. So an earlier banner that ends late cannot release a later turn early
      const releaseThis = () => { if (held?.[0] === msg) release(); };
      // A sound or a log line arrives after its own board. It is held with that board when the board is held, but never starts a hold
      if (msg.t === 'state' && !msg.full && Date.now() < showUntil) {
        held = [msg];
        setBoardBehind(true);
        setTimeout(releaseThis, showUntil - Date.now());
      } else if (msg.t === 'state' && announceComing(model, msg, releaseThis)) {
        held = [msg];
        setBoardBehind(true);
        announcing = true;
        // The end of the banner releases the turn. This timer only protects the page from a release that never comes
        setTimeout(releaseThis, HOLD_LIMIT_MS);
      } else {
        apply(msg);
        if (msg.t === 'state' && worthSeeing(msg.events, msg.localPlayers)) {
          showUntil = Date.now() + Number(setting('pace'));
        } else if (msg.t === 'request' || (msg.t === 'prompt' && asksPlayer(msg, passingFor(model.controls)))) {
          // When the player is asked something, they are looking at the current board. So what they do next is shown at once
          showUntil = 0;
        }
      }
    } else if (msg.t === 'hello') {
      // A new table or a reconnection starts over. The held messages are applied at once, and none of them starts a new hold
      const waiting = held;
      held = null;
      setBoardBehind(false);
      announcing = false;
      showUntil = 0;
      // The held sounds are for moments that have passed. They are dropped so they do not all play at once
      waiting.filter(m => m.t !== 'sound').forEach(apply);
      apply(msg);
    } else if (!HELD_MESSAGES.has(msg.t)) {
      apply(msg);
    } else if (msg.t === 'sound' && msg.name === TURN_SOUND && announcing && !held.slice(1).some(m => m.t === 'state')) {
      // The turn's sound plays when its banner is shown. A later turn's sound waits behind that turn's own state
      apply(msg);
    } else {
      held.push(msg);
    }
  }

  function release(): void {
    const waiting = held ?? [];
    held = null;
    setBoardBehind(false);
    announcing = false;
    waiting.forEach(receive);
  }

  function hurry(): void {
    const waiting = held ?? [];
    held = null;
    setBoardBehind(false);
    announcing = false;
    showUntil = 0;
    hurrying = true;
    // The held sounds are for moments the player skipped
    waiting.filter(m => m.t !== 'sound').forEach(receive);
  }
  return { receive, holding: () => held !== null, hurry };
}
