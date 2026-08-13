package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class DiscoverAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            // If its a subability, just let the AI do it. Casting spells is fun!
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        Card c = sa.getHostCard();
        Cost cost = sa.getPayCosts();
        if (cost != null) {
            // IF self sacrifice, make sure its "work it". For lands, make sure we still have a decent amount of lands. Or not losing a color source we might need
            if (ComputerUtilCost.isSacrificeSelfCost(cost)) {
                if (c.isLand()) {
                    // The discover lands cost 5 plus tapping this land to activate.
                    // Let's make sure we don't have too few lands in play before we sacrifice this land.
                    if (ai.getLandsInPlay().size() <= 6) {
                        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                    }
                }
            }

            // Other discover costs that we should probably consider:
            // Sub Loyalty
            // Exile creature card from graveyard
            // Tapping creatures/artifacts
         }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * doTriggerAINoCost
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // If cost is free, and we're just resolving a trigger, lets just do it.
        if (sa.getPayCosts() == null || sa.getPayCosts().isFree()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return checkApiLogic(ai, sa);
    }

    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        Card c = (Card)params.get("Card");
        for (SpellAbility s : AbilityUtils.getBasicSpellsFromPlayEffect(c, ai)) {
            if (s.isLandAbility()) {
                // return false or we get a ClassCastException later if the AI encounters MDFC with land backside
                return false;
            }
            Spell spell = (Spell) s;
            if (AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlayFromEffectAI(spell, false, true)) {
                // Before accepting, see if the spell has a valid number of targets (it should at this point).
                // Proceeding past this point if the spell is not correctly targeted will result
                // in "Failed to add to stack" error and the card disappearing from the game completely.
                if (!spell.isTargetNumberValid()) {
                    // if we won't be able to pay the cost, don't choose the card
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
