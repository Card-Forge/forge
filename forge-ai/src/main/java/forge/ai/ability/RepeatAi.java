package forge.ai.ability;


import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.ai.*;
import forge.game.GlobalRuleChange;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;

public class RepeatAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        String logic = sa.getParamOrDefault("AILogic", "");

        if (sa.usesTargeting()) {
            if (!sa.canTarget(opp)) {
                return false;
            }
            sa.resetTargets();
            sa.getTargets().add(opp);
        }
        if ("MaxX".equals(logic) || "MaxXAtOppEOT".equals(logic)) {
            if ("MaxXAtOppEOT".equals(logic) && !(ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN) && ai.getGame().getPhaseHandler().getNextTurn() == ai)) {
                return false;
            }
            // Set PayX here to maximum value.
            final int max = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(max);
            return max > 0;
        }
        return true;
    }
    
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
      //TODO add logic to have computer make better choice (ArsenalNut)
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        String logic = sa.getParamOrDefault("AILogic", "");

        if (sa.usesTargeting()) {
            if (logic.startsWith("CopyBestCreature")) {
                Card best = null;
                if (!ai.getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule) || logic.endsWith("IgnoreLegendary")) {
                    best = ComputerUtilCard.getBestAI(Iterables.filter(ai.getCreaturesInPlay(), Predicates.and(
                            CardPredicates.isTargetableBy(sa), new Predicate<Card>() {
                                @Override
                                public boolean apply(Card card) {
                                    return !card.getType().isLegendary();
                                }
                            })));
                } else {
                    best = ComputerUtilCard.getBestAI(Iterables.filter(ai.getCreaturesInPlay(), CardPredicates.isTargetableBy(sa)));
                }
                if (best == null && mandatory && sa.canTarget(sa.getHostCard())) {
                    best = sa.getHostCard();
                }
                if (best != null) {
                    sa.resetTargets();
                    sa.getTargets().add(best);
                    return true;
                }
                return false;
            }

            PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
            if (opp != null) {
                sa.resetTargets();
                sa.getTargets().add(opp);
            } else if (!mandatory) {
                return false;
            }

        }

    	// setup subability to repeat
        final SpellAbility repeat = sa.getAdditionalAbility("RepeatSubAbility");

        if (repeat == null) {
        	return mandatory;
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        return aic.doTrigger(repeat, mandatory);
    }
}
