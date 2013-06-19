package forge.card.ability;


import java.util.List;

import forge.Card;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;

public abstract class SpellAbilityAi extends SaTargetRountines {

    public final boolean canPlayAIWithSubs(final Player aiPlayer, final SpellAbility sa) {
        if (!canPlayAI(aiPlayer, sa)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        return subAb == null || chkDrawbackWithSubs(aiPlayer,  subAb);
    }

    protected abstract boolean canPlayAI(final Player aiPlayer, final SpellAbility sa);

    public final boolean doTriggerAI(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if (!ComputerUtilCost.canPayCost(sa, aiPlayer) && !mandatory) {
            return false;
        }

        return doTriggerNoCostWithSubs(aiPlayer, sa, mandatory);
    }

    public final boolean doTriggerNoCostWithSubs(final Player aiPlayer, final SpellAbility sa, final boolean mandatory)
    {
        if (!doTriggerAINoCost(aiPlayer, sa, mandatory)) {
            return false;
        }
        final AbilitySub subAb = sa.getSubAbility();
        return subAb == null || chkDrawbackWithSubs(aiPlayer,  subAb) || mandatory;
    }

    protected boolean doTriggerAINoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    public boolean chkAIDrawback(final SpellAbility sa, final Player aiPlayer) {
        return true;
    }

    /**
     * <p>
     * isSorcerySpeed.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    protected static boolean isSorcerySpeed(final SpellAbility sa) {
        return ( sa.isSpell() &&  sa.getSourceCard().isSorcery() ) 
            || ( sa.isAbility() && sa.getRestrictions().isSorcerySpeed() );
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
    
        if (sa.getRestrictions().getPlaneswalker() && ai.getGame().getPhaseHandler().is(PhaseType.MAIN2)) {
            return true;
        }
        if (sa.isTrigger()) {
            return true;
        }
        if (sa.isSpell() && !sa.isBuyBackAbility()) {
            return false;
        }
        
        PhaseHandler phase = ai.getGame().getPhaseHandler();
        return phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param ai
     * @param subAb
     * @return
     */
    public boolean chkDrawbackWithSubs(Player aiPlayer, AbilitySub ab) {
        final AbilitySub subAb = ab.getSubAbility();
        return ab.getAi().chkAIDrawback(ab, aiPlayer) && (subAb == null || chkDrawbackWithSubs(aiPlayer, subAb));  
    }
    
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of confirmAction is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return true;
    }

    public Card chooseSingleCard(Player ai, SpellAbility sa, List<Card> options, boolean isOptional) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSingleCard is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return options.get(0);
    }
    
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, List<Player> options) {
        System.err.println("Warning: default (ie. inherited from base class) implementation of chooseSinglePlayer is used for " + this.getClass().getName() + ". Consider declaring an overloaded method");
        return options.get(0);
    }
}
