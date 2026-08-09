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
    private final XDecisionContext xContext;
    private final ModeDecisionContext modeContext;
    private final CardSelectionContext cardSelectionContext;

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates) {
        this(requestId, decisionType, candidates, null, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final TargetDecisionContext targetContext) {
        this(requestId, decisionType, candidates, targetContext, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final PaymentDecisionContext paymentContext) {
        this(requestId, decisionType, candidates, null, paymentContext, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final XDecisionContext xContext) {
        this(requestId, decisionType, candidates, null, null, xContext, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final ModeDecisionContext modeContext) {
        this(requestId, decisionType, candidates, null, null, null, modeContext, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final CardSelectionContext cardSelectionContext) {
        this(requestId, decisionType, candidates, null, null, null, null, cardSelectionContext);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext) {
        this.requestId = requestId;
        this.decisionType = Objects.requireNonNull(decisionType);
        this.candidates = List.copyOf(candidates);
        this.targetContext = targetContext;
        this.paymentContext = paymentContext;
        this.xContext = xContext;
        this.modeContext = modeContext;
        this.cardSelectionContext = cardSelectionContext;
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
        if (decisionType == DecisionType.X_VALUE && xContext == null) {
            throw new IllegalArgumentException("An X_VALUE DecisionRequest requires X context");
        }
        if (decisionType != DecisionType.X_VALUE && xContext != null) {
            throw new IllegalArgumentException("Only X_VALUE DecisionRequests may contain X context");
        }
        if (decisionType == DecisionType.MODE && modeContext == null) {
            throw new IllegalArgumentException("A MODE DecisionRequest requires mode context");
        }
        if (decisionType != DecisionType.MODE && modeContext != null) {
            throw new IllegalArgumentException("Only MODE DecisionRequests may contain mode context");
        }
        if (decisionType == DecisionType.CARD_SELECTION && cardSelectionContext == null) {
            throw new IllegalArgumentException("A CARD_SELECTION DecisionRequest requires card-selection context");
        }
        if (decisionType != DecisionType.CARD_SELECTION && cardSelectionContext != null) {
            throw new IllegalArgumentException("Only CARD_SELECTION DecisionRequests may contain card-selection context");
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

    /** X_VALUE-only metadata constructed from Forge's live announcement state. */
    public XDecisionContext getXContext() {
        return xContext;
    }

    /** MODE-only metadata constructed from Forge's live callback state. */
    public ModeDecisionContext getModeContext() {
        return modeContext;
    }

    /** CARD_SELECTION-only callback/session metadata. */
    public CardSelectionContext getCardSelectionContext() {
        return cardSelectionContext;
    }

    public boolean isForced() {
        return candidates.size() == 1;
    }
}
