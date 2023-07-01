package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ProtectAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if (!sa.usesTargeting() && !hostCard.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!willPayCosts(ai, sa, cost, hostCard)) {
            return false;
        }

        return false;
    } // protectAllCanPlayAI()

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return true;
    }
}
