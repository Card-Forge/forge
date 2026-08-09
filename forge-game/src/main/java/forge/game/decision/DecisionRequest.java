package forge.game.decision;

import java.util.List;
import java.util.Objects;

/** An immutable set of legal alternatives for one atomic player decision. */
public final class DecisionRequest {
    private final long requestId;
    private final DecisionType decisionType;
    private final List<LegalCandidate> candidates;
    private final TargetDecisionContext targetContext;

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates) {
        this(requestId, decisionType, candidates, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final TargetDecisionContext targetContext) {
        this.requestId = requestId;
        this.decisionType = Objects.requireNonNull(decisionType);
        this.candidates = List.copyOf(candidates);
        this.targetContext = targetContext;
        if (this.candidates.isEmpty()) {
            throw new IllegalArgumentException("A DecisionRequest must contain at least one legal candidate");
        }
        if (decisionType == DecisionType.TARGET && targetContext == null) {
            throw new IllegalArgumentException("A TARGET DecisionRequest requires target context");
        }
        if (decisionType != DecisionType.TARGET && targetContext != null) {
            throw new IllegalArgumentException("Only TARGET DecisionRequests may contain target context");
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

    /**
     * TARGET-only metadata constructed from the live Forge SpellAbility. This is {@code null} for other
     * decision families.
     */
    public TargetDecisionContext getTargetContext() {
        return targetContext;
    }

    public boolean isForced() {
        return candidates.size() == 1;
    }
}
