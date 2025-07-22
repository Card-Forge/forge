package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class ProtectAllAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        // if there is no target and host card isn't in play, don't activate
        if (!sa.usesTargeting() && !hostCard.isInPlay()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!willPayCosts(ai, sa, cost, hostCard)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    } // protectAllCanPlayAI()

    @Override
    protected AiAbilityDecision doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
