package forge.game.decision;

import forge.card.CardStateName;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Narrow provider for the FRL-02K-B1 Gelectrode and FRL-02K-D1 Blood confirmation slices.
 *
 * <p>A missing resolver preserves the native teacher callback, while an explicitly installed resolver
 * owns only an admitted request. Resolver ownership is captured with the request so later controller
 * mutation cannot change the owner of an already generated decision.</p>
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
    private static final String BLOOD_TARGET_UNSUPPORTED = "TARGET_A_UNSUPPORTED";
    private static final String EXTERNAL_CANDIDATE_INVALID = "EXTERNAL_CANDIDATE_INVALID";
    private static final String TARGET_A_INTEGRITY = "TARGET_A_INTEGRITY_FAILURE";
    private static final String NATIVE_MAPPING = "NATIVE_MAPPING_FAILED";

    public enum Status {
        ADMITTED,
        UNSUPPORTED_PROFILE,
        UNSUPPORTED_COST,
        UNSUPPORTED_PROVENANCE,
        UNSUPPORTED_HIDDEN,
        UNSUPPORTED_ACTION_CONTINUATION,
        INVALID_EXTERNAL_CANDIDATE,
        NATIVE_MAPPING_FAILED,
        TARGET_A_INTEGRITY_FAILURE
    }

    @FunctionalInterface
    public interface Resolver {
        LegalCandidate choose(DecisionRequest request);
    }

    private long nextRequestId;
    private DecisionRequest activeRequest;
    private Resolver resolver;
    private Resolver activeResolver;
    private long unsupportedCount;
    private boolean choiceMade;
    private LegalCandidate chosenCandidate;

    public void setResolver(final Resolver resolver0) {
        resolver = resolver0;
    }

    public boolean hasResolver() {
        return resolver != null;
    }

    /** Returns the owner captured for this active request without exposing the resolver itself. */
    public boolean isExternalOwner(final DecisionRequest request) {
        return request != null && request == activeRequest && activeResolver != null
                && request.getDecisionType() == DecisionType.CONFIRMATION
                && request.getConfirmationContext() != null;
    }

    public long getUnsupportedCount() {
        return unsupportedCount;
    }

    public Generation generate(final WrappedAbility wrapper, final Player decider) {
        Objects.requireNonNull(wrapper);
        Objects.requireNonNull(decider);
        clearActiveState();

        if (PriorityActionDiagnostics.hasActiveActionContinuation()) {
            return unsupported(Status.UNSUPPORTED_ACTION_CONTINUATION);
        }

        final Card source;
        final Trigger trigger;
        try {
            source = wrapper.getHostCard();
            trigger = wrapper.getTrigger();
        } catch (final RuntimeException ex) {
            return unsupported(Status.UNSUPPORTED_HIDDEN);
        }
        try {
            if (source == null || source.isFaceDown() || source.getView() == null || decider.getView() == null
                    || !source.getView().canBeShownTo(decider.getView())) {
                return unsupported(Status.UNSUPPORTED_HIDDEN);
            }
        } catch (final RuntimeException ex) {
            return unsupported(Status.UNSUPPORTED_HIDDEN);
        }

        final Admission admission;
        try {
            admission = classifyProfile(wrapper, trigger, source, decider);
        } catch (final RuntimeException ex) {
            return unsupported(Status.UNSUPPORTED_PROFILE);
        }
        if (admission.status != Status.ADMITTED) {
            return unsupported(admission.status, admission.reason);
        }

        final ConfirmationDecisionContext context;
        if (admission.profile == ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD) {
            if (!bloodOwnershipMatches(wrapper, source, decider)) {
                return unsupported(Status.UNSUPPORTED_PROFILE, "PLAYER_SCOPE");
            }
            final TargetCapture target = captureBloodTarget(wrapper, decider);
            if (!target.admitted) {
                return unsupported(Status.UNSUPPORTED_PROFILE, BLOOD_TARGET_UNSUPPORTED);
            }
            context = new ConfirmationDecisionContext(
                    ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD,
                    ConfirmationEventType.CHANGES_ZONE,
                    new CardSelectionCard(source), target.projection, null, decider.getId());
        } else {
            final Object activator;
            try {
                activator = wrapper.getTriggeringObject(AbilityKey.Activator);
            } catch (final RuntimeException ex) {
                return unsupported(Status.UNSUPPORTED_PROFILE);
            }
            if (!(activator instanceof Player triggeringPlayer)
                    || !source.getController().equals(triggeringPlayer)) {
                return unsupported(Status.UNSUPPORTED_PROFILE);
            }
            context = new ConfirmationDecisionContext(
                    ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF,
                    ConfirmationEventType.SPELL_CAST,
                    new CardSelectionCard(source), triggeringPlayer.getId(), decider.getId());
        }

        final List<LegalCandidate> candidates = List.of(
                LegalCandidate.confirmation(0, ConfirmationCandidateKind.ACCEPT),
                LegalCandidate.confirmation(1, ConfirmationCandidateKind.DECLINE));
        final DecisionRequest request = new DecisionRequest(nextRequestId++, DecisionType.CONFIRMATION,
                candidates, context);
        activeRequest = request;
        activeResolver = resolver;
        choiceMade = false;
        chosenCandidate = null;
        return Generation.admitted(request);
    }

    /** Chooses one candidate using the resolver captured when this request was generated, or native Forge. */
    public LegalCandidate choose(final DecisionRequest request, final BooleanSupplier nativeTeacher) {
        validateActiveRequest(request);
        if (choiceMade) {
            throw new IllegalArgumentException("Confirmation request already has a chosen candidate");
        }

        final Resolver requestResolver = activeResolver;
        if (requestResolver != null) {
            final LegalCandidate selected;
            try {
                selected = requestResolver.choose(request);
                if (request != activeRequest || requestResolver != activeResolver) {
                    throw new IllegalStateException("resolver changed active request");
                }
                validateCandidate(request, selected);
            } catch (final RuntimeException ex) {
                clearActiveState();
                throw invalidExternalCandidate();
            }
            choiceMade = true;
            chosenCandidate = selected;
            return selected;
        }

        try {
            Objects.requireNonNull(nativeTeacher);
            final boolean accepted = nativeTeacher.getAsBoolean();
            if (request != activeRequest) {
                clearActiveState();
                throw new IllegalArgumentException("Confirmation request was invalidated during native resolution");
            }
            final LegalCandidate selected = nativeCandidate(request, accepted);
            validateCandidate(request, selected);
            choiceMade = true;
            chosenCandidate = selected;
            return selected;
        } catch (final RuntimeException ex) {
            clearActiveState();
            throw ex;
        }
    }

    /** Applies the request-local boolean meaning of one validated candidate. */
    public boolean apply(final DecisionRequest request, final LegalCandidate candidate,
            final WrappedAbility wrapper) {
        validateActiveRequest(request);
        Objects.requireNonNull(wrapper);
        final ConfirmationDecisionContext context = request.getConfirmationContext();
        if (context == null) {
            throw new IllegalArgumentException("Confirmation request does not match the live wrapper");
        }
        if (choiceMade && chosenCandidate != candidate) {
            throw new IllegalArgumentException("Applied candidate does not match the chosen confirmation candidate");
        }
        validateCandidate(request, candidate);

        if (context.getProfile() == ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD) {
            try {
                validateBloodRequest(context, wrapper);
            } catch (final RuntimeException ex) {
                clearActiveState();
                throw ex;
            }
            if (!matchesLiveBloodTarget(context, wrapper)) {
                final boolean external = activeResolver != null;
                clearActiveState();
                throw new UnsupportedConfirmationDecisionException(
                        external ? Status.TARGET_A_INTEGRITY_FAILURE : Status.NATIVE_MAPPING_FAILED,
                        external ? TARGET_A_INTEGRITY : NATIVE_MAPPING);
            }
        } else if (context.getProfile() != ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF
                || context.getEvent() != ConfirmationEventType.SPELL_CAST
                || wrapper.getHostCard() == null
                || context.getSourcePublicIdentity().getCardId() != wrapper.getHostCard().getId()
                || context.getSourcePublicIdentity().getGameTimestamp() != wrapper.getHostCard().getGameTimestamp()
                || wrapper.getDecider() == null
                || context.getDeciderPlayerId() != wrapper.getDecider().getId()) {
            throw new IllegalArgumentException("Confirmation request does not match the live wrapper");
        }

        final boolean accepted = candidate.getConfirmationKind() == ConfirmationCandidateKind.ACCEPT;
        clearActiveState();
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

    private static UnsupportedConfirmationDecisionException invalidExternalCandidate() {
        return new UnsupportedConfirmationDecisionException(Status.INVALID_EXTERNAL_CANDIDATE,
                EXTERNAL_CANDIDATE_INVALID);
    }

    private Generation unsupported(final Status status) {
        return unsupported(status, status.name());
    }

    private Generation unsupported(final Status status, final String reason) {
        clearActiveState();
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
        final SpellAbility liveAbility = wrapper.getWrappedAbility();
        final Cost cost = liveAbility == null ? null : liveAbility.getPayCosts();
        if (cost != null && !cost.isFree()) {
            return Admission.of(Status.UNSUPPORTED_COST, "NONZERO_COST");
        }
        if ("Blood Operative".equals(source.getName())) {
            return classifyBlood(wrapper);
        }
        return classifyGelectrode(wrapper, trigger, source, decider);
    }

    private static Admission classifyBlood(final WrappedAbility wrapper) {
        final BloodOperativeEtbProfile.Validation validation =
                BloodOperativeEtbProfile.validateCommonSemanticProfile(wrapper);
        if (!validation.isAdmitted()) {
            return mapBloodFailure(validation.getFailure());
        }
        return Admission.admitted(ConfirmationTriggerProfile.BLOOD_OPERATIVE_ETB_EXILE_GRAVEYARD_CARD);
    }

    private static Admission mapBloodFailure(final BloodOperativeEtbProfile.Failure failure) {
        if (failure == BloodOperativeEtbProfile.Failure.SOURCE_PROVENANCE) {
            return Admission.of(Status.UNSUPPORTED_PROVENANCE, "UNTRUSTED_PROVENANCE");
        }
        if (failure == BloodOperativeEtbProfile.Failure.TARGETING_SHAPE) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "LIVE_EFFECT_MISMATCH");
        }
        if (failure == BloodOperativeEtbProfile.Failure.STATIC_EFFECT_DEFINITION) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "EFFECT_DEFINITION");
        }
        if (failure == BloodOperativeEtbProfile.Failure.LIVE_EFFECT_DEFINITION) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "LIVE_EFFECT_DEFINITION");
        }
        if (failure == BloodOperativeEtbProfile.Failure.SOURCE_STATE) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "CARD_STATE");
        }
        if (failure == BloodOperativeEtbProfile.Failure.TRIGGER_DEFINITION) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "TRIGGER_DEFINITION");
        }
        if (failure == BloodOperativeEtbProfile.Failure.SOURCE_IDENTITY) {
            return Admission.of(Status.UNSUPPORTED_PROFILE, "CARD_IDENTITY");
        }
        return Admission.of(Status.UNSUPPORTED_PROFILE, "UNSUPPORTED_PROFILE");
    }

    private static Admission classifyGelectrode(final WrappedAbility wrapper, final Trigger trigger,
            final Card source, final Player decider) {
        if (!trigger.isIntrinsic() || !wrapper.isIntrinsic()
                || wrapper.getWrappedAbility() == null || !wrapper.getWrappedAbility().isIntrinsic()) {
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
                    ? Admission.admitted(ConfirmationTriggerProfile.GELECTRODE_SPELL_CAST_UNTAP_SELF)
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

    private static boolean bloodOwnershipMatches(final WrappedAbility wrapper, final Card source,
            final Player decider) {
        try {
            final SpellAbility liveAbility = wrapper.getWrappedAbility();
            return samePlayer(decider, wrapper.getDecider())
                    && samePlayer(decider, liveAbility == null ? null : liveAbility.getActivatingPlayer())
                    && samePlayer(decider, source.getController());
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    private static TargetCapture captureBloodTarget(final WrappedAbility wrapper, final Player decider) {
        try {
            final SpellAbility liveAbility = wrapper.getWrappedAbility();
            final List<GameObject> targets = liveAbility == null ? null : liveAbility.getTargets();
            if (targets == null || decider.getView() == null || targets.size() != 1) {
                return TargetCapture.rejected();
            }
            final GameObject targetObject = targets.get(0);
            if (!(targetObject instanceof Card target) || target.getZone() == null
                    || target.getZone().getZoneType() != ZoneType.Graveyard || target.isFaceDown()
                    || target.getView() == null || !target.getView().canBeShownTo(decider.getView())) {
                return TargetCapture.rejected();
            }
            final CardSelectionCard projection = new CardSelectionCard(target);
            return isProjectable(projection) ? TargetCapture.admitted(projection) : TargetCapture.rejected();
        } catch (final RuntimeException ex) {
            return TargetCapture.rejected();
        }
    }

    private static boolean matchesLiveBloodTarget(final ConfirmationDecisionContext context,
            final WrappedAbility wrapper) {
        try {
            final Player decider = wrapper.getDecider();
            final SpellAbility liveAbility = wrapper.getWrappedAbility();
            final List<GameObject> targets = liveAbility == null ? null : liveAbility.getTargets();
            if (decider == null || decider.getView() == null || targets == null || targets.size() != 1) {
                return false;
            }
            final GameObject targetObject = targets.get(0);
            if (!(targetObject instanceof Card target) || target.getZone() == null
                    || target.getZone().getZoneType() != ZoneType.Graveyard || target.isFaceDown()
                    || target.getView() == null || !target.getView().canBeShownTo(decider.getView())) {
                return false;
            }
            final CardSelectionCard expected = context.getTargetPublicIdentity();
            return expected != null && isProjectable(expected)
                    && sameProjection(expected, new CardSelectionCard(target));
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    private static void validateBloodRequest(final ConfirmationDecisionContext context,
            final WrappedAbility wrapper) {
        try {
            final Card source = wrapper.getHostCard();
            final Player decider = wrapper.getDecider();
            if (context.getEvent() != ConfirmationEventType.CHANGES_ZONE
                    || context.getTargetPublicIdentity() == null
                    || context.getTriggeringPlayerId() != null
                    || source == null || decider == null
                    || context.getSourcePublicIdentity() == null
                    || context.getDeciderPlayerId() != decider.getId()
                    || !isProjectable(context.getSourcePublicIdentity())
                    || !sameProjection(context.getSourcePublicIdentity(), new CardSelectionCard(source))) {
                throw new IllegalArgumentException("Confirmation request does not match the live wrapper");
            }
        } catch (final IllegalArgumentException ex) {
            throw ex;
        } catch (final RuntimeException ex) {
            throw new IllegalArgumentException("Confirmation request does not match the live wrapper");
        }
    }

    private static boolean samePlayer(final Player first, final Player second) {
        return first != null && second != null && (first.equals(second) || first.getId() == second.getId());
    }

    private static boolean isProjectable(final CardSelectionCard projection) {
        return projection != null && projection.getVisibleName() != null && projection.getZone() != null
                && projection.getOwnerId() >= 0 && projection.getControllerId() >= 0;
    }

    private static boolean sameProjection(final CardSelectionCard expected, final CardSelectionCard actual) {
        return expected != null && actual != null
                && expected.getCardId() == actual.getCardId()
                && expected.getGameTimestamp() == actual.getGameTimestamp()
                && Objects.equals(expected.getVisibleName(), actual.getVisibleName())
                && expected.getZone() == actual.getZone()
                && expected.getOwnerId() == actual.getOwnerId()
                && expected.getControllerId() == actual.getControllerId();
    }

    private void clearActiveState() {
        activeRequest = null;
        activeResolver = null;
        choiceMade = false;
        chosenCandidate = null;
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
        private final ConfirmationTriggerProfile profile;

        private Admission(final Status status0, final String reason0,
                final ConfirmationTriggerProfile profile0) {
            status = status0;
            reason = reason0;
            profile = profile0;
        }

        private static Admission of(final Status status, final String reason) {
            return new Admission(status, reason, null);
        }

        private static Admission admitted(final ConfirmationTriggerProfile profile) {
            return new Admission(Status.ADMITTED, "ADMITTED", profile);
        }
    }

    private static final class TargetCapture {
        private final boolean admitted;
        private final CardSelectionCard projection;

        private TargetCapture(final boolean admitted0, final CardSelectionCard projection0) {
            admitted = admitted0;
            projection = projection0;
        }

        private static TargetCapture admitted(final CardSelectionCard projection) {
            return new TargetCapture(true, projection);
        }

        private static TargetCapture rejected() {
            return new TargetCapture(false, null);
        }
    }
}
