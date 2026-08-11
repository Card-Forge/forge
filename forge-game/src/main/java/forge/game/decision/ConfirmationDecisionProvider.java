package forge.game.decision;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.card.CardStateName;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Narrow provider for the FRL-02K-B1 Gelectrode confirmation slice.
 *
 * <p>This class deliberately has no generic optional-trigger predicate. A missing resolver preserves the
 * native teacher callback, while an explicitly installed resolver owns only admitted B1 requests.</p>
 */
public final class ConfirmationDecisionProvider {
    private static final Map<String, String> GELECTRODE_TRIGGER_PARAMS = Map.of(
            "Mode", "SpellCast",
            "ValidCard", "Instant,Sorcery",
            "ValidActivatingPlayer", "You",
            "TriggerZones", "Battlefield",
            "Execute", "TrigUntap",
            "OptionalDecider", "You");
    private static final Map<String, String> GELECTRODE_EFFECT_PARAMS = Map.of(
            "DB", "Untap",
            "Defined", "Self");

    public enum Status {
        ADMITTED,
        UNSUPPORTED_PROFILE,
        UNSUPPORTED_COST,
        UNSUPPORTED_PROVENANCE,
        UNSUPPORTED_HIDDEN,
        UNSUPPORTED_ACTION_CONTINUATION
    }

    @FunctionalInterface
    public interface Resolver {
        LegalCandidate choose(DecisionRequest request);
    }

    private long nextRequestId;
    private DecisionRequest activeRequest;
    private Resolver resolver;
    private long unsupportedCount;
    private boolean choiceMade;
    private LegalCandidate chosenCandidate;

    public void setResolver(final Resolver resolver0) {
        resolver = resolver0;
    }

    public boolean hasResolver() {
        return resolver != null;
    }

    public long getUnsupportedCount() {
        return unsupportedCount;
    }

    public Generation generate(final WrappedAbility wrapper, final Player decider) {
        Objects.requireNonNull(wrapper);
        Objects.requireNonNull(decider);

        if (PriorityActionDiagnostics.hasActiveActionContinuation()) {
            return unsupported(Status.UNSUPPORTED_ACTION_CONTINUATION);
        }
        final Card source = wrapper.getHostCard();
        if (source == null || source.isFaceDown() || !source.getView().canBeShownTo(decider.getView())) {
            return unsupported(Status.UNSUPPORTED_HIDDEN);
        }
        final Trigger trigger = wrapper.getTrigger();
        final Admission admission = classifyProfile(wrapper, trigger, source, decider);
        if (admission.status != Status.ADMITTED) {
            return unsupported(admission.status, admission.reason);
        }
        final Object activator = wrapper.getTriggeringObject(AbilityKey.Activator);
        if (!(activator instanceof Player triggeringPlayer)
                || !source.getController().equals(triggeringPlayer)) {
            return unsupported(Status.UNSUPPORTED_PROFILE);
        }

        final ConfirmationDecisionContext context = new ConfirmationDecisionContext(
                ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF,
                ConfirmationEventType.SPELL_CAST,
                new CardSelectionCard(source), triggeringPlayer.getId(), decider.getId());
        final List<LegalCandidate> candidates = List.of(
                LegalCandidate.confirmation(0, ConfirmationCandidateKind.ACCEPT),
                LegalCandidate.confirmation(1, ConfirmationCandidateKind.DECLINE));
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.CONFIRMATION,
                candidates, context);
        activeRequest = request;
        choiceMade = false;
        chosenCandidate = null;
        return Generation.admitted(request);
    }

    /** Chooses one candidate, either with the explicitly installed resolver or the native teacher callback. */
    public LegalCandidate choose(final DecisionRequest request, final BooleanSupplier nativeTeacher) {
        validateActiveRequest(request);
        if (choiceMade) {
            throw new IllegalArgumentException("Confirmation request already has a chosen candidate");
        }
        Objects.requireNonNull(nativeTeacher);
        final LegalCandidate selected = resolver == null
                ? nativeCandidate(request, nativeTeacher.getAsBoolean()) : resolver.choose(request);
        validateCandidate(request, selected);
        choiceMade = true;
        chosenCandidate = selected;
        return selected;
    }

    /** Applies the request-local boolean meaning of one validated candidate. */
    public boolean apply(final DecisionRequest request, final LegalCandidate candidate,
            final WrappedAbility wrapper) {
        validateActiveRequest(request);
        Objects.requireNonNull(wrapper);
        final ConfirmationDecisionContext context = request.getConfirmationContext();
        if (context == null || context.getProfile() != ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF
                || wrapper.getHostCard() == null
                || context.getSourcePublicIdentity().getCardId() != wrapper.getHostCard().getId()
                || context.getSourcePublicIdentity().getGameTimestamp() != wrapper.getHostCard().getGameTimestamp()
                || wrapper.getDecider() == null
                || context.getDeciderPlayerId() != wrapper.getDecider().getId()) {
            throw new IllegalArgumentException("Confirmation request does not match the live wrapper");
        }
        if (choiceMade && chosenCandidate != candidate) {
            throw new IllegalArgumentException("Applied candidate does not match the chosen confirmation candidate");
        }
        validateCandidate(request, candidate);
        final boolean accepted = candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT;
        activeRequest = null;
        choiceMade = false;
        chosenCandidate = null;
        return accepted;
    }

    private static LegalCandidate nativeCandidate(final DecisionRequest request, final boolean accepted) {
        final ConfirmationCandidateKind expected = accepted
                ? ConfirmationCandidateKind.ACCEPT : ConfirmationCandidateKind.DECLINE;
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getConfirmationKind() == expected)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Native result is not legal for this request"));
    }

    private void validateActiveRequest(final DecisionRequest request) {
        if (request == null || request != activeRequest || request.getDecisionType() != DecisionType.CONFIRMATION
                || request.getConfirmationContext() == null) {
            throw new IllegalArgumentException("Confirmation request is stale or has the wrong type");
        }
    }

    private static void validateCandidate(final DecisionRequest request, final LegalCandidate candidate) {
        if (candidate == null || candidate.getConfirmationKind() == null
                || !request.getCandidates().contains(candidate)) {
            throw new IllegalArgumentException("Candidate is not legal for this confirmation request");
        }
    }

    private Generation unsupported(final Status status) {
        return unsupported(status, status.name());
    }

    private Generation unsupported(final Status status, final String reason) {
        unsupportedCount++;
        return new Generation(status, reason, null);
    }

    private static Admission classifyProfile(final WrappedAbility wrapper, final Trigger trigger,
            final Card source, final Player decider) {
        if (trigger == null) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "NULL_TRIGGER");
        }
        if (trigger.isStatic()) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "STATIC_TRIGGER");
        }
        if (!wrapper.isOptionalTrigger()) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "MANDATORY_TRIGGER");
        }
        final Cost cost = wrapper.getWrappedAbility().getPayCosts();
        if (cost != null && !cost.isFree()) {
            return Admission.of(Status.UNSUPPORTED_COST, "NONZERO_COST");
        }
        if (!trigger.isIntrinsic() || !wrapper.isIntrinsic()) {
            return Admission.of(Status.UNSUPPORTED_PROVENANCE, "UNTRUSTED_PROVENANCE");
        }
        if (source.isCloned()) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "CLONED_SOURCE");
        }
        if (!"Gelectrode".equals(source.getName())) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "CARD_IDENTITY");
        }
        if (source.getCurrentStateName() != CardStateName.Original) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "CARD_STATE");
        }
        if (trigger.getMode() != TriggerType.SpellCast) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "EVENT_TYPE");
        }
        if (trigger.getSpawningAbility() != null) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "DERIVED_LIFECYCLE");
        }
        if (!decider.equals(source.getController())) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "PLAYER_SCOPE");
        }
        if (!GELECTRODE_TRIGGER_PARAMS.equals(normalizedTriggerParams(trigger))
                || !source.hasSVar("TrigUntap")) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "TRIGGER_DEFINITION");
        }
        try {
            if (!GELECTRODE_EFFECT_PARAMS.equals(AbilityFactory.getMapParams(source.getSVar("TrigUntap")))) {
                return Admission.of(Status.UNSUPPORTED_PROFILE, "EFFECT_DEFINITION");
            }
            return matchesLiveEffect(wrapper.getWrappedAbility())
                    ? Admission.of(Status.ADMITTED, "ADMITTED")
                    : Admission.of(Status.UNSUPPORTED_PROFILE, "LIVE_EFFECT_MISMATCH");
        } catch (final RuntimeException ex) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "EFFECT_DEFINITION");
        }
    }

    private static boolean matchesLiveEffect(final SpellAbility liveEffect) {
        return liveEffect != null
                && liveEffect.getApi() == ApiType.Untap
                && GELECTRODE_EFFECT_PARAMS.equals(liveEffect.getMapParams())
                && liveEffect.getSubAbility() == null
                && liveEffect.getAdditionalAbilities().isEmpty()
                && liveEffect.getAdditionalAbilityLists().isEmpty();
    }

    private static Map<String, String> normalizedTriggerParams(final Trigger trigger) {
        final Map<String, String> triggerParams = new HashMap<>(trigger.getOriginalMapParams());
        triggerParams.remove("TriggerDescription");
        return triggerParams;
    }

    public static final class Generation {
        private final Status status;
        private final String reason;
        private final DecisionRequest request;

        private Generation(final Status status, final String reason, final DecisionRequest request) {
            this.status = status;
            this.reason = reason;
            this.request = request;
        }

        private static Generation admitted(final DecisionRequest request) {
            return new Generation(Status.ADMITTED, "ADMITTED", request);
        }

        public Status getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        public DecisionRequest getRequest() {
            return request;
        }
    }

    private static final class Admission {
        private final Status status;
        private final String reason;

        private Admission(final Status status, final String reason) {
            this.status = status;
            this.reason = reason;
        }

        private static Admission of(final Status status, final String reason) {
            return new Admission(status, reason);
        }
    }
}
