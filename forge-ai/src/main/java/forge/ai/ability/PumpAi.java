package forge.ai.ability;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PumpAi extends PumpAiBase {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("MoveCounter".equals(aiLogic)) {
            final Game game = ai.getGame();
            List<Card> tgtCards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.isTargetableBy(sa));
            if (tgtCards.isEmpty()) {
                return false;
            }
            SpellAbility moveSA = null;
            SpellAbility sub = sa.getSubAbility();
            while (sub != null) {
                if (ApiType.MoveCounter.equals(sub.getApi())) {
                    moveSA = sub;
                    break;
                }
                sub = sub.getSubAbility();
            }

            if (moveSA == null) {
                System.err.println("MoveCounter AiLogic without MoveCounter SubAbility!");
                return false;
            }
        } else if ("Aristocrat".equals(aiLogic)) {
            return SpecialAiLogic.doAristocratLogic(ai, sa);
        } else if (aiLogic.startsWith("AristocratCounters")) {
            return SpecialAiLogic.doAristocratWithCountersLogic(ai, sa).willingToPlay();
        } else if (aiLogic.equals("SwitchPT")) {
            // Some more AI would be even better, but this is a good start to prevent spamming
            if (sa.isActivatedAbility() && sa.getActivationsThisTurn() > 0 && !sa.usesTargeting()) {
                // Will prevent flipping back and forth
                return false;
            }
        }

        return super.checkAiLogic(ai, sa, aiLogic);
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph,
            final String logic) {
        // special Phase check for various AI logics
        if (logic.equals("MoveCounter")) {
            if (ph.inCombat() && ph.getPlayerTurn().isOpponentOf(ai)) {
                return true;
            }

            return isSorcerySpeed(sa, ai) || (ph.getNextTurn().equals(ai) && !ph.getPhase().isBefore(PhaseType.END_OF_TURN));
        } else if (logic.equals("Aristocrat")) {
            final boolean isThreatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(sa.getHostCard());
            if (!ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && !isThreatened) {
                return false;
            }
        } else if (logic.equals("SwitchPT")) {
            // Some more AI would be even better, but this is a good start to prevent spamming
            if (ph.getPhase().isAfter(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE) || !ph.inCombat()) {
                return false;
            }
        }
        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Game game = ai.getGame();
        boolean main1Preferred = "Main1IfAble".equals(sa.getParam("AILogic")) && ph.is(PhaseType.MAIN1, ai);
        if (game.getStack().isEmpty() && sa.getPayCosts().hasTapCost()) {
            if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && ph.isPlayerTurn(ai)) {
                return false;
            }
            if (ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN) && ph.getPlayerTurn().isOpponentOf(ai)) {
                return false;
            }
        }
        if (game.getStack().isEmpty() && (ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN)
                || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
            // Instant-speed pumps should not be cast outside of combat when the
            // stack is empty
            return sa.isCurse() || isSorcerySpeed(sa, ai) || main1Preferred;
        }
        return true;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final SpellAbility root = sa.getRootAbility();
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & "))
                : Lists.newArrayList();
        final String numDefense = sa.getParamOrDefault("NumDef", "");
        final String numAttack = sa.getParamOrDefault("NumAtt", "");

        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        final boolean isFight = "Fight".equals(aiLogic) || "PowerDmg".equals(aiLogic);
        final boolean isBerserk = "Berserk".equals(aiLogic);

        if ("Pummeler".equals(aiLogic)) {
            return SpecialCardAi.ElectrostaticPummeler.consider(ai, sa);
        } else if (aiLogic.startsWith("AristocratCounters")) {
            // the preconditions to this are already tested in checkAiLogic
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else if ("GideonBlackblade".equals(aiLogic)) {
            return SpecialCardAi.GideonBlackblade.consider(ai, sa);
        } else if ("MoveCounter".equals(aiLogic)) {
            final SpellAbility moveSA = sa.findSubAbilityByType(ApiType.MoveCounter);

            if (moveSA == null) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            final String counterType = moveSA.getParam("CounterType");
            final String amountStr = moveSA.getParamOrDefault("CounterNum", "1");
            final CounterType cType = "Any".equals(counterType) ? null : CounterType.getType(counterType);

            final PhaseHandler ph = game.getPhaseHandler();
            if (ph.inCombat() && ph.getPlayerTurn().isOpponentOf(ai)) {
                CardCollection attr = ph.getCombat().getAttackers();
                attr = CardLists.getTargetableCards(attr, sa);

                if (cType != null) {
                    attr = CardLists.filter(attr, CardPredicates.hasCounter(cType));
                    if (attr.isEmpty()) {
                        return new AiAbilityDecision(0,AiPlayDecision.TargetingFailed);
                    }
                    CardCollection best = CardLists.filter(attr, card -> {
                        int amount = 0;
                        if (StringUtils.isNumeric(amountStr)) {
                            amount = AbilityUtils.calculateAmount(source, amountStr, moveSA);
                        } else if (source.hasSVar(amountStr)) {
                            if ("Count$ChosenNumber".equals(source.getSVar(amountStr))) {
                                amount = card.getCounters(cType);
                            }
                        }

                        int i = card.getCounters(cType);
                        if (i < amount) {
                            return false;
                        }

                        final Card srcCardCpy = CardCopyService.getLKICopy(card);
                        // can't use subtract on Copy
                        srcCardCpy.setCounters(cType, srcCardCpy.getCounters(cType) - amount);

                        if (cType.is(CounterEnumType.P1P1) && srcCardCpy.getNetToughness() <= 0) {
                            return srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                    || card.isToken();
                        }
                        return false;
                    });

                    if (best.isEmpty()) {
                        best = attr;
                    }

                    final Card card = ComputerUtilCard.getBestCreatureAI(best);
                    sa.getTargets().add(card);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            } else {
                final boolean sameCtrl = moveSA.getTargetRestrictions().isSameController();

                List<Card> list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
                if (cType != null) {
                    list = CardLists.filter(list, CardPredicates.hasCounter(cType));
                    if (list.isEmpty()) {
                        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
                    }
                    List<Card> oppList = CardLists.filterControlledBy(list, ai.getOpponents());
                    if (!oppList.isEmpty() && !sameCtrl) {
                        List<Card> best = CardLists.filter(oppList, card -> {
                            int amount = 0;
                            if (StringUtils.isNumeric(amountStr)) {
                                amount = AbilityUtils.calculateAmount(source, amountStr, moveSA);
                            } else if (source.hasSVar(amountStr)) {
                                if ("Count$ChosenNumber".equals(source.getSVar(amountStr))) {
                                    amount = card.getCounters(cType);
                                }
                            }

                            int i = card.getCounters(cType);
                            if (i < amount) {
                                return false;
                            }

                            final Card srcCardCpy = CardCopyService.getLKICopy(card);
                            // can't use subtract on Copy
                            srcCardCpy.setCounters(cType, srcCardCpy.getCounters(cType) - amount);

                            if (cType.is(CounterEnumType.P1P1) && srcCardCpy.getNetToughness() <= 0) {
                                return srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                        || card.isToken();
                            }
                            return true;
                        });

                        if (best.isEmpty()) {
                            best = oppList;
                        }

                        final Card card = ComputerUtilCard.getBestCreatureAI(best);
                        sa.getTargets().add(card);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }

            }
        } else if (aiLogic.startsWith("Donate")) {
            // Donate step 1 - try to target an opponent, preferably one who does not have a donate target yet
            return SpecialCardAi.Donate.considerTargetingOpponent(ai, sa);
        } else if (aiLogic.equals("InfernoOfTheStarMounts")) {
            int numRedMana = ComputerUtilMana.determineLeftoverMana(new SpellAbility.EmptySa(source), ai, "R", false);
            int currentPower = source.getNetPower();
            if (currentPower < 20 && currentPower + numRedMana >= 20) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }

        if (!game.getStack().isEmpty() && !sa.isCurse() && !isFight) {
            return ComputerUtilCard.canPumpAgainstRemoval(ai, sa);
        }

        if (sa.hasParam("ConditionActivationLimit")) {
            final int sacActivations = Integer.parseInt(sa.getParam("ConditionActivationLimit").substring(2));
            final int activations = sa.getActivationsThisTurn();
            // don't risk sacrificing a creature just to pump it
            if (activations >= sacActivations - 1) {
                return new AiAbilityDecision(0, AiPlayDecision.ConditionsNotMet);
            }
        }

        if (sa.getSVar("X").equals("Count$xPaid")) {
            root.setXManaCostPaid(null);
        }

        int defense;
        if (numDefense.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
            sa.setXManaCostPaid(xPay);
            defense = xPay;
            if (numDefense.equals("-X")) {
                defense = -xPay;
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
            if (numDefense.contains("X") && sa.getSVar("X").equals("Count$CardsInYourHand") && source.isInZone(ZoneType.Hand)) {
                defense--; // the card will be spent casting the spell, so actual toughness is 1 less
            }
        }

        int attack;
        if (numAttack.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (root.getXManaCostPaid() == null) {
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai, sa.isTrigger());
                root.setXManaCostPaid(xPay);
                attack = xPay;
            } else {
                attack = root.getXManaCostPaid();
            }
        } else {
            // TODO add Double
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
            if (numAttack.contains("X") && sa.getSVar("X").equals("Count$CardsInYourHand") && source.isInZone(ZoneType.Hand)) {
                attack--; // the card will be spent casting the spell, so actual power is 1 less
            }
        }

        if ((numDefense.contains("X") && defense == 0) || (numAttack.contains("X") && attack == 0 && !isBerserk)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (!sa.usesTargeting()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

            if (cards.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
            }

            // when this happens we need to expand AI to consider if its ok for everything?
            for (final Card card : cards) {
                if (sa.isCurse()) {
                    if (!card.getController().isOpponentOf(ai)) {
                        continue;
                    }

                    if (!containsUsefulKeyword(ai, keywords, card, sa, attack)) {
                        continue;
                    }

                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                if (!card.getController().isOpponentOf(ai)) {
                    if (ComputerUtilCard.shouldPumpCard(ai, sa, card, defense, attack, keywords, false)) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    } else if (containsUsefulKeyword(ai, keywords, card, sa, attack)) {
                        if (game.getPhaseHandler().is(PhaseType.MAIN1) && isSorcerySpeed(sa, ai) ||
                                game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai) ||
                                game.getPhaseHandler().is(PhaseType.COMBAT_BEGIN, ai)) {
                            Card pumped = ComputerUtilCard.getPumpedCreature(ai, sa, card, 0, 0, keywords);
                            if (ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                                // If the AI can attack with the pumped creature, then it is worth playing
                                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                            }
                            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactCombat);
                        }

                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (pumpTgtAI(ai, sa, defense, attack, false, false)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    private boolean pumpTgtAI(final Player ai, final SpellAbility sa, final int defense, final int attack, final boolean mandatory,
    		boolean immediately) {
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & "))
                : Lists.newArrayList();
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final boolean isFight = "Fight".equals(sa.getParam("AILogic")) || "PowerDmg".equals(sa.getParam("AILogic"));

        immediately = immediately || ComputerUtil.playImmediately(ai, sa);

        if (!mandatory
                && !immediately
                && (game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS) && !"AnyPhase".equals(sa.getParam("AILogic")))
                && !(sa.isCurse() && defense < 0)
                && !containsNonCombatKeyword(keywords)
                && !"UntilYourNextTurn".equals(sa.getParam("Duration"))
                && !"ReplaySpell".equals(sa.getParam("AILogic"))
                && !isFight) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();

        if ("PowerStruggle".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.PowerStruggle.considerFirstTarget(ai, sa);
        }

        if (sa.hasParam("TargetingPlayer") && sa.getActivatingPlayer().equals(ai) && !sa.isTrigger()) {
            if (!ComputerUtilAbility.isFullyTargetable(sa)) { // Volcanic Offering: only prompt if second part can happen too
                return false;
            }
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            return targetingPlayer.getController().chooseTargetsFor(sa);
        }

        CardCollection list;
        if (sa.hasParam("AILogic")) {
            if (sa.getParam("AILogic").equals("HighestPower") || sa.getParam("AILogic").equals("ContinuousBonus")) {
                list = CardLists.getValidCards(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES), tgt.getValidTgts(), ai, source, sa);
                list = CardLists.getTargetableCards(list, sa);
                CardLists.sortByPowerDesc(list);

                if (list.contains(source) && source.hasKeyword("You may choose not to untap CARDNAME during your untap step.") && sa.getPayCosts().hasTapCost()) {
                    list.remove(source); // don't tap a card that will be tapped as a part of the cost and won't untap normally.
                }

                // Try not to kill own creatures with this pump
                CardCollection canDieToPump = new CardCollection();
                for (Card c : list) {
                    if (c.isCreature() && c.getController() == ai
                            && c.getNetToughness() - c.getTempToughnessBoost() + defense <= 0) {
                        canDieToPump.add(c);
                    }
                    // Also, don't pump itself if the SA involves a sacrifice self cost
                    if (sa.getHostCard().equals(c) && ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts())) {
                        canDieToPump.add(c);
                    }
                }
                list.removeAll(canDieToPump);

                // Generally, don't pump anything that your opponents control
                if ("ContinuousBonus".equals(sa.getParam("AILogic"))) {
                    // TODO: make it possible for the AI to use this logic to kill opposing creatures
                    // when a toughness debuff is applied
                    list = CardLists.filter(list, CardPredicates.isController(ai));
                }

                if (!list.isEmpty()) {
                    sa.getTargets().add(list.get(0));
                    return true;
                } else {
                    return false;
                }
            }  else if (sa.getParam("AILogic").equals("SameName")) {
                return doSameNameLogic(ai, sa);
            } else if (sa.getParam("AILogic").equals("SacOneEach")) {
                // each player sacrifices one permanent, e.g. Vaevictis, Asmadi the Dire - grab the worst for allied and
                // the best for opponents
                return SacrificeAi.doSacOneEachLogic(ai, sa).willingToPlay();
            } else if (sa.getParam("AILogic").equals("Destroy")) {
                List<Card> tgts = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
                if (tgts.isEmpty()) {
                    return false;
                }

                List<Card> alliedTgts = CardLists.filter(tgts, CardPredicates.isControlledByAnyOf(ai.getAllies()).or(CardPredicates.isController(ai)));
                List<Card> oppTgts = CardLists.filter(tgts, CardPredicates.isControlledByAnyOf(ai.getOpponents()));

                Card destroyTgt = null;
                if (!oppTgts.isEmpty()) {
                    destroyTgt = ComputerUtilCard.getBestAI(oppTgts);
                } else {
                    // TODO: somehow limit this so that the AI doesn't always destroy own stuff when able?
                    destroyTgt = ComputerUtilCard.getWorstAI(alliedTgts);
                }

                if (destroyTgt != null) {
                    sa.getTargets().add(destroyTgt);
                    return true;
                }

                return false;
            }

            if (isFight) {
                return FightAi.canFight(ai, sa, attack, defense).willingToPlay();
            }
        }

        if (sa.isCurse()) {
            for (final Player opp : ai.getOpponents()) {
                if (sa.canTarget(opp)) {
                    sa.getTargets().add(opp);
                    return true;
                }
            }
            list = getCurseCreatures(ai, sa, defense, attack, keywords);
        } else {
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
                return true;
            }
            if (tgt.canTgtCreature()) {
                list = getPumpCreatures(ai, sa, defense, attack, keywords, immediately);
            } else {
                ZoneType zone = tgt.getZone().get(0);
                list = CardLists.getTargetableCards(game.getCardsIn(zone), sa);
            }
        }

        if (game.getStack().isEmpty() && sa.getPayCosts().hasTapCost()) {
            if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && game.getPhaseHandler().isPlayerTurn(ai)) {
                list.remove(source);
            }
            if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
                list.remove(source);
            }
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, true);

        if (list.isEmpty() && (mandatory || ComputerUtil.activateForCost(sa, ai))) {
            return pumpMandatoryTarget(ai, sa);
        }

        if (!sa.isCurse()) {
            // Don't target cards that will die.
            list = ComputerUtil.getSafeTargets(ai, sa, list);
        }

        if ("BetterCreatureThanSource".equals(sa.getParam("AILogic"))) {
            // Don't target cards that are not better in value than the targeting card
            final int sourceValue = ComputerUtilCard.evaluateCreature(source);
            list = CardLists.filter(list, card -> card.isCreature() && ComputerUtilCard.evaluateCreature(card) > sourceValue + 30);
        }

        if ("ReplaySpell".equals(sa.getParam("AILogic"))) {
            if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false, mandatory)) {
                return false;
            }
        }

        while (sa.canAddMoreTarget()) {
            Card t = null;
            // boolean goodt = false;

            list = CardLists.canSubsequentlyTarget(list, sa);

            if (list.isEmpty()) {
                if (!sa.isMinTargetChosen() || sa.isZeroTargets()) {
                    if (mandatory || ComputerUtil.activateForCost(sa, ai)) {
                        return pumpMandatoryTarget(ai, sa);
                    }

                    sa.resetTargets();
                    return false;
                } else {
                    // TODO is this good enough? for up to amounts?
                    break;
                }
            }

            t = ComputerUtilCard.getBestAI(list);
            //option to hold removal instead only applies for single targeted removal
            if (!immediately && tgt.getMaxTargets(source, sa) == 1 && sa.isCurse() && defense < 0) {
                if (!ComputerUtilCard.useRemovalNow(sa, t, -defense, ZoneType.Graveyard)
                        && !ComputerUtil.activateForCost(sa, ai)) {
                    return false;
                }
            }
            sa.getTargets().add(t);
            list.remove(t);
        }

        return true;
    }

    private boolean pumpMandatoryTarget(final Player ai, final SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        List<Card> list = CardUtil.getValidCardsToTarget(sa);

        if (list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        CardCollection pref;
        CardCollection forced;

        if (sa.isCurse()) {
            pref = CardLists.filterControlledBy(list, ai.getOpponents());
            forced = CardLists.filterControlledBy(list, ai.getYourTeam());
        } else {
            pref = CardLists.filterControlledBy(list, ai.getYourTeam());
            forced = CardLists.filterControlledBy(list, ai.getOpponents());
        }

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
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final SpellAbility root = sa.getRootAbility();
        final String numDefense = sa.getParamOrDefault("NumDef", "");
        final String numAttack = sa.getParamOrDefault("NumAtt", "");

        if (sa.getSVar("X").equals("Count$xPaid")) {
            sa.setXManaCostPaid(null);
        }

        int defense;
        if (numDefense.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (root.getXManaCostPaid() == null) {
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai, true);
                root.setXManaCostPaid(xPay);
                defense = xPay;
            } else {
                defense = root.getXManaCostPaid();
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
        }

        int attack;
        if (numAttack.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            if (root.getXManaCostPaid() == null) {
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai, true);
                root.setXManaCostPaid(xPay);
                attack = xPay;
            } else {
                attack = root.getXManaCostPaid();
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
        }

        if (!sa.usesTargeting()) {
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else {
            boolean result = pumpTgtAI(ai, sa, defense, attack, mandatory, true);
            return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        final SpellAbility root = sa.getRootAbility();
        final Card source = sa.getHostCard();

        final String numDefense = sa.getParamOrDefault("NumDef", "");
        final String numAttack = sa.getParamOrDefault("NumAtt", "");

        if (numDefense.equals("-X") && sa.getSVar("X").equals("Count$ChosenNumber")) {
            int energy = ai.getCounters(CounterEnumType.ENERGY);
            for (SpellAbility s : source.getSpellAbilities()) {
                if ("PayEnergy".equals(s.getParam("AILogic"))) {
                    energy += AbilityUtils.calculateAmount(source, s.getParam("CounterNum"), sa);
                    break;
                }
            }
            int minus = 0;
            for (; energy > 0; energy--) {
                if (pumpTgtAI(ai, sa, -energy, -energy, false, true)) {
                    minus = sa.getTargetCard().getNetToughness();
                    if (minus > energy || minus < 1) {
                        continue; // in case the calculation gets messed up somewhere
                    }
                    root.setSVar("EnergyToPay", "Number$" + minus);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        int attack;
        if (numAttack.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            if (root.getXManaCostPaid() == null) {
                // X is not set yet
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                root.setXManaCostPaid(xPay);
                attack = xPay;
            } else {
                attack = root.getXManaCostPaid();
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
        }

        int defense;
        if (numDefense.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            if (root.getXManaCostPaid() == null) {
                // X is not set yet
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                root.setXManaCostPaid(xPay);
                defense = xPay;
            } else {
                defense = root.getXManaCostPaid();
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
        }

        if (sa.usesTargeting()) {
            if (pumpTgtAI(ai, sa, defense, attack, false, true)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }

        if (source.isCreature()) {
            if (!source.hasKeyword(Keyword.INDESTRUCTIBLE) && source.getNetToughness() + defense <= source.getDamage()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            if (source.getNetToughness() + defense > 0) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            } else {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        //TODO Add logic here if necessary but I think the AI won't cast
        //the spell in the first place if it would curse its own creature
        //and the pump isn't mandatory
        return true;
    }

    private boolean doSameNameLogic(Player aiPlayer, SpellAbility sa) {
        final Game game = aiPlayer.getGame();
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final ZoneType origin = ZoneType.listValueOf(sa.getSubAbility().getParam("Origin")).get(0);
        CardCollection list = CardLists.getValidCards(game.getCardsIn(origin), tgt.getValidTgts(), aiPlayer,
                source, sa);
        list = CardLists.filterControlledBy(list, aiPlayer.getOpponents());
        if (list.isEmpty()) {
            return false; // no valid targets
        }

        Map<Player, Map.Entry<String, Integer>> data = Maps.newHashMap();

        // need to filter for the opponents first
        for (final Player opp : aiPlayer.getOpponents()) {
            CardCollection oppList = CardLists.filterControlledBy(list, opp);

            // no cards
            if (oppList.isEmpty()) {
                continue;
            }

            // Compute value for each possible target
            Map<String, Integer> values = ComputerUtilCard.evaluateCreatureListByName(oppList);

            // reject if none of them can be targeted
            oppList = CardLists.filter(oppList, CardPredicates.isTargetableBy(sa));
            // none can be targeted
            if (oppList.isEmpty()) {
                continue;
            }

            List<String> toRemove = Lists.newArrayList();
            for (final String name : values.keySet()) {
                if (!oppList.anyMatch(CardPredicates.nameEquals(name))) {
                    toRemove.add(name);
                }
            }
            values.keySet().removeAll(toRemove);

            data.put(opp, Collections.max(values.entrySet(), Map.Entry.comparingByValue()));
        }

        if (!data.isEmpty()) {
            Map.Entry<Player, Map.Entry<String, Integer>> max = Collections.max(data.entrySet(), Comparator.comparingInt(o -> o.getValue().getValue()));

            // filter list again by the opponent and a creature of the wanted name that can be targeted
            list = CardLists.filter(CardLists.filterControlledBy(list, max.getKey()),
                    CardPredicates.nameEquals(max.getValue().getKey()), CardPredicates.isTargetableBy(sa));

            // list should contain only one element or zero
            sa.resetTargets();
            for (Card c : list) {
                sa.getTargets().add(c);
                return true;
            }
        }

        return false;
    }
}
