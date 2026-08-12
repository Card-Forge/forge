package forge.game.decision;

import forge.card.CardStateName;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stateless admission boundary for the narrow FRL-02K-C2A triggered-target slice.
 *
 * <p>Task 5 deliberately admits the exact Blood Operative profile. Task 6 owns request generation,
 * external application, and native-result mapping without duplicating Forge's legality oracle.</p>
 */
public final class TriggeredTargetDecisionCoordinator {
    private static final String BLOOD_OPERATIVE = "Blood Operative";
    private static final String BLOOD_TRIGGER = "TrigChangeZone";

    private static final Map<String, String> BLOOD_TRIGGER_PARAMS = Map.of(
            "Mode", "ChangesZone",
            "Origin", "Any",
            "Destination", "Battlefield",
            "ValidCard", "Card.Self",
            "OptionalDecider", "You",
            "Execute", BLOOD_TRIGGER);
    private static final Map<String, String> BLOOD_EFFECT_PARAMS = Map.of(
            "DB", "ChangeZone",
            "Origin", "Graveyard",
            "Destination", "Exile",
            "ValidTgts", "Card");
    private static final Set<String> BLOOD_STATIC_EFFECT_PARAMS = Set.of(
            "DB", "Origin", "Destination", "ValidTgts", "TgtPrompt", "ValidTgtsDesc");
    private static final Set<String> BLOOD_LIVE_EFFECT_PARAMS = Set.of(
            "DB", "Origin", "Destination", "ValidTgts", "TgtPrompt", "ValidTgtsDesc",
            "TgtZone", "TargetMin", "TargetMax");

    public enum Classification {
        NOT_APPLICABLE,
        ADMITTED,
        UNSUPPORTED_TARGETED_TRIGGER
    }

    public enum PreparationStatus {
        NATIVE,
        NATIVE_WITH_TEACHER_CAPTURE,
        NATIVE_UNSUPPORTED_TARGETED_TRIGGER,
        PREPARED,
        NO_STACK,
        NOT_APPLICABLE
    }

    /** Classifies only the queued ability's trigger family; it does not generate a target request. */
    public Classification classify(final SpellAbility queuedAbility) {
        return evaluate(queuedAbility, implicitChooser(queuedAbility)).classification;
    }

    /** Returns the native trigger classification without recursing through a cyclic parent graph. */
    public boolean isTriggerForRouting(final SpellAbility ability) {
        return ability != null && !isUnclassifiableForRouting(ability) && ability.isTrigger();
    }

    /**
     * Returns whether native routing cannot classify this ability without recursing through a malformed parent graph.
     * A resolver-null caller preserves native ownership by dropping this unclassifiable route rather than treating it
     * as an ordinary non-trigger and inserting it onto the stack.
     */
    public boolean isUnclassifiableForRouting(final SpellAbility ability) {
        return ability != null && hasCyclicParentChain(ability);
    }

    /**
     * Prepares the narrow boundary and routes one atomic target decision through the provider seam.
     *
     * <p>A wrapped ability supplies its trigger decider when this adapter is used by later routing.</p>
     */
    public Preparation prepare(final SpellAbility queuedAbility, final TargetDecisionProvider provider,
            final TargetDecisionProvider.Resolver resolver) {
        return prepareInternal(queuedAbility, implicitChooser(queuedAbility), provider, resolver);
    }

    /** Four-argument compatibility overload used by the current RED tests and later controller routing. */
    public Preparation prepare(final SpellAbility queuedAbility, final Player chooser,
            final TargetDecisionProvider provider, final TargetDecisionProvider.Resolver resolver) {
        return prepareInternal(queuedAbility, chooser, provider, resolver);
    }

    /** Thin adapter preserving the wrapped-ability call shape. */
    public Preparation prepare(final WrappedAbility wrapper, final Player chooser,
            final TargetDecisionProvider provider, final TargetDecisionProvider.Resolver resolver) {
        return prepare((SpellAbility) wrapper, chooser, provider, resolver);
    }

    /** Completes the native callback and maps its live result to the captured request. */
    public boolean completeNative(final Preparation preparation, final boolean nativeResult) {
        requireNativePreparation(preparation);
        if (!nativeResult) {
            preparation.traceHandle.recordMappingFailed();
            throw mappingFailed();
        }

        final List<GameObject> afterTargets;
        try {
            afterTargets = List.copyOf(preparation.liveAbility.getTargets());
        } catch (final RuntimeException ex) {
            return failNativeMapping(preparation.traceHandle);
        }
        final List<GameObject> newTargets = new ArrayList<>();
        for (final GameObject afterTarget : afterTargets) {
            if (!containsByIdentity(preparation.beforeTargets, afterTarget)) {
                newTargets.add(afterTarget);
            }
        }
        if (newTargets.size() != 1) {
            return failNativeMapping(preparation.traceHandle);
        }

        final GameObject mappedTarget = newTargets.get(0);
        LegalCandidate mappedCandidate = null;
        int matchingCandidates = 0;
        for (final LegalCandidate candidate : preparation.request.getCandidates()) {
            if (isTargetCardCandidate(candidate) && candidate.getTarget() == mappedTarget) {
                mappedCandidate = candidate;
                matchingCandidates++;
            }
        }
        if (matchingCandidates != 1) {
            return failNativeMapping(preparation.traceHandle);
        }

        preparation.traceHandle.recordNativeMappedResult(mappedCandidate);
        return true;
    }

    /**
     * Enforces external ownership for a queued ability while preserving the native path when no resolver exists.
     * The exact admitted profile is also continuation-gated even when the resolver is null.
     */
    public void enforceExternalTargetBoundary(final SpellAbility queuedAbility,
            final TargetDecisionProvider.Resolver resolver) {
        enforceExternalTargetBoundary(queuedAbility, resolver, false);
    }

    /**
     * Enforces the boundary for a queued ability or a generic child reached from one.
     *
     * <p>Generic additional children do not inherit the trigger through their parent edge, so a
     * targeted child is still rejected when external ownership is active even though it is not
     * independently classified as a trigger. Standalone non-triggers remain not applicable.</p>
     */
    public void enforceExternalTargetBoundary(final SpellAbility queuedAbility,
            final TargetDecisionProvider.Resolver resolver, final boolean triggeredAncestor) {
        if (queuedAbility == null) {
            return;
        }

        if (resolver != null && hasCyclicParentChain(queuedAbility)) {
            throw unsupportedProfile();
        }

        final Admission admission = evaluate(queuedAbility, implicitChooser(queuedAbility));
        if (admission.classification == Classification.NOT_APPLICABLE) {
            if (triggeredAncestor && resolver != null && isTargeted(queuedAbility)) {
                throw unsupportedProfile();
            }
            return;
        }
        if (admission.classification == Classification.ADMITTED) {
            rejectActiveContinuation();
            return;
        }
        if (resolver != null) {
            throw unsupported(admission);
        }
    }

    private static Preparation prepareInternal(final SpellAbility queuedAbility, final Player chooser,
            final TargetDecisionProvider provider, final TargetDecisionProvider.Resolver resolver) {
        if (queuedAbility == null) {
            return Preparation.of(PreparationStatus.NO_STACK, "NO_STACK");
        }

        if (resolver != null && hasCyclicParentChain(queuedAbility)) {
            throw unsupportedProfile();
        }

        final Admission admission = evaluate(queuedAbility, chooser);
        if (admission.classification == Classification.NOT_APPLICABLE) {
            return Preparation.of(PreparationStatus.NOT_APPLICABLE, admission.reason());
        }
        if (admission.classification == Classification.ADMITTED) {
            rejectActiveContinuation();

            final WrappedAbility wrapper = (WrappedAbility) queuedAbility;
            final SpellAbility liveAbility = wrapper.getWrappedAbility();
            final TargetDecisionProvider.Generation generation;
            try {
                generation = Objects.requireNonNull(provider, "provider")
                        .generateTargetRequest(liveAbility, chooser, null);
            } catch (final TriggeredTargetIntegrityException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw targetApplicationIncomplete();
            }
            if (generation == null) {
                throw targetApplicationIncomplete();
            }
            if (generation.getStatus() == TargetDecisionProvider.Status.INVALID_TARGETING) {
                return Preparation.of(resolver == null ? PreparationStatus.NATIVE : PreparationStatus.NO_STACK,
                        TargetDecisionProvider.Status.INVALID_TARGETING.name());
            }
            if (generation.getStatus() != TargetDecisionProvider.Status.DECISION
                    || generation.getRequest() == null) {
                throw targetApplicationIncomplete();
            }

            final DecisionRequest request = generation.getRequest();
            final List<GameObject> beforeTargets = List.copyOf(liveAbility.getTargets());
            final DeterminismTrace.RequestHandle traceHandle = DeterminismTrace.recordRequest(
                    liveAbility.getHostCard().getGame(), chooser.getId(), request, "TRIGGERED_TARGET", 0);
            final Preparation preparation = Preparation.forRequest(
                    resolver == null ? PreparationStatus.NATIVE_WITH_TEACHER_CAPTURE : PreparationStatus.PREPARED,
                    admission.reason(), request, liveAbility, beforeTargets, traceHandle, resolver != null);
            if (resolver == null) {
                return preparation;
            }
            return prepareExternal(preparation, provider, resolver);
        }
        if (resolver != null) {
            throw unsupported(admission);
        }
        return Preparation.of(PreparationStatus.NATIVE_UNSUPPORTED_TARGETED_TRIGGER, admission.reason());
    }

    private static Preparation prepareExternal(final Preparation preparation,
            final TargetDecisionProvider provider, final TargetDecisionProvider.Resolver resolver) {
        final DecisionRequest request = preparation.request;
        if (request.isForced()) {
            if (request.getCandidates().size() != 1) {
                throw targetApplicationIncomplete();
            }
            final LegalCandidate selected = request.getCandidates().get(0);
            final TargetDecisionProvider.Generation applied;
            try {
                applied = provider.apply(request, selected);
            } catch (final TriggeredTargetIntegrityException ex) {
                throw ex;
            } catch (final RuntimeException ex) {
                throw targetApplicationIncomplete();
            }
            requireComplete(applied);
            requireExactlyOneLiveTarget(preparation.liveAbility, selected);
            preparation.traceHandle.recordEngineForced();
            return preparation;
        }

        final LegalCandidate selected;
        try {
            selected = resolver.resolve(request);
        } catch (final RuntimeException ex) {
            throw invalidExternalCandidate();
        }
        if (!isValidExternalCandidate(request, selected)) {
            throw invalidExternalCandidate();
        }

        final TargetDecisionProvider.Generation applied;
        try {
            applied = provider.apply(request, selected);
        } catch (final RuntimeException ex) {
            throw invalidExternalCandidate();
        }
        requireComplete(applied);
        requireExactlyOneLiveTarget(preparation.liveAbility, selected);
        preparation.traceHandle.recordExternalChosenResult(selected);
        return preparation;
    }

    private static boolean isValidExternalCandidate(final DecisionRequest request,
            final LegalCandidate candidate) {
        return candidate != null && request.getCandidates().contains(candidate)
                && isTargetCardCandidate(candidate);
    }

    private static boolean isTargetCardCandidate(final LegalCandidate candidate) {
        return candidate != null && candidate.getTargetKind() == TargetCandidateKind.TARGET_CARD
                && candidate.getTarget() != null;
    }

    private static void requireComplete(final TargetDecisionProvider.Generation generation) {
        if (generation == null || generation.getStatus() != TargetDecisionProvider.Status.COMPLETE) {
            throw targetApplicationIncomplete();
        }
    }

    private static void requireExactlyOneLiveTarget(final SpellAbility liveAbility,
            final LegalCandidate selected) {
        final List<GameObject> liveTargets = copyTargets(liveAbility);
        final GameObject selectedTarget = selected.getTarget();
        int matchingTargets = 0;
        for (final GameObject liveTarget : liveTargets) {
            if (liveTarget == selectedTarget) {
                matchingTargets++;
            }
        }
        if (selectedTarget == null || liveTargets.size() != 1 || matchingTargets != 1) {
            throw targetApplicationIncomplete();
        }
    }

    private static List<GameObject> copyTargets(final SpellAbility liveAbility) {
        try {
            return List.copyOf(liveAbility.getTargets());
        } catch (final RuntimeException ex) {
            throw targetApplicationIncomplete();
        }
    }

    private static boolean containsByIdentity(final List<GameObject> targets, final GameObject target) {
        for (final GameObject existing : targets) {
            if (existing == target) {
                return true;
            }
        }
        return false;
    }

    private static void requireNativePreparation(final Preparation preparation) {
        Objects.requireNonNull(preparation, "preparation");
        if (preparation.status != PreparationStatus.NATIVE_WITH_TEACHER_CAPTURE
                || preparation.request == null || preparation.liveAbility == null
                || preparation.beforeTargets == null || preparation.traceHandle == null
                || preparation.resolverOwned) {
            throw new IllegalArgumentException("Preparation does not own a native target capture");
        }
    }

    private static boolean failNativeMapping(final DeterminismTrace.RequestHandle traceHandle) {
        traceHandle.recordMappingFailed();
        throw mappingFailed();
    }

    private static TriggeredTargetIntegrityException invalidExternalCandidate() {
        return new TriggeredTargetIntegrityException(
                TriggeredTargetIntegrityException.Reason.INVALID_EXTERNAL_CANDIDATE);
    }

    private static TriggeredTargetIntegrityException targetApplicationIncomplete() {
        return new TriggeredTargetIntegrityException(
                TriggeredTargetIntegrityException.Reason.TARGET_APPLICATION_INCOMPLETE);
    }

    private static TriggeredTargetIntegrityException mappingFailed() {
        return new TriggeredTargetIntegrityException(TriggeredTargetIntegrityException.Reason.MAPPING_FAILED);
    }

    private static TriggeredTargetIntegrityException unsupportedProfile() {
        return new TriggeredTargetIntegrityException(
                TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
    }

    private static boolean isTargeted(final SpellAbility ability) {
        try {
            return ability.usesTargeting();
        } catch (final RuntimeException ex) {
            throw unsupportedProfile();
        }
    }

    private static Admission evaluate(final SpellAbility queuedAbility, final Player chooser) {
        if (queuedAbility == null) {
            return Admission.notApplicable();
        }
        if (hasCyclicParentChain(queuedAbility)) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }

        final Trigger trigger;
        try {
            trigger = queuedAbility.getTrigger();
            if (trigger == null) {
                return Admission.notApplicable();
            }
            if (!queuedAbility.usesTargeting()) {
                return Admission.notApplicable();
            }
        } catch (final RuntimeException ex) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }

        if (!(queuedAbility instanceof WrappedAbility wrapper)) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }

        try {
            return admitBlood(wrapper, chooser, trigger);
        } catch (final RuntimeException ex) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
    }

    private static boolean hasCyclicParentChain(final SpellAbility queuedAbility) {
        final Set<SpellAbility> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        if (hasCyclicParentChain(queuedAbility, visited)) {
            return true;
        }
        return queuedAbility instanceof WrappedAbility wrapper
                && hasCyclicParentChain(wrapper.getWrappedAbility(), visited);
    }

    private static boolean hasCyclicParentChain(final SpellAbility start,
            final Set<SpellAbility> visited) {
        SpellAbility current = start;
        while (current != null) {
            if (!visited.add(current)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static Admission admitBlood(final WrappedAbility wrapper, final Player chooser,
            final Trigger trigger) {
        final SpellAbility liveAbility = wrapper.getWrappedAbility();
        final Player decider = wrapper.getDecider();
        final Card source = wrapper.getHostCard();
        if (liveAbility == null || trigger == null || source == null || chooser == null || decider == null) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (!BLOOD_OPERATIVE.equals(source.getName())
                || source.getCurrentStateName() != CardStateName.Original
                || source.isCloned()) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (source.isFaceDown()
                || source.getView() == null
                || chooser.getView() == null
                || decider.getView() == null
                || !source.getView().canBeShownTo(chooser.getView())
                || !source.getView().canBeShownTo(decider.getView())) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (!trigger.isIntrinsic() || trigger.isStatic() || trigger.getMode() != TriggerType.ChangesZone
                || trigger.getSpawningAbility() != null || wrapper.isCopied()
                || liveAbility.isCopied() || !wrapper.isIntrinsic() || !liveAbility.isIntrinsic()) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (!samePlayer(chooser, decider)
                || !samePlayer(decider, liveAbility.getActivatingPlayer())
                || !samePlayer(decider, source.getController())) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (!BLOOD_TRIGGER_PARAMS.equals(normalize(trigger.getOriginalMapParams(), "TriggerDescription"))
                || !BLOOD_TRIGGER_PARAMS.equals(normalize(trigger.getMapParams(), "TriggerDescription"))) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (!matchesStaticEffect(source)) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }

        return matchesLiveEffect(liveAbility);
    }

    private static boolean matchesStaticEffect(final Card source) {
        if (!source.hasSVar(BLOOD_TRIGGER)) {
            return false;
        }
        try {
            final Map<String, String> params = AbilityFactory.getMapParams(source.getSVar(BLOOD_TRIGGER));
            if (!BLOOD_STATIC_EFFECT_PARAMS.containsAll(params.keySet())) {
                return false;
            }
            return BLOOD_EFFECT_PARAMS.equals(normalize(params, "TgtPrompt", "ValidTgtsDesc"));
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    private static Admission matchesLiveEffect(final SpellAbility liveAbility) {
        final Map<String, String> params = liveAbility.getMapParams();
        if (params == null || !BLOOD_LIVE_EFFECT_PARAMS.containsAll(params.keySet())) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (liveAbility.hasParam("Optional") || liveAbility.hasParam("TargetingPlayer")
                || liveAbility.getTargetingPlayer() != null) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.UNSUPPORTED_PROFILE);
        }
        if (liveAbility.getTargets() == null || !liveAbility.getTargets().isEmpty()) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.NON_EMPTY_INITIAL_TARGETS);
        }
        if (!BLOOD_EFFECT_PARAMS.equals(normalize(params, "TgtPrompt", "ValidTgtsDesc", "TgtZone",
                "TargetMin", "TargetMax")) || liveAbility.getApi() != ApiType.ChangeZone) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.LIVE_EFFECT_MISMATCH);
        }

        final TargetRestrictions restrictions = liveAbility.getTargetRestrictions();
        try {
            if (!liveAbility.usesTargeting() || restrictions == null || restrictions.isRandomTarget()
                    || restrictions.isRandomNumTargets() || !List.of(ZoneType.Graveyard).equals(restrictions.getZone())
                    || liveAbility.getMinTargets() != 1 || liveAbility.getMaxTargets() != 1
                    || liveAbility.getSubAbility() != null || !liveAbility.getAdditionalAbilities().isEmpty()
                    || !liveAbility.getAdditionalAbilityLists().isEmpty()
                    || liveAbility.getPayCosts() == null || !liveAbility.getPayCosts().isFree()
                    || (params.containsKey("TgtZone") && !"Graveyard".equals(params.get("TgtZone")))
                    || (params.containsKey("TargetMin") && !"1".equals(params.get("TargetMin")))
                    || (params.containsKey("TargetMax") && !"1".equals(params.get("TargetMax")))) {
                return Admission.unsupported(TriggeredTargetIntegrityException.Reason.LIVE_EFFECT_MISMATCH);
            }
        } catch (final RuntimeException ex) {
            return Admission.unsupported(TriggeredTargetIntegrityException.Reason.LIVE_EFFECT_MISMATCH);
        }
        return Admission.admitted();
    }

    private static Map<String, String> normalize(final Map<String, String> params, final String... ignoredKeys) {
        if (params == null) {
            return Map.of();
        }
        final Map<String, String> normalized = new HashMap<>(params);
        for (final String ignoredKey : ignoredKeys) {
            normalized.remove(ignoredKey);
        }
        return normalized;
    }

    private static Player implicitChooser(final SpellAbility queuedAbility) {
        return queuedAbility instanceof WrappedAbility wrapper ? wrapper.getDecider() : null;
    }

    private static boolean samePlayer(final Player first, final Player second) {
        return first != null && second != null && (first.equals(second) || first.getId() == second.getId());
    }

    private static void rejectActiveContinuation() {
        if (PriorityActionDiagnostics.hasActiveActionContinuation()) {
            throw new TriggeredTargetIntegrityException(
                    TriggeredTargetIntegrityException.Reason.UNSUPPORTED_ACTION_CONTINUATION);
        }
    }

    private static TriggeredTargetIntegrityException unsupported(final Admission admission) {
        return new TriggeredTargetIntegrityException(admission.failureReason);
    }

    public static final class Preparation {
        private final PreparationStatus status;
        private final String reason;
        private final DecisionRequest request;
        private final SpellAbility liveAbility;
        private final List<GameObject> beforeTargets;
        private final DeterminismTrace.RequestHandle traceHandle;
        private final boolean resolverOwned;

        private Preparation(final PreparationStatus status0, final String reason0,
                final DecisionRequest request0, final SpellAbility liveAbility0,
                final List<GameObject> beforeTargets0, final DeterminismTrace.RequestHandle traceHandle0,
                final boolean resolverOwned0) {
            status = status0;
            reason = reason0;
            request = request0;
            liveAbility = liveAbility0;
            beforeTargets = beforeTargets0;
            traceHandle = traceHandle0;
            resolverOwned = resolverOwned0;
        }

        private static Preparation of(final PreparationStatus status, final String reason) {
            return new Preparation(status, reason, null, null, null, null, false);
        }

        private static Preparation forRequest(final PreparationStatus status, final String reason,
                final DecisionRequest request, final SpellAbility liveAbility,
                final List<GameObject> beforeTargets, final DeterminismTrace.RequestHandle traceHandle,
                final boolean resolverOwned) {
            return new Preparation(status, reason, request, liveAbility, beforeTargets, traceHandle, resolverOwned);
        }

        public PreparationStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }

        /* Package-private for the current same-package RED tests; Task 6 owns request exposure/orchestration. */
        DecisionRequest getRequest() {
            return request;
        }
    }

    private static final class Admission {
        private final Classification classification;
        private final String reason;
        private final TriggeredTargetIntegrityException.Reason failureReason;

        private Admission(final Classification classification0, final String reason0,
                final TriggeredTargetIntegrityException.Reason failureReason0) {
            classification = classification0;
            reason = reason0;
            failureReason = failureReason0;
        }

        private static Admission notApplicable() {
            return new Admission(Classification.NOT_APPLICABLE, "NOT_APPLICABLE", null);
        }

        private static Admission admitted() {
            return new Admission(Classification.ADMITTED, "ADMITTED", null);
        }

        private static Admission unsupported(final TriggeredTargetIntegrityException.Reason reason) {
            return new Admission(Classification.UNSUPPORTED_TARGETED_TRIGGER, reason.name(), reason);
        }

        private String reason() {
            return reason;
        }
    }
}
