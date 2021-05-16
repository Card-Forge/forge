package forge.ai.ability;

import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class LifeGainAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#willPayCosts(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.cost.Cost,
     * forge.game.card.Card)
     */
    @Override
    protected boolean willPayCosts(Player ai, SpellAbility sa, Cost cost, Card source) {
        final Game game = source.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final int life = ai.getLife();

        boolean lifeCritical = life <= 5;
        lifeCritical |= ph.getPhase().isBefore(PhaseType.COMBAT_DAMAGE)
                && ComputerUtilCombat.lifeInDanger(ai, game.getCombat());

        if (!lifeCritical) {
            // return super.willPayCosts(ai, sa, cost, source);
            if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source, sa, false)) {
                return false;
            }
            if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 4, sa)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, cost, source, sa)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(cost, source, sa)) {
                return false;
            }
        } else {
            // don't sac possible blockers
            if (!ph.getPhase().equals(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || !game.getCombat().getDefenders().contains(ai)) {
                boolean skipCheck = false;
                // if it's a sac self cost and the effect source is not a
                // creature, skip this check
                // (e.g. Woodweaver's Puzzleknot)
                skipCheck |= ComputerUtilCost.isSacrificeSelfCost(cost) && !source.isCreature();

                if (!skipCheck) {
                    if (!ComputerUtilCost.checkSacrificeCost(ai, cost, source, sa,false)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Game game = ai.getGame();
        final int life = ai.getLife();
        boolean activateForCost = ComputerUtil.activateForCost(sa, ai);

        boolean lifeCritical = life <= 5;
        lifeCritical |= ph.getPhase().isBefore(PhaseType.COMBAT_DAMAGE)
                && ComputerUtilCombat.lifeInDanger(ai, game.getCombat());

        // When life is critical but there is no immediate danger, try to wait until declare blockers
        // before using the lifegain ability if it's an ability on a creature with a detrimental activation cost
        if (lifeCritical
                && sa.isAbility()
                && sa.getHostCard() != null && sa.getHostCard().isCreature()
                && (sa.getPayCosts().hasSpecificCostType(CostRemoveCounter.class) || sa.getPayCosts().hasSpecificCostType(CostSacrifice.class))) {
            if (!game.getStack().isEmpty()) {
                SpellAbility saTop = game.getStack().peekAbility();
                if (saTop.getTargets() != null && Iterables.contains(saTop.getTargets().getTargetPlayers(), ai)) {
                    return ComputerUtil.predictDamageFromSpell(saTop, ai) > 0;
                }
            }
            if (game.getCombat() == null) { return false; }
            if (!ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) { return false; }
        }

        // Don't use lifegain before main 2 if possible
        if (!lifeCritical && ph.getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                && !ComputerUtil.castSpellInMain1(ai, sa)) {
            return false;
        }

        return lifeCritical || activateForCost
                || (ph.getNextTurn().equals(ai) && !ph.getPhase().isBefore(PhaseType.END_OF_TURN))
                || sa.hasParam("PlayerTurn") || SpellAbilityAi.isSorcerySpeed(sa);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);

        final int life = ai.getLife();
        final String amountStr = sa.getParam("LifeAmount");
        int lifeAmount = 0;
        boolean activateForCost = ComputerUtil.activateForCost(sa, ai);
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(xPay);
            lifeAmount = xPay;
        } else {
            lifeAmount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        }

        // Ugin AI: always use ultimate
        if (sourceName.equals("Ugin, the Spirit Dragon")) {
            // TODO: somehow link with DamageDealAi for cases where +1 = win
            return true;
        }

        // don't use it if no life to gain
        if (!activateForCost && lifeAmount <= 0) {
            return false;
        }
        // don't play if the conditions aren't met, unless it would trigger a
        // beneficial sub-condition
        if (!activateForCost && !sa.metConditions()) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!abSub.getConditions().areMet(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (!activateForCost && !ai.canGainLife()) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (sa.usesTargeting()) {
            if (!target(ai, sa, true)) {
                return false;
            }
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }

        if (SpellAbilityAi.isSorcerySpeed(sa)
                || sa.getSubAbility() != null || SpellAbilityAi.playReusable(ai, sa)) {
            return true;
        }
        
        // Save instant-speed life-gain unless it is really worth it
        final float value = 0.9f * lifeAmount / life;
        if (value < 0.2f) {
            return false;
        }
        return MyRandom.getRandom().nextFloat() < value;
    }

    /**
     * <p>
     * gainLifeDoTriggerAINoCost.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa,
    final boolean mandatory) {

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        if (sa.usesTargeting()) {
            if (!target(ai, sa, mandatory)) {
                return false;
            }
        }

        final String amountStr = sa.getParam("LifeAmount");
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(xPay);
        }

        return true;
    }
    
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
    	return doTriggerAINoCost(ai, sa, true);
    }

    private boolean target(Player ai, SpellAbility sa, boolean mandatory) {
        Card source = sa.getHostCard();
        sa.resetTargets();
        // TODO : add add even more logic into it
        // try to target opponents first if that would kill them

        PlayerCollection opps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        PlayerCollection allies = ai.getAllies().filter(PlayerPredicates.isTargetableBy(sa));

        if (sa.canTarget(ai) && ComputerUtil.lifegainPositive(ai, source)) {
            sa.getTargets().add(ai);
        } else {
            boolean hasTgt = false;
            // check for Lifegain negative on opponents
            for (Player opp : opps) {
                if (ComputerUtil.lifegainNegative(opp, source)) {
                    sa.getTargets().add(opp);
                    hasTgt = true;
                    break;
                }
            }
            if (!hasTgt) {
                // lifegain on ally
                for (Player ally : allies) {
                    if (ComputerUtil.lifegainPositive(ally, source)) {
                        sa.getTargets().add(ally);
                        hasTgt = true;
                        break;
                    }
                }
            }
            if (!hasTgt && mandatory) {
                // need to target something but its neither negative against
                // opponents, nor positive against allies

                // hurting ally is probably better than healing opponent
                // look for Lifegain not Negative (case of lifegain negated)
                for (Player ally : allies) {
                    if (!ComputerUtil.lifegainNegative(ally, source)) {
                        sa.getTargets().add(ally);
                        hasTgt = true;
                        break;
                    }
                }
                if (!hasTgt) {
                    // same for opponent lifegain not positive
                    for (Player opp : opps) {
                        if (!ComputerUtil.lifegainPositive(opp, source)) {
                            sa.getTargets().add(opp);
                            hasTgt = true;
                            break;
                        }
                    }
                }

                // still no luck, try to target ally with most life
                if (!allies.isEmpty()) {
                    Player ally = allies.max(PlayerPredicates.compareByLife());
                    sa.getTargets().add(ally);
                    hasTgt = true;
                }
                // better heal opponent which most life then the one with the lowest
                if (!hasTgt) {
                    Player opp = opps.max(PlayerPredicates.compareByLife());
                    sa.getTargets().add(opp);
                    hasTgt = true;
                }
            }
            return hasTgt;
        }
        return true;
    }
}
