package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DebuffAi extends SpellAbilityAi {
    // *************************************************************************
    // ***************************** Debuff ************************************
    // *************************************************************************

    @Override
    protected boolean canPlayAI(final Player ai, final SpellAbility sa) {
        // if there is no target and host card isn't in play, don't activate
        final Card source = sa.getSourceCard();
        final Game game = ai.getGame(); 
        if ((sa.getTargetRestrictions() == null) && !source.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until AI is improved
        if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, cost, source)) {
            return false;
        }

        if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 40, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, source)) {
            return false;
        }

        final SpellAbilityRestriction restrict = sa.getRestrictions();
        final PhaseHandler ph =  game.getPhaseHandler();

        // Phase Restrictions
        if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !game.getStack().isEmpty()) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        }

        final int activations = restrict.getNumberTurnActivations();
        final int sacActivations = restrict.getActivationNumberSacrifice();
        // don't risk sacrificing a creature just to pump it
        if ((sacActivations != -1) && (activations >= (sacActivations - 1))) {
            return false;
        }

        if (!sa.usesTargeting() || !sa.getTargetRestrictions().doesTarget()) {
            List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);


            final Combat combat = game.getCombat();
            return Iterables.any(cards, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {

                    if (c.getController().equals(sa.getActivatingPlayer()) || combat == null)
                        return false;

                    if (!combat.isBlocking(c) && !combat.isAttacking(c)) {
                        return false;
                    }
                    // don't add duplicate negative keywords
                    return sa.hasParam("Keywords") && c.hasAnyKeyword(Arrays.asList(sa.getParam("Keywords").split(" & ")));
                }
            });
        } else {
            return debuffTgtAI(ai, sa, sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : null, false);
        }
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            // TODO - copied from AF_Pump.pumpDrawbackAI() - what should be
            // here?
        } else {
            return debuffTgtAI(ai, sa, sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : null, false);
        }

        return true;
    } // debuffDrawbackAI()

    /**
     * <p>
     * debuffTgtAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param kws
     *            a {@link java.util.ArrayList} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean debuffTgtAI(final Player ai, final SpellAbility sa, final List<String> kws, final boolean mandatory) {
        // this would be for evasive things like Flying, Unblockable, etc
        if (!mandatory && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        List<Card> list = getCurseCreatures(ai, sa, kws == null ? Lists.<String>newArrayList() : kws);
        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        // several uses here:
        // 1. make human creatures lose evasion when they are attacking
        // 2. make human creatures lose Flying/Horsemanship/Shadow/etc. when
        // Comp is attacking
        // 3. remove Indestructible keyword so it can be destroyed?
        // 3a. remove Persist?

        if (list.isEmpty()) {
            return mandatory && debuffMandatoryTarget(ai, sa, mandatory);
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    if (mandatory) {
                        return debuffMandatoryTarget(ai, sa, mandatory);
                    }

                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = ComputerUtilCard.getBestCreatureAI(list);
            sa.getTargets().add(t);
            list.remove(t);
        }

        return true;
    } // pumpTgtAI()

    /**
     * <p>
     * getCurseCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param kws
     *            a {@link java.util.ArrayList} object.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getCurseCreatures(final Player ai, final SpellAbility sa, final List<String> kws) {
        final Player opp = ai.getOpponent();
        List<Card> list = opp.getCreaturesInPlay();
        list = CardLists.getTargetableCards(list, sa);

        if (!list.isEmpty()) {
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.hasAnyKeyword(kws); // don't add duplicate negative
                                                 // keywords
                }
            });
        }

        return list;
    } // getCurseCreatures()

    /**
     * <p>
     * debuffMandatoryTarget.
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
    private boolean debuffMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        List<Card> list = ai.getGame().getCardsIn(ZoneType.Battlefield);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : sa.getTargets().getTargetCards()) {
            list.remove(c);
        }

        final List<Card> pref = CardLists.filterControlledBy(list, ai.getOpponent());
        final List<Card> forced = CardLists.filterControlledBy(list, ai);
        final Card source = sa.getSourceCard();

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(pref, "Creature").size() == 0) {
                c = ComputerUtilCard.getBestCreatureAI(pref);
            } else {
                c = ComputerUtilCard.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            sa.getTargets().add(c);
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            if (forced.isEmpty()) {
                break;
            }

            // TODO - if forced targeting, just pick something without the given
            // keyword
            Card c;
            if (CardLists.getNotType(forced, "Creature").size() == 0) {
                c = ComputerUtilCard.getWorstCreatureAI(forced);
            } else {
                c = ComputerUtilCard.getCheapestPermanentAI(forced, sa, true);
            }

            forced.remove(c);

            sa.getTargets().add(c);
        }

        if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final List<String> kws = sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>();

        if (sa.getTargetRestrictions() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return debuffTgtAI(ai, sa, kws, mandatory);
        }

        return true;
    }

}
