package forge.card.ability.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCost;
import forge.game.ai.ComputerUtilMana;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class PumpAi extends PumpAiBase {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        final PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler();
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<String>();
        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

        if (!ComputerUtilCost.checkLifeCost(ai, cost, sa.getSourceCard(), 4, null)) {
            return false;
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, cost, sa.getSourceCard())) {
            return false;
        }

        if (!ComputerUtilCost.checkCreatureSacrificeCost(ai, cost, sa.getSourceCard())) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(cost, sa.getSourceCard())) {
            return false;
        }

        if (Singletons.getModel().getGame().getStack().isEmpty() && CostUtil.hasTapCost(cost, sa.getSourceCard())) {
                if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && ph.isPlayerTurn(ai)) {
                    return false;
                }
                if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS) && ph.isPlayerTurn(ai.getOpponent())) {
                    return false;
                }
        }
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        // Phase Restrictions
        if ((Singletons.getModel().getGame().getStack().size() == 0) && ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            if (!sa.isCurse() && !SpellAbilityAi.isSorcerySpeed(sa)) {
                return false;
            }
        } else if (Singletons.getModel().getGame().getStack().size() > 0) {
            if (!keywords.contains("Shroud") && !keywords.contains("Hexproof")) {
                return false;
            }
        }

        final SpellAbilityRestriction restrict = sa.getRestrictions();
        final int activations = restrict.getNumberTurnActivations();
        final int sacActivations = restrict.getActivationNumberSacrifice();
        // don't risk sacrificing a creature just to pump it
        if ((sacActivations != -1) && (activations >= (sacActivations - 1))) {
            return false;
        }

        final Card source = sa.getSourceCard();
        if (source.getSVar("X").equals("Count$xPaid")) {
            source.setSVar("PayX", "");
        }

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
            if (numDefense.equals("-X")) {
                defense = -xPay;
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getSourceCard(), numDefense, sa);
        }

        int attack;
        if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getSourceCard(), numAttack, sa);
        }

        if ((numDefense.contains("X") && defense == 0)
                || (numAttack.contains("X") && attack == 0)) {
            return false;
        }

        //Untargeted
        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getSourceCard(),
                    sa.getParam("Defined"), sa);

            if (cards.size() == 0) {
                return false;
            }

            // when this happens we need to expand AI to consider if its ok for
            // everything?
            for (final Card card : cards) {
                if (sa.isCurse()) {
                    if (!card.getController().isOpponentOf(ai)) {
                        return false;
                    }

                    if (!containsUsefulKeyword(ai, keywords, card, sa, attack)) {
                        continue;
                    }

                    return true;
                }
                if (!card.getController().isOpponentOf(ai) && shouldPumpCard(ai, sa, card, defense, attack, keywords)) {
                    return true;
                }
            }
            return false;
        }
        //Targeted
        if (!this.pumpTgtAI(ai, sa, defense, attack, false)) {
            return false;
        }

        return true;
    } // pumpPlayAI()

    private boolean pumpTgtAI(final Player ai, final SpellAbility sa, final int defense, final int attack, final boolean mandatory) {
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & ")) : new ArrayList<String>();

        if (!mandatory
                && !sa.isTrigger()
                && Singletons.getModel().getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS_INSTANT_ABILITY)
                && !(sa.isCurse() && (defense < 0))
                && !this.containsNonCombatKeyword(keywords)
                && !sa.hasParam("UntilYourNextTurn")) {
            return false;
        }

        final Player opp = ai.getOpponent();
        final Target tgt = sa.getTarget();
        tgt.resetTargets();
        List<Card> list = new ArrayList<Card>();
        if (sa.hasParam("AILogic")) {
            if (sa.getParam("AILogic").equals("HighestPower")) {
                list = CardLists.getValidCards(CardLists.filter(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
                list = CardLists.getTargetableCards(list, sa);
                CardLists.sortAttack(list);
                if (!list.isEmpty()) {
                    tgt.addTarget(list.get(0));
                    return true;
                } else {
                    return false;
                }
            }
        } else if (sa.isCurse()) {
            if (sa.canTarget(opp)) {
                tgt.addTarget(opp);
                return true;
            }
            list = this.getCurseCreatures(ai, sa, defense, attack, keywords);
        } else {
            if (!tgt.canTgtCreature()) {
                ZoneType zone = tgt.getZone().get(0);
                list = Singletons.getModel().getGame().getCardsIn(zone);
            } else {
                list = this.getPumpCreatures(ai, sa, defense, attack, keywords);
            }
            if (sa.canTarget(ai)) {
                tgt.addTarget(ai);
                return true;
            }
        }

        list = CardLists.getValidCards(list, tgt.getValidTgts(), ai, sa.getSourceCard());
        if (Singletons.getModel().getGame().getStack().size() == 0) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getSourceCard());
                }
                if (Singletons.getModel().getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(opp)) {
                    list.remove(sa.getSourceCard());
                }
            }
        }

        if (list.isEmpty()) {
            return mandatory && this.pumpMandatoryTarget(ai, sa, mandatory);
        }

        if (!sa.isCurse()) {
            // Don't target cards that will die.
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.getSVar("Targeting").equals("Dies");
                }
            });
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) || (tgt.getNumTargeted() == 0)) {
                    if (mandatory) {
                        return this.pumpMandatoryTarget(ai, sa, mandatory);
                    }

                    tgt.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = CardFactoryUtil.getBestAI(list);
            tgt.addTarget(t);
            list.remove(t);
        }

        return true;
    } // pumpTgtAI()

    private boolean pumpMandatoryTarget(final Player ai, final SpellAbility sa, final boolean mandatory) {
        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        final Target tgt = sa.getTarget();
        final Player opp = ai.getOpponent();
        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        list = CardLists.getTargetableCards(list, sa);

        if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            tgt.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : tgt.getTargetCards()) {
            list.remove(c);
        }

        List<Card> pref;
        List<Card> forced;
        final Card source = sa.getSourceCard();

        if (sa.isCurse()) {
            pref = CardLists.filterControlledBy(list, opp);
            forced = CardLists.filterControlledBy(list, ai);
        } else {
            pref = CardLists.filterControlledBy(list, ai);
            forced = CardLists.filterControlledBy(list, opp);
        }

        while (tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(pref, "Creature").size() == 0) {
                c = CardFactoryUtil.getBestCreatureAI(pref);
            } else {
                c = CardFactoryUtil.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            tgt.addTarget(c);
        }

        while (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(forced, "Creature").size() == 0) {
                c = CardFactoryUtil.getWorstCreatureAI(forced);
            } else {
                c = CardFactoryUtil.getCheapestPermanentAI(forced, sa, true);
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
        final Card source = sa.getSourceCard();
        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
        } else {
            defense = AbilityUtils.calculateAmount(sa.getSourceCard(), numDefense, sa);
        }

        int attack;
        if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final String toPay = source.getSVar("PayX");

            if (toPay.equals("")) {
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(toPay);
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getSourceCard(), numAttack, sa);
        }

        if (sa.getTarget() == null) {
            if (mandatory) {
                return true;
            }
        } else {
            return this.pumpTgtAI(ai, sa, defense, attack, mandatory);
        }

        return true;
    } // pumpTriggerAI

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {

        final Card source = sa.getSourceCard();

        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            defense = Integer.parseInt(source.getSVar("PayX"));
        } else {
            defense = AbilityUtils.calculateAmount(sa.getSourceCard(), numDefense, sa);
        }

        int attack;
        if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            if (source.getSVar("PayX").equals("")) {
                // X is not set yet
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa.getRootAbility(), ai);
                source.setSVar("PayX", Integer.toString(xPay));
                attack = xPay;
            } else {
                attack = Integer.parseInt(source.getSVar("PayX"));
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getSourceCard(), numAttack, sa);
        }

        if ((sa.getTarget() == null) || !sa.getTarget().doesTarget()) {
            if (source.isCreature()) {
                if (!source.hasKeyword("Indestructible")
                        && ((source.getNetDefense() + defense) <= source.getDamage())) {
                    return false;
                }
                if ((source.getNetDefense() + defense) <= 0) {
                    return false;
                }
            }
        } else {
            //Targeted
            if (!this.pumpTgtAI(ai, sa, defense, attack, false)) {
                return false;
            }
        }

        return true;
    } // pumpDrawbackAI()
}
