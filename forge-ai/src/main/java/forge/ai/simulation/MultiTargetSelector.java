package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;

public class MultiTargetSelector {
    public static class Targets {
        private ArrayList<PossibleTargetSelector.Targets> targets;

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

    private List<PossibleTargetSelector> selectors;
    private List<SpellAbility> targetingSAs;
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
        for (int i = 0; i < selectors.size(); i++) {
            targets.targets.add(selectors.get(i).getLastSelectedTargets());
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
        if (i < currentIndex) {
            reset();
        }
        while (currentIndex < i) {
            selectNextTargets();
        }
    }

    public boolean selectNextTargets() {
        if (currentIndex == -1) {
            for (PossibleTargetSelector selector : selectors) {
                if (!selector.selectNextTargets()) {
                    return false;
                }
            }
            currentIndex = 0;
            return true;
        }
        for (int i = selectors.size() - 1; i >= 0; i--) {
            if (selectors.get(i).selectNextTargets()) {
                currentIndex++;
                return true;
            }
            selectors.get(i).reset();
            selectors.get(i).selectNextTargets();
        }
        return false;
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
