package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Constant;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cost.Cost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameState;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilCombat;
import forge.game.ai.ComputerUtilCost;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
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
        final GameState game = Singletons.getModel().getGame();
        
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

                // is the creature blocking and unable to destroy the attacker
                // or would be destroyed itself?
                if (c.isBlocking()
                        && (ComputerUtilCombat.blockerWouldBeDestroyed(ai, c))) {
                    return true;
                }

                // is the creature in blocked and the blocker would survive
                if (game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && game.getCombat().isAttacking(c) && game.getCombat().isBlocked(c)
                        && ComputerUtilCombat.blockerWouldBeDestroyed(ai, game.getCombat().getBlockers(c).get(0))) {
                    return true;
                }

                return false;
            }
        });
        return list;
    } // getProtectCreatures()

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Card hostCard = sa.getSourceCard();
        // if there is no target and host card isn't in play, don't activate
        if ((sa.getTarget() == null) && !hostCard.isInPlay()) {
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
        if ((Singletons.getModel().getGame().getStack().size() == 0) && Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE)) {
            // Instant-speed protections should not be cast outside of combat
            // when the stack is empty
            if (!SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (Singletons.getModel().getGame().getStack().size() > 0) {
            // TODO protection something only if the top thing on the stack will
            // kill it via damage or destroy
            return false;
        }

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
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
        if (!mandatory && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)) {
            return false;
        }

        final Card source = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        tgt.resetTargets();
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

        if (Singletons.getModel().getGame().getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getSourceCard());
                }
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && protectMandatoryTarget(ai, sa, mandatory);
        }

        // Don't target cards that will die.
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                System.out.println("Not Protecting");
                return !c.getSVar("Targeting").equals("Dies");
            }
        });

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return protectMandatoryTarget(ai, sa, mandatory);
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
    } // protectTgtAI()

    private static boolean protectMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {


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

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
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

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(source, sa)) {
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

            tgt.addTarget(c);
        }

        if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        return true;
    } // protectMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return protectTgtAI(ai, sa, mandatory);
        }

        return true;
    } // protectTriggerAI

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final Card host = sa.getSourceCard();
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (host.isCreature()) {
                // TODO
            }
        } else {
            return protectTgtAI(ai, sa, false);
        }

        return true;
    } // protectDrawbackAI()

}
