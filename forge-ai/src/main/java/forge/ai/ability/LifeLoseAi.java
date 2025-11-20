package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

import java.util.List;

public class LifeLoseAi extends SpellAbilityAi {

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#chkAIDrawback(forge.game.spellability.
     * SpellAbility, forge.game.player.Player)
     */
    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        final PlayerCollection tgtPlayers = getPlayers(ai, sa);

        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // something already set PayX
            SpellAbility root = sa.getRootAbility();
            if (root.getXManaCostPaid() != null) {
                amount = root.getXManaCostPaid();
            } else if (root.getPayCosts() != null && root.getPayCosts().hasXInAnyCostPart()) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                root.setXManaCostPaid(xPay);
                amount = xPay;
            }
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        if (tgtPlayers.contains(ai) && amount > 0 && amount + 3 > ai.getLife()) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (sa.usesTargeting()) {
            boolean result = doTgt(ai, sa, false);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#willPayCosts(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.cost.Cost,
     * forge.game.card.Card)
     */
    @Override
    protected boolean willPayCosts(Player payer, SpellAbility sa, Cost cost, Card source) {
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;

        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilCost.getMaxXValue(sa, payer, sa.isTrigger());
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        // special logic for checkLifeCost
        if (!ComputerUtilCost.checkLifeCost(payer, cost, source, amount, sa)) {
            return false;
        }

        // other cost as the same
        return super.willPayCosts(payer, sa, cost, source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#checkApiLogic(forge.game.player.Player,
     * forge.game.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        int amount = 0;

        if (sa.usesTargeting()) {
            if (!doTgt(ai, sa, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            sa.setXManaCostPaid(amount);
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        if (amount <= 0) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (ComputerUtil.playImmediately(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        final PlayerCollection tgtPlayers = getPlayers(ai, sa);
         // TODO: check against the amount we could obtain when multiple activations are possible
        PlayerCollection filteredPlayer = tgtPlayers
                .filter(PlayerPredicates.isOpponentOf(ai).and(PlayerPredicates.lifeLessOrEqualTo(amount)));
        // killing opponents asap
        if (!filteredPlayer.isEmpty()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        // Sacrificing a creature in response to something dangerous is generally good in any phase
        boolean isSacCost = false;
        if (sa.getPayCosts() != null && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
            isSacCost = true;
        }

        // Don't use loselife before main 2 if possible
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                && !ComputerUtil.castSpellInMain1(ai, sa) && !aiLogic.contains("AnyPhase") && !isSacCost) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForMain2);
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }

        if (isSorcerySpeed(sa, ai) || sa.hasParam("ActivationPhases") || playReusable(ai, sa)
                || ComputerUtil.activateForCost(sa, ai)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.ai.SpellAbilityAi#doTriggerAINoCost(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, boolean)
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa,
                                                final boolean mandatory) {
        if (sa.usesTargeting()) {
            if (!doTgt(ai, sa, mandatory)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        final Card source = sa.getHostCard();
        final String amountStr = sa.getParam("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && sa.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, true);
            sa.setXManaCostPaid(xPay);
            amount = xPay;
        } else {
            amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        }

        final List<Player> tgtPlayers = sa.usesTargeting() && !sa.hasParam("Defined")
                ? new FCollection<>(sa.getTargets().getTargetPlayers())
                : AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

        // For cards like Foul Imp, ETB you lose life
        if (mandatory) {
            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
        }

        if (!tgtPlayers.contains(ai) || amount <= 0 || amount + 3 <= ai.getLife()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid,
                                     FCollectionView<Player> payers) {
        if (!payer.canLoseLife() || payer.cantLoseForZeroOrLessLife()) {
            return false;
        }

        final Card source = sa.getHostCard();

        // Withercrown should be sacrificed early?
        if (source.canBeSacrificedBy(sa, true) && cost.hasOnlySpecificCostType(CostSacrifice.class)) {
            CostSacrifice costSac = cost.getCostPartByType(CostSacrifice.class);
            if (costSac.payCostFromSource()) {
                return true;
            }
        }
        int n = AbilityUtils.calculateAmount(source, sa.getParam("LifeAmount"), sa);
        // what should be the limit where AI stops letting it lose life?
        // TODO predict lose life modifier
        // also check if life loss would trigger life gain for Activating Player
        // and that resulting in another life loss
        if (payer.getLife() < 2 * n) {
            return true;
        }

        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }

    protected boolean doTgt(Player ai, SpellAbility sa, boolean mandatory) {
        sa.resetTargets();
        PlayerCollection opps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        // try first to find Opponent that can lose life and lose the game
        if (!opps.isEmpty()) {
            for (Player opp : opps) {
                if (opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife()) {
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
