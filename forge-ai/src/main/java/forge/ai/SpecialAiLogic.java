package forge.ai;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import forge.ai.ability.TokenAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Expressions;

import java.util.List;

/*
 * This class contains logic which is shared by several cards with different ability types (e.g. AF ChangeZone / AF Destroy)
 * Ideally, the naming scheme for methods in this class should be doXXXLogic, where XXX is the name of the logic,
 * and the signature of the method should be "public static boolean doXXXLogic(final Player ai, final SpellAbility sa),
 * possibly followed with any additional necessary parameters. These AI logic routines generally do all the work, so returning
 * true from them should indicate that the AI has made a decision and configured the spell ability (targeting, etc.) as it
 * deemed necessary.
 */

public class SpecialAiLogic {
    // A logic for cards like Pongify, Crib Swap, Angelic Ascension
    public static boolean doPongifyLogic(final Player ai, final SpellAbility sa) {
        Card source = sa.getHostCard();
        Game game = source.getGame();
        PhaseHandler ph = game.getPhaseHandler();
        boolean isDestroy = ApiType.Destroy.equals(sa.getApi());
        SpellAbility tokenSA = sa.findSubAbilityByType(ApiType.Token);
        if (tokenSA == null) {
            // Used wrong AI logic?
            return false;
        }

        List<Card> targetable = CardUtil.getValidCardsToTarget(sa);

        CardCollection listOpp = CardLists.filterControlledBy(targetable, ai.getOpponents());
        if (isDestroy) {
            listOpp = CardLists.getNotKeyword(listOpp, Keyword.INDESTRUCTIBLE);
            // TODO add handling for cards like targeting dies
        }

        Card choice = null;
        if (!listOpp.isEmpty()) {
            choice = ComputerUtilCard.getMostExpensivePermanentAI(listOpp);
            // can choice even be null?

            if (choice != null) {
                final Card token = TokenAi.spawnToken(choice.getController(), tokenSA);
                if (!token.isCreature() || token.getNetToughness() < 1) {
                    sa.resetTargets();
                    sa.getTargets().add(choice);
                    return true;
                }
                if (choice.isPlaneswalker()) {
                    if (choice.getCurrentLoyalty() * 35 > ComputerUtilCard.evaluateCreature(token)) {
                        sa.resetTargets();
                        sa.getTargets().add(choice);
                        return true;
                    } else {
                        return false;
                    }
                }
                if ((!choice.isCreature() || choice.isTapped()) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS) && ph.isPlayerTurn(ai) // prevent surprise combatant
                        || ComputerUtilCard.evaluateCreature(choice) < 1.5 * ComputerUtilCard.evaluateCreature(token)) {
                    choice = null;
                }
            }
        }

        // See if we have anything we can upgrade
        if (choice == null) {
            CardCollection listOwn = CardLists.filterControlledBy(targetable, ai);
            final Card token = TokenAi.spawnToken(ai, tokenSA);

            Card bestOwnCardToUpgrade = null;
            if (isDestroy) {
                // just choose any Indestructible
                // TODO maybe filter something that doesn't like to be targeted, or does something benefit by targeting
                bestOwnCardToUpgrade = Iterables.getFirst(CardLists.getKeyword(listOwn, Keyword.INDESTRUCTIBLE), null);
            }
            if (bestOwnCardToUpgrade == null) {
                bestOwnCardToUpgrade = ComputerUtilCard.getWorstCreatureAI(CardLists.filter(listOwn, card -> card.isCreature()
                        && (ComputerUtilCard.isUselessCreature(ai, card)
                        || ComputerUtilCard.evaluateCreature(token) > 2 * ComputerUtilCard.evaluateCreature(card))
                ));
            }
            if (bestOwnCardToUpgrade != null) {
                if (ComputerUtilCard.isUselessCreature(ai, bestOwnCardToUpgrade) || (ph.getPhase().isAfter(PhaseType.COMBAT_END) || !ph.isPlayerTurn(ai))) {
                    choice = bestOwnCardToUpgrade;
                }
            }
        }

        if (choice != null) {
            sa.resetTargets();
            sa.getTargets().add(choice);
            return true;
        }

        return false;
    }

    // A logic for cards that say "Sacrifice a creature: CARDNAME gets +X/+X until EOT"
    public static boolean doAristocratLogic(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final Card source = sa.getHostCard();
        final int numOtherCreats = Math.max(0, ai.getCreaturesInPlay().size() - 1);
        final int powerBonus = sa.hasParam("NumAtt") ? AbilityUtils.calculateAmount(source, sa.getParam("NumAtt"), sa) : 0;
        final int toughnessBonus = sa.hasParam("NumDef") ? AbilityUtils.calculateAmount(source, sa.getParam("NumDef"), sa) : 0;
        final boolean indestructible = sa.hasParam("KW") && sa.getParam("KW").contains("Indestructible");
        final int selfEval = ComputerUtilCard.evaluateCreature(source);
        final boolean isThreatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(source);

        if (numOtherCreats == 0) {
            return false;
        }

        // Try to save the card from death by pumping it if it's threatened with a damage spell
        if (isThreatened && (toughnessBonus > 0 || indestructible)) {
            SpellAbility saTop = game.getStack().peekAbility();

            if (saTop.getApi() == ApiType.DealDamage || saTop.getApi() == ApiType.DamageAll) {
                int dmg = AbilityUtils.calculateAmount(saTop.getHostCard(), saTop.getParam("NumDmg"), saTop) + source.getDamage();
                final int numCreatsToSac = indestructible ? 1 : Math.max(1, (int)Math.ceil((dmg - source.getNetToughness() + 1) / toughnessBonus));

                if (numCreatsToSac > 1) { // probably not worth sacrificing too much
                    return false;
                }

                if (indestructible || (source.getNetToughness() <= dmg && source.getNetToughness() + toughnessBonus * numCreatsToSac > dmg)) {
                    final CardCollection sacFodder = CardLists.filter(ai.getCreaturesInPlay(),
                            card -> ComputerUtilCard.isUselessCreature(ai, card)
                                    || card.hasSVar("SacMe")
                                    || ComputerUtilCard.evaluateCreature(card) < selfEval // Maybe around 150 is OK?
                    );
                    return sacFodder.size() >= numCreatsToSac;
                }
            }

            return false;
        }

        if (combat == null) {
            return false;
        }

        if (combat.isAttacking(source)) {
            if (combat.getBlockers(source).isEmpty()) {
                // Unblocked. Check if able to deal lethal, then sac'ing everything is fair game if
                // the opponent is tapped out or if we're willing to risk it (will currently risk it
                // in case it sacs less than half its creatures to deal lethal damage)

                // TODO: also teach the AI to account for Trample, but that's trickier (needs to account fully
                // for potential damage prevention, various effects like reducing damage to 0, etc.)

                final Player defPlayer = combat.getDefendingPlayerRelatedTo(source);
                final boolean defTappedOut = ComputerUtilMana.getAvailableManaEstimate(defPlayer) == 0;

                final boolean isInfect = source.hasKeyword(Keyword.INFECT); // Flesh-Eater Imp
                int lethalDmg = isInfect ? 10 - defPlayer.getPoisonCounters() : defPlayer.getLife();

                if (isInfect && !combat.getDefenderByAttacker(source).canReceiveCounters(CounterType.get(CounterEnumType.POISON))) {
                    lethalDmg = Integer.MAX_VALUE; // won't be able to deal poison damage to kill the opponent
                }

                final int numCreatsToSac = indestructible ? 1 : (lethalDmg - source.getNetCombatDamage()) / (powerBonus != 0 ? powerBonus : 1);

                if (defTappedOut || numCreatsToSac < numOtherCreats / 2) {
                    return source.getNetCombatDamage() < lethalDmg
                            && source.getNetCombatDamage() + numOtherCreats * powerBonus >= lethalDmg;
                } else {
                    return false;
                }
            } else {
                // We have already attacked. Thus, see if we have a creature to sac that is worse to lose
                // than the card we attacked with.
                final CardCollection sacTgts = CardLists.filter(ai.getCreaturesInPlay(),
                        card -> ComputerUtilCard.isUselessCreature(ai, card)
                                || ComputerUtilCard.evaluateCreature(card) < selfEval
                );

                if (sacTgts.isEmpty()) {
                    return false;
                }

                final int minDefT = Aggregates.min(combat.getBlockers(source), Card::getNetToughness);
                final int DefP = indestructible ? 0 : Aggregates.sum(combat.getBlockers(source), Card::getNetPower);

                // Make sure we don't over-sacrifice, only sac until we can survive and kill a creature
                return source.getNetToughness() - source.getDamage() <= DefP || source.getNetCombatDamage() < minDefT;
            }
        } else {
            // We can't deal lethal, check if there's any sac fodder than can be used for other circumstances
            final CardCollection sacFodder = CardLists.filter(ai.getCreaturesInPlay(),
                    card -> ComputerUtilCard.isUselessCreature(ai, card)
                            || card.hasSVar("SacMe")
                            || ComputerUtilCard.evaluateCreature(card) < selfEval // Maybe around 150 is OK?
            );

            return !sacFodder.isEmpty();
        }
    }

    // A logic for cards that say "Sacrifice a creature: put X +1/+1 counters on CARDNAME" (e.g. Falkenrath Aristocrat)
    public static boolean doAristocratWithCountersLogic(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String logic = sa.getParam("AILogic"); // should not even get here unless there's an Aristocrats logic applied
        final boolean isDeclareBlockers = ai.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS);

        final int numOtherCreats = Math.max(0, ai.getCreaturesInPlay().size() - 1);
        if (numOtherCreats == 0) {
            // Cut short if there's nothing to sac at all
            return false;
        }

        // Check if the standard Aristocrats logic applies first (if in the right conditions for it)
        final boolean isThreatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(source);
        if (isDeclareBlockers || isThreatened) {
            if (doAristocratLogic(ai, sa)) {
                return true;
            }
        }

        // Check if anything is to be gained from the PutCounter subability
        SpellAbility countersSa = null;
        if (sa.getSubAbility() == null || sa.getSubAbility().getApi() != ApiType.PutCounter) {
            if (sa.getApi() == ApiType.PutCounter) {
                // called directly from CountersPutAi
                countersSa = sa;
            }
        } else {
            countersSa = sa.getSubAbility();
        }

        if (countersSa == null) {
            // Shouldn't get here if there is no PutCounter subability (wrong AI logic specified?)
            System.err.println("Warning: AILogic AristocratCounters was specified on " + source + ", but there was no PutCounter SA in chain!");
            return false;
        }

        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final int selfEval = ComputerUtilCard.evaluateCreature(source);

        String typeToGainCtr = "";
        if (logic.contains(".")) {
            typeToGainCtr = logic.substring(logic.indexOf(".") + 1);
        }
        CardCollection relevantCreats = typeToGainCtr.isEmpty() ? ai.getCreaturesInPlay()
                : CardLists.filter(ai.getCreaturesInPlay(), CardPredicates.isType(typeToGainCtr));
        relevantCreats.remove(source);
        if (relevantCreats.isEmpty()) {
            // No relevant creatures to sac
            return false;
        }

        int numCtrs = AbilityUtils.calculateAmount(source, countersSa.getParam("CounterNum"), countersSa);

        if (combat != null && combat.isAttacking(source) && isDeclareBlockers) {
            if (combat.getBlockers(source).isEmpty()) {
                // Unblocked. Check if we can deal lethal after receiving counters.
                final Player defPlayer = combat.getDefendingPlayerRelatedTo(source);
                final boolean defTappedOut = ComputerUtilMana.getAvailableManaEstimate(defPlayer) == 0;

                final boolean isInfect = source.hasKeyword(Keyword.INFECT);
                int lethalDmg = isInfect ? 10 - defPlayer.getPoisonCounters() : defPlayer.getLife();

                if (isInfect && !combat.getDefenderByAttacker(source).canReceiveCounters(CounterType.get(CounterEnumType.POISON))) {
                    lethalDmg = Integer.MAX_VALUE; // won't be able to deal poison damage to kill the opponent
                }

                // Check if there's anything that will die anyway that can be eaten to gain a perma-bonus
                final CardCollection forcedSacTgts = CardLists.filter(relevantCreats,
                        card -> ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card)
                                || (combat.isAttacking(card) && combat.isBlocked(card) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, card, combat))
                );
                if (!forcedSacTgts.isEmpty()) {
                    return true;
                }

                final int numCreatsToSac = Math.max(0, (lethalDmg - source.getNetCombatDamage()) / numCtrs);

                if (defTappedOut || numCreatsToSac < relevantCreats.size() / 2) {
                    return source.getNetCombatDamage() < lethalDmg
                            && source.getNetCombatDamage() + relevantCreats.size() * numCtrs >= lethalDmg;
                } else {
                    return false;
                }
            } else {
                // We have already attacked. Thus, see if we have a creature to sac that is worse to lose
                // than the card we attacked with. Since we're getting a permanent bonus, consider sacrificing
                // things that are also threatened to be destroyed anyway.
                final CardCollection sacTgts = CardLists.filter(relevantCreats,
                        card -> ComputerUtilCard.isUselessCreature(ai, card)
                                || ComputerUtilCard.evaluateCreature(card) < selfEval
                                || ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card)
                );

                if (sacTgts.isEmpty()) {
                    return false;
                }

                final boolean sourceCantDie = ComputerUtilCombat.combatantCantBeDestroyed(ai, source);
                final int minDefT = Aggregates.min(combat.getBlockers(source), Card::getNetToughness);
                final int DefP = sourceCantDie ? 0 : Aggregates.sum(combat.getBlockers(source), Card::getNetPower);

                // Make sure we don't over-sacrifice, only sac until we can survive and kill a creature
                return source.getNetToughness() - source.getDamage() <= DefP || source.getNetCombatDamage() < minDefT;
            }
        } else {
            // We can't deal lethal, check if there's any sac fodder than can be used for other circumstances
            final boolean isBlocking = combat != null && combat.isBlocking(source);
            final CardCollection sacFodder = CardLists.filter(relevantCreats,
                    card -> ComputerUtilCard.isUselessCreature(ai, card)
                            || card.hasSVar("SacMe")
                            || (isBlocking && ComputerUtilCard.evaluateCreature(card) < selfEval)
                            || ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card)
            );

            return !sacFodder.isEmpty();
        }
    }

    // AF Branch Counterspell with UnlessCost logic (Bring the Ending, Anticognition)
    public static boolean doBranchCounterspellLogic(final Player ai, final SpellAbility sa) {
        // TODO: this is an ugly hack that needs a rewrite if more cards are added with different SA setups or
        // if this is to be made more generic in the future.
        SpellAbility top = ComputerUtilAbility.getTopSpellAbilityOnStack(ai.getGame(), sa);
        if (top == null || !sa.canTarget(top)) {
            return false;
        }
        Card host = sa.getHostCard();

        // pre-target the object to calculate the branch condition SVar, then clean up before running the real check
        sa.getTargets().add(top);
        int value = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("BranchConditionSVar"), sa);
        sa.resetTargets();

        String branchCompare = sa.getParamOrDefault("BranchConditionSVarCompare", "GE1");
        String operator = branchCompare.substring(0, 2);
        String operand = branchCompare.substring(2);
        final int operandValue = AbilityUtils.calculateAmount(host, operand, sa);
        boolean conditionMet = Expressions.compare(value, operator, operandValue);

        SpellAbility falseSub = sa.getAdditionalAbility("FalseSubAbility"); // this ability has the UnlessCost part
        boolean willPlay = false;
        if (!conditionMet && falseSub.hasParam("UnlessCost")) {
            // FIXME: We're emulating the UnlessCost on the SA to run the proper checks.
            // This is hacky, but it works. Perhaps a cleaner way exists?
            sa.getMapParams().put("UnlessCost", falseSub.getParam("UnlessCost"));
            willPlay = SpellApiToAi.Converter.get(ApiType.Counter).canPlayAIWithSubs(ai, sa);
            sa.getMapParams().remove("UnlessCost");
        } else {
            willPlay = SpellApiToAi.Converter.get(ApiType.Counter).canPlayAIWithSubs(ai, sa);
        }
        return willPlay;
    }

    public static boolean preferHasteForRiot(SpellAbility sa, Player player) {
        // returning true means preferring Haste, returning false means preferring a +1/+1 counter
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Card copy = CardCopyService.getLKICopy(host);
        copy.setLastKnownZone(player.getZone(ZoneType.Battlefield));

        // check state it would have on the battlefield
        CardCollection preList = new CardCollection(copy);
        game.getAction().checkStaticAbilities(false, Sets.newHashSet(copy), preList);
        // reset again?
        game.getAction().checkStaticAbilities(false);

        // can't gain counters, use Haste
        if (!copy.canReceiveCounters(CounterEnumType.P1P1)) {
            return true;
        }

        // already has Haste, use counter
        if (copy.hasKeyword(Keyword.HASTE)) {
            return false;
        }

        // not AI turn
        if (!game.getPhaseHandler().isPlayerTurn(player)) {
            return false;
        }

        // not before Combat
        if (!game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return false;
        }

        // TODO check other opponents too if able
        final Player opp = player.getWeakestOpponent();
        if (opp != null) {
            // TODO add predict Combat Damage?
            return opp.getLife() < copy.getNetPower();
        }

        // haste might not be good enough?
        return false;
    }
}