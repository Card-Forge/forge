package forge.game.decision;

import java.util.List;
import java.util.Objects;

/** An immutable set of legal alternatives for one atomic player decision. */
public final class DecisionRequest {
    private final long requestId;
    private final DecisionType decisionType;
    private final List<LegalCandidate> candidates;
    private final TargetDecisionContext targetContext;
    private final PaymentDecisionContext paymentContext;

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates) {
        this(requestId, decisionType, candidates, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final TargetDecisionContext targetContext) {
        this(requestId, decisionType, candidates, targetContext, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final PaymentDecisionContext paymentContext) {
        this(requestId, decisionType, candidates, null, paymentContext);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext) {
        this.requestId = requestId;
        this.decisionType = Objects.requireNonNull(decisionType);
        this.candidates = List.copyOf(candidates);
        this.targetContext = targetContext;
        this.paymentContext = paymentContext;
        if (this.candidates.isEmpty()) {
            throw new IllegalArgumentException("A DecisionRequest must contain at least one legal candidate");
        }
        if (decisionType == DecisionType.TARGET && targetContext == null) {
            throw new IllegalArgumentException("A TARGET DecisionRequest requires target context");
        }
        if (decisionType != DecisionType.TARGET && targetContext != null) {
            throw new IllegalArgumentException("Only TARGET DecisionRequests may contain target context");
        }
        if (decisionType == DecisionType.PAYMENT && paymentContext == null) {
            throw new IllegalArgumentException("A PAYMENT DecisionRequest requires payment context");
        }
        if (decisionType != DecisionType.PAYMENT && paymentContext != null) {
            throw new IllegalArgumentException("Only PAYMENT DecisionRequests may contain payment context");
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

    /** PAYMENT-only metadata constructed from Forge's live payment state. */
    public PaymentDecisionContext getPaymentContext() {
        return paymentContext;
    }

    public boolean isForced() {
        return candidates.size() == 1;
    }
}
