package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.util.MyRandom;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class AlterLifeAi {

    public static class GainLifeAi extends SpellAiLogic {
    
    /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {

        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        final int life = ai.getLife();
        final String amountStr = params.get("LifeAmount");
        int lifeAmount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            lifeAmount = xPay;
        } else {
            lifeAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
        }

        // don't use it if no life to gain
        if (lifeAmount <= 0) {
            return false;
        }
        // don't play if the conditions aren't met, unless it would trigger a
        // beneficial sub-condition
        if (!AbilityFactory.checkConditional(sa)) {
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null && !sa.isWrapper() && "True".equals(source.getSVar("AIPlayForSub"))) {
                if (!AbilityFactory.checkConditional(abSub)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !params.containsKey("ActivationPhases")) {
            return false;
        }
        boolean lifeCritical = life <= 5;
        lifeCritical |= (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DAMAGE) && CombatUtil
                .lifeInDanger(ai, Singletons.getModel().getGame().getCombat()));

        if (abCost != null && !lifeCritical) {
            if (!CostUtil.checkSacrificeCost(ai, abCost, source, false)) {
                return false;
            }

            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        if (!ai.canGainLife()) {
            return false;
        }

        // Don't use lifegain before main 2 if possible
        if (!lifeCritical && Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases")) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        // final int amount = calculateAmount(af.getHostCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else {
                return false;
            }
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (lifeCritical || AbilityFactory.playReusable(ai, sa)) {
            randomReturn = true;
        }

        return (randomReturn && chance);
    }


    /**
     * <p>
     * gainLifeDoTriggerAINoCost.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    public boolean doTriggerAINoCost(final Player ai, final Map<String, String> params,
            final SpellAbility sa, final boolean mandatory) {

        // If the Target is gaining life, target self.
        // if the Target is modifying how much life is gained, this needs to be
        // handled better
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else if (mandatory && sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else {
                return false;
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }

    /**
     * <p>
     * loseLifeCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    
    }
    
    public static class LoseLifeAi extends SpellAiLogic {
    
    /* (non-Javadoc)
         * @see forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
         */
        @Override
        public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {

        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getSourceCard();
        boolean priority = false;

        final String amountStr = params.get("LifeAmount");

        // TODO handle proper calculation of X values based on Cost and what
        // would be paid
        int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }

        if (amount <= 0) {
            return false;
        }

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, amount, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        Player opp = ai.getOpponent();

        if (!opp.canLoseLife()) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (amount >= opp.getLife()) {
            priority = true; // killing the human should be done asap
        }

        // Don't use loselife before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases") && !priority) {
            return false;
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa) && !priority) {
            return false;
        }

        final Target tgt = sa.getTarget();

        if (sa.getTarget() != null) {
            tgt.resetTargets();
            if (sa.canTarget(opp)) {
                sa.getTarget().addTarget(opp);
            } else {
                return false;
            }
        }

        boolean randomReturn = r.nextFloat() <= .6667;
        if (AbilityFactory.playReusable(ai, sa) || priority) {
            randomReturn = true;
        }

        return (randomReturn);
    }


    /**
     * <p>
     * loseLifeDoTriggerAINoCost.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    public boolean doTriggerAINoCost(final Player ai, final Map<String, String> params,
            final SpellAbility sa, final boolean mandatory) {
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            if (sa.canTarget(ai.getOpponent())) {
                tgt.addTarget(ai.getOpponent());
            } else if (mandatory && sa.canTarget(ai)) {
                tgt.addTarget(ai);
            } else {
                return false;
            }
        }

        final Card source = sa.getSourceCard();
        final String amountStr = params.get("LifeAmount");
        int amount = 0;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            amount = xPay;
        } else {
            amount = AbilityFactory.calculateAmount(source, amountStr, sa);
        }

        ArrayList<Player> tgtPlayers;
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (!mandatory && tgtPlayers.contains(ai)) {
            // For cards like Foul Imp, ETB you lose life
            if ((amount + 3) > ai.getLife()) {
                return false;
            }
        }

        // check SubAbilities DoTrigger?
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            return abSub.doTrigger(mandatory);
        }

        return true;
    }
    }

    public static class SetLifeAi extends SpellAiLogic {

        @Override
        public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
            final Random r = MyRandom.getRandom();
            // Ability_Cost abCost = sa.getPayCosts();
            final Card source = sa.getSourceCard();
            final int myLife = ai.getLife();
            final Player opponent = ai.getOpponent();
            final int hlife = opponent.getLife();
            final String amountStr = params.get("LifeAmount");

            if (!ai.canGainLife()) {
                return false;
            }

            // Don't use setLife before main 2 if possible
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && !params.containsKey("ActivationPhases")) {
                return false;
            }

            // TODO handle proper calculation of X values based on Cost and what
            // would be paid
            int amount;
            // we shouldn't have to worry too much about PayX for SetLife
            if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                amount = xPay;
            } else {
                amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
            }

            // prevent run-away activations - first time will always return true
            final boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    tgt.addTarget(opponent);
                    // if we can only target the human, and the Human's life
                    // would
                    // go up, don't play it.
                    // possibly add a combo here for Magister Sphinx and
                    // Higedetsu's
                    // (sp?) Second Rite
                    if ((amount > hlife) || !opponent.canLoseLife()) {
                        return false;
                    }
                } else {
                    if ((amount > myLife) && (myLife <= 10)) {
                        tgt.addTarget(ai);
                    } else if (hlife > amount) {
                        tgt.addTarget(opponent);
                    } else if (amount > myLife) {
                        tgt.addTarget(ai);
                    } else {
                        return false;
                    }
                }
            } else {
                if (params.containsKey("Each") && params.get("Defined").equals("Each")) {
                    if (amount == 0) {
                        return false;
                    } else if (myLife > amount) { // will decrease computer's
                                                  // life
                        if ((myLife < 5) || ((myLife - amount) > (hlife - amount))) {
                            return false;
                        }
                    }
                }
                if (amount < myLife) {
                    return false;
                }
            }

            // if life is in danger, always activate
            if ((myLife < 3) && (amount > myLife)) {
                return true;
            }

            return ((r.nextFloat() < .6667) && chance);
        }

        @Override
        public boolean doTriggerAI(Player ai, Map<String, String> params, SpellAbility sa, boolean mandatory) {
            final int myLife = ai.getLife();
            final Player opponent = ai.getOpponent();
            final int hlife = opponent.getLife();
            final Card source = sa.getSourceCard();

            final String amountStr = params.get("LifeAmount");

            // If there is a cost payment it's usually not mandatory
            if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
                return false;
            }

            int amount;
            if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                amount = xPay;
            } else {
                amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);
            }

            if (source.getName().equals("Eternity Vessel")
                    && (opponent.isCardInPlay("Vampire Hexmage") || (source.getCounters(Counters.CHARGE) == 0))) {
                return false;
            }

            // If the Target is gaining life, target self.
            // if the Target is modifying how much life is gained, this needs to
            // be
            // handled better
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (tgt.canOnlyTgtOpponent()) {
                    tgt.addTarget(opponent);
                } else {
                    if ((amount > myLife) && (myLife <= 10)) {
                        tgt.addTarget(ai);
                    } else if (hlife > amount) {
                        tgt.addTarget(opponent);
                    } else if (amount > myLife) {
                        tgt.addTarget(ai);
                    } else {
                        return false;
                    }
                }
            }

            // check SubAbilities DoTrigger?
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return abSub.doTrigger(mandatory);
            }

            return true;
        }

    }

    public static class ExchangeLifeAi extends SpellAiLogic {

        /*
         * (non-Javadoc)
         * 
         * @see
         * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
         * (forge.game.player.Player, java.util.Map,
         * forge.card.spellability.SpellAbility)
         */
        @Override
        public boolean canPlayAI(Player aiPlayer, Map<String, String> params, SpellAbility sa) {
            final Random r = MyRandom.getRandom();
            final int myLife = aiPlayer.getLife();
            Player opponent = aiPlayer.getOpponent();
            final int hLife = opponent.getLife();

            if (!aiPlayer.canGainLife()) {
                return false;
            }

            // prevent run-away activations - first time will always return true
            boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

            /*
             * TODO - There is one card that takes two targets (Soul Conduit)
             * and one card that has a conditional (Psychic Transfer) that are
             * not currently handled
             */
            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.resetTargets();
                if (opponent.canBeTargetedBy(sa)) {
                    // never target self, that would be silly for exchange
                    tgt.addTarget(opponent);
                    if (!opponent.canLoseLife()) {
                        return false;
                    }
                }
            }

            // if life is in danger, always activate
            if ((myLife < 5) && (hLife > myLife)) {
                return true;
            }

            // cost includes sacrifice probably, so make sure it's worth it
            chance &= (hLife > (myLife + 8));

            return ((r.nextFloat() < .6667) && chance);
        }


    }

    public static class PoisonAi extends SpellAiLogic {

        /*
         * (non-Javadoc)
         * 
         * @see
         * forge.card.abilityfactory.AbilityFactoryAlterLife.SpellAiLogic#canPlayAI
         * (forge.game.player.Player, java.util.Map,
         * forge.card.spellability.SpellAbility)
         */
        @Override
        public boolean canPlayAI(Player ai, Map<String, String> params, SpellAbility sa) {
            final Cost abCost = sa.getPayCosts();
            final Card source = sa.getSourceCard();
            // int humanPoison = AllZone.getHumanPlayer().getPoisonCounters();
            // int humanLife = AllZone.getHumanPlayer().getLife();
            // int aiPoison = AllZone.getComputerPlayer().getPoisonCounters();

            // TODO handle proper calculation of X values based on Cost and what
            // would be paid
            // final int amount =
            // AbilityFactory.calculateAmount(af.getHostCard(),
            // amountStr, sa);

            if (abCost != null) {
                // AI currently disabled for these costs
                if (!CostUtil.checkLifeCost(ai, abCost, source, 1, null)) {
                    return false;
                }

                if (!CostUtil.checkSacrificeCost(ai, abCost, source)) {
                    return false;
                }
            }

            // Don't use poison before main 2 if possible
            if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                    && !params.containsKey("ActivationPhases")) {
                return false;
            }

            // Don't tap creatures that may be able to block
            if (ComputerUtil.waitForBlocking(sa)) {
                return false;
            }

            final Target tgt = sa.getTarget();

            if (sa.getTarget() != null) {
                tgt.resetTargets();
                sa.getTarget().addTarget(ai.getOpponent());
            }

            return true;
        }

        @Override
        public boolean doTriggerAI(Player ai, Map<String, String> params, SpellAbility sa, boolean mandatory) {

            if (!ComputerUtil.canPayCost(sa, ai) && !mandatory) {
                // payment it's usually
                // not mandatory
                return false;
            }

            final Target tgt = sa.getTarget();
            if (tgt != null) {
                tgt.addTarget(ai.getOpponent());
            } else {
                final ArrayList<Player> players = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                        params.get("Defined"), sa);
                for (final Player p : players) {
                    if (!mandatory && p.isComputer() && (p.getPoisonCounters() > p.getOpponent().getPoisonCounters())) {
                        return false;
                    }
                }
            }

            // check SubAbilities DoTrigger?
            final AbilitySub abSub = sa.getSubAbility();
            if (abSub != null) {
                return abSub.doTrigger(mandatory);
            }

            return true;
        }
    }
}
