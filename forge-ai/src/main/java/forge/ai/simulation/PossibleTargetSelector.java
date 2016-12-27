package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityCondition;
import forge.game.spellability.TargetRestrictions;

public class PossibleTargetSelector {
    private final SpellAbility sa;
    private SpellAbility targetingSa;
    private int targetingSaIndex;
    private TargetRestrictions tgt;
    private int targetIndex;
    private List<GameObject> validTargets;

    public static class Targets {
        final int targetingSaIndex;
        final int originalTargetCount;
        final int targetIndex;
        final String description;
        
        private Targets(int targetingSaIndex, int originalTargetCount, int targetIndex, String description)  {
            this.targetingSaIndex = targetingSaIndex;
            this.originalTargetCount = originalTargetCount;
            this.targetIndex = targetIndex;
            this.description = description;

            if (targetIndex < 0 || targetIndex >= originalTargetCount) {
                throw new IllegalArgumentException("Invalid targetIndex=" + targetIndex);
            }
        }
        
        @Override
        public String toString() {
            return description;
        }
    }

    public PossibleTargetSelector(Game game, Player self, SpellAbility sa) {
        this.sa = sa;
        chooseTargetingSubAbility();
        this.tgt = targetingSa != null ? targetingSa.getTargetRestrictions() : null;
        this.targetIndex = 0;
        this.validTargets = new ArrayList<GameObject>();
        if (targetingSa != null) {
            sa.setActivatingPlayer(self);
            targetingSa.resetTargets();
            for (GameObject o : tgt.getAllCandidates(sa, true)) {
                validTargets.add(o);
            }
        }
    }

    private static boolean conditionsAreMet(SpellAbility saOrSubSa) {
        SpellAbilityCondition conditions = saOrSubSa.getConditions();
        return conditions == null || conditions.areMet(saOrSubSa);
    }

    private void chooseTargetingSubAbility() {
        // TODO: This needs to handle case where multiple sub-abilities each have targets.
        SpellAbility saOrSubSa = sa;
        int index = 0;
        do {
            if (saOrSubSa.usesTargeting() && conditionsAreMet(saOrSubSa)) {
                targetingSaIndex = index;
                targetingSa = saOrSubSa;
                return;
            }
            saOrSubSa = saOrSubSa.getSubAbility();
            index++;
        } while (saOrSubSa != null);
    }

    public boolean hasPossibleTargets() {
        return !validTargets.isEmpty();
    }

    private void selectTargetsByIndex(int index) {
        targetingSa.resetTargets();

        // TODO: smarter about multiple targets, identical targets, etc...
        while (targetingSa.getTargets().getNumTargeted() < tgt.getMaxTargets(sa.getHostCard(), targetingSa) && index < validTargets.size()) {
            targetingSa.getTargets().add(validTargets.get(index++));
        }

        // Divide up counters, since AI is expected to do this. For now,
        // divided evenly with left-overs going to the first target.
        if (targetingSa.hasParam("DividedAsYouChoose")) {
            final int targetCount = targetingSa.getTargets().getTargetCards().size();
            if (targetCount > 0) {
                final String amountStr = targetingSa.getParam("CounterNum");
                final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, targetingSa);
                final int amountPerCard = amount / targetCount;
                int amountLeftOver = amount - (amountPerCard * targetCount);
                final TargetRestrictions tgtRes = targetingSa.getTargetRestrictions();
                for (GameObject target : targetingSa.getTargets().getTargets()) {
                    tgtRes.addDividedAllocation(target, amountPerCard + amountLeftOver);
                    amountLeftOver = 0;
                }
            }
        }
    }

    public Targets getLastSelectedTargets() {
        return new Targets(targetingSaIndex, validTargets.size(), targetIndex - 1, targetingSa.getTargets().getTargetedString());
    }

    public boolean selectTargets(Targets targets) {
        if (targets.originalTargetCount != validTargets.size() || targets.targetingSaIndex != targetingSaIndex) {
            return false;
        }
        selectTargetsByIndex(targets.targetIndex);
        return true;
    }
 
    public boolean selectNextTargets() {
        if (targetIndex >= validTargets.size()) {
            return false;
        }
        selectTargetsByIndex(targetIndex);
        targetIndex++;
        return true;
    }
}
