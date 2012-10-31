package forge.card.abilityfactory.ai;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public abstract class TapAiBase extends SpellAiLogic  {

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param tapList
     *            a {@link forge.CardList} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean tapTargetList(final Player ai, final SpellAbility sa, final List<Card> tapList, final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
    
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
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }
    
            if (CardLists.getNotType(tapList, "Creature").size() == 0) {
                // if only creatures take the best
                choice = CardFactoryUtil.getBestCreatureAI(tapList);
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
            }
    
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }
    
            tapList.remove(choice);
            tgt.addTarget(choice);
        }
    
        return true;
    }

    /**
     * <p>
     * tapPrefTargeting.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected boolean tapPrefTargeting(final Player ai, final Card source, final Target tgt, final SpellAbility sa, final boolean mandatory) {
        final Player opp = ai.getOpponent();
        List<Card> tapList = opp.getCardsIn(ZoneType.Battlefield);
        tapList = CardLists.filter(tapList, Presets.UNTAPPED);
        tapList = CardLists.getValidCards(tapList, tgt.getValidTgts(), source.getController(), source);
        // filter out enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        tapList = CardLists.getValidCards(tapList, tappablePermanents, source.getController(), source);
        tapList = CardLists.getTargetableCards(tapList, sa);
    
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
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }
    
            PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
            if (phase.isPlayerTurn(ai)
                    && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
    
                List<Card> attackers;
                if (phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    //Combat has already started
                    attackers = Singletons.getModel().getGame().getCombat().getAttackerList();
                } else {
                    attackers = CardLists.filter(ai.getCreaturesInPlay(), new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canAttack(c, opp);
                        }
                    });
                    attackers.remove(sa.getSourceCard());
                }
                Predicate<Card> findBlockers = CardPredicates.possibleBlockerForAtLeastOne(attackers);
                List<Card> creatureList = CardLists.filter(tapList, findBlockers);
                if (!attackers.isEmpty() && !creatureList.isEmpty()) {
                    choice = CardFactoryUtil.getBestCreatureAI(creatureList);
                } else if (sa.isTrigger()){
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
                }
                
            } else if (phase.isPlayerTurn(opp)
                    && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                if (Iterables.any(tapList, CardPredicates.Presets.CREATURES)) {
                    List<Card> creatureList = CardLists.filter(tapList, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.isCreature() && CombatUtil.canAttack(c, opp);
                        }
                    });
                    choice = CardFactoryUtil.getBestCreatureAI(creatureList);
                } else { // no creatures available
                    choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
                }
            } else {
                choice = CardFactoryUtil.getMostExpensivePermanentAI(tapList, sa, false);
            }
    
            if (choice == null) { // can't find anything left
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (!mandatory) {
                        tgt.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }
    
            tapList.remove(choice);
            tgt.addTarget(choice);
        }
    
        return true;
    }

    /**
     * <p>
     * tapUnpreferredTargeting.
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
    protected  boolean tapUnpreferredTargeting(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
    
        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);
    
        // filter by enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        List<Card> tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source);
    
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
    
        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.TAPPED);
    
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
    
        // just tap whatever we can
        tapList = list;
    
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
    
        return false;
    }
    
}