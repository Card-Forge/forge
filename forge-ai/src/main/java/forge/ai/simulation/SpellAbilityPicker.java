package forge.ai.simulation;

import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.ability.ExploreAi;
import forge.ai.ability.LearnAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

public class SpellAbilityPicker {
    private Game game;
    private Player player;
    private Score bestScore;
    private boolean printOutput;
    private SpellAbilityChoicesIterator interceptor;

    private Plan plan;
    private int numSimulations;

    public SpellAbilityPicker(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public void setInterceptor(SpellAbilityChoicesIterator in) {
        this.interceptor = in;
    }

    private void print(String str) {
        if (printOutput) {
            System.out.println(str);
        }
    }

    private void printPhaseInfo() {
        String phaseStr = game.getPhaseHandler().getPhase().toString();
        if (game.getPhaseHandler().getPlayerTurn() != player) {
            phaseStr = "opponent " + phaseStr;
        }
        print("---- choose ability  (phase = " + phaseStr + ")");
    }

    public List<SpellAbility> getCandidateSpellsAndAbilities() {
        CardCollection cards = ComputerUtilAbility.getAvailableCards(game, player);
        cards = ComputerUtilCard.dedupeCards(cards);
        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);
        List<SpellAbility> candidateSAs = ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player);
        int writeIndex = 0;
        for (SpellAbility sa : candidateSAs) {
            if (sa.isManaAbility()) {
                continue;
            }
            sa.setActivatingPlayer(player, true);

            AiPlayDecision opinion = canPlayAndPayForSim(sa);
            // print("  " + opinion + ": " + sa);
            // PhaseHandler ph = game.getPhaseHandler();
            // System.out.printf("Ai thinks '%s' of %s -> %s @ %s %s >>> \n", opinion, sa.getHostCard(), sa, Lang.getPossesive(ph.getPlayerTurn().getName()), ph.getPhase());

            if (opinion != AiPlayDecision.WillPlay)
                continue;
            candidateSAs.set(writeIndex, sa);
            writeIndex++;
        }
        candidateSAs.subList(writeIndex, candidateSAs.size()).clear();
        return candidateSAs;
    }

    public SpellAbility chooseSpellAbilityToPlay(SimulationController controller) {
        printOutput = controller == null;

        // Pass if top of stack is owned by me.
        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getActivatingPlayer().equals(player)) {
            return null;
        }

        Score origGameScore = new GameStateEvaluator().getScoreForGameState(game, player);
        List<SpellAbility> candidateSAs = getCandidateSpellsAndAbilities();
        if (controller != null) {
            // This is a recursion during a higher-level simulation. Just return the head of the best
            // sequence directly, no need to create a Plan object.
            return chooseSpellAbilityToPlayImpl(controller, candidateSAs, origGameScore, null);
        }

        printPhaseInfo();
        SpellAbility sa = getPlannedSpellAbility(origGameScore, candidateSAs);
        if (sa != null) {
            return sa;
        }
        createNewPlan(origGameScore, candidateSAs);
        return getPlannedSpellAbility(origGameScore, candidateSAs);
    }

    private Plan formulatePlanWithPhase(Score origGameScore, List<SpellAbility> candidateSAs, PhaseType phase) {
        SimulationController controller = new SimulationController(origGameScore);
        SpellAbility sa = chooseSpellAbilityToPlayImpl(controller, candidateSAs, origGameScore, phase);
        if (sa != null) {
            return controller.getBestPlan();
        }
        return null;
    }

    private void printPlan(Plan plan, String intro) {
        if (plan == null) {
            print(intro + ": no plan!");
        }
        print(intro +" plan with score " + plan.getFinalScore() + ":");
        int i = 0;
        for (Plan.Decision d : plan.getDecisions()) {
            print(++i + ". " + d);
        }
    }

    private static boolean isSorcerySpeed(SpellAbility sa, Player player) {
        // TODO: Can we use the actual rules engine for this instead of trying to do the logic ourselves?
        if (sa instanceof LandAbility) {
            return true;
        }
        if (sa.isSpell()) {
            return !sa.withFlash(sa.getHostCard(), player);
        }
        if (sa.isPwAbility()) {
            return !sa.withFlash(sa.getHostCard(), player);
        }
        return sa.isActivatedAbility() && sa.getRestrictions().isSorcerySpeed();
    }

    private void createNewPlan(Score origGameScore, List<SpellAbility> candidateSAs) {
        plan = null;

        Plan bestPlan = formulatePlanWithPhase(origGameScore, candidateSAs, null);
        if (bestPlan == null) {
            print("No good plan at this time");
            return;
        }

        PhaseType currentPhase = game.getPhaseHandler().getPhase();
        if (currentPhase.isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            List<SpellAbility> candidateSAs2 = new ArrayList<>();
            for (SpellAbility sa : candidateSAs) {
                if (!isSorcerySpeed(sa, player)) {
                    System.err.println("Not sorcery: " + sa);
                    candidateSAs2.add(sa);
                }
            }
            if (!candidateSAs2.isEmpty()) {
                System.err.println("Formula plan with phase bloom");
                Plan afterBlockersPlan = formulatePlanWithPhase(origGameScore, candidateSAs2, PhaseType.COMBAT_DECLARE_BLOCKERS);
                if (afterBlockersPlan != null && afterBlockersPlan.getFinalScore().value >= bestPlan.getFinalScore().value) {
                    printPlan(afterBlockersPlan, "After blockers");
                    print("Deciding to wait until after declare blockers.");
                    return;
                }
            }
        }

        printPlan(bestPlan, "Current phase (" + currentPhase + ")");
        plan = bestPlan;
    }

    private SpellAbility chooseSpellAbilityToPlayImpl(SimulationController controller, List<SpellAbility> candidateSAs, Score origGameScore, PhaseType phase) {
        long startTime = System.currentTimeMillis();

        SpellAbility bestSa = null;
        Score bestSaValue = origGameScore;
        print("Evaluating... (orig score = " + origGameScore +  ")");
        for (int i = 0; i < candidateSAs.size(); i++) {
            Score value = evaluateSa(controller, phase, candidateSAs, i);
            if (value.value > bestSaValue.value) {
                bestSaValue = value;
                bestSa = candidateSAs.get(i);
            }
        }

        // To make the AI hold-off on playing creatures in MAIN1 if they give no other benefits,
        // check the score for the bestSA while counting summon sick creatures for 0.
        // Do it here on the best SA, rather than for all evaluations, so that if the best SA
        // is indeed a creature spell, we don't pick something else to play now and then have
        // no mana to play the truly best SA post-combat.
        if (bestSa != null && bestSaValue.summonSickValue <= origGameScore.summonSickValue) {
            bestSa = null;
        }

        long execTime = System.currentTimeMillis() - startTime;
        print("BEST: " + abilityToString(bestSa) + " SCORE: " + bestSaValue.summonSickValue + " TIME: " + execTime);
        this.bestScore = bestSaValue;
        return bestSa;
    }

    public boolean hasActivePlan() {
        return plan != null && plan.hasNextDecision();
    }

    public Plan getPlan() {
        return plan;
    }

    private void printPlannedActionFailure(Plan.Decision decision, String cause) {
        print("Failed to continue planned action (" + decision.saRef + "). Cause:");
        print("  " + cause + "!");
        plan = null;
    }

    private SpellAbility getPlannedSpellAbility(Score origGameScore, List<SpellAbility> availableSAs) {
        if (!hasActivePlan()) {
            plan = null;
            return null;
        }
        PhaseType startPhase = plan.getStartPhase();
        if (startPhase != null && game.getPhaseHandler().getPhase().isBefore(startPhase)) {
            print("Waiting until phase " + startPhase + " to proceed with the plan.");
            return null;
        }
        Plan.Decision decision = plan.selectNextDecision();
        if (!decision.initialScore.equals(origGameScore)) {
            printPlannedActionFailure(decision, "Unexpected game score (" + decision.initialScore + " vs. expected " + origGameScore + ")");
            return null;
        }
        SpellAbility sa = decision.saRef.findReferencedAbility(availableSAs);
        if (sa == null) {
            printPlannedActionFailure(decision, "Couldn't find spell/ability!");
            return null;
        }
        // If modes != null, targeting will be done in chooseModeForAbility().
        if (decision.modes == null && decision.targets != null) {
            MultiTargetSelector selector = new MultiTargetSelector(sa, null);
            if (!selector.selectTargets(decision.targets)) {
                printPlannedActionFailure(decision, "Bad targets");
                return null;
            }
        }
        if (decision.xMana != null) {
            sa.setXManaCostPaid(decision.xMana);
        }
        print("Planned decision " + plan.getNextDecisionIndex() + ": " + decision);
        return sa;
    }

    public Score getScoreForChosenAbility() {
        return bestScore;
    }

    public static String abilityToString(SpellAbility sa) {
        return abilityToString(sa, true);
    }
    public static String abilityToString(SpellAbility sa, boolean withTargets) {
        StringBuilder saString = new StringBuilder("N/A");
        if (sa != null) {
            saString = new StringBuilder(sa.toString());
            String cardName = sa.getHostCard().getName();
            if (!cardName.isEmpty()) {
                saString = new StringBuilder(TextUtil.fastReplace(saString.toString(), cardName, "<$>"));
            }
            if (saString.length() > 40) {
                saString = new StringBuilder(saString.substring(0, 40) + "...");
            }
            if (withTargets) {
                SpellAbility saOrSubSa = sa;
                do {
                    if (saOrSubSa.usesTargeting()) {
                        saString.append(" (targets: ").append(saOrSubSa.getTargets()).append(")");
                    }
                    saOrSubSa = saOrSubSa.getSubAbility();
                } while (saOrSubSa != null);
            }
            saString.insert(0, sa.getHostCard() + " -> ");
        }
        return saString.toString();
    }

    private boolean shouldWaitForLater(final SpellAbility sa) {
        final PhaseType phase = game.getPhaseHandler().getPhase();
        final boolean isEarlyPhase = phase == PhaseType.UNTAP || phase == PhaseType.UPKEEP || phase == PhaseType.DRAW;

        // Until the AI can be made smarter, hold off playing instants until MAIN1,
        // so that they can be compared to sorcery-speed spells. Else, the AI is too
        // eager to play them.
        if (isEarlyPhase) {
            // Only hold off if this spell can actually be played in MAIN1.
            final SpellAbilityCondition conditions = sa.getConditions();
            if (conditions == null) {
                return true;
            }
            Set<PhaseType> phases = conditions.getPhases();
            return phases.isEmpty() || phases.contains(PhaseType.MAIN1);
        }

        return false;
    }

    private boolean atLeastOneConditionMet(SpellAbility saOrSubSa) {
        do {
            SpellAbilityCondition conditions = saOrSubSa.getConditions();
            if (conditions == null || conditions.areMet(saOrSubSa)) {
                return true;
            }
            saOrSubSa = saOrSubSa.getSubAbility();
        } while (saOrSubSa != null);
        return false;
    }

    private AiPlayDecision canPlayAndPayForSim(final SpellAbility sa) {
        if (!sa.isLegalAfterStack()) {
            return AiPlayDecision.CantPlaySa;
        }
        if (!sa.checkRestrictions(sa.getHostCard(), player)) {
            return AiPlayDecision.CantPlaySa;
        }

        if (sa instanceof LandAbility) {
            return AiPlayDecision.WillPlay;
        }
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }

        // Note: Can't just check condition on the top ability, because it may have
        // sub-abilities without conditions (e.g. wild slash's main ability has a
        // main ability with conditions but the burn sub-ability has none).
        if (!atLeastOneConditionMet(sa)) {
            return AiPlayDecision.CantPlaySa;
        }

        if (!ComputerUtilCost.canPayCost(sa, player, sa.isTrigger())) {
            return AiPlayDecision.CantAfford;
        }
        if (!ComputerUtilAbility.isFullyTargetable(sa)) {
            return AiPlayDecision.TargetingFailed;
        }
        if (shouldWaitForLater(sa)) {
            return AiPlayDecision.AnotherTime;
        }

        return AiPlayDecision.WillPlay;
    }

    public Score evaluateSa(final SimulationController controller, PhaseType phase, List<SpellAbility> saList, int saIndex) {
        controller.evaluateSpellAbility(saList, saIndex);
        SpellAbility sa = saList.get(saIndex);

        // Use a deterministic random seed when evaluating different choices of a spell ability.
        // This is needed as otherwise random effects may result in a different number of choices
        // each iteration, which will break the logic in SpellAbilityChoicesIterator.
        Random origRandom = MyRandom.getRandom();
        long randomSeedToUse = origRandom.nextLong();

        Score bestScore = new Score(Integer.MIN_VALUE);
        final SpellAbilityChoicesIterator choicesIterator = new SpellAbilityChoicesIterator(controller);
        Score lastScore;
        do {
            // TODO: MyRandom should be an instance on the game object, so that we could do
            // simulations in parallel without messing up global state.
            MyRandom.setRandom(new Random(randomSeedToUse));
            GameSimulator simulator = new GameSimulator(controller, game, player, phase);
            simulator.setInterceptor(choicesIterator);
            lastScore = simulator.simulateSpellAbility(sa);
            numSimulations++;
            if (lastScore.value > bestScore.value) {
                bestScore = lastScore;
            }
        } while (choicesIterator.advance(lastScore));
        controller.doneEvaluating(bestScore);
        MyRandom.setRandom(origRandom);
        return bestScore;
    }

    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> choices, int min, int num, boolean allowRepeat) {
        if (interceptor != null) {
            return interceptor.chooseModesForAbility(choices, min, num, allowRepeat);
        }
        if (plan != null && plan.getSelectedDecision() != null && plan.getSelectedDecision().modes != null) {
            Plan.Decision decision = plan.getSelectedDecision();
            // TODO: Validate that there's no discrepancies between choices and modes?
            List<AbilitySub> plannedModes = SpellAbilityChoicesIterator.getModeCombination(choices, decision.modes);
            if (plan.getSelectedDecision().targets != null) {
                MultiTargetSelector selector = new MultiTargetSelector(sa, plannedModes);
                if (!selector.selectTargets(decision.targets)) {
                    printPlannedActionFailure(decision, "Bad targets for modes");
                    return null;
                }
            }
            return plannedModes;
        }
        return null;
    }

    private Card getPlannedChoice(CardCollection fetchList) {
        // TODO: Make the below more robust?
        if (plan != null && plan.getSelectedDecision() != null) {
            String choice = plan.getSelectedDecisionNextChoice();
            for (Card c : fetchList) {
                if (c.getName().equals(choice)) {
                    print("  Planned choice: " + c);
                    return c;
                }
            }
            print("Failed to use planned choice (" + choice + "). Not found!");
        }
        return null;
    }

    public Card chooseCardToHiddenOriginChangeZone(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
            CardCollection fetchList, Player player2, Player decider) {
        if (fetchList.size() >= 2) {
            if (interceptor != null) {
                return interceptor.chooseCard(fetchList);
            }
            Card card = getPlannedChoice(fetchList);
            if (card != null) {
                plan.advanceNextChoice();
                return card;
            }
        }
        if (sa.getApi() == ApiType.Explore) {
            return ExploreAi.shouldPutInGraveyard(fetchList, decider);
        } else if (sa.getApi() == ApiType.Learn) {
            return LearnAi.chooseCardToLearn(fetchList, decider, sa);
        } else {
            return ChangeZoneAi.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player2, decider);
        }
    }

    public CardCollectionView chooseSacrificeType(String type, SpellAbility ability, final boolean effect, int amount, final CardCollectionView exclude) {
        if (amount == 1) {
            Card source = ability.getHostCard();
            CardCollection cardList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, null);
            cardList = CardLists.filter(cardList, CardPredicates.canBeSacrificedBy(ability, effect));
            if (cardList.size() >= 2) {
                if (interceptor != null) {
                    return new CardCollection(interceptor.chooseCard(cardList));
                }
                Card card = getPlannedChoice(cardList);
                if (card != null) {
                    plan.advanceNextChoice();
                    return new CardCollection(card);
                }
            }
        }
        return ComputerUtil.chooseSacrificeType(player, type, ability, ability.getTargetCard(), effect, amount, exclude);
    }

    public int getNumSimulations() {
        return numSimulations;
    }
}
