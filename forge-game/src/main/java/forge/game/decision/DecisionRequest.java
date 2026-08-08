package forge.game.decision;

import java.util.List;
import java.util.Objects;

/** An immutable set of legal alternatives for one atomic player decision. */
public final class DecisionRequest {
    private final long requestId;
    private final DecisionType decisionType;
    private final List<LegalCandidate> candidates;

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates) {
        this.requestId = requestId;
        this.decisionType = Objects.requireNonNull(decisionType);
        this.candidates = List.copyOf(candidates);
        if (this.candidates.isEmpty()) {
            throw new IllegalArgumentException("A DecisionRequest must contain at least one legal candidate");
        }
    }

    public long getRequestId() {
        return requestId;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public List<LegalCandidate> getCandidates() {
        return candidates;
    }

    public boolean isForced() {
        return candidates.size() == 1;
    }
}
