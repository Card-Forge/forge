package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.math3.util.CombinatoricsUtils;

import forge.ai.ComputerUtilCost;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

public class SpellAbilityChoicesIterator {
    private SimulationController controller;

    private Iterator<int[]> modeIterator;
    private int[] selectedModes;
    private Score bestScoreForMode = new Score(Integer.MIN_VALUE);
    private boolean advancedToNextMode;

    private ArrayList<Score> cachedTargetScores;
    private int nextTarget = 0;
    private Score bestScoreForTarget = new Score(Integer.MIN_VALUE);
    private boolean pushTarget = true;

    private static class ChoicePoint {
        int numChoices = -1;
        int nextChoice = 0;
        Card selectedChoice;
        Score bestScoreForChoice = new Score(Integer.MIN_VALUE);
    }
    private ArrayList<ChoicePoint> choicePoints = new ArrayList<>();
    private int incrementedCpIndex = 0;
    private int cpIndex = -1;

    private int evalDepth;

    public SpellAbilityChoicesIterator(SimulationController controller) {
        this.controller = controller;
    }

    public List<AbilitySub> chooseModesForAbility(List<AbilitySub> choices, int min, int num, boolean allowRepeat) {
        if (modeIterator == null) {
            // TODO: Need to skip modes that are invalid (e.g. targets don't exist)!
            // TODO: Do we need to do something special to support cards that have extra costs
            // when choosing more modes, like Blessed Alliance?
            if (!allowRepeat) {
                modeIterator = CombinatoricsUtils.combinationsIterator(choices.size(), num);
            } else {
                // Note: When allowRepeat is true, it does result in many possibilities being tried.
                // We should ideally prune some of those at a higher level.
                modeIterator = new AllowRepeatModesIterator(choices.size(), min, num);
            }
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
                    sb.append(sub.getHostCard().getName()).append(" -> ");
                }
                sb.append(sub);
            }
            controller.evaluateChosenModes(selectedModes, sb.toString());
            evalDepth++;
            advancedToNextMode = false;
        }
        return result;
    }

    public Card chooseCard(CardCollection fetchList) {
        cpIndex++;
        if (cpIndex >= choicePoints.size()) {
            choicePoints.add(new ChoicePoint());
        }
        ChoicePoint cp = choicePoints.get(cpIndex);
        // Prune duplicates.
        HashSet<String> uniqueCards = new HashSet<>();
        for (int i = 0; i < fetchList.size(); i++) {
            Card card = fetchList.get(i);
            if (uniqueCards.add(card.getName()) && uniqueCards.size() == cp.nextChoice + 1) {
                cp.selectedChoice = card;
            }
        }
        if (cp.selectedChoice == null) {
            throw new RuntimeException();
        }
        cp.numChoices = uniqueCards.size();
        if (cpIndex >= incrementedCpIndex) {
            controller.evaluateCardChoice(cp.selectedChoice);
            evalDepth++;
        }
        return cp.selectedChoice;
    }

    public void chooseTargets(SpellAbility sa, GameSimulator simulator) {
        // Note: Can't just keep a TargetSelector object cached because it's
        // responsible for setting state on a SA and the SA object changes each
        // time since it's a different simulation.
        MultiTargetSelector selector = new MultiTargetSelector(sa, null);
        if (selector.hasPossibleTargets()) {
            if (cachedTargetScores == null) {
                cachedTargetScores = new ArrayList<>();
                nextTarget = -1;
                for (int i = 0; selector.selectNextTargets(); i++) {
                    Score score = controller.shouldSkipTarget(sa, simulator);
                    cachedTargetScores.add(score);
                    if (score != null) {
                        controller.printState(score, sa, " - via estimate (skipped)", false);
                    } else if (nextTarget == -1) {
                        nextTarget = i;
                    }
                }
                selector.reset();
                // If all targets were cached, we unfortunately have to evaluate the first target again
                // because at this point we're already running the simulation code and there's no turning
                // back. This used to be not possible when the PossibleTargetSelector was controlling the
                // flow. :(
                if (nextTarget == -1) { nextTarget = 0; }
            }
            selector.selectTargetsByIndex(nextTarget);
            controller.setHostAndTarget(sa, simulator);
            // The hierarchy is modes -> targets -> choices[]. In the presence of choices, we want to call
            // evaluate just once at the top level.
            if (pushTarget) {
                controller.evaluateTargetChoices(sa, selector.getLastSelectedTargets());
                evalDepth++;
                pushTarget = false;
            }
            return;
        }
    }

    public int[] getSelectModes() {
        return selectedModes;
    }

    public boolean advance(Score lastScore) {
        cpIndex = -1;
        for (ChoicePoint cp : choicePoints) {
            if (lastScore.value > cp.bestScoreForChoice.value) {
                cp.bestScoreForChoice = lastScore;
            }
        }
        if (lastScore.value > bestScoreForTarget.value) {
            bestScoreForTarget = lastScore;
        }
        if (lastScore.value > bestScoreForMode.value) {
            bestScoreForMode = lastScore;
        }

        if (!choicePoints.isEmpty()) {
            for (int i = choicePoints.size() - 1; i >= 0; i--) {
                ChoicePoint cp = choicePoints.get(i);
                if (cp.nextChoice + 1 < cp.numChoices) {
                    cp.nextChoice++;
                    // Remove tail of the list.
                    incrementedCpIndex = i;
                    for (int j = choicePoints.size() - 1; j >= i; j--) {
                        doneEvaluating(choicePoints.get(j).bestScoreForChoice);
                    }
                    choicePoints.subList(i + 1, choicePoints.size()).clear();
                    return true;
                }
            }
            for (int i = choicePoints.size() - 1; i >= 0; i--) {
                doneEvaluating(choicePoints.get(i).bestScoreForChoice);
            }
            choicePoints.clear();
        }
        if (cachedTargetScores != null) {
            pushTarget = true;
            doneEvaluating(bestScoreForTarget);
            bestScoreForTarget = new Score(Integer.MIN_VALUE);
            while (nextTarget + 1 < cachedTargetScores.size()) {
                nextTarget++;
                if (cachedTargetScores.get(nextTarget) == null) {
                    return true;
                }
            }
            nextTarget = -1;
            cachedTargetScores = null;
        }
        if (modeIterator != null) {
            doneEvaluating(bestScoreForMode);
            bestScoreForMode = new Score(Integer.MIN_VALUE);
            if (modeIterator.hasNext()) {
                selectedModes = modeIterator.next();
                advancedToNextMode = true;
                return true;
            }
            modeIterator = null;
        }

        if (evalDepth != 0) {
            throw new RuntimeException("" + evalDepth);
        }
        return false;
    }

    private void doneEvaluating(Score bestScore) {
        controller.doneEvaluating(bestScore);
        evalDepth--;
    }

    public static List<AbilitySub> getModeCombination(List<AbilitySub> choices, int[] modeIndexes) {
        ArrayList<AbilitySub> modes = new ArrayList<>();
        for (int modeIndex : modeIndexes) {
            modes.add(choices.get(modeIndex));
        }
        return modes;
    }

    public void announceX(SpellAbility sa) {
        // TODO this should also iterate over all possible values
        // (currently no additional complexity to keep performance reasonable)
        if (sa.costHasManaX()) {
            Integer x = ComputerUtilCost.getMaxXValue(sa, sa.getActivatingPlayer(), sa.isTrigger());
            sa.setXManaCostPaid(x);
            controller.getLastDecision().xMana = x;
        }
    }

    private static class AllowRepeatModesIterator implements Iterator<int[]> {
        private int numChoices;
        private int max;
        private int[] indexes;

        public AllowRepeatModesIterator(int numChoices, int min, int max) {
            this.numChoices = numChoices;
            this.max = max;
            this.indexes = new int[min];
        }

        @Override
        public boolean hasNext() {
            return indexes != null;
        }

        // Note: This returns a new int[] array and doesn't modify indexes in place,
        // since that gets returned to the caller.
        private int[] getNextIndexes() {
            // TODO: In some cases, ordering has no effect - e.g. AAB and BAA are equivalent.
            // We should detect those and skip equivalent modes.
            for (int i = indexes.length - 1; i >= 0; i--) {
                if (indexes[i] < numChoices - 1) {
                    int[] nextIndexes = new int[indexes.length];
                    System.arraycopy(indexes, 0, nextIndexes, 0, i);
                    nextIndexes[i] = indexes[i] + 1;
                    return nextIndexes;
                }
            }
            if (indexes.length < max) {
                return new int[indexes.length + 1];
            }
            return null;
        }

        @Override
        public int[] next() {
            if (indexes == null) {
                throw new NoSuchElementException();
            }
            int[] result = indexes;
            indexes = getNextIndexes();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
