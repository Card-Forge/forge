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
    private List<CachedEffect> effectCache = new ArrayList<>();
    private GameObject[] currentHostAndTarget;

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
        scoreStack = new ArrayList<>();
        scoreStack.add(score);
        simulatorStack = new ArrayList<>();
        currentStack = new ArrayList<>();
    }
    
    private int getRecursionDepth() {
        return scoreStack.size() - 1;
    }

    public boolean shouldRecurse() {
        return bestScore.value != Integer.MAX_VALUE && getRecursionDepth() < MAX_DEPTH;
    }

    public Plan.Decision getLastDecision() {
        if (currentStack.isEmpty()) {
            return null;
        }
        return currentStack.get(currentStack.size() - 1);
    }

    private Score getCurrentScore() {
        return scoreStack.get(scoreStack.size() - 1);
    }

    public void evaluateSpellAbility(List<SpellAbility> saList, int saIndex) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), new Plan.SpellAbilityRef(saList, saIndex)));
    }

    public void evaluateCardChoice(Card choice) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), choice));
    }

    public void evaluateChosenModes(int[] chosenModes, String modesStr) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), chosenModes, modesStr));
    }

    public void evaluateTargetChoices(SpellAbility sa, MultiTargetSelector.Targets targets) {
        currentStack.add(new Plan.Decision(getCurrentScore(), getLastDecision(), targets));
    }

    public void doneEvaluating(Score score) {
        // if we're here during a deeper level this hasn't been called for the level above yet
        // in such case we need to check that this decision has really lead to the improvement in score
        if (getLastDecision().initialScore.value < score.value && score.value > bestScore.value) {
            bestScore = score;
            bestSequence = getLastDecision();
        }
        currentStack.remove(currentStack.size() - 1);
    }

    public Score getBestScore() {
        return bestScore;
    }

    public Plan getBestPlan() {
        if (!currentStack.isEmpty()) {
            throw new RuntimeException("getBestPlan() expects currentStack to be empty!");
        }

        ArrayList<Plan.Decision> sequence = new ArrayList<>();
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
            if (d.saRef != null) {
                sequence.set(writeIndex, d);
                writeIndex++;
            } else if (d.targets != null) {
                sequence.get(writeIndex - 1).targets = d.targets;
            } else if (d.choices != null) {
                Plan.Decision to = sequence.get(writeIndex - 1);
                if (to.choices == null) {
                    to.choices = new ArrayList<>();
                }
                to.choices.addAll(d.choices);
            } else if (d.modes != null) {
                sequence.get(writeIndex - 1).modes = d.modes;
                sequence.get(writeIndex - 1).modesStr = d.modesStr;
            }
        }
        sequence.subList(writeIndex, sequence.size()).clear();
        return new Plan(sequence, getBestScore());
    }

    private Plan.Decision getLastMergedDecision() {
        MultiTargetSelector.Targets targets = null;
        List<String> choices = new ArrayList<>();
        int[] modes = null;
        String modesStr = null;

        Plan.Decision d = currentStack.get(currentStack.size() - 1);
        while (d.saRef == null) {
            if (d.targets != null) {
                targets = d.targets;
            } else if (d.choices != null) {
                // Since we're iterating backwards, add to the front.
                choices.addAll(0, d.choices);
            } else if (d.modes != null) {
                modes = d.modes;
                modesStr = d.modesStr;
            }
            d = d.prevDecision;
        }

        Plan.Decision merged  = new Plan.Decision(d.initialScore, d.prevDecision, d.saRef);
        merged.targets = targets;
        if (!choices.isEmpty()) {
            merged.choices = choices;
        }
        merged.modes = modes;
        merged.modesStr = modesStr;
        merged.xMana = d.xMana;
        return merged;
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

    public GameObject[] getOriginalHostCardAndTarget(SpellAbility sa) {
        SpellAbility saOrSubSa = sa;
        while (saOrSubSa != null && !saOrSubSa.usesTargeting()) {
            saOrSubSa = saOrSubSa.getSubAbility();
        }

        if (saOrSubSa == null || saOrSubSa.getTargets() == null || saOrSubSa.getTargets().size() != 1) {
            return null;
        }
        GameObject target = saOrSubSa.getTargets().get(0);
        GameObject originalTarget = target;
        if (!(target instanceof Card)) {  return null; }
        Card hostCard = sa.getHostCard();
        for (int i = simulatorStack.size() - 1; i >= 0; i--) {
            if (target == null || hostCard == null) {
                // This could happen when evaluating something that couldn't exist
                // in the original game - for example, targeting a token that came
                // into being as a result of simulating something earlier. Unfortunately,
                // we can't cache this case.
                return null;
            }
            GameCopier copier = simulatorStack.get(i).getGameCopier();
            if (copier.getCopiedGame() != hostCard.getGame()) {
                throw new RuntimeException("Expected hostCard and copier game to match!");
            }
            if (copier.getCopiedGame() != ((Card) target).getGame()) {
                throw new RuntimeException("Expected target and copier game to match!");
            }
            target = copier.reverseFind(target);
            hostCard = (Card) copier.reverseFind(hostCard);
        }
        return new GameObject[] { hostCard, target, originalTarget };
    }

    public void setHostAndTarget(SpellAbility sa, GameSimulator simulator) {
        simulatorStack.add(simulator);
        currentHostAndTarget = getOriginalHostCardAndTarget(sa);
        simulatorStack.remove(simulatorStack.size() - 1);
    }

    public Score shouldSkipTarget(SpellAbility sa, GameSimulator simulator) {
        simulatorStack.add(simulator);
        GameObject[] hostAndTarget = getOriginalHostCardAndTarget(sa);
        simulatorStack.remove(simulatorStack.size() - 1);
        if (hostAndTarget != null) {
            String saString = sa.toString();
            for (CachedEffect effect : effectCache) {
                if (effect.hostCard == hostAndTarget[0] && effect.target == hostAndTarget[1] && effect.sa.equals(saString)) {
                    GameStateEvaluator evaluator = new GameStateEvaluator();
                    Player player = sa.getActivatingPlayer();
                    int cardScore = evaluator.evalCard(player.getGame(), player, (Card) hostAndTarget[2]);
                    if (cardScore == effect.targetScore) {
                        Score currentScore = getCurrentScore();
                        // TODO: summonSick score?
                        return new Score(currentScore.value + effect.scoreDelta, currentScore.summonSickValue);
                    }
                }
            }
        }
        return null;
    }

    public void possiblyCacheResult(Score score, SpellAbility sa) {
        String cached = "";

        // TODO: Why is the check below needed by tests?
        if (!currentStack.isEmpty()) {
            Plan.Decision d = currentStack.get(currentStack.size() - 1);
            int scoreDelta = score.value - d.initialScore.value;
            // Needed to make sure below is only executed when target decisions are ended.
            // Also, only cache negative effects - so that in those cases we don't need to
            // recurse.
            if (scoreDelta <= 0 && d.targets != null) {
                // FIXME: Support more than one target in this logic.
                GameObject[] hostAndTarget = currentHostAndTarget;
                if (currentHostAndTarget != null) {
                    GameStateEvaluator evaluator = new GameStateEvaluator();
                    Player player = sa.getActivatingPlayer();
                    int cardScore = evaluator.evalCard(player.getGame(), player, (Card) hostAndTarget[2]);
                    effectCache.add(new CachedEffect(hostAndTarget[0], sa, hostAndTarget[1], cardScore, scoreDelta));
                    cached = " (added to cache)";
                }
            }
        }

        currentHostAndTarget = null;
        printState(score, sa, cached, true);
    }

    public void printState(Score score, SpellAbility origSa, String suffix, boolean useStack) {
        int recursionDepth = getRecursionDepth();
        for (int i = 0; i < recursionDepth; i++)
            System.err.print("  ");
        String str;
        if (useStack && !currentStack.isEmpty()) {
            str = getLastMergedDecision().toString(true);
        } else {
            str = SpellAbilityPicker.abilityToString(origSa);
        }
        System.err.println(recursionDepth + ": [" + score.value + "] " + str + suffix);
    }
}
