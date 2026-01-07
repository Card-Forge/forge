package forge.ai.ability;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.ProtectEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;

public class ProtectAi extends SpellAbilityAi {
    private static boolean hasProtectionFrom(final Card card, final String color) {
        final List<String> onlyColors = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);

        // make sure we have a valid color
        if (!onlyColors.contains(color)) {
            return false;
        }

        final String protection = "Protection from " + color;

        return card.hasKeyword(protection);
    }

    private static boolean hasProtectionFromAny(final Card card, final Iterable<String> colors) {
        boolean protect = false;
        for (final String color : colors) {
            protect |= hasProtectionFrom(card, color);
        }
        return protect;
    }

    private static boolean hasProtectionFromAll(final Card card, final Iterable<String> colors) {
        boolean protect = true;
        boolean isEmpty = true;
        for (final String color : colors) {
            protect &= hasProtectionFrom(card, color);
            isEmpty = false;
        }
        return protect && !isEmpty;
    }
    
    /**
     * \brief Find a choice for a Protect SpellAbility that protects from a specific threat card.
     * @param threat Card to protect against
     * @param sa Protect SpellAbility
     * @return choice that can protect against the given threat, null if no such choice exists
     */
    public static String toProtectFrom(final Card threat, SpellAbility sa) {
        if (sa.getApi() != ApiType.Protection) {
            return null;
        }
        final List<String> choices = ProtectEffect.getProtectionList(sa);
        if (threat.isArtifact() && choices.contains("Artifact")) {
            return "Artifact";
        }
        if (threat.isBlack() && choices.contains("black")) {
            return "black";
        }
        if (threat.isBlue() && choices.contains("blue")) {
            return "blue";
        }
        if (threat.isGreen() && choices.contains("green")) {
            return "green";
        }
        if (threat.isRed() && choices.contains("red")) {
            return "red";
        }
        if (threat.isWhite() && choices.contains("white")) {
            return "white";
        }
        return null;
    }

    /**
     * <p>
     * getProtectCreatures.
     * </p>
     *
     */
    public static CardCollection getProtectCreatures(final Player ai, final SpellAbility sa) {
        final List<String> gains = ProtectEffect.getProtectionList(sa);
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        
        CardCollection list = ai.getCreaturesInPlay();
        final List<GameObject> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);
        list = CardLists.filter(list, c -> {
            if (!c.canBeTargetedBy(sa)) {
                return false;
            }

            // Don't add duplicate protections
            if (hasProtectionFromAll(c, gains)) {
                return false;
            }

            if (threatenedObjects.contains(c)) {
                return true;
            }

            if (combat != null) {
                //creature is blocking and would be destroyed itself
                if (combat.isBlocking(c) && ComputerUtilCombat.blockerWouldBeDestroyed(ai, c, combat)) {
                    List<Card> threats = combat.getAttackersBlockedBy(c);
                    return threats != null && !threats.isEmpty() && ProtectAi.toProtectFrom(threats.get(0), sa) != null;
                }

                //creature is attacking and would be destroyed itself
                if (combat.isAttacking(c) && combat.isBlocked(c) && ComputerUtilCombat.attackerWouldBeDestroyed(ai, c, combat)) {
                    CardCollection threats = combat.getBlockers(c);
                    if (threats != null && !threats.isEmpty()) {
                        ComputerUtilCard.sortByEvaluateCreature(threats);
                        return ProtectAi.toProtectFrom(threats.get(0), sa) != null;
                    }
                }
            }

            //make unblockable
            if (ph.getPlayerTurn() == ai && ph.getPhase() == PhaseType.MAIN1) {
                AiAttackController aiAtk = new AiAttackController(ai, c);
                String s = aiAtk.toProtectAttacker(sa);
                if (s == null) {
                    return false;
                }
                Player opponent = ai.getWeakestOpponent();
                Combat combat1 = ai.getGame().getCombat();
                int dmg = ComputerUtilCombat.damageIfUnblocked(c, opponent, combat1, true);
                float ratio = 1.0f * dmg / opponent.getLife();
                return MyRandom.getRandom().nextFloat() < ratio;
            }
            return false;
        });
        return list;
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final boolean notAiMain1 = !(ph.getPlayerTurn() == ai && ph.getPhase() == PhaseType.MAIN1);
        // sorceries can only give protection in order to create an unblockable attacker
        return !isSorcerySpeed(sa, ai) || !notAiMain1;
    }
    
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.usesTargeting()) {
            return protectTgtAI(ai, sa, false);
        }

        final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
        if (cards.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        } else if (cards.size() == 1) {
            // Affecting single card
            if (getProtectCreatures(ai, sa).contains(cards.get(0))) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        /*
         * when this happens we need to expand AI to consider if its ok
         * for everything? for (Card card : cards) { // TODO if AI doesn't
         * control Card and Pump is a Curse, than maybe use?
         * }
         */
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    private AiAbilityDecision protectTgtAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        if (!mandatory && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS) 
        		&& game.getStack().isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.WaitForCombat);
        }

        final Card source = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        CardCollection list = getProtectCreatures(ai, sa);

        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), source, sa);

        if (game.getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare attack/block
            if (sa.getPayCosts().hasTapCost()) {
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getHostCard());
                }
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getHostCard());
                }
            }
        }

        // Don't target cards that will die.
        list = ComputerUtil.getSafeTargets(ai, sa, list);

        // Don't target self if the cost includes sacrificing itself
        if (ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts())) {
            list.remove(source);
        }

        if (list.isEmpty()) {
            if (mandatory && protectMandatoryTarget(ai, sa)) {
                return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
            } else {
                sa.resetTargets();
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        while (sa.canAddMoreTarget()) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((sa.getTargets().size() < tgt.getMinTargets(source, sa)) || sa.getTargets().size() == 0) {
                    if (mandatory) {
                        if (protectMandatoryTarget(ai, sa)) {
                            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
                        }
                    }

                    sa.resetTargets();
                    return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = ComputerUtilCard.getBestCreatureAI(list);
            sa.getTargets().add(t);
            list.remove(t);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    } // protectTgtAI()

    private static boolean protectMandatoryTarget(final Player ai, final SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final List<Card> list = CardUtil.getValidCardsToTarget(sa);

        if (list.size() < tgt.getMinTargets(source, sa)) {
            sa.resetTargets();
            return false;
        }

        CardCollection pref = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, c -> !hasProtectionFromAll(c, ProtectEffect.getProtectionList(sa)));
        final CardCollection pref2 = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, c -> !hasProtectionFromAny(c, ProtectEffect.getProtectionList(sa)));
        final List<Card> forced = CardLists.filterControlledBy(list, ai);

        while (sa.canAddMoreTarget()) {
            if (pref.isEmpty()) {
                break;
            }

            Card c = ComputerUtilCard.getBestAI(pref);
            pref.remove(c);
            sa.getTargets().add(c);
        }

        while (sa.canAddMoreTarget()) {
            if (pref2.isEmpty()) {
                break;
            }

            Card c = ComputerUtilCard.getBestAI(pref2);
            pref2.remove(c);
            sa.getTargets().add(c);
        }

        while (!sa.isMinTargetChosen()) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(forced, "Creature").isEmpty()) {
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
    } // protectMandatoryTarget()

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (!sa.usesTargeting()) {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        } else {
            return protectTgtAI(ai, sa, mandatory);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    } // protectTriggerAI

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        if (sa.usesTargeting()) {
            return protectTgtAI(ai, sa, false);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    } // protectDrawbackAI()

}
