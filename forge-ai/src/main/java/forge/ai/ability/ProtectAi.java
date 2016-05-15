package forge.ai.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.base.Predicate;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.ProtectEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ProtectAi extends SpellAbilityAi {
    private static boolean hasProtectionFrom(final Card card, final String color) {
        final List<String> onlyColors = new ArrayList<String>(MagicColor.Constant.ONLY_COLORS);

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
        if (threat.isArtifact() && choices.contains("artifacts")) {
            return "artifacts";
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
        final List<GameObject> threatenedObjects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa);
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
                    if (s==null) {
                        return false;
                    } else {
                        Combat combat = ai.getGame().getCombat();
                        int dmg = ComputerUtilCombat.damageIfUnblocked(c, ai.getOpponent(), combat, true);
                        float ratio = 1.0f * dmg / ai.getOpponent().getLife();
                        Random r = MyRandom.getRandom();
                        return r.nextFloat() < ratio;
                    }
                }
                return false;
            }
        });
        return list;
    } // getProtectCreatures()

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final boolean notAiMain1 = !(ph.getPlayerTurn() == ai && ph.getPhase() == PhaseType.MAIN1);
        if (SpellAbilityAi.isSorcerySpeed(sa)) {
            // sorceries can only give protection in order to create an unblockable attacker
            if (notAiMain1) {
                return false;
            }
        } else {
            final Game game = ai.getGame();
            if (game.getStack().isEmpty()) {
                // try to save attacker or blocker
                if (ph.getPhase() != PhaseType.COMBAT_DECLARE_BLOCKERS) {
                    if (notAiMain1) {
                        return false;
                    }
                }
            } else {
                // prevent repeated protects
                if (game.getStack().peekAbility().getApi() == ApiType.Protection) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            if (cards.size() == 0) {
                return false;
            }
            /*
             * when this happens we need to expand AI to consider if its ok
             * for everything? for (Card card : cards) { // TODO if AI doesn't
             * control Card and Pump is a Curse, than maybe use?
             * }
             */
        } else {
            return protectTgtAI(ai, sa, false);
        }
        return false;
    }

    private boolean protectTgtAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final Game game = ai.getGame();
        if (!mandatory && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS) 
        		&& game.getStack().isEmpty()) {
            return false;
        }

        final Card source = sa.getHostCard();

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
        CardCollection list = getProtectCreatures(ai, sa);

        list = CardLists.getValidCards(list, tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);

        if (game.getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if ((sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
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

        if (list.isEmpty()) {
            return mandatory && protectMandatoryTarget(ai, sa, mandatory);
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if ((sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) || sa.getTargets().getNumTargeted() == 0) {
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

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);

        if (list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : sa.getTargets().getTargetCards()) {
            list.remove(c);
        }

        CardCollection pref = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !hasProtectionFromAll(c, ProtectEffect.getProtectionList(sa));
            }
        });
        final CardCollection pref2 = CardLists.filterControlledBy(list, ai);
        pref = CardLists.filter(pref, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !hasProtectionFromAny(c, ProtectEffect.getProtectionList(sa));
            }
        });
        final List<Card> forced = CardLists.filterControlledBy(list, ai);
        final Card source = sa.getHostCard();

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

        if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(sa.getHostCard(), sa)) {
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
        final Card host = sa.getHostCard();
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
