package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class Plan {
    private List<Decision> decisions;
    private int nextDecisionIndex;
    private Decision selectedDecision;

    public Plan(ArrayList<Decision> decisions) {
        this.decisions = decisions;
    }
    
    public List<Decision> getDecisions() {
        return decisions;
    }
    
    public boolean hasNextDecision() {
        return nextDecisionIndex < decisions.size();
    }

    public Decision selectNextDecision() {
        selectedDecision = decisions.get(nextDecisionIndex);
        nextDecisionIndex++;
        return selectedDecision;
    }
    
    public Decision getSelectedDecision() {
        return selectedDecision;
    }

    public int getNextDecisionIndex() {
        return nextDecisionIndex;
    }

    public static class SpellAbilityRef {
        private final String sa;
        private final String saHumanStr;

        private SpellAbilityRef(SpellAbility sa) {
            this.sa = sa.toString();
            this.saHumanStr = SpellAbilityPicker.abilityToString(sa);
        }

        public boolean matches(SpellAbility sa) {
            return sa.toString().equals(this.sa);
        }

        public String toString(boolean showHostCard) {
            return showHostCard ? saHumanStr : sa;
        }

        @Override
        public String toString() {
            return toString(false);
        }
    }

    public static class Decision {
        final Decision prevDecision;
        final Score initialScore;

        final SpellAbilityRef saRef;
        PossibleTargetSelector.Targets targets;
        String choice;
        int[] modes;
        String modesStr; // for human pretty-print consumption only

        public Decision(Score initialScore, Decision prevDecision, SpellAbility sa) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = new SpellAbilityRef(sa);
            this.targets = null;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, SpellAbilityRef saRef) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = saRef;
            this.targets = null;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, PossibleTargetSelector.Targets targets) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
            this.targets = targets;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, Card choice) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
            this.targets = null;
            this.choice = choice.getName();
        }

        public Decision(Score initialScore, Decision prevDecision, int[] modes, String modesStr) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
            this.targets = null;
            this.choice = null;
            this.modes = modes;
            this.modesStr = modesStr;
        }

        public String toString(boolean showHostCard) {
            StringBuilder sb = new StringBuilder();
            if (!showHostCard) {
                sb.append("[initScore=").append(initialScore).append(" ");
            }
            if (modesStr != null) {
                sb.append(modesStr);
            } else {
                sb.append(saRef.toString(showHostCard));
            }
            if (targets != null) {
                sb.append(" (targets: ").append(targets).append(")");
            }
            if (!showHostCard) {
                sb.append("]");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toString(false);
        }
    }
}
