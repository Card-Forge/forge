package forge.card.ability.ai;

import java.util.Random;

import forge.Card;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.util.MyRandom;

public class CountersRemoveAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        // List<Card> list;
        // Card choice = null;

        final String type = sa.getParam("CounterType");
        // String amountStr = sa.get("CounterNum");

        // TODO - currently, not targeted, only for Self

        // Player player = af.isCurse() ? AllZone.getHumanPlayer() :
        // AllZone.getComputerPlayer();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // TODO handle proper calculation of X values based on Cost
        // final int amount = calculateAmount(sa.getSourceCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // currently, not targeted
        if (abTgt != null) {
            return false;
        }

        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")
                && !type.equals("M1M1")) {
            return false;
        }

        if (!type.matches("Any")) {
            final int currCounters = sa.getSourceCard().getCounters(CounterType.valueOf(type));
            if (currCounters < 1) {
                return false;
            }
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer aiPlayer, SpellAbility sa, boolean mandatory) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the
        // expected targets could be
        boolean chance = true;

        // TODO - currently, not targeted, only for Self

        // Note: Not many cards even use Trigger and Remove Counters. And even
        // fewer are not mandatory
        // Since the targeting portion of this would be what


        return chance;
    }

}
