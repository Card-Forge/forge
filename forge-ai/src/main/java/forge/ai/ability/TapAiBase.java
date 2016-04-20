package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

import java.util.List;

public abstract class TapAiBase extends SpellAbilityAi  {

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 

     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tapList
     *            a CardCollection object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean tapTargetList(final Player ai, final SpellAbility sa, final CardCollection tapList, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Card c : sa.getTargets().getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.isEmpty()) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa) || sa.getTargets().getNumTargeted() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            if (CardLists.getNotType(tapList, "Creature").isEmpty()) {
                // if only creatures take the best
                choice = ComputerUtilCard.getBestCreatureAI(tapList);
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa) || sa.getTargets().getNumTargeted() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
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
            sa.getTargets().add(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapPrefTargeting.
     * </p>
     * 
     * @param source
     *            a {@link forge.game.card.Card} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected boolean tapPrefTargeting(final Player ai, final Card source, final TargetRestrictions tgt, final SpellAbility sa, final boolean mandatory) {
        final Player opp = ai.getOpponent();
        final Game game = ai.getGame();
        CardCollection tapList = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponents());
        tapList = CardLists.getValidCards(tapList, tgt.getValidTgts(), source.getController(), source, sa);
        tapList = CardLists.getTargetableCards(tapList, sa);
        tapList = CardLists.filter(tapList, Presets.UNTAPPED);
        tapList = CardLists.filter(tapList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (c.isCreature()) {
                    return true;
                }

                for (final SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.isAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
                        return true;
                    }
                }
                return false;
            }
        });

        //use broader approach when the cost is a positive thing
        if (tapList.isEmpty() && ComputerUtil.activateForCost(sa, ai)) { 
    		tapList = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponents());
            tapList = CardLists.getValidCards(tapList, tgt.getValidTgts(), source.getController(), source, sa);
            tapList = CardLists.getTargetableCards(tapList, sa);
    		tapList = CardLists.filter(tapList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c.isCreature()) {
                        return true;
                    }

                    for (final SpellAbility sa : c.getSpellAbilities()) {
                        if (sa.isAbility() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        
        if (tapList.isEmpty()) {
        	return false;
        }

        boolean goodTargets = false;
        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.isEmpty()) {
                if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa) || sa.getTargets().getNumTargeted() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
                    }
                    return false;
                } else {
                    if (!goodTargets && !ComputerUtil.shouldCastLessThanMax(ai, source)) {
                        return false;
                    }
                    break;
                }
            }

            PhaseHandler phase = game.getPhaseHandler();
            Card primeTarget = ComputerUtil.getKilledByTargeting(sa, tapList);
            if (primeTarget != null) {
            	choice = primeTarget;
            	goodTargets = true;
            } else if (phase.isPlayerTurn(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Tap creatures possible blockers before combat during AI's turn.
                List<Card> attackers;
                if (phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    //Combat has already started
                    attackers = game.getCombat().getAttackers();
                } else {
                    attackers = CardLists.filter(ai.getCreaturesInPlay(), new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return CombatUtil.canAttack(c, opp);
                        }
                    });
                    attackers.remove(sa.getHostCard());
                }
                Predicate<Card> findBlockers = CardPredicates.possibleBlockerForAtLeastOne(attackers);
                List<Card> creatureList = CardLists.filter(tapList, findBlockers);

                if (!attackers.isEmpty() && !creatureList.isEmpty()) {
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else if (sa.isTrigger() || ComputerUtil.castSpellInMain1(ai, sa)) {
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
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
                    choice = ComputerUtilCard.getBestCreatureAI(creatureList);
                } else { // no creatures available
                    choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
                }
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa) || sa.getTargets().getNumTargeted() == 0) {
                    if (!mandatory) {
                        sa.resetTargets();
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
            sa.getTargets().add(choice);
        }

        return true;
    }

    /**
     * <p>
     * tapUnpreferredTargeting.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected  boolean tapUnpreferredTargeting(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = ai.getGame();

        CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), source.getController(), source, sa);
        list = CardLists.getTargetableCards(list, sa);
        
        // try to tap anything controlled by the computer
        CardCollection tapList = CardLists.filterControlledBy(list, ai.getOpponents());
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
        
        if (sa.getTargets().getNumTargeted() >= tgt.getMinTargets(sa.getHostCard(), sa)) {
            return true;
        }

        // filter by enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source, sa);

        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }

        // try to just tap already tapped things
        tapList = CardLists.filter(list, Presets.TAPPED);

        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }
        
        if (sa.getTargets().getNumTargeted() >= tgt.getMinTargets(sa.getHostCard(), sa)) {
            return true;
        }

        // just tap whatever we can
        tapList = list;

        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result

            return true;
        } else {
            sa.resetTargets();
            if (tapPrefTargeting(ai, source, tgt, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return tapUnpreferredTargeting(ai, sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            sa.resetTargets();
            if (!tapPrefTargeting(ai, source, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

}
