package forge.card.abilityfactory;


import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.AIPlayer;

public abstract class SpellAiLogic {

    public final boolean canPlayAIWithSubs(final AIPlayer aiPlayer, final SpellAbility sa) {
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback()) {
            return false;
        }
        return canPlayAI(aiPlayer, sa);
    }

    protected abstract boolean canPlayAI(final AIPlayer aiPlayer, final SpellAbility sa);

    public final boolean doTriggerAI(final AIPlayer aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtilCost.canPayCost(sa, aiPlayer) && !mandatory) {
            return false;
        }

        return doTriggerNoCostWithSubs(aiPlayer, sa, mandatory);
    }

    public final boolean doTriggerNoCostWithSubs(final AIPlayer aiPlayer, final SpellAbility sa, final boolean mandatory)
    {
        if (!doTriggerAINoCost(aiPlayer, sa, mandatory)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback() && !mandatory) {
            return false;
        }
        return true;
    }

    protected boolean doTriggerAINoCost(final AIPlayer aiPlayer, final SpellAbility sa, final boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    public boolean chkAIDrawback(final SpellAbility sa, final AIPlayer aiPlayer) {
        return true;
    }
}
