package forge.ai.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class DebuffAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(final Player ai, final SpellAbility sa) {
        // if there is no target and host card isn't in play, don't activate
        final Card source = sa.getHostCard();
        final Game game = ai.getGame(); 
        if (!sa.usesTargeting() && !source.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until AI is improved
        if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, cost, source, sa)) {
            return false;
        }

        if (!ComputerUtilCost.checkLifeCost(ai, cost, source, 40, sa)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, source, sa)) {
            return false;
        }

        final PhaseHandler ph =  game.getPhaseHandler();

        // Phase Restrictions
        if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                || !game.getStack().isEmpty()) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty, unless there are specific activation phase requirements
            if (!isSorcerySpeed(sa, ai) && !sa.hasParam("ActivationPhases")) {
                return false;
            }
        }

        if (!sa.usesTargeting()) {
            List<Card> cards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

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
        if (!sa.usesTargeting()) {
            // TODO - copied from AF_Pump.pumpDrawbackAI() - what should be here?
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
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
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
        CardCollection list = getCurseCreatures(ai, sa, kws == null ? Lists.newArrayList() : kws);

        // several uses here:
        // 1. make human creatures lose evasion when they are attacking
        // 2. make human creatures lose Flying/Horsemanship/Shadow/etc. when
        // Comp is attacking
        // 3. remove Indestructible keyword so it can be destroyed?
        // 3a. remove Persist?

        if (list.isEmpty()) {
            return mandatory && debuffMandatoryTarget(ai, sa, mandatory);
        }

        while (sa.canAddMoreTarget()) {
            Card t = null;

            if (list.isEmpty()) {
                if ((sa.getTargets().size() < tgt.getMinTargets(sa.getHostCard(), sa)) || (sa.getTargets().size() == 0)) {
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
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param kws
     *            a {@link java.util.ArrayList} object.
     * @return a CardCollection.
     */
    private CardCollection getCurseCreatures(final Player ai, final SpellAbility sa, final List<String> kws) {
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        CardCollection list = CardLists.getTargetableCards(opp.getCreaturesInPlay(), sa);
        if (!list.isEmpty()) {
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return c.hasAnyKeyword(kws); // don't add duplicate negative keywords
                }
            });
        }
        return list;
    }

    /**
     * <p>
     * debuffMandatoryTarget.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean debuffMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Card> list = CardUtil.getValidCardsToTarget(sa);

        if (list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        final CardCollection pref = CardLists.filterControlledBy(list, ai.getOpponents());
        final CardCollection forced = CardLists.filterControlledBy(list, ai);
        final Card source = sa.getHostCard();

        while (sa.canAddMoreTarget()) {
            if (pref.isEmpty()) {
                break;
            }

            Card c = ComputerUtilCard.getBestAI(pref);
            pref.remove(c);
            sa.getTargets().add(c);
        }

        while (!sa.isMinTargetChosen()) {
            if (forced.isEmpty()) {
                break;
            }

            // TODO - if forced targeting, just pick something without the given keyword
            Card c;
            if (CardLists.getNotType(forced, "Creature").size() == 0) {
                c = ComputerUtilCard.getWorstCreatureAI(forced);
            } else {
                c = ComputerUtilCard.getCheapestPermanentAI(forced, sa, false);
            }

            forced.remove(c);

            sa.getTargets().add(c);
        }

        if (!sa.isMinTargetChosen()) {
            sa.resetTargets();
            return false;
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final List<String> kws = sa.hasParam("Keywords") ? Arrays.asList(sa.getParam("Keywords").split(" & ")) : new ArrayList<>();

        if (!sa.usesTargeting()) {
            if (mandatory) {
                return true;
            }
        } else {
            return debuffTgtAI(ai, sa, kws, mandatory);
        }

        return true;
    }

}
