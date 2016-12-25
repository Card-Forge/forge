package forge.ai.simulation;

import java.util.HashSet;
import java.util.List;

import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCost;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;

public class SpellAbilityPicker {
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
            print("  " + opinion + ": " + sa);
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

    public SpellAbility chooseSpellAbilityToPlay(SimulationController controller, List<SpellAbility> all, boolean skipCounter) {
        printOutput = (controller == null);

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
            return sa;
        }
        createNewPlan(origGameScore, candidateSAs);
        return getPlannedSpellAbility(origGameScore, candidateSAs);
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
        for (final SpellAbility sa : candidateSAs) {
            Score value = evaluateSa(controller, sa);
            if (value.value > bestSaValue.value) {
                bestSaValue = value;
                bestSa = sa;
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

    private SpellAbility getPlannedSpellAbility(Score origGameScore, List<SpellAbility> availableSAs) {
        if (plan != null && plan.hasNextDecision()) {
            boolean badTargets = false;
            boolean saNotFound = false;
            Plan.Decision decision = plan.selectNextDecision();
            if (decision.initialScore.equals(origGameScore)) {
                // TODO: Other safeguards like list of SAs and maybe the index and such?
                for (final SpellAbility sa : availableSAs) {
                    if (sa.toString().equals(decision.sa)) {
                        if (decision.targets != null) {
                            PossibleTargetSelector selector = new PossibleTargetSelector(game, player, sa);
                            if (!selector.selectTargets(decision.targets)) {
                                badTargets = true;
                                break;
                            }
                        }
                        print("Planned decision " + plan.getNextDecisionIndex() + ": " + abilityToString(sa) + " " + decision.choice);
                        return sa;
                    }
                }
                saNotFound = true;
            }
            print("Failed to continue planned action (" + decision.sa + "). Cause:");
            if (badTargets) {
                print("  Bad targets!");
            } else if (saNotFound) {
                print("  Couldn't find spell/ability!");
            } else {
                print("  Unexpected game score (" + decision.initialScore + " vs. expected " + origGameScore + ")!");
            }
            plan = null;
        }
        return null;
    }
 
    public Score getScoreForChosenAbility() {
        return bestScore;
    }

    public static String abilityToString(SpellAbility sa) {
        String saString = "N/A";
        if (sa != null) {
            saString = sa.toString();
            if (sa.usesTargeting()) {
                saString += " (targets: " + sa.getTargets().getTargetedString() + ")";
            }
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
    
    private AiPlayDecision canPlayAndPayForSim(final SpellAbility sa) {
        if (!sa.canPlay()) {
            return AiPlayDecision.CantPlaySa;
        }
        SpellAbilityCondition conditions = sa.getConditions();
        if (conditions != null && !conditions.areMet(sa)) {
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

    private Score evaluateSa(final SimulationController controller, SpellAbility sa) {
        controller.evaluateSpellAbility(sa);

        Score bestScore = new Score(Integer.MIN_VALUE);
        if (!sa.usesTargeting()) {
            Interceptor interceptor = new Interceptor() {
                private int numChoices = -1;
                private int nextChoice = 0;
                private Card choice;

                @Override
                public Card chooseCard(CardCollection fetchList) {
                    choice = null;
                    // Prune duplicates.
                    HashSet<String> uniqueCards = new HashSet<String>();
                    for (int i = 0; i < fetchList.size(); i++) {
                        Card card = fetchList.get(i);
                        if (uniqueCards.add(card.getName()) && uniqueCards.size() == nextChoice + 1) {
                            choice = card;
                        }
                    }
                    numChoices = uniqueCards.size();
                    nextChoice++;
                    if (choice != null) {
                        controller.evaluateCardChoice(choice);
                    }
                    return choice;
                }

                @Override
                public Card getLastChoice() {
                    return choice;
                }

                @Override
                public boolean hasMoreChoices() {
                    return nextChoice < numChoices;
                }
            };

            do {
                GameSimulator simulator = new GameSimulator(controller, game, player);
                simulator.setInterceptor(interceptor);
                Score score = simulator.simulateSpellAbility(sa);
                if (interceptor.getLastChoice() != null) {
                    controller.doneEvaluating(score);
                }
                if (score.value > bestScore.value) {
                    bestScore = score;
                }
            } while (interceptor.hasMoreChoices());
            controller.doneEvaluating(bestScore);
            return bestScore;
        }

        PossibleTargetSelector selector = new PossibleTargetSelector(game, player, sa);
        TargetChoices tgt = null;
        while (selector.selectNextTargets()) {
            controller.evaluateTargetChoices(selector.getLastSelectedTargets());
            GameSimulator simulator = new GameSimulator(controller, game, player);
            Score score = simulator.simulateSpellAbility(sa);
            controller.doneEvaluating(score);
            // TODO: Get rid of the below when no longer needed.
            if (score.value > bestScore.value) {
                bestScore = score;
                tgt = sa.getTargets();
                sa.resetTargets();
            }
        }
        controller.doneEvaluating(bestScore);

        if (tgt != null) {
            sa.setTargets(tgt);
        }
        return bestScore;
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

    public interface Interceptor {
        public Card chooseCard(CardCollection fetchList);
        public Card getLastChoice();
        public boolean hasMoreChoices();
    }
}
