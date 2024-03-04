package forge.ai.ability;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.LandAbility;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class DiscoverAi extends SpellAbilityAi {

    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false; // prevent infinite loop
        }

        return true;
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
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return mandatory || checkApiLogic(ai, sa);
    }

    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        Card c = (Card)params.get("Card");
        for (SpellAbility s : AbilityUtils.getBasicSpellsFromPlayEffect(c, ai)) {
            if (s instanceof LandAbility) {
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
