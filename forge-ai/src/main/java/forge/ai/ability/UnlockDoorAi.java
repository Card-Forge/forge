package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * "Lock or unlock a door of target Room you control" can only ever lock when every door of the
 * chosen Room is already unlocked (see the Mode$ LockOrUnlock / case 0 branch of
 * UnlockDoorEffect), so pointing it at a fully unlocked Room shuts off one of your own Rooms.
 * Keys to the House pays {3}, taps and sacrifices itself for that ability, so doing it for nothing
 * also throws away a permanent.
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
            sa.getTargets().add(targets.getFirst());
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
     * pick the locked door to make sure the ability opens a Room instead of closing one.
     */
    @Override
    public CardState chooseCardState(Player ai, SpellAbility sa, List<CardState> faces,
            Map<String, Object> params) {
        if (isLockOrUnlock(sa) && params != null && params.get("Object") instanceof Card room) {
            for (CardState state : faces) {
                if (room.getLockedRooms().contains(state.getStateName())) {
                    return state;
                }
            }
        }
        return faces.isEmpty() ? null : faces.get(0);
    }

    private static boolean isLockOrUnlock(SpellAbility sa) {
        return "LockOrUnlock".equals(sa.getParamOrDefault("Mode", "ThisDoor"));
    }

    private static boolean hasLockedDoor(Card c) {
        for (var stateName : c.getLockedRooms()) {
            if (c.hasState(stateName)) {
                return true;
            }
        }
        return false;
    }
}
