package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.CombinatoricsUtils;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCost;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.zone.ZoneType;

public class SpellAbilityPicker {
    public static boolean SIMULATE_LAND_PLAYS = true;

    private Game game;
    private Player player;
    private Score bestScore;
    private boolean printOutput;
    private Interceptor interceptor;

    private Plan plan;

    public SpellAbilityPicker(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public void setInterceptor(Interceptor in) {
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
    
    private List<SpellAbility> getCandidateSpellsAndAbilities(List<SpellAbility> all) {
        List<SpellAbility> candidateSAs = ComputerUtilAbility.getOriginalAndAltCostAbilities(all, player);
        int writeIndex = 0;
        for (int i = 0; i < candidateSAs.size(); i++) {
            SpellAbility sa = candidateSAs.get(i);
            if (sa.isManaAbility()) {
                continue;
            }
            sa.setActivatingPlayer(player);
            
            AiPlayDecision opinion = canPlayAndPayForSim(sa);
            // print("  " + opinion + ": " + sa);
            // PhaseHandler ph = game.getPhaseHandler();
            // System.out.printf("Ai thinks '%s' of %s -> %s @ %s %s >>> \n", opinion, sa.getHostCard(), sa, Lang.getPossesive(ph.getPlayerTurn().getName()), ph.getPhase());
            
            if (opinion != AiPlayDecision.WillPlay)
                continue;
            candidateSAs.set(writeIndex,  sa);
            writeIndex++;
        }
        candidateSAs.subList(writeIndex, candidateSAs.size()).clear();
        return candidateSAs;
    }

    public SpellAbility chooseSpellAbilityToPlay(SimulationController controller) {
        printOutput = (controller == null);

        // Pass if top of stack is owned by me.
        if (!game.getStack().isEmpty() && game.getStack().peekAbility().getActivatingPlayer().equals(player)) {
            return null;
        }

        CardCollection cards = ComputerUtilAbility.getAvailableCards(game, player);
        List<SpellAbility> all = ComputerUtilAbility.getSpellAbilities(cards, player);
        if (SIMULATE_LAND_PLAYS) {
            CardCollection landsToPlay = ComputerUtilAbility.getAvailableLandsToPlay(game, player);
            if (landsToPlay != null) {
                HashMap<String, Card> landsDeDupe = new HashMap<String, Card>();
                for (Card land : landsToPlay) {
                    Card previousLand = landsDeDupe.get(land.getName());
                    // Skip identical lands.
                    if (previousLand != null && previousLand.getZone() == land.getZone() && previousLand.getOwner() == land.getOwner()) {
                        continue;
                    }
                    landsDeDupe.put(land.getName(), land);
                    all.add(new PlayLandAbility(land));
                }
            }
        }

        Score origGameScore = new GameStateEvaluator().getScoreForGameState(game, player);
        List<SpellAbility> candidateSAs = getCandidateSpellsAndAbilities(all);
        if (controller != null) {
            // This is a recursion during a higher-level simulation. Just return the head of the best
            // sequence directly, no need to create a Plan object.
            return chooseSpellAbilityToPlayImpl(controller, candidateSAs, origGameScore);
        }

        printPhaseInfo();
        SpellAbility sa = getPlannedSpellAbility(origGameScore, candidateSAs);
        if (sa != null) {
            return transformSA(sa);
        }
        createNewPlan(origGameScore, candidateSAs);
        return transformSA(getPlannedSpellAbility(origGameScore, candidateSAs));
    }

    private SpellAbility transformSA(SpellAbility sa) {
        if (sa instanceof PlayLandAbility) {
            game.PLAY_LAND_SURROGATE.setHostCard(sa.getHostCard());
            return game.PLAY_LAND_SURROGATE;
        }
        return sa;
    }

    private void createNewPlan(Score origGameScore, List<SpellAbility> candidateSAs) {
        plan = null;
        SimulationController controller = new SimulationController(origGameScore);
        SpellAbility sa = chooseSpellAbilityToPlayImpl(controller, candidateSAs, origGameScore);
        if (sa == null) {
            print("No good plan at this time");
            return;
        }

        plan = controller.getBestPlan();
        print("New plan with score " + controller.getBestScore() + ":");
        int i = 0;
        for (Plan.Decision d : plan.getDecisions()) {
            print(++i + ". " + d);
        }
    }

    private SpellAbility chooseSpellAbilityToPlayImpl(SimulationController controller, List<SpellAbility> candidateSAs, Score origGameScore) {
        long startTime = System.currentTimeMillis();

        SpellAbility bestSa = null;
        Score bestSaValue = origGameScore;
        print("Evaluating... (orig score = " + origGameScore +  ")");
        for (int i = 0; i < candidateSAs.size(); i++) {
            Score value = evaluateSa(controller, candidateSAs, i);
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
            PossibleTargetSelector selector = new PossibleTargetSelector(sa);
            if (!selector.selectTargets(decision.targets)) {
                printPlannedActionFailure(decision, "Bad targets");
                return null;
            }
        }
        print("Planned decision " + plan.getNextDecisionIndex() + ": " + decision);
        return sa;
    }
 
    public Score getScoreForChosenAbility() {
        return bestScore;
    }

    public static String abilityToString(SpellAbility sa) {
        String saString = "N/A";
        if (sa != null) {
            saString = sa.toString();
            String cardName = sa.getHostCard().getName();
            if (!cardName.isEmpty()) {
                saString = saString.replace(cardName, "<$>");
            }
            if (saString.length() > 40) {
                saString = saString.substring(0, 40) + "...";
            }
            SpellAbility saOrSubSa = sa;
            do {
                if (saOrSubSa.usesTargeting()) {
                    saString += " (targets: " + saOrSubSa.getTargets().getTargetedString() + ")";
                }
                saOrSubSa = saOrSubSa.getSubAbility();
            } while (saOrSubSa != null);
            saString = sa.getHostCard() + " -> " + saString;
        }
        return saString;
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
            List<PhaseType> phases = conditions.getPhases();
            if (phases.isEmpty() || phases.contains(PhaseType.MAIN1)) {
                return true;
            }
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
        if (sa instanceof PlayLandAbility) {
            return AiPlayDecision.WillPlay;
        }
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }

        // Note: Can'tjust check condition on the top ability, because it may have
        // sub-abilities without conditions (e.g. wild slash's main ability has a
        // main ability with conditions but the burn sub-ability has none).
        if (!atLeastOneConditionMet(sa)) {
            return AiPlayDecision.CantPlaySa;
        }

        if (!ComputerUtilCost.canPayCost(sa, player)) {
            return AiPlayDecision.CantAfford;
        }

        if (shouldWaitForLater(sa)) {
            return AiPlayDecision.AnotherTime;
        }

        return AiPlayDecision.WillPlay;
    }

    private static List<AbilitySub> getModeCombination(List<AbilitySub> choices, int[] modeIndexes) {
        ArrayList<AbilitySub> modes = new ArrayList<AbilitySub>();
        for (int modeIndex : modeIndexes) {
            modes.add(choices.get(modeIndex));
        }
        return modes;
    }

    private Score evaluateSa(final SimulationController controller, List<SpellAbility> saList, int saIndex) {
        controller.evaluateSpellAbility(saList, saIndex);
        SpellAbility sa = saList.get(saIndex);

        Score bestScore = new Score(Integer.MIN_VALUE);
        Interceptor interceptor = new Interceptor() {
            private Iterator<int[]> modeIterator;
            private int[] selectedModes;
            private Score bestScoreForMode = new Score(Integer.MIN_VALUE);
            private boolean advancedToNextMode;

            private Score[] cachedTargetScores;
            private int nextTarget = 0;
            private Score bestScoreForTarget = new Score(Integer.MIN_VALUE);

            private int numChoices = -1;
            private int nextChoice = 0;
            private Card selectedChoice;
            private Score bestScoreForChoice = new Score(Integer.MIN_VALUE);

            public List<AbilitySub> chooseModesForAbility(List<AbilitySub> choices, int min, int num, boolean allowRepeat) {
                if (modeIterator == null) {
                    // TODO: Below doesn't support allowRepeat!
                    modeIterator = CombinatoricsUtils.combinationsIterator(choices.size(), num);
                    selectedModes = modeIterator.next();
                    advancedToNextMode = true;
                }
                // Note: If modeIterator already existed, selectedModes would have been updated in advance().
                List<AbilitySub> result = getModeCombination(choices, selectedModes);
                if (advancedToNextMode) {
                    StringBuilder sb = new StringBuilder();
                    for (AbilitySub sub : result) {
                        if (sb.length() > 0) {
                            sb.append(" ");
                        } else {
                            sb.append(sub.getHostCard()).append(" -> ");
                        }
                        sb.append(sub);
                    }
                    controller.evaluateChosenModes(selectedModes, sb.toString());
                    advancedToNextMode = false;
                }
                return result;
            }

            @Override
            public Card chooseCard(CardCollection fetchList) {
                // Prune duplicates.
                HashSet<String> uniqueCards = new HashSet<String>();
                for (int i = 0; i < fetchList.size(); i++) {
                    Card card = fetchList.get(i);
                    if (uniqueCards.add(card.getName()) && uniqueCards.size() == nextChoice + 1) {
                        selectedChoice = card;
                    }
                }
                numChoices = uniqueCards.size();
                if (selectedChoice != null) {
                    controller.evaluateCardChoice(selectedChoice);
                }
                return selectedChoice;
            }

            @Override
            public void chooseTargets(SpellAbility sa, GameSimulator simulator) {
                // Note: Can't just keep a TargetSelector object cached because it's
                // responsible for setting state on a SA and the SA object changes each
                // time since it's a different simulation.
                PossibleTargetSelector selector = new PossibleTargetSelector(sa);
                if (selector.hasPossibleTargets()) {
                    if (cachedTargetScores == null) {
                        cachedTargetScores = new Score[selector.getValidTargetsSize()];
                        nextTarget = -1;
                        for (int i = 0; i < cachedTargetScores.length; i++) {
                            selector.selectTargetsByIndex(i);
                            cachedTargetScores[i] = controller.shouldSkipTarget(sa, selector.getLastSelectedTargets(), simulator);
                            if (cachedTargetScores[i] != null) {
                                controller.printState(cachedTargetScores[i], sa, " - via estimate (skipped)", false);
                            } else if (nextTarget == -1) {
                                nextTarget = i;
                            }
                        }
                        // If all targets were cached, we unfortunately have to evaluate the first target again
                        // because at this point we're already running the simulation code and there's no turning
                        // back. This used to be not possible when the PossibleTargetSelector was controlling the
                        // flow. :(
                        if (nextTarget == -1) { nextTarget = 0; }
                    }
                    selector.selectTargetsByIndex(nextTarget);
                    controller.setHostAndTarget(sa, simulator);
                    // The hierarchy is modes -> targets -> choices. In the presence of multiple choices, we want to call
                    // evaluate just once at the top level. We can do this by only calling when numChoices is -1.
                    if (numChoices == -1) {
                        controller.evaluateTargetChoices(sa, selector.getLastSelectedTargets());
                    }
                    return;
                }
            }

            @Override
            public Card getSelectedChoice() {
                return selectedChoice;
            }

            @Override
            public int[] getSelectModes() {
                return selectedModes;
            }

            @Override
            public boolean advance(Score lastScore) {
                if (lastScore.value > bestScoreForChoice.value) {
                    bestScoreForChoice = lastScore;
                }
                if (lastScore.value > bestScoreForTarget.value) {
                    bestScoreForTarget = lastScore;
                }
                if (lastScore.value > bestScoreForMode.value) {
                    bestScoreForMode = lastScore;
                }

                if (numChoices != -1) {
                    if (selectedChoice != null) {
                        controller.doneEvaluating(bestScoreForChoice);
                    }
                    bestScoreForChoice = new Score(Integer.MIN_VALUE);
                    selectedChoice = null;
                    if (nextChoice + 1 < numChoices) {
                        nextChoice++;
                        return true;
                    }
                    nextChoice = 0;
                    numChoices = -1;
                }
                if (cachedTargetScores != null) {
                    controller.doneEvaluating(bestScoreForTarget);
                    bestScoreForTarget = new Score(Integer.MIN_VALUE);
                    while (nextTarget + 1 < cachedTargetScores.length) {
                        nextTarget++;
                        if (cachedTargetScores[nextTarget] == null) {
                            return true;
                        }
                    }
                    nextTarget = -1;
                    cachedTargetScores = null;
                }
                if (modeIterator != null) {
                    controller.doneEvaluating(bestScoreForMode);
                    bestScoreForMode = new Score(Integer.MIN_VALUE);
                    if (modeIterator.hasNext()) {
                        selectedModes = modeIterator.next();
                        advancedToNextMode = true;
                        return true;
                    }
                    modeIterator = null;
                }
                return false;
            }
        };

        Score lastScore = null;
        do {
            GameSimulator simulator = new GameSimulator(controller, game, player);
            simulator.setInterceptor(interceptor);
            lastScore = simulator.simulateSpellAbility(sa);
            if (lastScore.value > bestScore.value) {
                bestScore = lastScore;
            }
        } while (interceptor.advance(lastScore));
        controller.doneEvaluating(bestScore);
        return bestScore;
    }

    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num, boolean allowRepeat) {
        if (interceptor != null) {
            List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
            return interceptor.chooseModesForAbility(choices, min, num, allowRepeat);
        }
        if (plan != null && plan.getSelectedDecision() != null && plan.getSelectedDecision().modes != null) {
            Plan.Decision decision = plan.getSelectedDecision();
            List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);
            // TODO: Validate that there's no discrepancies between choices and modes?
            List<AbilitySub> plannedModes = getModeCombination(choices, decision.modes);
            if (plan.getSelectedDecision().targets != null) {
                PossibleTargetSelector selector = new PossibleTargetSelector(sa, plannedModes);
                if (!selector.selectTargets(decision.targets)) {
                    printPlannedActionFailure(decision, "Bad targets for modes");
                    return null;
                }
            }
            return plannedModes;
        }
        return null;
    }

    public Card chooseCardToHiddenOriginChangeZone(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
            CardCollection fetchList, Player player2, Player decider) {
        if (interceptor != null) {
            return interceptor.chooseCard(fetchList);
        }
        // TODO: Make the below more robust?
        if (plan != null && plan.getSelectedDecision() != null) {
            String choice = plan.getSelectedDecision().choice;
            for (Card c : fetchList) {
                if (c.getName().equals(choice)) {
                    print("  Planned choice: " + c);
                    return c;
                }
            }
            print("Failed to use planned choice (" + choice + "). Not found!");
        }
        return ChangeZoneAi.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player2, decider);
    }
    
    public static class PlayLandAbility extends Ability {
        public PlayLandAbility(Card land) {
            super(null, (Cost) null);
            setHostCard(land);
        }
   
        @Override
        public boolean canPlay() {
            return true; //if this ability is added anywhere, it can be assumed that land can be played
        }
        @Override
        public void resolve() {
            throw new RuntimeException("This ability is intended to indicate \"land to play\" choice only");
        }
        @Override
        public String toUnsuppressedString() { return "Play land " + (getHostCard() != null ? getHostCard().getName() : ""); }
    }

    public interface Interceptor {
        public List<AbilitySub> chooseModesForAbility(List<AbilitySub> choices, int min, int num, boolean allowRepeat);
        public Card chooseCard(CardCollection fetchList);
        public void chooseTargets(SpellAbility sa, GameSimulator simulator);
        public Card getSelectedChoice();
        public int[] getSelectModes();
        public boolean advance(Score lastScore);
    }
}
