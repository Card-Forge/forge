package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.Card;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.MyRandom;

public class CountersRemoveAi extends SpellAiLogic {
    
    @Override
    protected boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        // List<Card> list;
        // Card choice = null;

        final String type = params.get("CounterType");
        // String amountStr = params.get("CounterNum");

        // TODO - currently, not targeted, only for Self

        // Player player = af.isCurse() ? AllZone.getHumanPlayer() :
        // AllZone.getComputerPlayer();

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!CostUtil.checkLifeCost(ai, abCost, source, 4, null)) {
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

        // TODO handle proper calculation of X values based on Cost
        // final int amount = calculateAmount(sa.getSourceCard(), amountStr, sa);

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        // currently, not targeted
        if (abTgt != null) {
            return false;
        }

        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !params.containsKey("ActivationPhases")
                && !type.equals("M1M1")) {
            return false;
        }

        if (!type.matches("Any")) {
            final int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
            if (currCounters < 1) {
                return false;
            }
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
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