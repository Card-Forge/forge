package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import forge.ai.*;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.Cost;
import forge.game.cost.CostTapType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class PumpAi extends PumpAiBase {

    private static boolean hasTapCost(final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        return cost.hasSpecificCostType(CostTapType.class);
    }
    
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("FellTheMighty".equals(aiLogic)) {
            CardCollection aiList = ai.getCreaturesInPlay();
            if (aiList.isEmpty()) {
                return false;
            }
            CardLists.sortByPowerAsc(aiList);
            if (!sa.canTarget(aiList.get(0))) {
                return false;
            }
        } else if ("MoveCounter".equals(aiLogic)) {
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
            return SpecialAiLogic.doAristocratWithCountersLogic(ai, sa);
        } else if ("RiskFactor".equals(aiLogic)) {
            if (ai.getCardsIn(ZoneType.Hand).size() + 3 >= ai.getMaxHandSize()) {
                return false;
            }
        } else if (aiLogic.equals("SwitchPT")) {
            // Some more AI would be even better, but this is a good start to prevent spamming
            if (sa.isAbility() && sa.getActivationsThisTurn() > 0 && !sa.usesTargeting()) {
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

            return SpellAbilityAi.isSorcerySpeed(sa) || (ph.getNextTurn().equals(ai) && !ph.getPhase().isBefore(PhaseType.END_OF_TURN));
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
        if (game.getStack().isEmpty() && hasTapCost(sa.getPayCosts(), sa.getHostCard())) {
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
            return sa.isCurse() || SpellAbilityAi.isSorcerySpeed(sa) || main1Preferred;
        }
        return true;
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final SpellAbility root = sa.getRootAbility();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
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
            return true; // the preconditions to this are already tested in checkAiLogic
        } else if ("MoveCounter".equals(aiLogic)) {
            final SpellAbility moveSA = sa.findSubAbilityByType(ApiType.MoveCounter);

            if (moveSA == null) {
                return false;
            }

            final String counterType = moveSA.getParam("CounterType");
            final CounterType cType = "Any".equals(counterType) ? null : CounterType.getType(counterType);

            final PhaseHandler ph = game.getPhaseHandler();
            if (ph.inCombat() && ph.getPlayerTurn().isOpponentOf(ai)) {
                CardCollection attr = ph.getCombat().getAttackers();
                attr = CardLists.getTargetableCards(attr, sa);

                if (cType != null) {
                    attr = CardLists.filter(attr, CardPredicates.hasCounter(cType));
                    if (attr.isEmpty()) {
                        return false;
                    }
                    final String amountStr = moveSA.getParam("CounterNum");
                    CardCollection best = CardLists.filter(attr, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {

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

                            final Card srcCardCpy = CardUtil.getLKICopy(card);
                            // cant use substract on Copy
                            srcCardCpy.setCounters(cType, srcCardCpy.getCounters(cType) - amount);

                            if (cType.is(CounterEnumType.P1P1) && srcCardCpy.getNetToughness() <= 0) {
                                return srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                        || card.isToken();
                            }
                            return false;
                        }

                    });

                    if (best.isEmpty()) {
                        best = attr;
                    }

                    final Card card = ComputerUtilCard.getBestCreatureAI(best);
                    sa.getTargets().add(card);
                    return true;
                }
            } else {
                final String amountStr = moveSA.getParam("CounterNum");
                final boolean sameCtrl = moveSA.getTargetRestrictions().isSameController();

                List<Card> list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
                if (cType != null) {
                    list = CardLists.filter(list, CardPredicates.hasCounter(cType));
                    if (list.isEmpty()) {
                        return false;
                    }
                    List<Card> oppList = CardLists.filterControlledBy(list, ai.getOpponents());
                    if (!oppList.isEmpty() && !sameCtrl) {
                        List<Card> best = CardLists.filter(oppList, new Predicate<Card>() {
                            @Override
                            public boolean apply(Card card) {
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

                                final Card srcCardCpy = CardUtil.getLKICopy(card);
                                // cant use substract on Copy
                                srcCardCpy.setCounters(cType, srcCardCpy.getCounters(cType) - amount);

                                if (cType.is(CounterEnumType.P1P1) && srcCardCpy.getNetToughness() <= 0) {
                                    return srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                            || card.isToken();
                                }
                                return true;
                            }
                        });

                        if (best.isEmpty()) {
                            best = oppList;
                        }

                        final Card card = ComputerUtilCard.getBestCreatureAI(best);
                        sa.getTargets().add(card);
                        return true;
                    }
                }

            }
        } else if ("FellTheMighty".equals(aiLogic)) {
            CardCollection aiList = ai.getCreaturesInPlay();
            CardLists.sortByPowerAsc(aiList);
            Card lowest = aiList.get(0);

            CardCollection oppList = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                    CardPredicates.Presets.CREATURES, CardPredicates.isControlledByAnyOf(ai.getOpponents()));

            oppList = CardLists.filterPower(oppList, lowest.getNetPower() + 1);
            if (ComputerUtilCard.evaluateCreatureList(oppList) > 200) {
                sa.resetTargets();
                sa.getTargets().add(lowest);
                return true;
            }
        } else if (aiLogic.startsWith("Donate")) {
            // Donate step 1 - try to target an opponent, preferably one who does not have a donate target yet
            return SpecialCardAi.Donate.considerTargetingOpponent(ai, sa);
        } else if (aiLogic.equals("InfernoOfTheStarMounts")) {
            int numRedMana = ComputerUtilMana.determineLeftoverMana(sa, ai, "R");
            int currentPower = source.getNetPower();
            if (currentPower < 20 && currentPower + numRedMana >= 20) {
                return true;
            }
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if (!game.getStack().isEmpty() && !sa.isCurse() && !isFight) {
            return ComputerUtilCard.canPumpAgainstRemoval(ai, sa);
        }

        if (sa.hasParam("ActivationNumberSacrifice")) {
            final int sacActivations = Integer.parseInt(sa.getParam("ActivationNumberSacrifice").substring(2));
            final int activations = sa.getActivationsThisTurn();
            // don't risk sacrificing a creature just to pump it
            if (activations >= sacActivations - 1) {
                return false;
            }
        }

        if (sa.getSVar("X").equals("Count$xPaid")) {
            root.setXManaCostPaid(null);
        }

        int defense;
        if (numDefense.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
            if (sourceName.equals("Necropolis Fiend")) {
            	xPay = Math.min(xPay, sa.getActivatingPlayer().getCardsIn(ZoneType.Graveyard).size());
                sa.setSVar("X", Integer.toString(xPay));
            }
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
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai);
                root.setXManaCostPaid(xPay);
                attack = xPay;
            } else {
                attack = root.getXManaCostPaid();
            }
        } else {
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
            if (numAttack.contains("X") && sa.getSVar("X").equals("Count$CardsInYourHand") && source.isInZone(ZoneType.Hand)) {
                attack--; // the card will be spent casting the spell, so actual power is 1 less
            }
        }

        if ("ContinuousBonus".equals(aiLogic)) {
            // P/T bonus in a continuous static ability
            for (StaticAbility stAb : source.getStaticAbilities()) {
                if ("Continuous".equals(stAb.getParam("Mode"))) {
                    if (stAb.hasParam("AddPower")) {
                        attack += AbilityUtils.calculateAmount(source, stAb.getParam("AddPower"), stAb);
                    }
                    if (stAb.hasParam("AddToughness")) {
                        defense += AbilityUtils.calculateAmount(source, stAb.getParam("AddToughness"), stAb);
                    }
                    if (stAb.hasParam("AddKeyword")) {
                        keywords.addAll(Lists.newArrayList(stAb.getParam("AddKeyword").split(" & ")));
                    }
                }
            }
        }

        if ((numDefense.contains("X") && defense == 0) || (numAttack.contains("X") && attack == 0 && !isBerserk)) {
            return false;
        }

        //Untargeted
        if (!sa.usesTargeting()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

            if (cards.isEmpty()) {
                return false;
            }

            // when this happens we need to expand AI to consider if its ok for everything?
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
                if (!card.getController().isOpponentOf(ai)) {
                    if (ComputerUtilCard.shouldPumpCard(ai, sa, card, defense, attack, keywords, false)) {
                        return true;
                    } else if (containsUsefulKeyword(ai, keywords, card, sa, attack)) {
                        Card pumped = ComputerUtilCard.getPumpedCreature(ai, sa, card, 0, 0, keywords);
                        if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai)
                                || game.getPhaseHandler().is(PhaseType.COMBAT_BEGIN, ai)) {
                            return ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped);
                        }

                        return true;
                    } else if (grantsUsefulExtraBlockOpts(ai, sa, card, keywords)) {
                        return true;
                    }
                }
            }
            return false;
        }
        //Targeted
        if (!pumpTgtAI(ai, sa, defense, attack, false, false)) {
            return false;
        }

        return true;
    } // pumpPlayAI()

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
                && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
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
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            return targetingPlayer.getController().chooseTargetsFor(sa);
        }

        CardCollection list;
        if (sa.hasParam("AILogic")) {
            if (sa.getParam("AILogic").equals("HighestPower") || sa.getParam("AILogic").equals("ContinuousBonus")) {
                list = CardLists.getValidCards(CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES), tgt.getValidTgts(), ai, source, sa);
                list = CardLists.getTargetableCards(list, sa);
                CardLists.sortByPowerDesc(list);

                // Try not to kill own creatures with this pump
                CardCollection canDieToPump = new CardCollection();
                for (Card c : list) {
                    if (c.isCreature() && c.getController() == ai
                            && c.getNetToughness() - c.getTempToughnessBoost() + defense <= 0) {
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
            } else if (sa.getParam("AILogic").equals("DonateTargetPerm")) {
                // Donate step 2 - target a donatable permanent.
                return SpecialCardAi.Donate.considerDonatingPermanent(ai, sa);
            } else if (sa.getParam("AILogic").equals("SacOneEach")) {
                // each player sacrifices one permanent, e.g. Vaevictis, Asmadi the Dire - grab the worst for allied and
                // the best for opponents
                return SacrificeAi.doSacOneEachLogic(ai, sa);
            } else if (sa.getParam("AILogic").equals("Destroy")) {
                List<Card> tgts = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
                if (tgts.isEmpty()) {
                    return false;
                }

                List<Card> alliedTgts = CardLists.filter(tgts, Predicates.or(CardPredicates.isControlledByAnyOf(ai.getAllies()), CardPredicates.isController(ai)));
                List<Card> oppTgts = CardLists.filter(tgts, CardPredicates.isControlledByAnyOf(ai.getRegisteredOpponents()));

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
                return FightAi.canFightAi(ai, sa, attack, defense);
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
                list = new CardCollection(game.getCardsIn(zone));
            }
        }

        list = CardLists.getValidCards(list, tgt.getValidTgts(), ai, source, sa);
        if (game.getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare attack/block
            if (sa.getPayCosts().hasTapCost()) {
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && game.getPhaseHandler().isPlayerTurn(ai)) {
                    list.remove(sa.getHostCard());
                }
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        && game.getPhaseHandler().getPlayerTurn().isOpponentOf(ai)) {
                    list.remove(sa.getHostCard());
                }
            }
        }

        // Detain target nonland permanent: don't target noncreature permanents that don't have
        // any activated abilities.
        if ("DetainNonLand".equals(sa.getParam("AILogic"))) {
            list = CardLists.filter(list, Predicates.or(CardPredicates.Presets.CREATURES, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    for (SpellAbility sa : card.getSpellAbilities()) {
                        if (sa.isAbility()) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, list, true);

        if (list.isEmpty()) {
            if (ComputerUtil.activateForCost(sa, ai)) {
                return pumpMandatoryTarget(ai, sa);
            }
            return mandatory && pumpMandatoryTarget(ai, sa);
        }

        if (!sa.isCurse()) {
            // Don't target cards that will die.
            list = ComputerUtil.getSafeTargets(ai, sa, list);
        }

        if ("BetterCreatureThanSource".equals(sa.getParam("AILogic"))) {
            // Don't target cards that are not better in value than the targeting card
            final int sourceValue = ComputerUtilCard.evaluateCreature(source);
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    return card.isCreature() && ComputerUtilCard.evaluateCreature(card) > sourceValue + 30;
                }
            });
        }

        if ("ReplaySpell".equals(sa.getParam("AILogic"))) {
            if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false, mandatory)) {
                return false;
            }
        }

        while (sa.canAddMoreTarget()) {
            Card t = null;
            // boolean goodt = false;

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
    } // pumpTgtAI()

    private boolean pumpMandatoryTarget(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        CardCollection list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);

        if (list.size() < tgt.getMinTargets(sa.getHostCard(), sa)) {
            sa.resetTargets();
            return false;
        }

        // Remove anything that's already been targeted
        for (final Card c : sa.getTargets().getTargetCards()) {
            list.remove(c);
        }

        CardCollection pref;
        CardCollection forced;
        final Card source = sa.getHostCard();

        if (sa.isCurse()) {
            pref = CardLists.filterControlledBy(list, ai.getOpponents());
            forced = CardLists.filterControlledBy(list, ai);
        } else {
            pref = CardLists.filterControlledBy(list, ai);
            forced = CardLists.filterControlledBy(list, ai.getOpponents());
        }

        while (sa.canAddMoreTarget()) {
            if (pref.isEmpty()) {
                break;
            }

            Card c = ComputerUtilCard.getBestAI(list);
            pref.remove(c);
            sa.getTargets().add(c);
        }

        while (sa.getTargets().size() < tgt.getMinTargets(source, sa)) {
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

        if (sa.getTargets().size() < tgt.getMinTargets(source, sa)) {
            sa.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
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
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai);
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
                final int xPay = ComputerUtilCost.getMaxXValue(root, ai);
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
                return true;
            }
        } else {
            return pumpTgtAI(ai, sa, defense, attack, mandatory, true);
        }

        return true;
    } // pumpTriggerAI

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
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
                    source.setSVar("EnergyToPay", "Number$" + minus);
                    return true;
                }
            }
            return false;
        }

        int attack;
        if (numAttack.contains("X") && sa.getSVar("X").equals("Count$xPaid")) {
            if (root.getXManaCostPaid() == null) {
                // X is not set yet
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
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
                final int xPay = ComputerUtilCost.getMaxXValue(sa, ai);
                root.setXManaCostPaid(xPay);
                defense = xPay;
            } else {
                defense = root.getXManaCostPaid();
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
        }

        if (sa.usesTargeting()) {
            return pumpTgtAI(ai, sa, defense, attack, false, true);
        }

        if (source.isCreature()) {
            if (!source.hasKeyword(Keyword.INDESTRUCTIBLE) && source.getNetToughness() + defense <= source.getDamage()) {
                return false;
            }
            return source.getNetToughness() + defense > 0;
        }

        return true;
    } // pumpDrawbackAI()

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        //TODO Add logic here if necessary but I think the AI won't cast
        //the spell in the first place if it would curse its own creature
        //and the pump isn't mandatory
        return true;
    }
}
