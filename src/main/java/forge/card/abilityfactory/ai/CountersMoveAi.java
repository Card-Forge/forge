package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.Counters;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class CountersMoveAi extends SpellAiLogic { 
    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final String amountStr = params.get("CounterNum");

        // TODO handle proper calculation of X values based on Cost
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), amountStr, sa);

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        boolean chance = false;

        if (AbilityFactory.playReusable(ai, sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    } // moveCounterCanPlayAI

    @Override
    protected boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Card host = sa.getSourceCard();
        final Target abTgt = sa.getTarget();
        final String type = params.get("CounterType");
        final String amountStr = params.get("CounterNum");
        final int amount = AbilityFactory.calculateAmount(sa.getAbilityFactory().getHostCard(), amountStr, sa);
        boolean chance = false;
        boolean preferred = true;

        final Counters cType = Counters.valueOf(params.get("CounterType"));
        final ArrayList<Card> srcCards = AbilityFactory.getDefinedCards(host, params.get("Source"), sa);
        final ArrayList<Card> destCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);
        if (abTgt == null) {
            if ((srcCards.size() > 0)
                    && cType.equals(Counters.P1P1) // move +1/+1 counters away
                                                   // from
                                                   // permanents that cannot use
                                                   // them
                    && (destCards.size() > 0) && destCards.get(0).getController().isComputer()
                    && (!srcCards.get(0).isCreature() || srcCards.get(0).hasStartOfKeyword("CARDNAME can't attack"))) {

                chance = true;
            }
        } else { // targeted
            boolean isCurse = sa.getAbilityFactory().isCurse(); 
            final Player player = isCurse ? ai.getOpponent() : ai;
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
            if (isCurse) {
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