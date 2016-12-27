package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class SimulationController {
    private static int MAX_DEPTH = 3;

    private List<Plan.Decision> currentStack;
    private List<Score> scoreStack;
    private List<GameSimulator> simulatorStack;
    private Plan.Decision bestSequence; // last action of sequence
    private Score bestScore;
    private List<CachedEffect> effectCache = new ArrayList<CachedEffect>();

    private static class CachedEffect {
        final GameObject hostCard;
        final String sa;
        final GameObject target;
        final int targetScore;
        final int scoreDelta;

        public CachedEffect(GameObject hostCard, SpellAbility sa, GameObject target, int targetScore, int scoreDelta) {
            this.hostCard = hostCard;
            this.sa = sa.toString();
            this.target = target;
            this.targetScore = targetScore;
            this.scoreDelta = scoreDelta;
        }
    }

    public SimulationController(Score score) {
        bestScore = score;
        scoreStack = new ArrayList<Score>();
        scoreStack.add(score);
        simulatorStack = new ArrayList<GameSimulator>();
        currentStack = new ArrayList<Plan.Decision>();
    }
    
    private int getRecursionDepth() {
        return scoreStack.size() - 1;
    }
    
    public boolean shouldRecurse() {
        return getRecursionDepth() < MAX_DEPTH;
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
    
    private GameObject[] getOriginalHostCardAndTarget(SpellAbility sa) {
        SpellAbility saOrSubSa = sa;
        do {
            if (saOrSubSa.usesTargeting()) {
                break;
            }
            saOrSubSa = saOrSubSa.getSubAbility();
        } while (saOrSubSa != null);

        if (saOrSubSa == null || saOrSubSa.getTargets() == null || saOrSubSa.getTargets().getTargets().size() != 1) {
            return null;
        }
        GameObject target = saOrSubSa.getTargets().getTargets().get(0);
        GameObject originalTarget = target;
        if (!(target instanceof Card)) { return null; }
        GameObject hostCard = sa.getHostCard();
        for (int i = simulatorStack.size() - 1; i >= 0; i--) {
            GameCopier copier = simulatorStack.get(i).getGameCopier();
            target = copier.reverseFind(target);
            hostCard = copier.reverseFind(hostCard);
        }
        return new GameObject[] { hostCard, target, originalTarget };
    }

    public Score evaluateTargetChoices(SpellAbility sa, PossibleTargetSelector.Targets targets) {
        GameObject[] hostAndTarget = getOriginalHostCardAndTarget(sa);
        if (hostAndTarget != null) {
            String saString = sa.toString();
            for (CachedEffect effect : effectCache) {
                if (effect.hostCard == hostAndTarget[0] && effect.target == hostAndTarget[1] && effect.sa.equals(saString)) {
                    GameStateEvaluator evaluator = new GameStateEvaluator();
                    Player player = sa.getActivatingPlayer();
                    int cardScore = evaluator.evalCard(player.getGame(), player, (Card) hostAndTarget[2], null);
                    if (cardScore == effect.targetScore) {
                        Score currentScore = getCurrentScore();
                        // TODO: summonSick score?
                        return new Score(currentScore.value + effect.scoreDelta, currentScore.summonSickValue);
                    }
                }
            }
        }

        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), targets));
        return null;
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
    
    public void push(SpellAbility sa, Score score, GameSimulator simulator) {
        GameSimulator.debugPrint("Recursing DEPTH=" + getRecursionDepth());
        GameSimulator.debugPrint("  With: " + sa);
        scoreStack.add(score);
        simulatorStack.add(simulator);
    }

    public void pop(Score score, SpellAbility nextSa) {
        scoreStack.remove(scoreStack.size() - 1);
        simulatorStack.remove(simulatorStack.size() - 1);
        GameSimulator.debugPrint("DEPTH"+getRecursionDepth()+" best score " + score + " " + nextSa);
    }

    public void possiblyCacheResult(Score score, SpellAbility sa) {
        boolean cached = false;

        // TODO: Why is the check below needed by tests?
        if (!currentStack.isEmpty()) {
            Plan.Decision d = currentStack.get(currentStack.size() - 1);
            int scoreDelta = score.value - d.initialScore.value;
            // Needed to make sure below is only executed when target decisions are ended.
            // Also, only cache negative effects - so that in those cases we don't need to
            // recurse.
            if (scoreDelta <= 0 && d.targets != null) {
                // FIXME: Support more than one target in this logic.
                GameObject[] hostAndTarget = getOriginalHostCardAndTarget(sa);
                if (hostAndTarget != null) {
                    GameStateEvaluator evaluator = new GameStateEvaluator();
                    Player player = sa.getActivatingPlayer();
                    int cardScore = evaluator.evalCard(player.getGame(), player, (Card) hostAndTarget[2], null);
                    effectCache.add(new CachedEffect(hostAndTarget[0], sa, hostAndTarget[1], cardScore, scoreDelta));
                    cached = true;
                }
            }
        }

        printState(score, sa, cached ? " (added to cache)" : "");
    }

    public void printState(Score score, SpellAbility origSa, String suffix) {
        int recursionDepth = getRecursionDepth();
        for (int i = 0; i < recursionDepth; i++)
            System.err.print("  ");
        String choice = "";
        if (!currentStack.isEmpty() && currentStack.get(currentStack.size() - 1).choice != null) {
            choice = " -> " + currentStack.get(currentStack.size() - 1).choice;
        }
        System.err.println(recursionDepth + ": [" + score.value + "] " + SpellAbilityPicker.abilityToString(origSa) + choice + suffix);
    }
}
