package forge.game.decision;

import forge.game.ability.ApiType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/** Produces one callback-local neutral MODE request from Forge's already-filtered Charm choices. */
public final class ModeDecisionProvider {
    public enum Status {
        DECISION,
        INVALID_MODE,
        NOT_APPLICABLE,
        UNSUPPORTED
    }

    public enum UnsupportedReason {
        UNSUPPORTED_SHAPE,
        MODE_TARGET_COMPLETION,
        CALLBACK_MODE_NOT_IN_CHOICES,
        MODE_X_PAYMENT_DOMAIN,
        MODE_PAYMENT_SUPPORT
    }

    private final TargetDecisionProvider targetProvider = new TargetDecisionProvider();
    private final XDecisionProvider xProvider = new XDecisionProvider();
    private final PriorityCostFeasibility paymentFeasibility = new PriorityCostFeasibility();
    private final PaymentDecisionProvider paymentProvider = new PaymentDecisionProvider();
    private long nextRequestId;

    public Generation generateModeRequest(final SpellAbility root, final List<AbilitySub> possible,
            final int min, final int num, final boolean allowRepeat, final Player choosingPlayer,
            final ActionContinuation continuation) {
        final long startedAtNanos = System.nanoTime();
        Objects.requireNonNull(root);
        Objects.requireNonNull(possible);
        Objects.requireNonNull(choosingPlayer);
        final Assessment assessment = assess(root, possible, min, num, allowRepeat, choosingPlayer);
        if (assessment.status() != Status.DECISION) {
            return Generation.of(assessment.status(), assessment.unsupportedReason(), null,
                    assessment.ruleLegalityProbes(), assessment.downstreamCompletionProbes(),
                    System.nanoTime() - startedAtNanos);
        }
        final Integer subdecisionIndex = continuation == null ? null : continuation.nextSubdecisionIndex();
        final ModeDecisionContext context = new ModeDecisionContext(root, possible, choosingPlayer,
                min, num, allowRepeat, continuation, subdecisionIndex);
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.MODE,
                assessment.candidates(), context);
        return Generation.of(Status.DECISION, null, request, assessment.ruleLegalityProbes(),
                assessment.downstreamCompletionProbes(),
                System.nanoTime() - startedAtNanos);
    }

    /** Revalidates callback-local state and returns the live mode without attaching it to the root. */
    public AbilitySub apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.MODE || request.getModeContext() == null
                || !request.getCandidates().contains(candidate) || candidate.getModeOrdinal() == null) {
            throw new IllegalArgumentException("Candidate does not belong to this live MODE request");
        }
        final ModeDecisionContext context = request.getModeContext();
        final Assessment current = assess(context.getAbility(), context.getPossible(), context.getMin(),
                context.getMax(), context.isAllowRepeat(), context.getChoosingPlayer());
        if (current.status() != Status.DECISION) {
            throw new IllegalArgumentException("MODE request is no longer completion-safe");
        }
        for (final LegalCandidate live : current.candidates()) {
            if (live.getModeOrdinal().equals(candidate.getModeOrdinal())
                    && live.getSemanticKey().equals(candidate.getSemanticKey())) {
                return live.getMode();
            }
        }
        throw new IllegalArgumentException("Stale MODE candidate is no longer legal");
    }

    private Assessment assess(final SpellAbility root, final List<AbilitySub> possible,
            final int min, final int num, final boolean allowRepeat, final Player choosingPlayer) {
        if (root.getApi() != ApiType.Charm || root.isCopied() || root.isEntwine()) {
            return Assessment.status(Status.NOT_APPLICABLE, null, 1, 0);
        }
        if (!isSupportedShape(root, possible, min, num, allowRepeat, choosingPlayer)) {
            return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.UNSUPPORTED_SHAPE, 1, 0);
        }

        final boolean futureX = !root.isAnnouncing("X") && root.costHasX();
        final List<AbilitySub> original = root.getAdditionalAbilityList("Choices");
        final List<LegalCandidate> candidates = new ArrayList<>();
        int downstreamProbes = 0;
        for (final AbilitySub mode : possible) {
            final int ordinal = identityIndexOf(original, mode);
            if (ordinal < 0) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.CALLBACK_MODE_NOT_IN_CHOICES,
                        possible.size() + 1, downstreamProbes);
            }
            if (futureX && XDecisionProvider.hasXDependentTargeting(mode)) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_TARGET_COMPLETION,
                        possible.size() + 1, downstreamProbes);
            }
            if (hasNestedTargeting(mode)) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_TARGET_COMPLETION,
                        possible.size() + 1, downstreamProbes);
            }
            downstreamProbes++;
            final TargetDecisionProvider.CompletionAssessment target =
                    targetProvider.assessBranchCompletion(mode, root.getActivatingPlayer());
            if (target.getStatus() == TargetDecisionProvider.CompletionStatus.UNSUPPORTED) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_TARGET_COMPLETION,
                        possible.size() + 1, downstreamProbes);
            }
            if (target.getStatus() == TargetDecisionProvider.CompletionStatus.COMPLETE) {
                candidates.add(LegalCandidate.mode(candidates.size(), ordinal, mode));
            }
        }
        if (candidates.isEmpty()) {
            return Assessment.status(Status.INVALID_MODE, null, possible.size() + 1, downstreamProbes);
        }

        downstreamProbes++;
        if (futureX) {
            final XDecisionProvider.DomainAssessment x =
                    xProvider.assessFutureXPaymentDomain(root, root.getActivatingPlayer());
            if (x.getStatus() == XDecisionProvider.Status.INVALID_X) {
                return Assessment.status(Status.INVALID_MODE, null, possible.size() + 1, downstreamProbes);
            }
            if (x.getStatus() != XDecisionProvider.Status.DECISION) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_X_PAYMENT_DOMAIN,
                        possible.size() + 1, downstreamProbes);
            }
        } else {
            final PriorityCostFeasibility.Assessment payment =
                    paymentFeasibility.assessPayment(root.getActivatingPlayer(), root);
            if (payment.getResult() == PriorityCostFeasibility.Result.UNPAYABLE) {
                return Assessment.status(Status.INVALID_MODE, null, possible.size() + 1, downstreamProbes);
            }
            if (payment.getResult() == PriorityCostFeasibility.Result.UNSUPPORTED) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_PAYMENT_SUPPORT,
                        possible.size() + 1, downstreamProbes);
            }
            downstreamProbes++;
            final PaymentDecisionProvider.SupportAssessment support =
                    paymentProvider.assessFuturePaymentSupport(root, root.getActivatingPlayer());
            if (support.getStatus() == PaymentDecisionProvider.SupportStatus.INVALID_PAYMENT) {
                return Assessment.status(Status.INVALID_MODE, null, possible.size() + 1, downstreamProbes);
            }
            if (support.getStatus() != PaymentDecisionProvider.SupportStatus.SUPPORTED) {
                return Assessment.status(Status.UNSUPPORTED, UnsupportedReason.MODE_PAYMENT_SUPPORT,
                        possible.size() + 1, downstreamProbes);
            }
        }
        return new Assessment(Status.DECISION, null, List.copyOf(candidates), possible.size() + 1,
                downstreamProbes);
    }

    private static boolean isSupportedShape(final SpellAbility root, final List<AbilitySub> possible,
            final int min, final int num, final boolean allowRepeat, final Player choosingPlayer) {
        if (root.isTrigger() || root.hasParam("Optional") || root.hasParam("Chooser")
                || root.hasParam("ChoiceRestriction")
                || root.hasParam("Random") || root.hasParam("Pawprint") || min != 1 || num != 1 || allowRepeat
                || root.getActivatingPlayer() == null || !root.getActivatingPlayer().equals(choosingPlayer)
                || root.getSubAbility() != null || root.getHostCard().hasKeyword(Keyword.SPREE)
                || root.getHostCard().hasKeyword(Keyword.TIERED)) {
            return false;
        }
        final String charmNum = root.getParamOrDefault("CharmNum", "1");
        if (!StringUtils.isNumeric(charmNum) || Integer.parseInt(charmNum) != 1) {
            return false;
        }
        if (root.hasParam("MinCharmNum")) {
            final String minCharmNum = root.getParam("MinCharmNum");
            if (!StringUtils.isNumeric(minCharmNum) || Integer.parseInt(minCharmNum) != 1) {
                return false;
            }
        }
        return possible.stream().noneMatch(mode -> mode.hasParam("ModeCost") || mode.hasParam("Pawprint"));
    }

    private static int identityIndexOf(final List<AbilitySub> choices, final AbilitySub candidate) {
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index) == candidate) {
                return index;
            }
        }
        return -1;
    }

    private static boolean hasNestedTargeting(final AbilitySub mode) {
        SpellAbility current = mode.getSubAbility();
        while (current != null) {
            if (current.usesTargeting()) {
                return true;
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private record Assessment(Status status, UnsupportedReason unsupportedReason,
            List<LegalCandidate> candidates, int ruleLegalityProbes, int downstreamCompletionProbes) {
        private static Assessment status(final Status status, final UnsupportedReason unsupportedReason,
                final int ruleLegalityProbes, final int downstreamCompletionProbes) {
            return new Assessment(status, unsupportedReason, List.of(), ruleLegalityProbes,
                    downstreamCompletionProbes);
        }
    }

    public static final class Generation {
        private final Status status;
        private final UnsupportedReason unsupportedReason;
        private final DecisionRequest request;
        private final int ruleLegalityProbes;
        private final int downstreamCompletionProbes;
        private final long generationNanos;

        private Generation(final Status status, final UnsupportedReason unsupportedReason,
                final DecisionRequest request, final int ruleLegalityProbes,
                final int downstreamCompletionProbes, final long generationNanos) {
            this.status = status;
            this.unsupportedReason = unsupportedReason;
            this.request = request;
            this.ruleLegalityProbes = ruleLegalityProbes;
            this.downstreamCompletionProbes = downstreamCompletionProbes;
            this.generationNanos = generationNanos;
        }

        private static Generation of(final Status status, final UnsupportedReason unsupportedReason,
                final DecisionRequest request, final int ruleLegalityProbes,
                final int downstreamCompletionProbes, final long generationNanos) {
            return new Generation(status, unsupportedReason, request, ruleLegalityProbes,
                    downstreamCompletionProbes, generationNanos);
        }

        public Status getStatus() {
            return status;
        }

        public UnsupportedReason getUnsupportedReason() {
            return unsupportedReason;
        }

        public DecisionRequest getRequest() {
            return request;
        }

        public int getRuleLegalityProbes() {
            return ruleLegalityProbes;
        }

        public int getDownstreamCompletionProbes() {
            return downstreamCompletionProbes;
        }

        public long getGenerationNanos() {
            return generationNanos;
        }
    }
}
