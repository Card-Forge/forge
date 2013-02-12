package forge.card.ability.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class CountersMoveAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final String amountStr = sa.getParam("CounterNum");

        // TODO handle proper calculation of X values based on Cost
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = false;

        if (SpellAiLogic.playReusable(ai, sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    } // moveCounterCanPlayAI

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getSourceCard();
        final Target abTgt = sa.getTarget();
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);
        boolean chance = false;
        boolean preferred = true;

        final CounterType cType = CounterType.valueOf(sa.getParam("CounterType"));
        final List<Card> srcCards = AbilityUtils.getDefinedCards(host, sa.getParam("Source"), sa);
        final List<Card> destCards = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
        if (abTgt == null) {
            if ((srcCards.size() > 0)
                    && cType.equals(CounterType.P1P1) // move +1/+1 counters away
                                                   // from
                                                   // permanents that cannot use
                                                   // them
                    && (destCards.size() > 0) && destCards.get(0).getController() == ai
                    && (!srcCards.get(0).isCreature() || srcCards.get(0).hasStartOfKeyword("CARDNAME can't attack"))) {

                chance = true;
            }
        } else { // targeted
            final Player player = sa.isCurse() ? ai.getOpponent() : ai;
            List<Card> list = CardLists.getTargetableCards(player.getCardsIn(ZoneType.Battlefield), sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), host.getController(), host);
            if (list.isEmpty() && mandatory) {
                // If there isn't any prefered cards to target, gotta choose
                // non-preferred ones
                list = CardLists.getTargetableCards(player.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
                list = CardLists.getValidCards(list, abTgt.getValidTgts(), host.getController(), host);
                preferred = false;
            }
            // Not mandatory, or the the list was regenerated and is still
            // empty,
            // so return false since there are no targets
            if (list.isEmpty()) {
                return false;
            }

            Card choice = null;

            // Choose targets here:
            if (sa.isCurse()) {
                if (preferred) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                }

                else {
                    if (type.equals("M1M1")) {
                        choice = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        choice = CardFactoryUtil.getRandomCard(list);
                    }
                }
            } else {
                if (preferred) {
                    choice = CountersAi.chooseBoonTarget(list, type);
                }

                else {
                    if (type.equals("P1P1")) {
                        choice = CardFactoryUtil.getWorstCreatureAI(list);
                    } else {
                        choice = CardFactoryUtil.getRandomCard(list);
                    }
                }
            }

            // TODO - I think choice can be null here. Is that ok for
            // addTarget()?
            abTgt.addTarget(choice);
        }

        return chance;
    }

}
