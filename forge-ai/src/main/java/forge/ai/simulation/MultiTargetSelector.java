package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;

public class MultiTargetSelector {
    public static class Targets {
        private ArrayList<PossibleTargetSelector.Targets> targets;

        public int size() {
            return targets.size();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (PossibleTargetSelector.Targets tgt : targets) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(tgt.toString());
            }
            return sb.toString();
        }
    }

    private final List<PossibleTargetSelector> selectors;
    private final List<SpellAbility> targetingSAs;
    private int currentIndex;

    public MultiTargetSelector(SpellAbility sa, List<AbilitySub> plannedSubs) {
        targetingSAs = getTargetingSAs(sa, plannedSubs);
        selectors = new ArrayList<>(targetingSAs.size());
        for (int i = 0; i < targetingSAs.size(); i++) {
            selectors.add(new PossibleTargetSelector(sa, targetingSAs.get(i), i));
        }
        currentIndex = -1;
    }

    public boolean hasPossibleTargets() {
        if (targetingSAs.isEmpty()) {
            return false;
        }
        for (PossibleTargetSelector selector : selectors) {
            if (!selector.hasPossibleTargets()) {
                return false;
            }
        }
        return true;
    }

    public Targets getLastSelectedTargets() {
        Targets targets = new Targets();
        targets.targets = new ArrayList<>(selectors.size());
        for (PossibleTargetSelector selector : selectors) {
            targets.targets.add(selector.getLastSelectedTargets());
        }
        return targets;
    }

    public boolean selectTargets(Targets targets) {
        if (targets.targets.size() != selectors.size()) {
            return false;
        }
        for (int i = 0; i < selectors.size(); i++) {
            selectors.get(i).reset();
            if (!selectors.get(i).selectTargets(targets.targets.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void reset() {
        for (PossibleTargetSelector selector : selectors) {
             selector.reset();
        }
        currentIndex = -1;
    }

    public void selectTargetsByIndex(int i) {
        // The caller is telling us to select the i-th possible set of targets.
        if (i < currentIndex) {
            reset();
        }
        while (currentIndex < i) {
            selectNextTargets();
        }
    }

    private boolean selectTargetsStartingFrom(int selectorIndex) {
        // Don't reset the current selector, as it still has the correct list of targets set and has
        // to remember its current/next target index. Subsequent selectors need a reset since their
        // possible targets may change based on what was chosen for earlier ones.
        if (selectors.get(selectorIndex).selectNextTargets()) {
            for (int i = selectorIndex + 1; i < selectors.size(); i++) {
                selectors.get(i).reset();
                if (!selectors.get(i).selectNextTargets()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean selectNextTargets() {
        if (selectors.size() == 0) {
            return false;
        }
        if (currentIndex == -1) {
            // Select the first set of targets (calls selectNextTargets() on each selector).
            if (selectTargetsStartingFrom(0)) {
                currentIndex = 0;
                return true;
            }
            // No possible targets.
            return false;
        }
        // Subsequent call, first try selecting a new target for the last selector. If that doesn't
        // work, backtrack (decrement selector index) and try selecting targets from there.
        // This approach ensures that leaf selectors (end of list) are advanced first, before
        // previous ones, so that we get an AA,AB,BA,BB ordering.
        int selectorIndex = selectors.size() - 1;
        while (!selectTargetsStartingFrom(selectorIndex)) {
            if (selectorIndex == 0) {
                // No more possible targets.
                return false;
            }
            selectorIndex--;
        }
        currentIndex++;
        return true;
    }

    private static boolean conditionsAreMet(SpellAbility saOrSubSa) {
        SpellAbilityCondition conditions = saOrSubSa.getConditions();
        return conditions == null || conditions.areMet(saOrSubSa);
    }

    private List<SpellAbility> getTargetingSAs(SpellAbility sa, List<AbilitySub> plannedSubs) {
        List<SpellAbility> result = new ArrayList<>();
        for (SpellAbility saOrSubSa = sa; saOrSubSa != null; saOrSubSa = saOrSubSa.getSubAbility()) {
            if (saOrSubSa.usesTargeting() && conditionsAreMet(saOrSubSa)) {
                result.add(saOrSubSa);
            }
        }
        // When plannedSubs is provided, also consider them even though they've not yet been added to the
        // sub-ability chain. This is the case when we're choosing modes for a charm-style effect.
        if (plannedSubs != null) {
            for (AbilitySub sub : plannedSubs) {
                if (sub.usesTargeting() && conditionsAreMet(sub)) {
                    result.add(sub);
                }
            }
        }
        return result;
    }
}
