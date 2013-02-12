package forge.card.ability.ai;


import forge.Card;
import forge.card.ability.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.AIPlayer;

public class ProtectAllAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTarget() == null) && !hostCard.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, cost, hostCard, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        return false;
    } // protectAllCanPlayAI()


    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
