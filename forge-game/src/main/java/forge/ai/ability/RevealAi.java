package forge.ai.ability;

import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

import java.util.Random;

public class RevealAi extends RevealAiBase {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        // we can reuse this function here...
        final boolean bFlag = revealHandTargetAI(ai, sa/*, true, false*/);

        if (!bFlag) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (SpellAbilityAi.playReusable(ai, sa)) {
            randomReturn = true;
        }
        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        if (!revealHandTargetAI(ai, sa/*, false, mandatory*/)) {
            return false;
        }

        return true;
    }

}
