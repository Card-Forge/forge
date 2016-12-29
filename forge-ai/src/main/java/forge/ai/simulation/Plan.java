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

    public static class Decision {
        final Decision prevDecision;
        final Score initialScore;

        final String sa;
        PossibleTargetSelector.Targets targets;
        String choice;
        int[] modes;
        String modesStr; // for human pretty-print consumption only

        public Decision(Score initialScore, Decision prevDecision, SpellAbility sa) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.sa = sa.toString();
            this.targets = null;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, String saString) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.sa = saString;
            this.targets = null;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, PossibleTargetSelector.Targets targets) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.sa = null;
            this.targets = targets;
            this.choice = null;
        }
        
        public Decision(Score initialScore, Decision prevDecision, Card choice) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.sa = null;
            this.targets = null;
            this.choice = choice.getName();
        }

        public Decision(Score initialScore, Decision prevDecision, int[] modes, String modesStr) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.sa = null;
            this.targets = null;
            this.choice = null;
            this.modes = modes;
            this.modesStr = modesStr;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[initScore=").append(initialScore).append(" ");
            if (modesStr != null) {
                sb.append(modesStr);
            } else {
                sb.append(sa);
            }
            if (targets != null) {
                sb.append(" (targets: ").append(targets).append(")");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
