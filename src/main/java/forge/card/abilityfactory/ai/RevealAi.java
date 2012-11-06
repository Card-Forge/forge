package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.util.MyRandom;

public class RevealAi extends RevealAiBase {
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // AI cannot use this properly until he can use SAs during Humans turn
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }

        }

        // we can reuse this function here...
        final boolean bFlag = revealHandTargetAI(ai, sa, true, false);

        if (!bFlag) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (AbilityFactory.playReusable(ai, sa)) {
            randomReturn = true;
        }
        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
    
        if (!revealHandTargetAI(ai, sa, false, mandatory)) {
            return false;
        }
    
        return true;
    }

}