// The banner that shows whose turn it is when a turn starts

import { deref, game, me, players, sharesTurns, takesTurn, teamOf, type Model } from '../model';
import { byId, make } from '../dom';
import { playerAvatarUrl } from '../avatarsleeves';
import { t } from '../text';
import type { GameView, PlayerView, Ref, StateMessage } from '../protocol';

// The last turn announced. It is null on a new table, so a turn already under way when the table is first drawn is not announced
let announced: string | null = null;

export function resetTurnBanner(): void {
  announced = null;
  // A banner from a match left mid-way never ends, because a hidden page does not run its animation
  document.querySelectorAll('#phase-strip .turn-banner').forEach(banner => banner.remove());
  document.getElementById('phase-strip')?.classList.remove('announcing');
}

/** Announces the turn that a state message starts, before the message is shown. If a banner is shown, runs `then` as it leaves. */
export function announceComing(model: Model, msg: StateMessage, then: () => void): boolean {
  const g = game(model);
  const delta = msg.deltas[model.root] as { Turn?: number; PlayerTurn?: Ref } | undefined;
  // Only a new turn number starts a turn. The turn's player is set in the cleanup step, which can arrive one message earlier
  if (msg.full || !g || !delta || delta.Turn === undefined) {
    return false;
  }
  const turn = delta.PlayerTurn ?? g.PlayerTurn;
  return announce(model, delta.Turn, deref(model, turn), takesTurn(me(model), turn), then);
}

// Handles only the turn in which the table was first drawn. Every later turn is announced when its message arrives
export function announceTurn(model: Model, g: GameView): void {
  if (announced === null) announce(model, g.Turn, deref(model, g.PlayerTurn), takesTurn(me(model), g.PlayerTurn), () => {});
}

/** How long before the turn banner is removed that the turn's own actions start. */
const TURN_OVERLAP_MS = 300;

function announce(model: Model, turnNumber: number | undefined, active: PlayerView | undefined, mine: boolean, then: () => void): boolean {
  const turn = `${turnNumber ?? 0}/${active?.$key ?? ''}`;
  if (!active || announced === turn) {
    return false;
  }
  const seenBefore = announced !== null;
  announced = turn;
  if (!seenBefore && turnNumber !== 1) {
    return false;
  }
  // With several opponents most turns belong to someone else. The phase pill already shows whose turn it is. Two teams that share their turns are two sides, as in a two-player game
  if (!mine && players(model).length > 2 && !sharesTurns(model)) {
    return false;
  }
  const banner = make('div', `turn-banner${mine ? ' mine' : ''}`);
  const team = teamOf(model, active);
  for (const player of team) {
    const url = playerAvatarUrl(player);
    const face = make(url ? 'img' : 'span', 'mugshot');
    if (face instanceof HTMLImageElement) {
      face.alt = '';
      face.src = url;
    } else {
      face.textContent = (player.Name ?? '?').slice(0, 1).toUpperCase();
    }
    banner.append(face);
  }
  const whose = make('b', '');
  whose.textContent = team.length > 1
    ? (mine ? t('lblWebPhaseYourTeamsTurn') : t('lblWebBoardTeamsTurn', team[0].Name ?? '', team[1].Name ?? ''))
    : (mine ? t('lblWebPhaseYourTurn') : t('lblWebBoardPlayersTurn', active.Name ?? ''));
  banner.append(whose);
  // The banner replaces the pill instead of covering it, because the pill animates its own width
  const strip = byId('phase-strip');
  strip.append(banner);
  // Auto margins can centre it on a half pixel, which blurs the text
  const size = banner.getBoundingClientRect();
  banner.style.margin = `${Math.round((strip.clientHeight - size.height) / 2)}px auto auto ${Math.round((strip.clientWidth - size.width) / 2)}px`;
  strip.classList.add('announcing');
  // The pill comes back when the banner starts to fade out. So one fades in while the other fades out
  const out = banner.getAnimations().find(a => (a as CSSAnimation).animationName === 'turn-out');
  const fades = Number(out?.effect?.getTiming().delay ?? 0);
  const sweep = Number(out?.effect?.getComputedTiming().endTime ?? 0);
  // Skip this if the next turn's banner is already shown. That happens when a turn passes at once
  setTimeout(() => {
    if ([...strip.querySelectorAll('.turn-banner')].at(-1) === banner) strip.classList.remove('announcing');
  }, fades);
  // The turn continues just before the banner is fully gone
  setTimeout(then, Math.max(0, sweep - TURN_OVERLAP_MS));
  // The ::after light's animationend event also reaches the banner. So the name is checked, or the banner is removed early
  banner.addEventListener('animationend', e => {
    if (e.animationName === 'turn-out') banner.remove();
  });
  return true;
}
