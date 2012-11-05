package forge.card.abilityfactory.ai;

import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class ProtectAllAi extends SpellAiLogic {
    
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        final Card hostCard = sa.getAbilityFactory().getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTarget() == null) && !hostCard.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!CostUtil.checkLifeCost(ai, cost, hostCard, 4, null)) {
            return false;
        }

        if (!CostUtil.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!CostUtil.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        return false;
    } // protectAllCanPlayAI()


    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(Map<String, String> params, SpellAbility sa, Player aiPlayer) {
        return true;
    } // protectAllDrawbackAI()
}