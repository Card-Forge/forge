package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class CountersPutAi extends SpellAiLogic {
    @Override
    protected boolean canPlayAI(AIPlayer ai, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on
        // what the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        List<Card> list;
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");

        final Player player = sa.isCurse() ? ai.getOpponent() : ai;

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canBeTargetedBy(sa) && !c.hasKeyword("CARDNAME can't have counters placed on it.")
                        && !(c.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && type.equals("M1M1"));
            }
        });

        if (abTgt != null) {
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);

            if (list.size() < abTgt.getMinTargets(source, sa)) {
                return false;
            }
        }

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

            if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source)) {
                return false;
            }
        }

        // TODO handle proper calculation of X values based on Cost
        int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);

        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        }

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        // Targeting
        if (abTgt != null) {
            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                } else {
                    choice = CountersAi.chooseBoonTarget(list, type);
                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        } else {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
            // Don't activate Curse abilities on my cards and non-curse abilites
            // on my opponents
            if (cards.isEmpty() || !cards.get(0).getController().equals(player)) {
                return false;
            }

            final int currCounters = cards.get(0).getCounters(CounterType.valueOf(type));
            // each non +1/+1 counter on the card is a 10% chance of not
            // activating this ability.

            if (!(type.equals("P1P1") || type.equals("M1M1") || type.equals("ICE")) && (r.nextFloat() < (.1 * currCounters))) {
                return false;
            }
        }

        // Don't use non P1P1/M1M1 counters before main 2 if possible
        if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases")
                && !(type.equals("P1P1") || type.equals("M1M1"))) {
            return false;
        }

        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        return true;
    } // putCanPlayAI

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, AIPlayer ai) {
        boolean chance = true;
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);

        final Player player = sa.isCurse() ? ai.getOpponent() : ai;

        if (abTgt != null) {
            List<Card> list =
                    CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), abTgt.getValidTgts(), source.getController(), source);

            if (list.size() == 0) {
                return false;
            }

            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return sa.canTarget(c);
                    }
                });
                if (list.size() == 0) {
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = CountersAi.chooseCursedTarget(list, type, amount);
                } else {
                    choice = CountersAi.chooseBoonTarget(list, type);
                }

                if (choice == null) { // can't find anything left
                    if ((abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa))
                            || (abTgt.getNumTargeted() == 0)) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        }

        return chance;
    } // putPlayDrawbackAI

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Target abTgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        // boolean chance = true;
        boolean preferred = true;
        List<Card> list;
        boolean isCurse = sa.isCurse();
        final Player player = isCurse ? ai.getOpponent() : ai;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), amountStr, sa);

        if (abTgt == null) {
            // No target. So must be defined
            list = new ArrayList<Card>(AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa));

            if (!mandatory) {
                // TODO - If Trigger isn't mandatory, when wouldn't we want to
                // put a counter?
                // things like Powder Keg, which are way too complex for the AI
            }
        } else {
            list = CardLists.getTargetableCards(player.getCardsIn(ZoneType.Battlefield), sa);
            list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);

            if (list.isEmpty() && mandatory) {
                // If there isn't any prefered cards to target, gotta choose
                // non-preferred ones
                list = player.getOpponent().getCardsIn(ZoneType.Battlefield);
                list = CardLists.getTargetableCards(list, sa);
                list = CardLists.getValidCards(list, abTgt.getValidTgts(), source.getController(), source);
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

        return true;
    }

}
