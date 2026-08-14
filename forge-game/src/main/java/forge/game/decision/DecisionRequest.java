package forge.game.decision;

import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

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
    private final AttackDeclarationContext attackContext;
    private final BlockDeclarationContext blockContext;
    private final MulliganContext mulliganContext;
    private final ConfirmationDecisionContext confirmationContext;
    private final SimultaneousTriggerOrderContext orderContext;
    private final CopySpellResolveFirstOrderContext copySpellResolveFirstOrderContext;
    private final SurveilPartitionContext surveilPartitionContext;

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final TargetDecisionContext targetContext) {
        this(requestId, decisionType, candidates, targetContext, null, null, null, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final PaymentDecisionContext paymentContext) {
        this(requestId, decisionType, candidates, null, paymentContext, null, null, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final XDecisionContext xContext) {
        this(requestId, decisionType, candidates, null, null, xContext, null, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final ModeDecisionContext modeContext) {
        this(requestId, decisionType, candidates, null, null, null, modeContext, null, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final CardSelectionContext cardSelectionContext) {
        this(requestId, decisionType, candidates, null, null, null, null, cardSelectionContext, null, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final AttackDeclarationContext attackContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, attackContext, null, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final BlockDeclarationContext blockContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, blockContext, null);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final MulliganContext mulliganContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, mulliganContext);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final ConfirmationDecisionContext confirmationContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, null,
                confirmationContext);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final SimultaneousTriggerOrderContext orderContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, null,
                null, orderContext);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final CopySpellResolveFirstOrderContext copySpellResolveFirstOrderContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, null,
                null, null, copySpellResolveFirstOrderContext);
    }

    DecisionRequest(final long requestId, final DecisionType decisionType, final List<LegalCandidate> candidates,
            final SurveilPartitionContext surveilPartitionContext) {
        this(requestId, decisionType, candidates, null, null, null, null, null, null, null, null,
                null, null, null, surveilPartitionContext);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext,
            final AttackDeclarationContext attackContext, final BlockDeclarationContext blockContext,
            final MulliganContext mulliganContext) {
        this(requestId, decisionType, candidates, targetContext, paymentContext, xContext, modeContext,
                cardSelectionContext, attackContext, blockContext, mulliganContext, null);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext,
            final AttackDeclarationContext attackContext, final BlockDeclarationContext blockContext,
            final MulliganContext mulliganContext, final ConfirmationDecisionContext confirmationContext) {
        this(requestId, decisionType, candidates, targetContext, paymentContext, xContext, modeContext,
                cardSelectionContext, attackContext, blockContext, mulliganContext, confirmationContext, null);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext,
            final AttackDeclarationContext attackContext, final BlockDeclarationContext blockContext,
            final MulliganContext mulliganContext, final ConfirmationDecisionContext confirmationContext,
            final SimultaneousTriggerOrderContext orderContext) {
        this(requestId, decisionType, candidates, targetContext, paymentContext, xContext, modeContext,
                cardSelectionContext, attackContext, blockContext, mulliganContext, confirmationContext,
                orderContext, null);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext,
            final AttackDeclarationContext attackContext, final BlockDeclarationContext blockContext,
            final MulliganContext mulliganContext, final ConfirmationDecisionContext confirmationContext,
            final SimultaneousTriggerOrderContext orderContext,
            final CopySpellResolveFirstOrderContext copySpellResolveFirstOrderContext) {
        this(requestId, decisionType, candidates, targetContext, paymentContext, xContext, modeContext,
                cardSelectionContext, attackContext, blockContext, mulliganContext, confirmationContext,
                orderContext, copySpellResolveFirstOrderContext, null);
    }

    private DecisionRequest(final long requestId, final DecisionType decisionType,
            final List<LegalCandidate> candidates, final TargetDecisionContext targetContext,
            final PaymentDecisionContext paymentContext, final XDecisionContext xContext,
            final ModeDecisionContext modeContext, final CardSelectionContext cardSelectionContext,
            final AttackDeclarationContext attackContext, final BlockDeclarationContext blockContext,
            final MulliganContext mulliganContext, final ConfirmationDecisionContext confirmationContext,
            final SimultaneousTriggerOrderContext orderContext,
            final CopySpellResolveFirstOrderContext copySpellResolveFirstOrderContext,
            final SurveilPartitionContext surveilPartitionContext) {
        this.requestId = requestId;
        this.decisionType = Objects.requireNonNull(decisionType);
        this.candidates = List.copyOf(candidates);
        this.targetContext = targetContext;
        this.paymentContext = paymentContext;
        this.xContext = xContext;
        this.modeContext = modeContext;
        this.cardSelectionContext = cardSelectionContext;
        this.attackContext = attackContext;
        this.blockContext = blockContext;
        this.mulliganContext = mulliganContext;
        this.confirmationContext = confirmationContext;
        this.orderContext = orderContext;
        this.copySpellResolveFirstOrderContext = copySpellResolveFirstOrderContext;
        this.surveilPartitionContext = surveilPartitionContext;
        if (this.candidates.isEmpty()) {
            throw new IllegalArgumentException("A DecisionRequest must contain at least one legal candidate");
        }
        final Set<String> semanticKeys = new HashSet<>();
        for (final LegalCandidate candidate : this.candidates) {
            if (!semanticKeys.add(candidate.getSemanticKey())) {
                throw new IllegalArgumentException("DecisionRequest candidate semantic keys must be unique: "
                        + candidate.getSemanticKey());
            }
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
        if (decisionType == DecisionType.CARD_SELECTION
                && (cardSelectionContext == null) == (surveilPartitionContext == null)) {
            throw new IllegalArgumentException(
                    "A CARD_SELECTION DecisionRequest requires exactly one selection context");
        }
        if (decisionType != DecisionType.CARD_SELECTION
                && (cardSelectionContext != null || surveilPartitionContext != null)) {
            throw new IllegalArgumentException(
                    "Only CARD_SELECTION DecisionRequests may contain selection context");
        }
        if (surveilPartitionContext != null) {
            validateSurveilPartitionRequest(this.candidates, surveilPartitionContext);
        } else if (this.candidates.stream().anyMatch(candidate ->
                candidate.getSurveilPartitionCandidateKind() != null
                        || candidate.getSurveilPartitionCard() != null)) {
            throw new IllegalArgumentException(
                    "Surveil partition candidates require a Surveil partition context");
        }
        if (decisionType == DecisionType.ATTACK && attackContext == null) {
            throw new IllegalArgumentException("An ATTACK DecisionRequest requires attack context");
        }
        if (decisionType != DecisionType.ATTACK && attackContext != null) {
            throw new IllegalArgumentException("Only ATTACK DecisionRequests may contain attack context");
        }
        if (decisionType == DecisionType.BLOCK && blockContext == null) {
            throw new IllegalArgumentException("A BLOCK DecisionRequest requires block context");
        }
        if (decisionType != DecisionType.BLOCK && blockContext != null) {
            throw new IllegalArgumentException("Only BLOCK DecisionRequests may contain block context");
        }
        if (decisionType == DecisionType.MULLIGAN && mulliganContext == null) {
            throw new IllegalArgumentException("A MULLIGAN DecisionRequest requires mulligan context");
        }
        if (decisionType != DecisionType.MULLIGAN && mulliganContext != null) {
            throw new IllegalArgumentException("Only MULLIGAN DecisionRequests may contain mulligan context");
        }
        if (decisionType == DecisionType.CONFIRMATION && confirmationContext == null) {
            throw new IllegalArgumentException("A CONFIRMATION DecisionRequest requires confirmation context");
        }
        if (decisionType != DecisionType.CONFIRMATION && confirmationContext != null) {
            throw new IllegalArgumentException("Only CONFIRMATION DecisionRequests may contain confirmation context");
        }
        if (decisionType == DecisionType.ORDER && orderContext == null
                && copySpellResolveFirstOrderContext == null) {
            throw new IllegalArgumentException("An ORDER DecisionRequest requires order context");
        }
        if (decisionType != DecisionType.ORDER
                && (orderContext != null || copySpellResolveFirstOrderContext != null)) {
            throw new IllegalArgumentException("Only ORDER DecisionRequests may contain order context");
        }
        if (decisionType == DecisionType.ORDER) {
            if (candidates.size() < 2) {
                throw new IllegalArgumentException("An ORDER DecisionRequest requires at least two candidates");
            }
            if (orderContext != null) {
                if (copySpellResolveFirstOrderContext != null
                        || orderContext.getProfile() != SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER
                        || orderContext.getDirection() != OrderDirection.RESOLVE_FIRST
                        || orderContext.getOriginalItemCount() < candidates.size()
                        || orderContext.getOriginalItemCount() < 2) {
                    throw new IllegalArgumentException("ORDER request does not match the exact L1 profile");
                }
                for (final LegalCandidate candidate : candidates) {
                    if (candidate.getOrderKind() != OrderCandidateKind.SELECT_RESOLVE_FIRST
                            || candidate.getOrderItem() == null
                            || candidate.getCopySpellResolveFirstOrderKind() != null
                            || candidate.getCopySpellResolveFirstOrderItem() != null
                            || !candidate.getSemanticKey().equals("RESOLVE_FIRST|"
                                    + candidate.getOrderItem().getItemId())) {
                        throw new IllegalArgumentException("ORDER candidates must be SELECT_RESOLVE_FIRST items");
                    }
                }
            } else {
                if (copySpellResolveFirstOrderContext.getProfile()
                        != CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER
                        || copySpellResolveFirstOrderContext.getDirection() != OrderDirection.RESOLVE_FIRST
                        || copySpellResolveFirstOrderContext.getOriginalItemCount() < candidates.size()
                        || copySpellResolveFirstOrderContext.getOriginalItemCount() < 2) {
                    throw new IllegalArgumentException("ORDER request does not match the exact L1C profile");
                }
                for (final LegalCandidate candidate : candidates) {
                    if (candidate.getCopySpellResolveFirstOrderKind()
                            != CopySpellResolveFirstOrderItemKind.COPIED_SPELL
                            || candidate.getCopySpellResolveFirstOrderItem() == null
                            || candidate.getOrderKind() != null
                            || candidate.getOrderItem() != null
                            || !candidate.getSemanticKey().equals("RESOLVE_FIRST|"
                                    + candidate.getCopySpellResolveFirstOrderItem().getItemId())) {
                        throw new IllegalArgumentException("ORDER candidates must be COPIED_SPELL items");
                    }
                }
            }
        } else if (candidates.stream().anyMatch(candidate -> candidate.getOrderKind() != null
                || candidate.getOrderItem() != null
                || candidate.getCopySpellResolveFirstOrderKind() != null
                || candidate.getCopySpellResolveFirstOrderItem() != null)) {
            throw new IllegalArgumentException("ORDER candidates require DecisionType.ORDER");
        }
    }

    private static void validateSurveilPartitionRequest(final List<LegalCandidate> candidates,
            final SurveilPartitionContext context) {
        if (context.getProfile() != SurveilPartitionProfile.SURVEIL_PARTITION
                || context.getDecisionStepIndex() < 0
                || context.getDecisionStepIndex() >= context.getOriginalItemCount()) {
            throw new IllegalArgumentException("Surveil partition context does not match the exact profile");
        }
        if (candidates.size() != 2) {
            throw new IllegalArgumentException("A Surveil partition request requires exactly two candidates");
        }

        boolean hasGraveyardCandidate = false;
        boolean hasRetainCandidate = false;
        for (final LegalCandidate candidate : candidates) {
            final SurveilPartitionCandidateKind kind = candidate.getSurveilPartitionCandidateKind();
            final SurveilPartitionCard item = candidate.getSurveilPartitionCard();
            if (kind == null || item == null || item.getItemId() != context.getCurrentItemId()
                    || hasUnrelatedPayload(candidate)
                    || !candidate.getSemanticKey().equals(surveilSemanticKey(kind, item))) {
                throw new IllegalArgumentException(
                        "Surveil partition candidates must match the current typed item and operation");
            }
            if (kind == SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD) {
                if (hasGraveyardCandidate) {
                    throw new IllegalArgumentException("Surveil partition candidates must contain one of each operation");
                }
                hasGraveyardCandidate = true;
            } else if (kind == SurveilPartitionCandidateKind.CLASSIFY_RETAIN) {
                if (hasRetainCandidate) {
                    throw new IllegalArgumentException("Surveil partition candidates must contain one of each operation");
                }
                hasRetainCandidate = true;
            } else {
                throw new IllegalArgumentException("Unknown Surveil partition candidate kind");
            }
        }
        if (!hasGraveyardCandidate || !hasRetainCandidate) {
            throw new IllegalArgumentException("Surveil partition candidates must contain one of each operation");
        }
    }

    private static String surveilSemanticKey(final SurveilPartitionCandidateKind kind,
            final SurveilPartitionCard item) {
        final String operation = kind == SurveilPartitionCandidateKind.CLASSIFY_GRAVEYARD
                ? "CLASSIFY_GRAVEYARD" : "CLASSIFY_RETAIN";
        return "SURVEIL_PARTITION|" + operation + "|" + item.getItemId();
    }

    private static boolean hasUnrelatedPayload(final LegalCandidate candidate) {
        return candidate.getKind() != null
                || candidate.getTargetKind() != null
                || candidate.getPaymentKind() != null
                || candidate.getXValue() != null
                || candidate.getModeOrdinal() != null
                || !candidate.getModeDescription().isEmpty()
                || candidate.isModeUsesTargeting()
                || candidate.getCardSelectionKind() != null
                || candidate.getCardSelectionCard() != null
                || candidate.getAttackKind() != null
                || candidate.getAttackCard() != null
                || candidate.getAttackDefender() != null
                || candidate.getBlockKind() != null
                || candidate.getBlockerCard() != null
                || candidate.getBlockAttackerCard() != null
                || candidate.getMulliganKind() != null
                || candidate.getConfirmationKind() != null
                || candidate.getOrderKind() != null
                || candidate.getOrderItem() != null
                || candidate.getCopySpellResolveFirstOrderKind() != null
                || candidate.getCopySpellResolveFirstOrderItem() != null
                || candidate.getTargetEntityId() != -1
                || !candidate.getTargetName().isEmpty()
                || candidate.getTargetZone() != null
                || candidate.getSourceCardId() != -1
                || !candidate.getSourceName().isEmpty()
                || candidate.getSourceZone() != null
                || candidate.getSourceState() != null
                || !candidate.getAbilityDescription().isEmpty()
                || candidate.getSpellAbility() != null
                || candidate.getTarget() != null
                || candidate.getMana() != null;
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

    /** SURVEIL_PARTITION-only typed context. */
    public SurveilPartitionContext getSurveilPartitionContext() {
        return surveilPartitionContext;
    }

    /** ATTACK-only metadata for one atomic turn-based declaration step. */
    public AttackDeclarationContext getAttackContext() {
        return attackContext;
    }

    /** BLOCK-only metadata for one atomic turn-based declaration step. */
    public BlockDeclarationContext getBlockContext() {
        return blockContext;
    }

    /** MULLIGAN-only metadata for one KEEP/REDRAW callback. */
    public MulliganContext getMulliganContext() {
        return mulliganContext;
    }

    /** CONFIRMATION-only semantic trigger context. */
    public ConfirmationDecisionContext getConfirmationContext() {
        return confirmationContext;
    }

    /** ORDER-only semantic session context. */
    public SimultaneousTriggerOrderContext getOrderContext() {
        return orderContext;
    }

    /** L1C-only semantic session context. */
    public CopySpellResolveFirstOrderContext getCopySpellResolveFirstOrderContext() {
        return copySpellResolveFirstOrderContext;
    }

    public boolean isForced() {
        return candidates.size() == 1;
    }
}
