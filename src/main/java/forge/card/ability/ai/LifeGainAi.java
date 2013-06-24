package forge.card.ability.ai;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * TODO: Write javadoc for this type.
 *
 */

public class LifeGainAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final Game game = source.getGame();
        final int life = ai.getLife();
        final String amountStr = sa.getParam("LifeAmount");
        int lifeAmount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            lifeAmount = xPay;
        } else {
            lifeAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        // don't use it if no life to gain
        if (lifeAmount <= 0) {
            return false;
        }
        // don't play if the conditions aren't met, unless it would trigger a
        // beneficial sub-condition
        if (!sa.getConditions().areMet(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!abSub.getConditions().areMet(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !sa.hasParam("ActivationPhases")) {
            return false;
        }
        boolean lifeCritical = life <= 5;
        lifeCritical |= game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DAMAGE) && ComputerUtilCombat.lifeInDanger(ai, game.getCombat());

        if (abCost != null && !lifeCritical) {
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
        }

        if (!ai.canGainLife()) {
            return false;
        }

        // Don't use lifegain before main 2 if possible
        if (!lifeCritical && game.getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")) {
            return false;
        }

        // Don't use lifegain before main 2 if possible
        if (!lifeCritical && (!game.getPhaseHandler().getNextTurn().equals(ai)
                || game.getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN))
                && !sa.hasParam("PlayerTurn") && !SpellAbilityAi.isSorcerySpeed(sa)
                && !ComputerUtil.ActivateForSacCost(sa, ai)) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
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
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
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

        final Card source = sa.getSourceCard();
        final String amountStr = sa.getParam("LifeAmount");
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        return true;
    }
}
