package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * "Lock or unlock a door of target Room you control" can only ever lock when every door of the
 * chosen Room is already unlocked (see the Mode$ LockOrUnlock / case 0 branch of
 * UnlockDoorEffect), so pointing it at a fully unlocked Room shuts off one of your own Rooms.
 * Keys to the House pays {3}, taps and sacrifices itself for that ability, so doing it for nothing
 * also throws away a permanent.
 *
 * Most Rooms carry a "when you unlock this door" trigger on each face, so where there is a choice
 * the AI prefers a door that actually pays out over one that only changes which half is active.
 */
public class UnlockDoorAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        if (!isLockOrUnlock(sa)) {
            // Unlock / ThisDoor can only open doors, which is always what we want
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        if (sa.usesTargeting()) {
            CardCollection targets = new CardCollection();
            for (Card c : aiPlayer.getGame().getCardsIn(ZoneType.Battlefield)) {
                if (sa.canTarget(c) && hasLockedDoor(c)) {
                    targets.add(c);
                }
            }
            if (targets.isEmpty()) {
                // every Room we could point at is already fully unlocked
                return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
            }
            sa.resetTargets();
            sa.getTargets().add(bestTarget(targets, aiPlayer));
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        for (Card c : forge.game.ability.AbilityUtils.getDefinedCards(sa.getHostCard(),
                sa.getParamOrDefault("Defined", "Self"), sa)) {
            if (hasLockedDoor(c)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
    }

    /**
     * With one door locked the effect offers both doors and locks whichever one is still open, so
     * pick a locked door to make sure the ability opens a Room instead of closing one. Where more
     * than one door is locked, open the most useful one.
     */
    @Override
    public CardState chooseCardState(Player ai, SpellAbility sa, List<CardState> faces,
            Map<String, Object> params) {
        if (isLockOrUnlock(sa) && params != null && params.get("Object") instanceof Card room) {
            CardState best = null;
            int bestRank = Integer.MAX_VALUE;
            for (CardState state : faces) {
                if (!room.getLockedRooms().contains(state.getStateName())) {
                    continue;
                }
                int rank = rankDoor(room, state.getStateName(), ai);
                if (rank < bestRank) {
                    bestRank = rank;
                    best = state;
                }
            }
            if (best != null) {
                return best;
            }
        }
        return faces.isEmpty() ? null : faces.get(0);
    }

    /** Prefer a Room with a door actually worth opening over one that only swaps which half is active. */
    private static Card bestTarget(CardCollection rooms, Player ai) {
        Card best = null;
        int bestRank = Integer.MAX_VALUE;
        for (Card room : rooms) {
            for (CardStateName stateName : room.getLockedRooms()) {
                if (!room.hasState(stateName)) {
                    continue;
                }
                int rank = rankDoor(room, stateName, ai);
                if (rank < bestRank) {
                    bestRank = rank;
                    best = room;
                }
            }
        }
        return best != null ? best : rooms.getFirst();
    }

    /**
     * How much we want to open a particular door, lower being better:
     *   0 - it has a "when you unlock this door" trigger the AI wants to run right now
     *   1 - it has no unlock trigger, so opening it just turns the other half on
     *   2 - it has an unlock trigger the AI does not want, so opening it is actively bad
     *
     * The last case is the reason this is ranked rather than a boolean. Derelict Attic's trigger
     * draws two cards and loses 2 life, which is lethal at 2 life, so that door has to lose to the
     * Widow's Walk half rather than merely fail to win. Note that Card.getLockedRooms returns a
     * HashSet, so relying on iteration order to break ties is not reproducible between runs.
     */
    private static int rankDoor(Card room, CardStateName stateName, Player ai) {
        if (!room.hasState(stateName)) {
            return 1;
        }
        int rank = 1;
        for (Trigger t : room.getState(stateName).getTriggers()) {
            if (t.getMode() != TriggerType.UnlockDoor || !stateName.equals(t.getCardStateName())) {
                continue;
            }
            SpellAbility effect = t.ensureAbility();
            if (effect == null) {
                continue;
            }
            // a trigger's ability has no activator until it actually fires, and the AI logic below
            // needs one to work out targets and costs
            effect.setActivatingPlayer(ai);
            if (SpellApiToAi.Converter.get(effect).doTrigger(ai, effect, false)) {
                return 0;
            }
            rank = 2;
        }
        return rank;
    }

    private static boolean isLockOrUnlock(SpellAbility sa) {
        return "LockOrUnlock".equals(sa.getParamOrDefault("Mode", "ThisDoor"));
    }

    private static boolean hasLockedDoor(Card c) {
        for (CardStateName stateName : c.getLockedRooms()) {
            if (c.hasState(stateName)) {
                return true;
            }
        }
        return false;
    }
}
