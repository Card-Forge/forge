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
     * than one door is locked, prefer one whose face has an unlock trigger to run.
     */
    @Override
    public CardState chooseCardState(Player ai, SpellAbility sa, List<CardState> faces,
            Map<String, Object> params) {
        if (isLockOrUnlock(sa) && params != null && params.get("Object") instanceof Card room) {
            CardState fallback = null;
            for (CardState state : faces) {
                if (!room.getLockedRooms().contains(state.getStateName())) {
                    continue;
                }
                if (unlockingTriggers(room, state.getStateName(), ai)) {
                    return state;
                }
                if (fallback == null) {
                    fallback = state;
                }
            }
            if (fallback != null) {
                return fallback;
            }
        }
        return faces.isEmpty() ? null : faces.get(0);
    }

    /** Prefer a Room that pays out when opened over one that just changes which half is active. */
    private static Card bestTarget(CardCollection rooms, Player ai) {
        for (Card room : rooms) {
            for (CardStateName stateName : room.getLockedRooms()) {
                if (room.hasState(stateName) && unlockingTriggers(room, stateName, ai)) {
                    return room;
                }
            }
        }
        return rooms.getFirst();
    }

    /**
     * True when opening this particular door would run a "when you unlock this door" trigger that
     * the AI actually wants right now. Those triggers are declared with ThisDoor$ True and only
     * fire for the face they sit on (see TriggerUnlockDoor.performTest), so the trigger's own state
     * name is what identifies it.
     *
     * Asking the effect's own AI via doTrigger, rather than just checking that a trigger exists,
     * matters because plenty of Rooms have an unlock trigger that is useless or harmful in the
     * current state: Cramped Vents deals 6 damage to a creature an opponent controls, which does
     * nothing with no creatures to hit, and Derelict Attic draws two cards and loses 2 life, which
     * is lethal at 2 life.
     */
    private static boolean unlockingTriggers(Card room, CardStateName stateName, Player ai) {
        if (!room.hasState(stateName)) {
            return false;
        }
        for (Trigger t : room.getState(stateName).getTriggers()) {
            if (t.getMode() != TriggerType.UnlockDoor || !stateName.equals(t.getCardStateName())) {
                continue;
            }
            SpellAbility effect = t.ensureAbility();
            if (effect == null) {
                continue;
            }
            // the trigger's ability has no activator until it actually fires, and the AI logic
            // below needs one to work out targets and costs
            if (effect.getActivatingPlayer() == null) {
                effect.setActivatingPlayer(ai);
            }
            if (SpellApiToAi.Converter.get(effect).doTrigger(ai, effect, false)) {
                return true;
            }
        }
        return false;
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
