package forge.ai.ability;

import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.List;
import java.util.Random;

public class CountersMoveAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final String amountStr = sa.getParam("CounterNum");

        // TODO handle proper calculation of X values based on Cost
        int amount = 0;
        if (!sa.getParam("CounterNum").equals("All")) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        }
        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = false;

        if (SpellAbilityAi.playReusable(ai, sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    } // moveCounterCanPlayAI

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getHostCard();
        final TargetRestrictions abTgt = sa.getTargetRestrictions();
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        int amount = 0;
        if (!sa.getParam("CounterNum").equals("All")) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        }
        boolean chance = false;
        boolean preferred = true;

        final CounterType cType = CounterType.valueOf(sa.getParam("CounterType"));
        final List<Card> srcCards = AbilityUtils.getDefinedCards(host, sa.getParam("Source"), sa);
        final List<Card> destCards = AbilityUtils.getDefinedCards(host, sa.getParam("Defined"), sa);
        if ((srcCards.size() > 0 && sa.getParam("CounterNum").equals("All"))) {
            amount = srcCards.get(0).getCounters(cType);
        }
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
            CardCollectionView list = CardLists.getTargetableCards(player.getCardsIn(ZoneType.Battlefield), sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), host.getController(), host, sa);
            if (list.isEmpty() && mandatory) {
                // If there isn't any prefered cards to target, gotta choose
                // non-preferred ones
                list = CardLists.getTargetableCards(player.getOpponent().getCardsIn(ZoneType.Battlefield), sa);
                list = CardLists.getValidCards(list, abTgt.getValidTgts(), host.getController(), host, sa);
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
                else if (type.equals("M1M1")) {
                    choice = ComputerUtilCard.getWorstCreatureAI(list);
                }
                else {
                    choice = Aggregates.random(list);
                }
            }
            else {
                if (preferred) {
                    choice = CountersAi.chooseBoonTarget(list, type);
                }
                else if (type.equals("P1P1")) {
                    choice = ComputerUtilCard.getWorstCreatureAI(list);
                }
                else {
                    choice = Aggregates.random(list);
                }
            }

            // TODO - I think choice can be null here. Is that ok for
            // addTarget()?
            sa.getTargets().add(choice);
        }
        return chance;
    }
}
