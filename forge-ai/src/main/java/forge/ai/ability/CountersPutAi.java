package forge.ai.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AiCardMemory;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.ai.SpecialAiLogic;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;

public class CountersPutAi extends CountersAi {

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#willPayCosts(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.cost.Cost,
     * forge.game.card.Card)
     */
    @Override
    protected boolean willPayCosts(Player ai, SpellAbility sa, Cost cost, Card source) {
        final String type = sa.getParam("CounterType");
        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (!super.willPayCosts(ai, sa, cost, source)) {
            return false;
        }

        // disable moving counters (unless a specialized AI logic supports it)
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter) {
                final CostRemoveCounter remCounter = (CostRemoveCounter) part;
                final CounterType counterType = remCounter.counter;
                if (counterType.getName().equals(type) && !aiLogic.startsWith("MoveCounter")) {
                    return false;
                }
                if (!part.payCostFromSource()) {
                    if (counterType.is(CounterEnumType.P1P1)) {
                        return false;
                    }
                    continue;
                }
                // don't kill the creature
                if (counterType.is(CounterEnumType.P1P1) && source.getLethalDamage() <= 1) {
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        final Card source = sa.getHostCard();

        if (sa.isOutlast()) {
            if (ph.is(PhaseType.MAIN2, ai)) { // applicable to non-attackers only
                float chance = 0.8f;
                if (ComputerUtilCard.doesSpecifiedCreatureBlock(ai, source)) {
                    return false;
                }
                return chance > MyRandom.getRandom().nextFloat();
            } else {
                return false;
            }
        }

        if (sa.hasParam("LevelUp")) {
            // creatures enchanted by curse auras have low priority
            if (ph.getPhase().isBefore(PhaseType.MAIN2)) {
                for (Card aura : source.getEnchantedBy()) {
                    if (aura.getController().isOpponentOf(ai)) {
                        return false;
                    }
                }
            }
            int maxLevel = Integer.parseInt(sa.getParam("MaxLevel"));
            return source.getCounters(CounterEnumType.LEVEL) < maxLevel;
        }

        if ("CrawlingBarrens".equals(sa.getParam("AILogic"))) {
            return true;
        }

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    @Override
    protected boolean checkApiLogic(Player ai, final SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what the expected targets could be
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        CardCollection list;
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParamOrDefault("CounterNum", "1");
        final boolean divided = sa.isDividedAsYouChoose();
        final String logic = sa.getParamOrDefault("AILogic", "");
        PhaseHandler ph = ai.getGame().getPhaseHandler();

        final boolean isClockwork = "True".equals(sa.getParam("UpTo")) && "Self".equals(sa.getParam("Defined"))
                && "P1P0".equals(sa.getParam("CounterType")) && "Count$xPaid".equals(source.getSVar("X"))
                && sa.hasParam("MaxFromEffect");
        boolean playAggro = ((PlayerControllerAi) ai.getController()).getAi().getProperty(AiProps.PLAY_AGGRO).equals("true");

        if ("ExistingCounter".equals(type)) {
            final boolean eachExisting = sa.hasParam("EachExistingCounter");
            // Prevent animation module from crashing for now
            // TODO needs AI for targeting with this type

            if (sa.usesTargeting()) {
                sa.resetTargets();
                final TargetRestrictions abTgt = sa.getTargetRestrictions();
                if (abTgt.canTgtPlayer()) {
                    // try to kill opponent with Poison
                    PlayerCollection oppList = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
                    PlayerCollection poisonList = oppList.filter(PlayerPredicates.hasCounter(CounterEnumType.POISON, 9));
                    if (!poisonList.isEmpty()) {
                        sa.getTargets().add(poisonList.max(PlayerPredicates.compareByLife()));
                        return true;
                    }
                }

                if (abTgt.canTgtCreature()) {
                    // try to kill creature with -1/-1 counters if it can
                    // receive counters, except it has undying
                    CardCollection oppCreat = CardLists.getTargetableCards(ai.getOpponents().getCreaturesInPlay(), sa);
                    CardCollection oppCreatM1 = CardLists.filter(oppCreat, CardPredicates.hasCounter(CounterEnumType.M1M1));
                    oppCreatM1 = CardLists.getNotKeyword(oppCreatM1, Keyword.UNDYING);

                    oppCreatM1 = CardLists.filter(oppCreatM1, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card input) {
                            return input.getNetToughness() <= 1 && input.canReceiveCounters(CounterType.get(CounterEnumType.M1M1));
                        }

                    });

                    Card best = ComputerUtilCard.getBestAI(oppCreatM1);
                    if (best != null) {
                        sa.getTargets().add(best);
                        return true;
                    }

                    CardCollection aiCreat = CardLists.getTargetableCards(ai.getCreaturesInPlay(), sa);
                    aiCreat = CardLists.filter(aiCreat, CardPredicates.hasCounters());

                    aiCreat = CardLists.filter(aiCreat, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card input) {
                            for (CounterType counterType : input.getCounters().keySet()) {
                                if (!ComputerUtil.isNegativeCounter(counterType, input)
                                        && input.canReceiveCounters(counterType)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });

                    // TODO check whole state which creature would be the best
                    best = ComputerUtilCard.getBestAI(aiCreat);
                    if (best != null) {
                        sa.getTargets().add(best);
                        return true;
                    }
                }

                if (sa.canTarget(ai)) {
                    // don't target itself when its forced to add poison counters too
                    if (!ai.getCounters().isEmpty()) {
                        if (!eachExisting || ai.getPoisonCounters() < 5) {
                            sa.getTargets().add(ai);
                            return true;
                        }
                    }
                }

            }

            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("Never".equals(logic)) {
            return false;
        } else if ("AlwaysWithNoTgt".equals(logic)) {
            return true;
        } else if ("AristocratCounters".equals(logic)) {
            return SpecialAiLogic.doAristocratWithCountersLogic(ai, sa);
        } else if ("PayEnergy".equals(logic)) {
            return true;
        } else if ("PayEnergyConservatively".equals(logic)) {
            boolean onlyInCombat = ai.getController().isAI()
                    && ((PlayerControllerAi) ai.getController()).getAi().getBooleanProperty(AiProps.CONSERVATIVE_ENERGY_PAYMENT_ONLY_IN_COMBAT);
            boolean onlyDefensive = ai.getController().isAI()
                    && ((PlayerControllerAi) ai.getController()).getAi().getBooleanProperty(AiProps.CONSERVATIVE_ENERGY_PAYMENT_ONLY_DEFENSIVELY);

            if (playAggro) {
                // aggro profiles ignore conservative play for this AI logic
                return true;
            } else if (ai.getGame().getCombat() != null && source != null) {
                if (ai.getGame().getCombat().isAttacking(source) && !onlyDefensive) {
                    return true;
                } else if (ai.getGame().getCombat().isBlocking(source)) {
                    // when blocking, consider this if it's possible to save the blocker and/or kill at least one attacker
                    CardCollection blocked = ai.getGame().getCombat().getAttackersBlockedBy(source);
                    int totBlkPower = Aggregates.sum(blocked, CardPredicates.Accessors.fnGetNetPower);
                    int totBlkToughness = Aggregates.min(blocked, CardPredicates.Accessors.fnGetNetToughness);

                    int numActivations = ai.getCounters(CounterEnumType.ENERGY) / sa.getPayCosts().getCostEnergy().convertAmount();
                    if (source.getNetToughness() + numActivations > totBlkPower
                            || source.getNetPower() + numActivations >= totBlkToughness) {
                        return true;
                    }
                }
            } else if (sa.getSubAbility() != null
                        && "Self".equals(sa.getSubAbility().getParam("Defined"))
                        && sa.getSubAbility().getParamOrDefault("KW", "").contains("Hexproof")
                        && !AiCardMemory.isRememberedCard(ai, source, AiCardMemory.MemorySet.ANIMATED_THIS_TURN)) {
                    // Bristling Hydra: save from death using a ping activation
                    if (ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(source)) {
                        AiCardMemory.rememberCard(ai, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
                        return true;
                    }
            } else if (ai.getCounters(CounterEnumType.ENERGY) > ComputerUtilCard.getMaxSAEnergyCostOnBattlefield(ai) + sa.getPayCosts().getCostEnergy().convertAmount()) {
                // outside of combat, this logic only works if the relevant AI profile option is enabled
                // and if there is enough energy saved
                if (!onlyInCombat) {
                    return true;
                }
            }
        } else if (logic.equals("MarkOppCreature")) {
            if (!ph.is(PhaseType.END_OF_TURN)) {
                return false;
            }

            CardCollection oppCreats = CardLists.filter(ai.getOpponents().getCreaturesInPlay(),
                    Predicates.and(Predicates.not(CardPredicates.hasCounter(CounterType.getType(type))),
                            CardPredicates.isTargetableBy(sa)));

            if (!oppCreats.isEmpty()) {
                Card bestCreat = ComputerUtilCard.getBestCreatureAI(oppCreats);
                sa.resetTargets();
                sa.getTargets().add(bestCreat);
                return true;
            }
        } else if (logic.equals("CheckDFC")) {
            // for cards like Ludevic's Test Subject
            if (!source.canTransform(null)) {
                return false;
            }
        } else if (logic.startsWith("MoveCounter")) {
            return doMoveCounterLogic(ai, sa, ph);
        } else if (logic.equals("CrawlingBarrens")) {
            boolean willActivate = SpecialCardAi.CrawlingBarrens.consider(ai, sa);
            if (willActivate && ph.getPhase().isBefore(PhaseType.MAIN2)) {
                // don't use this for mana until after combat
                AiCardMemory.rememberCard(ai, source, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
            }
            return willActivate;
        }

        if (!sa.metConditions() && sa.getSubAbility() == null) {
            return false;
        }

        if (sourceName.equals("Feat of Resistance")) { // sub-ability should take precedence
            CardCollection prot = ProtectAi.getProtectCreatures(ai, sa.getSubAbility());
            if (!prot.isEmpty()) {
                sa.getTargets().add(prot.get(0));
                return true;
            }
        }

        if (sa.hasParam("Bolster")) {
            CardCollection creatsYouCtrl = ai.getCreaturesInPlay();
            CardCollection leastToughness = new CardCollection(Aggregates.listWithMin(creatsYouCtrl, CardPredicates.Accessors.fnGetDefense));
            if (leastToughness.isEmpty()) {
                return false;
            }
            // TODO If Creature that would be Bolstered for some reason is useless, also return False
        }

        if (sa.hasParam("Monstrosity") && source.isMonstrous()) {
            return false;
        }

        // TODO handle proper calculation of X values based on Cost
        int amount = AbilityUtils.calculateAmount(source, amountStr, sa);

        if (amount == 0 && logic.equals("FromDiceRoll")) {
            amount = 1; // TODO: improve this to possibly account for some variability depending on the roll outcome (e.g. 4 for 1d8, perhaps)
        }

        if (sa.hasParam("Adapt")) {
            Game game = ai.getGame();
            Combat combat = game.getCombat();

            if (!source.canReceiveCounters(CounterType.get(CounterEnumType.P1P1)) || source.getCounters(CounterEnumType.P1P1) > 0) {
                return false;
            } else if (combat != null && ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return doCombatAdaptLogic(source, amount, combat);
            }
        }

        if ("Fight".equals(logic)) {
            int nPump = 0;
            if (type.equals("P1P1")) {
                nPump = amount;
            }
            return FightAi.canFightAi(ai, sa, nPump, nPump);
        }

        if (amountStr.equals("X")) {
            if (sa.getSVar(amountStr).equals("Count$xPaid")) {
                // By default, set PayX here to maximum value (used for most SAs of this type).
                amount = ComputerUtilCost.getMaxXValue(sa, ai);

                if (isClockwork) {
                    // Clockwork Avian and other similar cards: do not tap all mana for X,
                    // instead only rewind to max allowed value when the power gets low enough.
                    int curCtrs = source.getCounters(CounterEnumType.P1P0);
                    int maxCtrs = Integer.parseInt(sa.getParam("MaxFromEffect"));

                    // This will "rewind" clockwork cards when they fall to 50% power or below, consider improving
                    if (curCtrs > Math.ceil(maxCtrs / 2.0)) {
                        return false;
                    }

                    amount = Math.min(amount, maxCtrs - curCtrs);
                    if (amount <= 0) {
                        return false;
                    }
                }

                sa.setXManaCostPaid(amount);
            } else if ("ExiledCreatureFromGraveCMC".equals(logic)) {
                // e.g. Necropolis
                amount = Aggregates.max(CardLists.filter(ai.getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES), CardPredicates.Accessors.fnGetCmc);
                if (amount > 0 && ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)) {
                    return true;
                }
            }
        }

        // don't use it if no counters to add
        if (amount <= 0) {
            return false;
        }

        if ("Polukranos".equals(logic)) {
            boolean found = false;
            for (Trigger tr : source.getTriggers()) {
                if (!tr.getMode().equals(TriggerType.BecomeMonstrous)) {
                    continue;
                }
                SpellAbility oa = tr.ensureAbility();
                if (oa == null) {
                    continue;
                }

                // need to set Activating player
                oa.setActivatingPlayer(ai);
                CardCollection targets = CardLists.getTargetableCards(ai.getOpponents().getCreaturesInPlay(), oa);

                if (!targets.isEmpty()) {
                    boolean canSurvive = false;
                    for (Card humanCreature : targets) {
                        if (!FightAi.canKill(humanCreature, source, 0)) {
                            canSurvive = true;
                            break;
                        }
                    }
                    if (!canSurvive) {
                        return false;
                    }
                }
                found = true;
                break;
            }
            if (!found) {
                return false;
            }
        }

        if ("AlwaysAtOppEOT".equals(logic)) {
            if (ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai)) {
                return true;
            }
        }

        if (!ai.getGame().getStack().isEmpty() && !SpellAbilityAi.isSorcerySpeed(sa)) {
            // only evaluates case where all tokens are placed on a single target
            if (sa.usesTargeting() && sa.getMinTargets() < 2) {
                if (ComputerUtilCard.canPumpAgainstRemoval(ai, sa)) {
                    Card c = sa.getTargets().getFirstTargetedCard();
                    if (sa.getTargets().size() > 1) {
                        sa.resetTargets();
                        sa.getTargets().add(c);
                    }
                    sa.addDividedAllocation(sa.getTargetCard(), amount);
                    return true;
                } else {
                    return false;
                }
            }
        }

        // Targeting
        if (sa.usesTargeting()) {
            sa.resetTargets();

            final boolean sacSelf = ComputerUtilCost.isSacrificeSelfCost(abCost);

            if (sa.isCurse()) {
                list = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            } else {
                list = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
            }

            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    // don't put the counter on the dead creature
                    if (sacSelf && c.equals(source)) {
                        return false;
                    }
                    return sa.canTarget(c) && c.canReceiveCounters(CounterType.getType(type));
                }
            });

            // Filter AI-specific targets if provided
            list = ComputerUtil.filterAITgts(sa, ai, list, false);

            if (abCost.hasSpecificCostType(CostSacrifice.class)) {
                Card sacTarget = ComputerUtil.getCardPreference(ai, source, "SacCost", list);
                // this card is planned to be sacrificed during cost payment, so don't target it
                // (otherwise the AI can cheat by activating this SA and not paying the sac cost, e.g. Extruder)
                // TODO needs update if amount > 1 gets printed, 
                // maybe also check putting the counter on that exact creature is more important than sacrificing it (though unlikely?)
                list.remove(sacTarget);
            }

            if (list.size() < sa.getTargetRestrictions().getMinTargets(source, sa)) {
                return false;
            }

            // Activate +Loyalty planeswalker abilities even if they have no target (e.g. Vivien of the Arkbow),
            // but try to do it in Main 2 then so that the AI has a chance to play creatures first.
            if (list.isEmpty()
                    && sa.isPwAbility()
                    && sa.getPayCosts().hasOnlySpecificCostType(CostPutCounter.class)
                    && sa.isTargetNumberValid()
                    && sa.getTargets().size() == 0
                    && ai.getGame().getPhaseHandler().is(PhaseType.MAIN2, ai)) {
                return true;
            }

            if (sourceName.equals("Abzan Charm")) {
                // specific AI for instant with distribute two +1/+1 counters
                ComputerUtilCard.sortByEvaluateCreature(list);
                // maximise the number of targets
                for (int i = 1; i < amount + 1; i++) {
                    int left = amount;
                    for (Card c : list) {
                        if (ComputerUtilCard.shouldPumpCard(ai, sa, c, i, i,
                                Lists.newArrayList())) {
                            sa.getTargets().add(c);
                            sa.addDividedAllocation(c, i);
                            left -= i;
                        }
                        if (left < i || sa.getTargets().size() == sa.getMaxTargets()) {
                            sa.addDividedAllocation(sa.getTargets().getFirstTargetedCard(), left + i);
                            left = 0;
                            break;
                        }
                    }
                    if (left == 0) {
                        return true;
                    }
                    sa.resetTargets();
                }
                return false;
            }

            // target loop
            while (sa.canAddMoreTarget()) {
                if (list.isEmpty()) {
                    if (!sa.isTargetNumberValid() || (sa.getTargets().size() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = chooseCursedTarget(list, type, amount, ai);
                } else {
                    if (type.equals("P1P1") && !SpellAbilityAi.isSorcerySpeed(sa)) {
                        for (Card c : list) {
                            if (ComputerUtilCard.shouldPumpCard(ai, sa, c, amount, amount,
                                    Lists.newArrayList())) {
                                choice = c;
                                break;
                            }
                        }
                        if (!source.isSpell()) {    // does not cost a card
                            if (choice == null) {   // find generic target
                                if (abCost == null
                                        || (ph.is(PhaseType.END_OF_TURN) && ph.getPlayerTurn().isOpponentOf(ai))) {
                                    // only use at opponent EOT unless it is free
                                    choice = chooseBoonTarget(list, type);
                                }
                            }
                        }
                        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Dromoka's Command")) {
                            choice = chooseBoonTarget(list, type);
                        }
                    } else {
                        choice = chooseBoonTarget(list, type);
                    }
                }

                if (choice == null) { // can't find anything left
                    if (!sa.isTargetNumberValid() || sa.getTargets().size() == 0) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                list.remove(choice);
                sa.getTargets().add(choice);
                if (divided) {
                    sa.addDividedAllocation(choice, amount);
                    break;
                }
                choice = null;
            }
            if (sa.getTargets().isEmpty()) {
                return false;
            }
        } else {
            final List<Card> cards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
            // Don't activate Curse abilities on my cards and non-curse abilities
            // on my opponents
            if (cards.isEmpty() || (cards.get(0).getController().isOpponentOf(ai) && !sa.isCurse())) {
                return false;
            }

            final int currCounters = cards.get(0).getCounters(CounterType.get(type));
            // each non +1/+1 counter on the card is a 10% chance of not
            // activating this ability.

            if (!(type.equals("P1P1") || type.equals("M1M1") || type.equals("ICE")) && (MyRandom.getRandom().nextFloat() < (.1 * currCounters))) {
                return false;
            }
            // Instant +1/+1
            if (type.equals("P1P1") && !SpellAbilityAi.isSorcerySpeed(sa)) {
                if (!(ph.getNextTurn() == ai && ph.is(PhaseType.END_OF_TURN) && abCost.isReusuableResource())) {
                    return false; // only if next turn and cost is reusable
                }
            }
        }

        boolean immediately = ComputerUtil.playImmediately(ai, sa);

        if (abCost != null && !ComputerUtilCost.checkSacrificeCost(ai, abCost, source, sa, immediately)) {
            return false;
        }

        if (immediately) {
            return true;
        }

        if (!type.equals("P1P1") && !type.equals("M1M1") && !sa.hasParam("ActivationPhases")) {
            // Don't use non P1P1/M1M1 counters before main 2 if possible
            if (ph.getPhase().isBefore(PhaseType.MAIN2) && !ComputerUtil.castSpellInMain1(ai, sa)) {
                return false;
            }
            if (ph.isPlayerTurn(ai) && !isSorcerySpeed(sa)) {
                return false;
            }
        }

        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(final SpellAbility sa, Player ai) {
        boolean chance = true;
        final Game game = ai.getGame();
        Card choice = null;
        final String type = sa.getParam("CounterType");
        final String logic = sa.getParamOrDefault("AILogic", "");

        final String amountStr = sa.getParamOrDefault("CounterNum", "1");
        final boolean divided = sa.isDividedAsYouChoose();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);

        final boolean isMandatoryTrigger = (sa.isTrigger() && !sa.isOptionalTrigger())
                || (sa.getRootAbility().isTrigger() && !sa.getRootAbility().isOptionalTrigger());

        if (sa.usesTargeting()) {
            CardCollection list = null;

            if (sa.isCurse()) {
                list = CardLists.filterControlledBy(game.getCardsIn(ZoneType.Battlefield), ai.getOpponents());
            } else {
                list = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
            }

            list = CardLists.getTargetableCards(list, sa);

            if (list.isEmpty() && isMandatoryTrigger) {
                // broaden the scope of possible targets if we are resolving a mandatory trigger
                list = CardLists.getTargetableCards(game.getCardsIn(ZoneType.Battlefield), sa);
            }

            sa.resetTargets();
            // target loop
            while (sa.canAddMoreTarget()) {
                if (list.isEmpty()) {
                    if (!sa.isTargetNumberValid()
                            || sa.getTargets().size() == 0) {
                        sa.resetTargets();
                        return false;
                    } else {
                        break;
                    }
                }

                if (sa.isCurse()) {
                    choice = chooseCursedTarget(list, type, amount, ai);
                } else {
                    CardCollection lands = CardLists.filter(list, CardPredicates.Presets.LANDS);
                    SpellAbility animate = sa.findSubAbilityByType(ApiType.Animate);
                    if (!lands.isEmpty() && animate != null) {
                        choice = ComputerUtilCard.getWorstLand(lands);
                    } else if ("BoonCounterOnOppCreature".equals(logic)) {
                        choice = ComputerUtilCard.getWorstCreatureAI(list);
                    } else {
                        choice = chooseBoonTarget(list, type);
                    }
                }

                if (choice == null) { // can't find anything left
                    if ((!sa.isTargetNumberValid()) || (sa.getTargets().size() == 0)) {
                        sa.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                sa.getTargets().add(choice);
                if (divided) {
                    sa.addDividedAllocation(choice, amount);
                    break;
                }
            }
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final SpellAbility root = sa.getRootAbility();
        final Card source = sa.getHostCard();
        // boolean chance = true;
        boolean preferred = true;
        CardCollection list;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParamOrDefault("CounterNum", "1");
        final boolean divided = sa.isDividedAsYouChoose();
        final int amount = AbilityUtils.calculateAmount(source, amountStr, sa);
        int left = amount;

        if (!sa.usesTargeting()) {
            // No target. So must be defined
            list = new CardCollection(AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa));

            if (amountStr.equals("X")
                    && root.getXManaCostPaid() != null /* SubAbility on something that already had set PayX, e.g. Endless One ETB counters */
                    && sa.hasParam(amountStr) && sa.getSVar(amountStr).equals("Count$xPaid")) {

                // detect if there's more than one X in the cost (Hangarback Walker, Walking Ballista, etc.)
                SpellAbility testSa = sa;
                int countX = 0;
                int nonXGlyphs = 0;
                while (testSa != null && countX == 0) {
                    countX = testSa.getPayCosts().getTotalMana().countX();
                    nonXGlyphs = testSa.getPayCosts().getTotalMana().getGlyphCount() - countX;
                    testSa = testSa.getSubAbility();
                }
                if (countX == 0) {
                    // try determining it from the card itself if neither SA has "X" in the cost
                    countX = source.getState(CardStateName.Original).getManaCost().countX();
                    nonXGlyphs = source.getState(CardStateName.Original).getManaCost().getGlyphCount() - countX;
                }

                // Spend all remaining mana to add X counters (eg. Hero of Leina Tower)
                int payX = ComputerUtilCost.getMaxXValue(sa, ai);

                // Account for the possible presence of additional glyphs in cost (e.g. Mikaeus, the Lunarch; Primordial Hydra)
                payX -= nonXGlyphs;

                // Account for the multiple X in cost
                if (countX > 1) { payX /= countX; }

                root.setXManaCostPaid(payX);
            }

            if (!mandatory) {
                // TODO - If Trigger isn't mandatory, when wouldn't we want to
                // put a counter?
                // things like Powder Keg, which are way too complex for the AI
            }
        } else if (sa.getTargetRestrictions().canOnlyTgtOpponent() && !sa.getTargetRestrictions().canTgtCreature()) {
            // can only target opponent
            List<Player> playerList = Lists.newArrayList(Iterables.filter(
                    sa.getTargetRestrictions().getAllCandidates(sa, true, true), Player.class));

            if (playerList.isEmpty()) {
                return false;
            }

            // try to choose player with less creatures
            Player choice = Collections.min(playerList, PlayerPredicates.compareByZoneSize(ZoneType.Battlefield, CardPredicates.Presets.CREATURES));

            if (choice != null) {
                sa.getTargets().add(choice);
            }
        } else {
            if (sa.isCurse()) {
                list = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
            } else {
                list = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
            }
            list = CardLists.getTargetableCards(list, sa);

            // Filter AI-specific targets if provided
            list = ComputerUtil.filterAITgts(sa, ai, list, false);

            int totalTargets = list.size();

            sa.resetTargets();
            while (sa.canAddMoreTarget()) {
                if (mandatory) {
                    // When things are mandatory, gotta handle a little differently
                    if ((list.isEmpty() || !preferred) && sa.isTargetNumberValid()) {
                        return true;
                    }

                    if (list.isEmpty() && preferred) {
                        // If it's required to choose targets and the list is empty, get a new list
                        list = CardLists.getTargetableCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), sa);
                        preferred = false;
                    }

                    if (list.isEmpty()) {
                        // Still an empty list, but we have to choose something (mandatory); expand targeting to
                        // include AI's own cards to see if there's anything targetable (e.g. Plague Belcher).
                        list = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
                        preferred = false;
                    }
                }

                if (list.isEmpty()) {
                    // Not mandatory, or the the list was regenerated and is still empty,
                    // so return whether or not we found enough targets
                    return sa.isTargetNumberValid();
                }

                Card choice = null;

                // Choose targets here:
                if (sa.isCurse()) {
                    if (preferred) {
                        choice = chooseCursedTarget(list, type, amount, ai);
                        if (choice == null && mandatory) {
                            choice = Aggregates.random(list);
                        }
                    } else {
                        if (type.equals("M1M1")) {
                            choice = ComputerUtilCard.getWorstCreatureAI(list);
                        } else {
                            choice = Aggregates.random(list);
                        }
                    }
                } else {
                    if (preferred) {
                        list = ComputerUtil.getSafeTargets(ai, sa, list);
                        choice = chooseBoonTarget(list, type);
                        if (choice == null && mandatory) {
                            choice = Aggregates.random(list);
                        }
                    } else {
                        if (type.equals("P1P1")) {
                            choice = ComputerUtilCard.getWorstCreatureAI(list);
                        } else {
                            choice = Aggregates.random(list);
                        }
                    }
                }
                if (choice != null && divided) {
                    int alloc = Math.max(amount / totalTargets, 1);
                    if (sa.getTargets().size() == Math.min(totalTargets, sa.getMaxTargets()) - 1) {
                        sa.addDividedAllocation(choice, left);
                    } else {
                        sa.addDividedAllocation(choice, alloc);
                        left -= alloc;
                    }
                }
                if (choice != null) {
                    sa.getTargets().add(choice);
                    list.remove(choice);
                } else {
                    // Didn't want to choose anything?
                    list.clear();
                }

            }
        }
        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        final Card source = sa.getHostCard();
        if (mode == PlayerActionConfirmMode.Tribute) {
            // add counter if that opponent has a giant creature
            final List<Card> creats = player.getCreaturesInPlay();
            final String amountStr = sa.getParamOrDefault("CounterNum", "1");
            final int tributeAmount = AbilityUtils.calculateAmount(source, amountStr, sa);

            final boolean isHaste = source.hasKeyword(Keyword.HASTE);
            List<Card> threatening = CardLists.filter(creats, new Predicate<Card>() {
                @Override
                public boolean apply(Card c) {
                    return CombatUtil.canBlock(source, c, !isHaste)
                            && (c.getNetToughness() > source.getNetPower() + tributeAmount || c.hasKeyword(Keyword.DEATHTOUCH));
                }
            });
            if (!threatening.isEmpty()) {
                return true;
            }
            if (source.hasSVar("TributeAILogic")) {
                final String logic = source.getSVar("TributeAILogic");
                if (logic.equals("Always")) {
                    return true;
                } else if (logic.equals("Never")) {
                    return false;
                } else if (logic.equals("CanBlockThisTurn")) {
                    // pump haste
                    List<Card> canBlock = CardLists.filter(creats, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card c) {
                            return CombatUtil.canBlock(source, c) && (c.getNetToughness() > source.getNetPower() || c.hasKeyword(Keyword.DEATHTOUCH));
                        }
                    });
                    if (!canBlock.isEmpty()) {
                        return false;
                    }
                } else if (logic.equals("DontControlCreatures")) {
                    return !creats.isEmpty();
                } else if (logic.equals("OppHasCardsInHand")) {
                    return sa.getActivatingPlayer().getCardsIn(ZoneType.Hand).isEmpty();
                }
            }
        }
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> options, Map<String, Object> params) {
        // used by Tribute, select player with lowest Life
        // TODO add more logic using TributeAILogic
        List<Player> list = Lists.newArrayList(options);
        return Collections.min(list, PlayerPredicates.compareByLife());
    }

    @Override
    protected Card chooseSingleCard(final Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        // Bolster does use this
        // TODO need more or less logic there?
        final CounterType m1m1 = CounterType.get(CounterEnumType.M1M1);
        final CounterType p1p1 = CounterType.get(CounterEnumType.P1P1);

        // no logic if there is no options or no to choice
        if (!isOptional && Iterables.size(options) <= 1) {
            return Iterables.getFirst(options, null);
        }

        final CounterType type = params.containsKey("CounterType") ? (CounterType)params.get("CounterType")
                : CounterType.getType(sa.getParam("CounterType"));

        final String amountStr = sa.getParamOrDefault("CounterNum", "1");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);

        final boolean isCurse = sa.isCurse();

        if (isCurse) {
            final CardCollection opponents = CardLists.filterControlledBy(options, ai.getOpponents());

            if (!opponents.isEmpty()) {
                final CardCollection negative = CardLists.filter(opponents, new Predicate<Card>() {
                    @Override
                    public boolean apply(Card input) {
                        if (input.hasSVar("EndOfTurnLeavePlay"))
                            return false;
                        if (ComputerUtilCard.isUselessCreature(ai, input))
                            return false;
                        if (type.is(CounterEnumType.M1M1) && amount >= input.getNetToughness())
                            return true;
                        return ComputerUtil.isNegativeCounter(type, input);
                    }
                });
                if (!negative.isEmpty()) {
                    return ComputerUtilCard.getBestAI(negative);
                }
            }
        }

        final CardCollection mine = CardLists.filterControlledBy(options, ai);
        // none of mine?
        if (mine.isEmpty()) {
            // Try to Benefit Ally if possible
            final CardCollection ally = CardLists.filterControlledBy(options, ai.getAllies());
            if (ally.isEmpty()) {
                return ComputerUtilCard.getBestAI(ally);
            }
            return isOptional ? null : ComputerUtilCard.getWorstAI(options);
        }

        CardCollection filtered = mine;

        // Try to filter out keywords that we already have on cards
        if (type.isKeywordCounter()) {
            Keyword kw = Keyword.smartValueOf(type.getName());
            final CardCollection doNotHaveKeyword = CardLists.filter(filtered, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    return !card.hasKeyword(kw) && sa.canTarget(card);
                }
            });

            if (doNotHaveKeyword.size() > 0)
                filtered = doNotHaveKeyword;
        }

        final CardCollection notUseless = CardLists.filter(filtered, new Predicate<Card>() {
            @Override
            public boolean apply(Card input) {
                if (input.hasSVar("EndOfTurnLeavePlay"))
                    return false;
                return !ComputerUtilCard.isUselessCreature(ai, input);
            }
        });

        if (!notUseless.isEmpty()) {
            filtered = notUseless;
        }

        // some special logic to reload Persist/Undying
        if (p1p1.equals(type)) {
            final CardCollection persist = CardLists.filter(filtered, new Predicate<Card>() {
                @Override
                public boolean apply(Card input) {
                    if (!input.hasKeyword(Keyword.PERSIST))
                        return false;
                    return input.getCounters(m1m1) <= amount;
                }
            });

            if (!persist.isEmpty()) {
                filtered = persist;
            }
        } else if (m1m1.equals(type)) {
            final CardCollection undying = CardLists.filter(filtered, new Predicate<Card>() {
                @Override
                public boolean apply(Card input) {
                    if (!input.hasKeyword(Keyword.UNDYING))
                        return false;
                    return input.getCounters(p1p1) <= amount && input.getNetToughness() > amount;
                }
            });

            if (!undying.isEmpty()) {
                filtered = undying;
            }
        }

        return ComputerUtilCard.getBestAI(filtered);
    }

    /**
     * used for ExistingCounterType
     */
    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, Map<String, Object> params) {
        Player ai = sa.getActivatingPlayer();
        GameEntity e = (GameEntity) params.get("Target");
        // for Card try to select not useless counter
        if (e instanceof Card) {
            Card c = (Card) e;
            if (c.getController().isOpponentOf(ai)) {
                if (options.contains(CounterType.get(CounterEnumType.M1M1)) && !c.hasKeyword(Keyword.UNDYING)) {
                    return CounterType.get(CounterEnumType.M1M1);
                }
                for (CounterType type : options) {
                    if (ComputerUtil.isNegativeCounter(type, c)) {
                        return type;
                    }
                }
            } else {
                for (CounterType type : options) {
                    if (!ComputerUtil.isNegativeCounter(type, c) && !ComputerUtil.isUselessCounter(type, c)) {
                        return type;
                    }
                }
            }
        } else if (e instanceof Player) {
            Player p = (Player) e;
            if (p.isOpponentOf(ai)) {
                if (options.contains(CounterType.get(CounterEnumType.POISON))) {
                    return CounterType.get(CounterEnumType.POISON);
                }
            } else {
                if (options.contains(CounterType.get(CounterEnumType.EXPERIENCE))) {
                    return CounterType.get(CounterEnumType.EXPERIENCE);
                }
            }

        }
        return Iterables.getFirst(options, null);
    }

    private boolean doMoveCounterLogic(final Player ai, SpellAbility sa, PhaseHandler ph) {
        // Spikes (Tempest)

        // Try not to do it unless at the end of opponent's turn or the creature is threatened
        final int creatDiff = sa.getParam("AILogic").contains("IsCounterUser") ? 450 : 1;
        final Combat combat = ai.getGame().getCombat();
        final Card source = sa.getHostCard();

        final boolean threatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(source)
                || (combat != null && (((combat.isBlocked(source) && ComputerUtilCombat.attackerWouldBeDestroyed(ai, source, combat)) && !ComputerUtilCombat.willKillAtLeastOne(ai, source, combat))
                || (combat.isBlocking(source) && ComputerUtilCombat.blockerWouldBeDestroyed(ai, source, combat) && !ComputerUtilCombat.willKillAtLeastOne(ai, source, combat))));

        if (!(threatened || (ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == ai))) {
            return false;
        }

        CardCollection targets = CardLists.getTargetableCards(ai.getCreaturesInPlay(), sa);
        targets.remove(source);

        targets = CardLists.filter(targets, new Predicate<Card>() {
            @Override
            public boolean apply(Card card) {
                boolean tgtThreatened = ComputerUtil.predictThreatenedObjects(ai, null, true).contains(card)
                        || (combat != null && ((combat.isBlocked(card) && ComputerUtilCombat.attackerWouldBeDestroyed(ai, card, combat))
                        || (combat.isBlocking(card) && ComputerUtilCombat.blockerWouldBeDestroyed(ai, card, combat))));
                // when threatened, any non-threatened target is good to preserve the counter
                return !tgtThreatened && (threatened || ComputerUtilCard.evaluateCreature(card, false, false) > ComputerUtilCard.evaluateCreature(source, false, false) + creatDiff);
            }
        });

        Card bestTgt = ComputerUtilCard.getBestCreatureAI(targets);

        if (bestTgt != null) {
            sa.getTargets().add(bestTgt);
            return true;
        }

        return false;
    }

    private boolean doCombatAdaptLogic(Card source, int amount, Combat combat) {
        if (combat.isAttacking(source)) {
            if (!combat.isBlocked(source)) {
                return true;
            } else {
                for (Card blockedBy : combat.getBlockers(source)) {
                    if (blockedBy.getNetToughness() > source.getNetPower()
                            && blockedBy.getNetToughness() <= source.getNetPower() + amount) {
                        return true;
                    }
                }

                int totBlkPower = Aggregates.sum(combat.getBlockers(source), CardPredicates.Accessors.fnGetNetPower);
                if (source.getNetToughness() <= totBlkPower
                        && source.getNetToughness() + amount > totBlkPower) {
                    return true;
                }
            }
        } else if (combat.isBlocking(source)) {
            for (Card blocked : combat.getAttackersBlockedBy(source)) {
                if (blocked.getNetToughness() > source.getNetPower()
                        && blocked.getNetToughness() <= source.getNetPower() + amount) {
                    return true;
                }
            }

            int totAtkPower = Aggregates.sum(combat.getAttackersBlockedBy(source), CardPredicates.Accessors.fnGetNetPower);
            if (source.getNetToughness() <= totAtkPower
                    && source.getNetToughness() + amount > totAtkPower) {
                return true;
            }
        }
        return false;
    }

}
