package forge.card.ability.ai;

import java.util.Random;

import forge.Card;
import forge.card.ability.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.AIPlayer;
import forge.util.MyRandom;

public class RevealAi extends RevealAiBase {
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

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

        if (SpellAiLogic.playReusable(ai, sa)) {
            randomReturn = true;
        }
        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        if (!revealHandTargetAI(ai, sa/*, false, mandatory*/)) {
            return false;
        }

        return true;
    }

}
