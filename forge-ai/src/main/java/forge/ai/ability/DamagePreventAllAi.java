package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class DamagePreventAllAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        boolean chance = false;

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!willPayCosts(ai, sa, cost, hostCard)) {
            return false;
        }

        if (!ai.getGame().getStack().isEmpty()) {
            // TODO check stack for something on the stack will kill anything i control

        } // Protect combatants
        else if (ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // TODO
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        boolean chance = true;

        return chance;
    }
}
