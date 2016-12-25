package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class SimulationController {
    private static int MAX_DEPTH = 2;
    
    private int recursionDepth;

    private List<Plan.Decision> currentStack;
    private List<Score> scoreStack;
    private Plan.Decision bestSequence; // last action of sequence
    private Score bestScore;
    
    public SimulationController(Score score) {
        bestScore = score;
        scoreStack = new ArrayList<Score>();
        scoreStack.add(score);
        currentStack = new ArrayList<Plan.Decision>();
    }
    
    public boolean shouldRecurse() {
        return recursionDepth < MAX_DEPTH;
    }
    
    private Plan.Decision getLastDecision() {
        if (currentStack.isEmpty()) {
            return null;
        }
        return currentStack.get(currentStack.size() - 1);
    }
    
    private Score getCurrentScore() {
        return scoreStack.get(scoreStack.size() - 1);
    }
    
    public void evaluateSpellAbility(SpellAbility sa) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), sa));
    }
    
    public void evaluateCardChoice(Card choice) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), choice));
    }
    
    public void evaluateTargetChoices(PossibleTargetSelector.Targets targets) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), targets));
    }

    public void doneEvaluating(Score score) {
        if (score.value > bestScore.value) {
            bestScore = score;
            bestSequence = currentStack.get(currentStack.size() - 1);
        }
        currentStack.remove(currentStack.size() - 1);
    }
    
    public Score getBestScore() {
        return bestScore;
    }
    
    public Plan getBestPlan() {
        ArrayList<Plan.Decision> sequence = new ArrayList<Plan.Decision>();
        Plan.Decision current = bestSequence;
        while (current != null) {
            sequence.add(current);
            current = current.prevDecision;
        }
        Collections.reverse(sequence);
        // Merge targets & choices into their parents.
        int writeIndex = 0;
        for (int i = 0; i < sequence.size(); i++) {
            Plan.Decision d = sequence.get(i);
            System.out.println("SeqInput: " + d);
            if (d.sa != null) {
                sequence.set(writeIndex, d);
                writeIndex++;
            } else if (d.targets != null) {
                sequence.get(writeIndex - 1).targets = d.targets;
            } else if (d.choice != null) {
                sequence.get(writeIndex - 1).choice = d.choice;
            }
        }
        sequence.subList(writeIndex, sequence.size()).clear();
        return new Plan(sequence);
    }
    
    public void push(SpellAbility sa, Score score) {
        GameSimulator.debugPrint("Recursing DEPTH=" + recursionDepth);
        GameSimulator.debugPrint("  With: " + sa);
        recursionDepth++;
        scoreStack.add(score);
    }

    public void pop(Score score, SpellAbility nextSa) {
        recursionDepth--;
        scoreStack.remove(scoreStack.size() - 1);
        GameSimulator.debugPrint("DEPTH"+recursionDepth+" best score " + score + " " + nextSa);
    }

    public void printState(Score score, SpellAbility origSa) {
        for (int i = 0; i < recursionDepth; i++)
            System.err.print("  ");
        System.err.println(recursionDepth + ": [" + score.value + "] " + SpellAbilityPicker.abilityToString(origSa));
    }
}
