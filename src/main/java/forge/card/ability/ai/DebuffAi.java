package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DebuffAi extends SpellAbilityAi {
    // *************************************************************************
    // ***************************** Debuff ************************************
    // *************************************************************************

    @Override
    protected boolean canPlayAI(final AIPlayer ai, final SpellAbility sa) {
        // if there is no target and host card isn't in play, don't activate
        final Card source = sa.getSourceCard();
        if ((sa.getTarget() == null) && !source.isInPlay()) {
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
        final PhaseHandler ph =  Singletons.getModel().getGame().getPhaseHandler();

        // Phase Restrictions
        if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)
                || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                || !Singletons.getModel().getGame().getStack().isEmpty()) {
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

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);

            if (!cards.isEmpty()) {
                cards = CardLists.filter(cards, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        if ((c.getController().equals(sa.getActivatingPlayer())) || (!c.isBlocking() && !c.isAttacking())) {
                            return false;
                        }
                        // don't add duplicate negative keywords
                        return sa.hasParam("Keywords") && c.hasAnyKeyword(Arrays.asList(sa.getParam("Keywords").split(" & ")));
                    }
                });
            }
            if (cards.isEmpty()) {
                return false;
            }
        } else {
            return debuffTgtAI(ai, sa, sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>(), false);
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            // TODO - copied from AF_Pump.pumpDrawbackAI() - what should be
            // here?
        } else {
            return debuffTgtAI(ai, sa, sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>(), false);
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
        if (!mandatory && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        List<Card> list = getCurseCreatures(ai, sa, kws);
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

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return debuffMandatoryTarget(ai, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = ComputerUtilCard.getBestCreatureAI(list);
            tgt.addTarget(t);
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
        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        final Target tgt = sa.getTarget();
        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        final List<Card> pref = CardLists.filterControlledBy(list, ai.getOpponent());
        final List<Card> forced = CardLists.filterControlledBy(list, ai);
        final Card source = sa.getSourceCard();

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
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

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
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

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        final List<String> kws = sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<String>();

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return debuffTgtAI(ai, sa, kws, mandatory);
        }

        return true;
    }

}
