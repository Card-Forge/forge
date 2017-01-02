package forge.ai.simulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.math3.util.CombinatoricsUtils;

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

    private Score[] cachedTargetScores;
    private int nextTarget = 0;
    private Score bestScoreForTarget = new Score(Integer.MIN_VALUE);

    private int numChoices = -1;
    private int nextChoice = 0;
    private Card selectedChoice;
    private Score bestScoreForChoice = new Score(Integer.MIN_VALUE);

    public SpellAbilityChoicesIterator(SimulationController controller) {
        this.controller = controller;
    }

    public List<AbilitySub> chooseModesForAbility(List<AbilitySub> choices, int min, int num, boolean allowRepeat) {
        if (modeIterator == null) {
            // TODO: Need to skip modes that are invalid (e.g. targets don't exist)!
            // TODO: Do we need to do something special to support cards that have extra costs
            // when choosing more modes, like Blessed Alliance?
            if (!allowRepeat) {
                modeIterator = CombinatoricsUtils.combinationsIterator(choices.size(), num);;
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
                    sb.append(sub.getHostCard()).append(" -> ");
                }
                sb.append(sub);
            }
            controller.evaluateChosenModes(selectedModes, sb.toString());
            advancedToNextMode = false;
        }
        return result;
    }

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

    public Card getSelectedChoice() {
        return selectedChoice;
    }

    public int[] getSelectModes() {
        return selectedModes;
    }

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

    public static List<AbilitySub> getModeCombination(List<AbilitySub> choices, int[] modeIndexes) {
        ArrayList<AbilitySub> modes = new ArrayList<AbilitySub>();
        for (int modeIndex : modeIndexes) {
            modes.add(choices.get(modeIndex));
        }
        return modes;
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
    }
}
