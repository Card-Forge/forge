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
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
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
import forge.util.Aggregates;
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
            return doAristocratLogic(sa, ai);
        } else if (aiLogic.startsWith("AristocratCounters")) {
            return doAristocratWithCountersLogic(sa, ai);
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

            if (!ph.getNextTurn().equals(ai) || ph.getPhase().isBefore(PhaseType.END_OF_TURN)) {
                return false;
            }
            return true;
        } else if (logic.equals("Aristocrat")) {
            final boolean isThreatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(sa.getHostCard());
            if (!ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && !isThreatened) {
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
            if (!sa.isCurse() && !SpellAbilityAi.isSorcerySpeed(sa) && !main1Preferred) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & "))
                : Lists.<String>newArrayList();
        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

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
            final CounterType cType = "Any".equals(counterType) ? null : CounterType.valueOf(counterType);

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

                            if (CounterType.P1P1.equals(cType) && srcCardCpy.getNetToughness() <= 0) {
                                if (srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                        || card.isToken()) {
                                    return true;
                                }
                                return false;
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

                                if (CounterType.P1P1.equals(cType) && srcCardCpy.getNetToughness() <= 0) {
                                    if (srcCardCpy.getCounters(cType) > 0 || !card.hasKeyword(Keyword.UNDYING)
                                            || card.isToken()) {
                                        return true;
                                    }
                                    return false;
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
        } else if (sa.hasParam("AILogic") && sa.getParam("AILogic").startsWith("Donate")) {
            // Donate step 1 - try to target an opponent, preferably one who does not have a donate target yet
            return SpecialCardAi.Donate.considerTargetingOpponent(ai, sa);
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

        if (source.getSVar("X").equals("Count$xPaid")) {
            source.setSVar("PayX", "");
        }

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            if (sourceName.equals("Necropolis Fiend")) {
            	xPay = Math.min(xPay, sa.getActivatingPlayer().getCardsIn(ZoneType.Graveyard).size());
                sa.setSVar("X", Integer.toString(xPay));
            }
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
            if (numDefense.equals("-X")) {
                defense = -xPay;
            }
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
            if (numDefense.contains("X") && sa.getSVar("X").equals("Count$CardsInYourHand") && source.getZone().is(ZoneType.Hand)) {
                defense--; // the card will be spent casting the spell, so actual toughness is 1 less
            }
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
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
            if (numAttack.contains("X") && sa.getSVar("X").equals("Count$CardsInYourHand") && source.getZone().is(ZoneType.Hand)) {
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
        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);

            if (cards.isEmpty()) {
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
                if (!card.getController().isOpponentOf(ai)) {
                    if (ComputerUtilCard.shouldPumpCard(ai, sa, card, defense, attack, keywords, false)) {
                        return true;
                    } else if (containsUsefulKeyword(ai, keywords, card, sa, attack)) {

                        Card pumped = ComputerUtilCard.getPumpedCreature(ai, sa, card, 0, 0, keywords);
                        if (game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai)
                                || game.getPhaseHandler().is(PhaseType.COMBAT_BEGIN, ai)) {
                            if (!ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                                return false;
                            }
                        }

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

        if ("DebuffForXCounters".equals(sa.getParam("AILogic")) && sa.getTargetCard() != null) {
            // e.g. Skullmane Baku
            CounterType ctrType = CounterType.KI;
            for (CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostRemoveCounter) {
                    ctrType = ((CostRemoveCounter)part).counter;
                    break;
                }
            }

            // Do not pay more counters than necessary to kill the targeted creature
            int chosenX = Math.min(source.getCounters(ctrType), sa.getTargetCard().getNetToughness());
            sa.setSVar("ChosenX", String.valueOf(chosenX));
        }

        return true;
    } // pumpPlayAI()

    private boolean pumpTgtAI(final Player ai, final SpellAbility sa, final int defense, final int attack, final boolean mandatory,
    		boolean immediately) {
        final List<String> keywords = sa.hasParam("KW") ? Arrays.asList(sa.getParam("KW").split(" & "))
                : Lists.<String>newArrayList();
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final boolean isFight = "Fight".equals(sa.getParam("AILogic")) || "PowerDmg".equals(sa.getParam("AILogic"));

        immediately |= ComputerUtil.playImmediately(ai, sa);

        if (!mandatory
                && !immediately
                && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !(sa.isCurse() && defense < 0)
                && !containsNonCombatKeyword(keywords)
                && !sa.hasParam("UntilYourNextTurn")
                && !"Snapcaster".equals(sa.getParam("AILogic"))
                && !isFight) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        sa.resetTargets();
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
            if (!tgt.canTgtCreature()) {
                ZoneType zone = tgt.getZone().get(0);
                list = new CardCollection(game.getCardsIn(zone));
            } else {
                list = getPumpCreatures(ai, sa, defense, attack, keywords, immediately);
            }
            if (sa.canTarget(ai)) {
                sa.getTargets().add(ai);
                return true;
            }
        }

        list = CardLists.getValidCards(list, tgt.getValidTgts(), ai, source, sa);
        if (game.getStack().isEmpty()) {
            // If the cost is tapping, don't activate before declare
            // attack/block
            if (sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()) {
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
                    for (SpellAbility sa: card.getSpellAbilities()) {
                        if (sa.isAbility()) {
                            return true;
                        }
                    }
                    return false;
                }
            }));
        }

        // Filter AI-specific targets if provided
        list = ComputerUtil.filterAITgts(sa, ai, (CardCollection)list, true);

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

        if ("Snapcaster".equals(sa.getParam("AILogic"))) {
            if (!ComputerUtil.targetPlayableSpellCard(ai, list, sa, false)) {
                return false;
            }
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            Card t = null;
            // boolean goodt = false;

            if (list.isEmpty()) {
                if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa) || sa.getTargets().getNumTargeted() == 0) {
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
        CardCollection list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getHostCard(), sa);
        list = CardLists.getTargetableCards(list, sa);

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

        while (sa.getTargets().getNumTargeted() < tgt.getMaxTargets(source, sa)) {
            if (pref.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(pref, "Creature").isEmpty()) {
                c = ComputerUtilCard.getBestCreatureAI(pref);
            } else {
                c = ComputerUtilCard.getMostExpensivePermanentAI(pref, sa, true);
            }

            pref.remove(c);

            sa.getTargets().add(c);
        }

        while (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) {
            if (forced.isEmpty()) {
                break;
            }

            Card c;
            if (CardLists.getNotType(forced, "Creature").isEmpty()) {
                c = ComputerUtilCard.getWorstCreatureAI(forced);
            } else {
                c = ComputerUtilCard.getCheapestPermanentAI(forced, sa, true);
            }

            forced.remove(c);

            sa.getTargets().add(c);
        }

        if (sa.getTargets().getNumTargeted() < tgt.getMinTargets(source, sa)) {
            sa.resetTargets();
            return false;
        }

        return true;
    } // pumpMandatoryTarget()

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(xPay));
            defense = xPay;
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
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
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
        }

        if (sa.getTargetRestrictions() == null) {
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

        final Card source = sa.getHostCard();

        final String numDefense = sa.hasParam("NumDef") ? sa.getParam("NumDef") : "";
        final String numAttack = sa.hasParam("NumAtt") ? sa.getParam("NumAtt") : "";

        if (numDefense.equals("-X") && sa.getSVar("X").equals("Count$ChosenNumber")) {
            int energy = ai.getCounters(CounterType.ENERGY);
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

        int defense;
        if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")) {
            defense = Integer.parseInt(source.getSVar("PayX"));
        } else {
            defense = AbilityUtils.calculateAmount(sa.getHostCard(), numDefense, sa);
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
            attack = AbilityUtils.calculateAmount(sa.getHostCard(), numAttack, sa);
        }

        if ((sa.getTargetRestrictions() == null) || !sa.getTargetRestrictions().doesTarget()) {
            if (source.isCreature()) {
                if (!source.hasKeyword(Keyword.INDESTRUCTIBLE) && source.getNetToughness() + defense <= source.getDamage()) {
                    return false;
                }
                if (source.getNetToughness() + defense <= 0) {
                    return false;
                }
            }
        } else {
            //Targeted
            if (!pumpTgtAI(ai, sa, defense, attack, false, true)) {
                return false;
            }
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

    public static boolean doAristocratLogic(final SpellAbility sa, final Player ai) {
        // A logic for cards that say "Sacrifice a creature: CARDNAME gets +X/+X until EOT"
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
                            new Predicate<Card>() {
                                @Override
                                public boolean apply(Card card) {
                                    return ComputerUtilCard.isUselessCreature(ai, card)
                                            || card.hasSVar("SacMe")
                                            || ComputerUtilCard.evaluateCreature(card) < selfEval; // Maybe around 150 is OK?
                                }
                            }
                    );
                    if (sacFodder.size() >= numCreatsToSac) {
                        return true;
                    }
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

                if (isInfect && !combat.getDefenderByAttacker(source).canReceiveCounters(CounterType.POISON)) {
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
                        new Predicate<Card>() {
                            @Override
                            public boolean apply(Card card) {
                                return ComputerUtilCard.isUselessCreature(ai, card)
                                        || ComputerUtilCard.evaluateCreature(card) < selfEval;
                            }
                        }
                );

                if (sacTgts.isEmpty()) {
                    return false;
                }

                final int minDefT = Aggregates.min(combat.getBlockers(source), CardPredicates.Accessors.fnGetNetToughness);
                final int DefP = indestructible ? 0 : Aggregates.sum(combat.getBlockers(source), CardPredicates.Accessors.fnGetNetPower);

                // Make sure we don't over-sacrifice, only sac until we can survive and kill a creature
                return source.getNetToughness() - source.getDamage() <= DefP || source.getNetCombatDamage() < minDefT;
            }
        } else {
            // We can't deal lethal, check if there's any sac fodder than can be used for other circumstances
            final CardCollection sacFodder = CardLists.filter(ai.getCreaturesInPlay(),
                    new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {
                            return ComputerUtilCard.isUselessCreature(ai, card)
                                    || card.hasSVar("SacMe")
                                    || ComputerUtilCard.evaluateCreature(card) < selfEval; // Maybe around 150 is OK?
                        }
                    }
            );

            return !sacFodder.isEmpty();
        }
    }

    public static boolean doAristocratWithCountersLogic(final SpellAbility sa, final Player ai) {
        // A logic for cards that say "Sacrifice a creature: put X +1/+1 counters on CARDNAME" (e.g. Falkenrath Aristocrat)
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
            if (doAristocratLogic(sa, ai)) {
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

                if (isInfect && !combat.getDefenderByAttacker(source).canReceiveCounters(CounterType.POISON)) {
                    lethalDmg = Integer.MAX_VALUE; // won't be able to deal poison damage to kill the opponent
                }

                // Check if there's anything that will die anyway that can be eaten to gain a perma-bonus
                final CardCollection forcedSacTgts = CardLists.filter(relevantCreats,
                        new Predicate<Card>() {
                            @Override
                            public boolean apply(Card card) {
                                return ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card)
                                        || (combat.isAttacking(card) && combat.isBlocked(card) && ComputerUtilCombat.combatantWouldBeDestroyed(ai, card, combat));
                            }
                        }
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
                        new Predicate<Card>() {
                            @Override
                            public boolean apply(Card card) {
                                return ComputerUtilCard.isUselessCreature(ai, card)
                                        || ComputerUtilCard.evaluateCreature(card) < selfEval
                                        || ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card);
                            }
                        }
                );

                if (sacTgts.isEmpty()) {
                    return false;
                }

                final boolean sourceCantDie = ComputerUtilCombat.attackerCantBeDestroyedInCombat(ai, source);
                final int minDefT = Aggregates.min(combat.getBlockers(source), CardPredicates.Accessors.fnGetNetToughness);
                final int DefP = sourceCantDie ? 0 : Aggregates.sum(combat.getBlockers(source), CardPredicates.Accessors.fnGetNetPower);

                // Make sure we don't over-sacrifice, only sac until we can survive and kill a creature
                return source.getNetToughness() - source.getDamage() <= DefP || source.getNetCombatDamage() < minDefT;
            }
        } else {
            // We can't deal lethal, check if there's any sac fodder than can be used for other circumstances
            final boolean isBlocking = combat != null && combat.isBlocking(source);
            final CardCollection sacFodder = CardLists.filter(relevantCreats,
                    new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {
                            return ComputerUtilCard.isUselessCreature(ai, card)
                                    || card.hasSVar("SacMe")
                                    || (isBlocking && ComputerUtilCard.evaluateCreature(card) < selfEval)
                                    || ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card);
                        }
                    }
            );

            return !sacFodder.isEmpty();
        }
    }
}
