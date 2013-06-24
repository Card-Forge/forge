package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Constant;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class ProtectAi extends SpellAbilityAi {
    private static boolean hasProtectionFrom(final Card card, final String color) {
        final ArrayList<String> onlyColors = new ArrayList<String>(Constant.Color.ONLY_COLORS);

        // make sure we have a valid color
        if (!onlyColors.contains(color)) {
            return false;
        }

        final String protection = "Protection from " + color;

        return card.hasKeyword(protection);
    }

    private static boolean hasProtectionFromAny(final Card card, final ArrayList<String> colors) {
        boolean protect = false;
        for (final String color : colors) {
            protect |= hasProtectionFrom(card, color);
        }
        return protect;
    }

    private static boolean hasProtectionFromAll(final Card card, final ArrayList<String> colors) {
        boolean protect = true;
        if (colors.isEmpty()) {
            return false;
        }

        for (final String color : colors) {
            protect &= hasProtectionFrom(card, color);
        }
        return protect;
    }

    /**
     * <p>
     * getProtectCreatures.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @return a {@link forge.CardList} object.
     */
    private static List<Card> getProtectCreatures(final Player ai, final SpellAbility sa) {
        final ArrayList<String> gains = AbilityUtils.getProtectionList(sa);
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        
        List<Card> list = ai.getCreaturesInPlay();
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                if (!c.canBeTargetedBy(sa)) {
                    return false;
                }

                // Don't add duplicate protections
                if (hasProtectionFromAll(c, gains)) {
                    return false;
                }

                // will the creature attack (only relevant for sorcery speed)?
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)
                        && ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
                    return true;
                }

                if( combat != null ) {
                    // is the creature blocking and unable to destroy the attacker
                    // or would be destroyed itself?
                    if (combat.isBlocking(c) && ComputerUtilCombat.blockerWouldBeDestroyed(ai, c, combat)) {
                        return true;
                    }
    
                    // is the creature in blocked and the blocker would survive
                    // TODO Potential NPE here if no blockers are actually left
                    if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS)
                            && combat.isAttacking(c) && combat.isBlocked(c)
                            && ComputerUtilCombat.blockerWouldBeDestroyed(ai, combat.getBlockers(c).get(0), combat)) {
                        return true;
                    }
                }

                return false;
            }
        });
        return list;
    } // getProtectCreatures()

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        final Game game = ai.getGame();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTargetRestrictions() == null) && !hostCard.isInPlay()) {
            return false;
        }

        final Cost cost = sa.getPayCosts();

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, cost, hostCard, 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, cost, hostCard)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, hostCard)) {
            return false;
        }

        // Phase Restrictions
        if (game.getStack().isEmpty() && game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE)) {
            // Instant-speed protections should not be cast outside of combat
            // when the stack is empty
            if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (!game.getStack().isEmpty()) {
            // TODO protection something only if the top thing on the stack will
            // kill it via damage or destroy
            return false;
        }

        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }

            /*
             * // when this happens we need to expand AI to consider if its ok
             * for everything? for (Card card : cards) { // TODO if AI doesn't
             * control Card and Pump is a Curse, than maybe use?
             * 
             * }
             */
        } else {
            return protectTgtAI(ai, sa, false);
        }

        return false;
    } // protectPlayAI()

    private boolean protectTgtAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        if (!mandatory && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            return false;
        }

        final Card source = sa.getSourceCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        List<Card> list = getProtectCreatures(ai, sa);

        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

        /*
         * TODO - What this should probably do is if it's time for instants and
         * abilities after Human declares attackers, determine desired
         * protection before assigning blockers.
         * 
         * The other time we want protection is if I'm targeted by a damage or
         * destroy spell on the stack
         * 
         * Or, add protection (to make it unblockable) when Compy is attacking.
         */

        if (game.getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getSourceCard());
                }
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && protectMandatoryTarget(ai, sa, mandatory);
        }

        // Don't target cards that will die.
        list = ComputerUtil.getSafeTargets(ai, sa, list);

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) || (sa.getTargets().getNumTargeted() == 0)) {
                    if (mandatory) {
                        return protectMandatoryTarget(ai, sa, mandatory);
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
    } // protectTgtAI()

    private static boolean protectMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();

        List<Card> list = game.getCardsIn(ZoneType.Battlefield);
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

        List<Card> pref = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !hasProtectionFromAll(c, AbilityUtils.getProtectionList(sa));
            }
        });
        final List<Card> pref2 = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !hasProtectionFromAny(c, AbilityUtils.getProtectionList(sa));
            }
        });
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

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref2.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(pref2, "Creature").size() == 0) {
                c = ComputerUtilCard.getBestCreatureAI(pref2);
            } else {
                c = ComputerUtilCard.getMostExpensivePermanentAI(pref2, sa, true);
            }

            pref2.remove(c);

            sa.getTargets().add(c);
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) {
            if (forced.isEmpty()) {
                break;
            }

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
    } // protectMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.getTargetRestrictions() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return protectTgtAI(ai, sa, mandatory);
        }

        return true;
    } // protectTriggerAI

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final Card host = sa.getSourceCard();
        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            if (host.isCreature()) {
                // TODO
            }
        } else {
            return protectTgtAI(ai, sa, false);
        }

        return true;
    } // protectDrawbackAI()

}
