package forge.ai.ability;

import java.util.List;

import com.google.common.base.Predicates;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollection;

public class LifeLoseAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#chkAIDrawback(forge.game.spellability.
     * SpellAbility, forge.game.player.Player)
     */
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final PlayerCollection tgtPlayers = getPlayers(ai, sa);

        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // something already set PayX
            SpellAbility root = sa.getRootAbility();
            if (root.getXManaCostPaid() != null) {
                amount = root.getXManaCostPaid();
            } else {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
                root.setXManaCostPaid(xPay);
                amount = xPay;
            }
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        if (tgtPlayers.contains(ai) && amount > 0 && amount + 3 > ai.getLife()) {
            return false;
        }

        if (sa.usesTargeting()) {
            return doTgt(ai, sa, false);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#willPayCosts(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.cost.Cost,
     * forge.game.card.Card)
     */
    @Override
    protected boolean willPayCosts(Player ai, SpellAbility sa, Cost cost, Card source) {
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;

        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        // special logic for checkLifeCost
        if (!ComputerUtilCost.checkLifeCost(ai, cost, source, amount, sa)) {
            return false;
        }

        // other cost as the same
        return super.willPayCosts(ai, sa, cost, source);
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
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;

        if (sa.usesTargeting()) {
            if (!doTgt(ai, sa, false)) {
                return false;
            }
        }

        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(amount);
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        if (amount <= 0) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return true;
        }

        final PlayerCollection tgtPlayers = getPlayers(ai, sa);
         // TODO: check against the amount we could obtain when multiple activations are possible
        PlayerCollection filteredPlayer = tgtPlayers
                .filter(Predicates.and(PlayerPredicates.isOpponentOf(ai), PlayerPredicates.lifeLessOrEqualTo(amount)));
        // killing opponents asap
        if (!filteredPlayer.isEmpty()) {
            return true;
        }

        // Don't use loselife before main 2 if possible
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                && !ComputerUtil.castSpellInMain1(ai, sa) && !"AnyPhase".equals(sa.getParam("AILogic"))) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        if (SpellAbilityAi.isSorcerySpeed(sa) || sa.hasParam("ActivationPhases") || SpellAbilityAi.playReusable(ai, sa)
                || ComputerUtil.activateForCost(sa, ai)) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(final Player ai, final SpellAbility sa,
    final boolean mandatory) {
        if (sa.usesTargeting()) {
            if (!doTgt(ai, sa, mandatory)) {
                return false;
            }
        }

        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            sa.setXManaCostPaid(xPay);
            amount = xPay;
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        final List<Player> tgtPlayers = sa.usesTargeting() && !sa.hasParam("Defined")
                ? new FCollection<>(sa.getTargets().getTargetPlayers())
                : AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

        // For cards like Foul Imp, ETB you lose life
        return mandatory || !tgtPlayers.contains(ai) || amount <= 0 || amount + 3 <= ai.getLife();
    }

    protected boolean doTgt(Player ai, SpellAbility sa, boolean mandatory) {
        sa.resetTargets();
        PlayerCollection opps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        // try first to find Opponent that can lose life and lose the game
        if (!opps.isEmpty()) {
            for (Player opp : opps) {
                if (opp.canLoseLife() && !opp.cantLose()) {
                    sa.getTargets().add(opp);
                    return true;
                }
            }
        }

        // do that only if needed
        if (mandatory) {
            if (!opps.isEmpty()) {
                // try another opponent even if it can't lose life
                sa.getTargets().add(opps.getFirst());
                return true;
            }
            // try hit ally instead of itself
            for (Player ally : ai.getAllies()) {
                if (sa.canTarget(ally)) {
                    sa.getTargets().add(ally);
                    return true;
                }
            }
            // need to hit itself
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
                return true;
            }
        }
        return false;
    }

    protected PlayerCollection getPlayers(Player ai, SpellAbility sa) {
        Iterable<Player> it;
        if (sa.usesTargeting() && !sa.hasParam("Defined")) {
            it = sa.getTargets().getTargetPlayers();
        } else {
            it = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
        }
        return new PlayerCollection(it);
    }
}
