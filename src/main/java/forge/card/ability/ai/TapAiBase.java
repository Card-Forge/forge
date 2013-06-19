package forge.card.ability.ai;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.phase.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public abstract class TapAiBase extends SpellAbilityAi  {

    /**
     * <p>
     * tapTargetList.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
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
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Card c : sa.getTargets().getTargetCards()) {
            tapList.remove(c);
        }

        if (tapList.size() == 0) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) || (sa.getTargets().getNumTargeted() == 0)) {
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

            if (CardLists.getNotType(tapList, "Creature").size() == 0) {
                // if only creatures take the best
                choice = ComputerUtilCard.getBestCreatureAI(tapList);
            } else {
                choice = ComputerUtilCard.getMostExpensivePermanentAI(tapList, sa, false);
            }

            if (choice == null) { // can't find anything left
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
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
     *            a {@link forge.Card} object.
     * @param tgt
     *            a {@link forge.card.spellability.TargetRestrictions} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected boolean tapPrefTargeting(final Player ai, final Card source, final TargetRestrictions tgt, final SpellAbility sa, final boolean mandatory) {
        final Player opp = ai.getOpponent();
        final Game game = ai.getGame();
        List<Card> tapList = opp.getCardsIn(ZoneType.Battlefield);
        tapList = CardLists.filter(tapList, Presets.UNTAPPED);
        tapList = CardLists.getValidCards(tapList, tgt.getValidTgts(), source.getController(), source);
        // filter out enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Creature", "Land", "Artifact" };
        tapList = CardLists.getValidCards(tapList, tappablePermanents, source.getController(), source);
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

        if (tapList.size() == 0) {
            return false;
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card choice = null;

            if (tapList.size() == 0) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) || (sa.getTargets().getNumTargeted() == 0)) {
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

            PhaseHandler phase = game.getPhaseHandler();
            if (phase.isPlayerTurn(ai)
                    && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
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
                    attackers.remove(sa.getSourceCard());
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
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
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
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    protected  boolean tapUnpreferredTargeting(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Game game = ai.getGame();

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
        list = CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source);
        list = CardLists.getTargetableCards(list, sa);
        
        // try to tap anything controlled by the computer
        List<Card> tapList = CardLists.filterControlledBy(list, ai.getOpponents());
        if (tapTargetList(ai, sa, tapList, mandatory)) {
            return true;
        }

        // filter by enchantments and planeswalkers, their tapped state doesn't matter.
        final String[] tappablePermanents = { "Enchantment", "Planeswalker" };
        tapList = CardLists.getValidCards(list, tappablePermanents, source.getController(), source);

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

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getSourceCard();

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
        final Card source = sa.getSourceCard();

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
