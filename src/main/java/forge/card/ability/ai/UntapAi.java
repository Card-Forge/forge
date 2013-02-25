package forge.card.ability.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class UntapAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();
        final Cost cost = sa.getPayCosts();

        if (!ComputerUtilCost.checkAddM1M1CounterCost(cost, source)) {
            return false;
        }

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        if (tgt == null) {
            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController() == ai) {
                return false;
            }
        } else {
            if (!untapPrefTargeting(ai, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result
            final List<Card> pDefined = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);
            if ((pDefined != null) && pDefined.get(0).isUntapped() && pDefined.get(0).getController() == ai) {
                return false;
            }

            return true;
        } else {
            if (untapPrefTargeting(ai, tgt, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return untapUnpreferredTargeting(sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final Target tgt = sa.getTarget();

        boolean randomReturn = true;

        if (tgt == null) {
            // who cares if its already untapped, it's only a subability?
        } else {
            if (!untapPrefTargeting(ai, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    /**
     * <p>
     * untapPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private static boolean untapPrefTargeting(final Player ai, final Target tgt, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getSourceCard();

        Player targetController = ai;

        if (sa.isCurse()) {
            targetController = ai.getOpponent();
        }

        List<Card> untapList = targetController.getCardsIn(ZoneType.Battlefield);
        untapList = CardLists.getTargetableCards(untapList, sa);
        untapList = CardLists.getValidCards(untapList, tgt.getValidTgts(), source.getController(), source);

        untapList = CardLists.filter(untapList, Presets.TAPPED);
        // filter out enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        untapList = CardLists.getValidCards(untapList, tappablePermanents, source.getController(), source);

        if (untapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card choice = null;

            if (untapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (CardLists.getNotType(untapList, "Creature").size() == 0) {
                choice = ComputerUtilCard.getBestCreatureAI(untapList); // if
                                                                       // only
                                                                       // creatures
                                                                       // take
                                                                       // the
                                                                       // best
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(untapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            untapList.remove(choice);
            tgt.addTarget(choice);
        }
        return true;
    }

    /**
     * <p>
     * untapUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean untapUnpreferredTargeting(final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);

        // filter by enchantments and planeswalkers, their tapped state doesn't
        // matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        List<Card> tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.UNTAPPED);

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (untapTargetList(source, tgt, sa, mandatory, tapList)) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * untapTargetList.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @return a boolean.
     */
    private boolean untapTargetList(final Card source, final Target tgt, final SpellAbility sa, final boolean mandatory, final List<Card> tapList) {
        for (final Card c : tgt.getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.size() == 0) {
            return false;
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            if (CardLists.getNotType(tapList, "Creature").size() == 0) {
                choice = ComputerUtilCard.getBestCreatureAI(tapList); // if only
                                                                     // creatures
                                                                     // take
                                                                     // the
                                                                     // best
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            tapList.remove(choice);
            tgt.addTarget(choice);
        }

        return true;
    }
}
