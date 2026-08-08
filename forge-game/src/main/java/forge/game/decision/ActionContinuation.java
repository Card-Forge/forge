package forge.game.decision;

import java.util.Objects;

/**
 * Correlates the atomic decisions made while one selected priority action is announced.
 *
 * <p>The sequence identifier is supplied by the request that selected the top-level action. It is deliberately
 * independent of Java object identity and SpellAbility creation identity. Subdecision zero is always the
 * {@link DecisionType#PRIORITY_ACTION} selection; each observed downstream controller callback receives the next
 * monotonically increasing index.</p>
 */
public final class ActionContinuation {
    private final long decisionSequenceId;
    private final PriorityActionKind topLevelCandidateKind;
    private final String topLevelSource;
    private int nextSubdecisionIndex = 1;

    public ActionContinuation(final long decisionSequenceId, final PriorityActionKind topLevelCandidateKind,
            final String topLevelSource) {
        if (decisionSequenceId < 0) {
            throw new IllegalArgumentException("decisionSequenceId must be non-negative");
        }
        this.decisionSequenceId = decisionSequenceId;
        this.topLevelCandidateKind = Objects.requireNonNull(topLevelCandidateKind);
        this.topLevelSource = Objects.requireNonNull(topLevelSource);
    }

    public long getDecisionSequenceId() {
        return decisionSequenceId;
    }

    public int getTopLevelSubdecisionIndex() {
        return 0;
    }

    public PriorityActionKind getTopLevelCandidateKind() {
        return topLevelCandidateKind;
    }

    public String getTopLevelSource() {
        return topLevelSource;
    }

    /** Allocates the index for the next observed downstream controller callback. */
    public int nextSubdecisionIndex() {
        return nextSubdecisionIndex++;
    }
}
