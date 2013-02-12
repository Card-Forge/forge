package forge.card.ability;


import forge.Singletons;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;

public abstract class SpellAiLogic {

    public final boolean canPlayAIWithSubs(final AIPlayer aiPlayer, final SpellAbility sa) {
        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null && !subAb.chkAIDrawback(aiPlayer)) {
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
        if (subAb != null && !subAb.chkAIDrawback(aiPlayer) && !mandatory) {
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

    /**
     * <p>
     * playReusable.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    protected static boolean playReusable(final Player ai, final SpellAbility sa) {
        // TODO probably also consider if winter orb or similar are out
    
        if (sa.getPayCosts() == null) {
            return true; // This is only true for Drawbacks and triggers
        }
    
        if (!sa.getPayCosts().isReusuableResource()) {
            return false;
        }
    
        if (sa.getRestrictions().getPlaneswalker() && Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return true;
        }
    
        return Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)
             && Singletons.getModel().getGame().getPhaseHandler().getNextTurn().equals(ai);
    }
}
