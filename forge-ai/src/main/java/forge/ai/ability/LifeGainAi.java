package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class LifeGainAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final int life = ai.getLife();
        final String amountStr = sa.getParam("LifeAmount");
        int lifeAmount = 0;
        boolean activateForCost = ComputerUtil.activateForCost(sa, ai);
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            lifeAmount = xPay;
        } else {
            lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        }

        //Ugin AI: always use ultimate
        if (source.getName().equals("Ugin, the Spirit Dragon")) {
          //TODO: somehow link with DamageDealAi for cases where +1 = win
            return true;
        }
        
        // don't use it if no life to gain
        if (!activateForCost && lifeAmount <= 0) {
            return false;
        }
        // don't play if the conditions aren't met, unless it would trigger a
        // beneficial sub-condition
        if (!activateForCost && !sa.getConditions().areMet(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!abSub.getConditions().areMet(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        boolean lifeCritical = life <= 5;
        lifeCritical |= ph.getPhase().isBefore(PhaseType.COMBAT_DAMAGE) && ComputerUtilCombat.lifeInDanger(ai, game.getCombat());

        if (abCost != null) {
            if (!lifeCritical) {
	            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source, false)) {
	                return false;
	            }
	            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
	                return false;
	            }
	
	            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
	                return false;
	            }
	
	            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
	                return false;
	            }
            } else {
            	// don't sac possible blockers
            	if (!ph.getPhase().equals(PhaseType.COMBAT_DECLARE_BLOCKERS) || !game.getCombat().getDefenders().contains(ai)) {
    	            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source, false)) {
    	                return false;
    	            }
            	}
            }
        }

        if (!activateForCost && !ai.canGainLife()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }
        
        // Don't use lifegain before main 2 if possible
        if (!lifeCritical && ph.getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases") && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
        }

        if (!lifeCritical && !activateForCost && (!ph.getNextTurn().equals(ai)
                || ph.getPhase().isBefore(PhaseType.END_OF_TURN))
                && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else {
                return false;
            }
        }

        return true;
    }


    /**
     * <p>
     * gainLifeDoTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     *
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa,
    final boolean mandatory) {

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
            } else if (mandatory && sa.canTarget(ai.getOpponent())) {
                sa.getTargets().add(ai.getOpponent());
            } else {
                return false;
            }
        }

        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        return true;
    }
    
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
    	return doTriggerAINoCost(ai, sa, true);
    }
}
