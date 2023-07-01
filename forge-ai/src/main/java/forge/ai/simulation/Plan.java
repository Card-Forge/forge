package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;

public class Plan {
    private final List<Decision> decisions;
    private final Score finalScore;
    private int nextDecisionIndex;
    private int nextChoice;
    private Decision selectedDecision;
    private PhaseType startPhase;

    public Plan(ArrayList<Decision> decisions, Score finalScore) {
        this.decisions = decisions;
        this.finalScore = finalScore;
    }

    public Score getFinalScore() {
        return finalScore;
    }

    public PhaseType getStartPhase() {
        return startPhase;
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
        nextChoice = 0;
        return selectedDecision;
    }

    public Decision getSelectedDecision() {
        return selectedDecision;
    }

    public String getSelectedDecisionNextChoice() {
        if (selectedDecision.choices != null && nextChoice < selectedDecision.choices.size()) {
            return selectedDecision.choices.get(nextChoice);
        }
        return null;
    }

    public void advanceNextChoice() {
        nextChoice++;
    }

    public int getNextDecisionIndex() {
        return nextDecisionIndex;
    }

    public static class SpellAbilityRef {
        private final int saIndex;
        private final int saCount;
        private final String saStr;
        private final String saHumanStr;

        public SpellAbilityRef(List<SpellAbility> saList, int saIndex) {
            this.saIndex = saIndex;
            this.saCount = saList.size();
            SpellAbility sa = saList.get(saIndex);
            this.saStr = sa.toString();
            this.saHumanStr = SpellAbilityPicker.abilityToString(sa, false);
        }

        public SpellAbility findReferencedAbility(List<SpellAbility> availableSAs) {
            if (availableSAs.size() != saCount) {
                return null;
            }
            SpellAbility sa = availableSAs.get(saIndex);
            return sa.toString().equals(saStr) ? sa : null;
        }

        public String toString(boolean showHostCard) {
            return showHostCard ? saHumanStr : saStr;
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
        Integer xMana;
        MultiTargetSelector.Targets targets;
        List<String> choices;
        int[] modes;
        String modesStr; // for human pretty-print consumption only

        public Decision(Score initialScore, Decision prevDecision, SpellAbilityRef saRef) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = saRef;
        }

        public Decision(Score initialScore, Decision prevDecision, MultiTargetSelector.Targets targets) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
            this.targets = targets;
        }

        public Decision(Score initialScore, Decision prevDecision, Card choice) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
            this.choices = new ArrayList<>();
            this.choices.add(choice.getName());
        }

        public Decision(Score initialScore, Decision prevDecision, int[] modes, String modesStr) {
            this.initialScore = initialScore;
            this.prevDecision = prevDecision;
            this.saRef = null;
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
                String sa = saRef.toString(showHostCard);
                if (xMana != null) {
                    sa = sa.replace("(X=0)", "(X=" + xMana + ")");
                }
                sb.append(sa);
            }
            if (targets != null) {
                sb.append(" (targets: ").append(targets).append(")");
            }
            if (choices != null) {
                sb.append(" (chosen: ").append(Joiner.on(", ").join(choices)).append(")");
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
