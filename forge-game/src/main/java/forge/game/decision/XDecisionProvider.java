package forge.game.decision;

import forge.game.ability.AbilityUtils;
import forge.game.cost.Cost;
import forge.game.cost.CostAdjustment;
import forge.game.cost.CostAdjustmentPreview;
import forge.game.cost.CostPartMana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Range;

/** Produces every completion-safe atomic choice for one Forge player-announced mana X. */
public final class XDecisionProvider {
    public enum Status {
        DECISION,
        INVALID_X,
        NOT_APPLICABLE,
        UNSUPPORTED
    }

    public enum UnsupportedReason {
        UNRESOLVED_MODE,
        NON_MANA_X,
        UNSUPPORTED_X_COST,
        UNSUPPORTED_FINITE_DOMAIN,
        COST_ADJUSTMENT_CHOICE_REQUIRED,
        COST_ADJUSTMENT_UNSUPPORTED,
        TARGET_COMPLETION,
        TARGET_COMPLETION_X_DEPENDENT,
        TARGETING_PLAYER_CHOICE_REQUIRED,
        PAYMENT_FEASIBILITY_UNSUPPORTED,
        DYNAMIC_ANNOUNCEMENT_BOUND,
        PAYMENT_PAYER_UNKNOWN
    }

    private final PriorityCostFeasibility feasibility = new PriorityCostFeasibility();
    private final TargetDecisionProvider targetProvider = new TargetDecisionProvider();
    private long nextRequestId;

    public Generation generateXRequest(final SpellAbility ability, final Player choosingPlayer,
            final ActionContinuation continuation) {
        final long startedAtNanos = System.nanoTime();
        final Domain domain = assessDomain(ability, choosingPlayer);
        if (domain.status() != Status.DECISION) {
            return Generation.of(domain.status(), domain.unsupportedReason(), null,
                    System.nanoTime() - startedAtNanos);
        }
        final Integer subdecisionIndex = continuation == null ? null : continuation.nextSubdecisionIndex();
        final XDecisionContext context = new XDecisionContext(ability.getRootAbility(), choosingPlayer,
                domain.rawMin(), domain.rawMax(), continuation, subdecisionIndex);
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.X_VALUE,
                domain.candidates(), context);
        return Generation.of(Status.DECISION, null, request, System.nanoTime() - startedAtNanos);
    }

    /** Revalidates the complete domain before changing Forge's announced-X state. */
    public void apply(final DecisionRequest request, final LegalCandidate candidate) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(candidate);
        if (request.getDecisionType() != DecisionType.X_VALUE || request.getXContext() == null
                || !request.getCandidates().contains(candidate) || candidate.getXValue() == null) {
            throw new IllegalArgumentException("Candidate does not belong to this live X_VALUE request");
        }
        final XDecisionContext context = request.getXContext();
        final Domain current = assessDomain(context.getAbility(), context.getChoosingPlayer());
        if (current.status() != Status.DECISION || current.candidates().stream()
                .noneMatch(value -> value.getXValue().equals(candidate.getXValue()))) {
            throw new IllegalArgumentException("Stale X candidate is no longer completion-safe");
        }
        context.getAbility().setXManaCostPaid(candidate.getXValue());
    }

    private Domain assessDomain(final SpellAbility suppliedAbility, final Player choosingPlayer) {
        Objects.requireNonNull(suppliedAbility);
        Objects.requireNonNull(choosingPlayer);
        final SpellAbility ability = suppliedAbility.getRootAbility();
        if (ability.isCopied() || ability.isWrapper() || ability.getXManaCostPaid() != null) {
            return Domain.status(Status.NOT_APPLICABLE, null);
        }
        final Cost cost = ability.getPayCosts();
        if (!requiresPlayerX(ability, cost)) {
            return Domain.status(Status.NOT_APPLICABLE, null);
        }
        if (!ability.getAdditionalAbilityList("Choices").isEmpty() && ability.getSubAbility() == null) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.UNRESOLVED_MODE);
        }
        if (cost == null || hasNonManaX(cost)) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.NON_MANA_X);
        }
        if (hasDynamicAnnouncementBound(ability)) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.DYNAMIC_ANNOUNCEMENT_BOUND);
        }
        final Player payer = ability.getActivatingPlayer();
        if (payer == null) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.PAYMENT_PAYER_UNKNOWN);
        }
        final List<CostPartMana> manaParts = cost.getCostParts().stream()
                .filter(CostPartMana.class::isInstance).map(CostPartMana.class::cast).toList();
        if (manaParts.size() != 1 || manaParts.get(0).getAmountOfX() <= 0) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.UNSUPPORTED_X_COST);
        }
        if (hasXDependentTargeting(ability)) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.TARGET_COMPLETION_X_DEPENDENT);
        }
        final TargetDecisionProvider.CompletionAssessment target = targetProvider.assessCompletion(ability);
        if (target.getStatus() == TargetDecisionProvider.CompletionStatus.INVALID_TARGETING) {
            return Domain.status(Status.INVALID_X, null);
        }
        if (target.getStatus() == TargetDecisionProvider.CompletionStatus.UNSUPPORTED) {
            final UnsupportedReason reason = "TARGETING_PLAYER_CHOICE_REQUIRED".equals(target.getUnsupportedReason())
                    ? UnsupportedReason.TARGETING_PLAYER_CHOICE_REQUIRED : UnsupportedReason.TARGET_COMPLETION;
            return Domain.status(Status.UNSUPPORTED, reason);
        }

        final Range<Integer> bounds = AbilityUtils.getAnnouncementBounds(ability, "X");
        final int rawMin = bounds.getMinimum();
        final int rawMax = bounds.getMaximum();
        final PriorityCostFeasibility.CapacityAssessment capacity =
                feasibility.assessManaCapacity(payer, ability);
        if (capacity.getResult() != PriorityCostFeasibility.CapacityResult.SUPPORTED) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);
        }
        final CostAdjustmentPreview adjustment = CostAdjustment.preview(cost, ability, payer,
                false, rawMin, ability.getXColor());
        if (adjustment.getStatus() == CostAdjustmentPreview.Status.CHOICE_REQUIRED) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED);
        }
        if (adjustment.getStatus() != CostAdjustmentPreview.Status.ADJUSTED) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.COST_ADJUSTMENT_UNSUPPORTED);
        }
        if (!adjustment.hasMaximumGenericReductionAllowance()) {
            return Domain.status(Status.UNSUPPORTED, UnsupportedReason.UNSUPPORTED_FINITE_DOMAIN);
        }

        final long allowance = adjustment.getMaximumGenericReductionAllowance();
        final long sum = capacity.getMaximumManaUnits() > Long.MAX_VALUE - allowance
                ? Long.MAX_VALUE : capacity.getMaximumManaUnits() + allowance;
        final int xMultiplicity = manaParts.get(0).getAmountOfX();
        final long paymentUpper = sum / xMultiplicity;
        final long upper = Math.min((long) rawMax, paymentUpper);
        final List<LegalCandidate> candidates = new ArrayList<>();
        for (long value = rawMin; value <= upper; value++) {
            final PriorityCostFeasibility.Assessment payment =
                    feasibility.assessPaymentAtX(payer, ability, (int) value);
            if (payment.getResult() == PriorityCostFeasibility.Result.UNSUPPORTED) {
                final UnsupportedReason reason = payment.getUnsupportedReason()
                        == PriorityCostFeasibility.UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED
                        ? UnsupportedReason.COST_ADJUSTMENT_CHOICE_REQUIRED
                        : UnsupportedReason.PAYMENT_FEASIBILITY_UNSUPPORTED;
                return Domain.status(Status.UNSUPPORTED, reason);
            }
            if (payment.getResult() == PriorityCostFeasibility.Result.PAYABLE) {
                candidates.add(LegalCandidate.xValue(candidates.size(), (int) value));
            }
        }
        return candidates.isEmpty() ? Domain.status(Status.INVALID_X, null)
                : new Domain(Status.DECISION, null, rawMin, rawMax, List.copyOf(candidates));
    }

    private static boolean requiresPlayerX(final SpellAbility ability, final Cost cost) {
        final String announce = ability.getParam("Announce");
        if (announce != null) {
            for (final String variable : announce.split(",")) {
                if ("X".equalsIgnoreCase(variable.trim())) {
                    return true;
                }
            }
        }
        if (cost == null || !cost.hasXInAnyCostPart()) {
            return false;
        }
        final String declared = ability.getParamOrDefault("XAlternative", ability.getSVar("X"));
        return declared.isEmpty() || "Count$xPaid".equals(declared);
    }

    private static boolean hasNonManaX(final Cost cost) {
        return cost.getCostParts().stream().anyMatch(part -> !(part instanceof CostPartMana)
                && "X".equals(part.getAmount()));
    }

    private static boolean hasDynamicAnnouncementBound(final SpellAbility ability) {
        return ability.hasParam("XMax") && !ability.getParam("XMax").matches("\\d+")
                || ability.hasParam("AnnounceMax") && !ability.getParam("AnnounceMax").matches("\\d+");
    }

    private static boolean hasXDependentTargeting(final SpellAbility root) {
        SpellAbility current = root;
        while (current != null) {
            if (current.usesTargeting()) {
                final TargetRestrictions restrictions = current.getTargetRestrictions();
                if (containsX(restrictions.getMinTargets()) || containsX(restrictions.getMaxTargets())) {
                    return true;
                }
                for (final String valid : restrictions.getValidTgts()) {
                    if (containsX(valid)) {
                        return true;
                    }
                }
            }
            current = current.getSubAbility();
        }
        return false;
    }

    private static boolean containsX(final String value) {
        return value != null && (value.equals("X") || value.contains("$X") || value.contains("EQX")
                || value.contains("LTX") || value.contains("GTX"));
    }

    private record Domain(Status status, UnsupportedReason unsupportedReason, int rawMin, int rawMax,
            List<LegalCandidate> candidates) {
        private static Domain status(final Status status, final UnsupportedReason reason) {
            return new Domain(status, reason, 0, 0, List.of());
        }
    }

    public static final class Generation {
        private final Status status;
        private final UnsupportedReason unsupportedReason;
        private final DecisionRequest request;
        private final long generationNanos;

        private Generation(final Status status, final UnsupportedReason unsupportedReason,
                final DecisionRequest request, final long generationNanos) {
            this.status = status;
            this.unsupportedReason = unsupportedReason;
            this.request = request;
            this.generationNanos = generationNanos;
        }

        private static Generation of(final Status status, final UnsupportedReason reason,
                final DecisionRequest request, final long generationNanos) {
            return new Generation(status, reason, request, generationNanos);
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

        public long getGenerationNanos() {
            return generationNanos;
        }
    }
}
