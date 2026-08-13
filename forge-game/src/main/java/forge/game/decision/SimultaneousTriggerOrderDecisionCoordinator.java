package forge.game.decision;

import forge.game.card.Card;
import forge.game.ability.ApiType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exact coordinator for the profiled simultaneous-trigger ORDER boundary.
 *
 * <p>The coordinator owns only request orchestration and private identity mapping.
 * Forge's native callback remains the ordering oracle when no external resolver
 * owns an admitted session.</p>
 */
public final class SimultaneousTriggerOrderDecisionCoordinator {
    private static final String TRACE_STAGE = "SIMULTANEOUS_TRIGGER_ORDER";

    @FunctionalInterface
    public interface NativeOrderer {
        List<SpellAbility> order(List<SpellAbility> input);
    }

    /**
     * Reverses the native insertion sequence into the semantic sequence that
     * resolves first under MagicStack's add-first LIFO insertion.
     */
    static <T> List<T> toSemanticResolveFirst(final List<T> nativeInsertionOrder) {
        return OrderResolutionTranslation.toSemanticResolveFirst(nativeInsertionOrder);
    }

    /** Reverses semantic resolve-first order into MagicStack insertion order. */
    static <T> List<T> toNativeInsertion(final List<T> semanticResolveFirstOrder) {
        return OrderResolutionTranslation.toNativeInsertion(semanticResolveFirstOrder);
    }

    public List<SpellAbility> order(final List<SpellAbility> active,
            final Player chooser, final SimultaneousTriggerOrderDecisionProvider provider,
            final NativeOrderer nativeOrderer) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(nativeOrderer);

        if (active == null) {
            if (provider.hasResolver()) {
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.UNSUPPORTED_ADMISSION);
            }
            return nativeOrderer.order(null);
        }
        if (active.size() <= 1) {
            if (provider.hasResolver()) {
                return active;
            }
            return nativeOrderer.order(active);
        }
        Objects.requireNonNull(chooser);

        final boolean l1ProfileSession = isSimultaneousTriggerProfileCandidate(active);
        final Snapshot snapshot = admit(active, chooser, provider.nextOrderSessionId());
        if (l1ProfileSession) {
            SimultaneousTriggerOrderAuditDiagnostics.recordSimultaneousTriggerProfileSession(snapshot != null);
        } else {
            SimultaneousTriggerOrderAuditDiagnostics.recordNonL1MultiItemCallback();
        }
        if (snapshot == null) {
            if (provider.hasResolver()) {
                if (l1ProfileSession) {
                    SimultaneousTriggerOrderAuditDiagnostics.recordL1UnsupportedFailure();
                }
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.UNSUPPORTED_ADMISSION);
            }
            if (l1ProfileSession) {
                SimultaneousTriggerOrderAuditDiagnostics.recordL1UnsupportedFallback();
            } else {
                SimultaneousTriggerOrderAuditDiagnostics.recordOutsideL1NativeFallback();
            }
            return nativeOrderer.order(active);
        }
        final SimultaneousTriggerOrderDecisionProvider.Resolver resolver = provider.getResolver();
        if (resolver == null) {
            return orderNative(active, chooser, provider, snapshot, nativeOrderer);
        }
        return orderExternal(chooser, provider, snapshot, resolver);
    }

    private Snapshot admit(final List<SpellAbility> active, final Player chooser,
            final long orderSessionId) {
        final Map<SpellAbility, Boolean> inputIdentities = new IdentityHashMap<>();
        for (final SpellAbility entry : active) {
            if (entry == null || inputIdentities.put(entry, Boolean.TRUE) != null) {
                SimultaneousTriggerOrderAuditDiagnostics.recordIntegrityFailure();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.SESSION_INTEGRITY_FAILURE);
            }
        }
        if (PriorityActionDiagnostics.hasActiveActionContinuation()) {
            SimultaneousTriggerOrderAuditDiagnostics.recordAdmissionRejection("ACTION_CONTINUATION");
            return null;
        }

        final List<Entry> entries = new ArrayList<>(active.size());
        final Map<SpellAbility, Entry> byIdentity = new IdentityHashMap<>();
        Player effectiveOrderingPlayer = null;
        for (int index = 0; index < active.size(); index++) {
            final SpellAbility entry = active.get(index);
            if (!(entry instanceof WrappedAbility wrapper)) {
                SimultaneousTriggerOrderAuditDiagnostics.recordAdmissionRejection("NON_WRAPPED_ENTRY");
                return null;
            }

            final Card source;
            final Trigger trigger;
            final ApiType effectApi;
            try {
                source = wrapper.getHostCard();
                trigger = wrapper.getTrigger();
                effectApi = wrapper.getApi();
            } catch (final RuntimeException ex) {
                SimultaneousTriggerOrderAuditDiagnostics.recordAdmissionRejection("PROJECTION_EXCEPTION");
                return null;
            }
            final String rejection = admissionRejection(entry, source, trigger, effectApi, chooser);
            if (rejection != null) {
                SimultaneousTriggerOrderAuditDiagnostics.recordAdmissionRejection(rejection);
                return null;
            }

            final Player effective = effectiveOrderingPlayer(entry);
            if (effective == null || !chooser.equals(effective)
                    || effectiveOrderingPlayer != null && !effectiveOrderingPlayer.equals(effective)) {
                SimultaneousTriggerOrderAuditDiagnostics.recordAdmissionRejection("EFFECTIVE_PLAYER");
                return null;
            }
            if (effectiveOrderingPlayer == null) {
                effectiveOrderingPlayer = effective;
            }

            final SimultaneousTriggerOrderItem item = new SimultaneousTriggerOrderItem(index + 1L,
                    new CardSelectionCard(source), trigger.getMode(), effectApi);
            final Entry snapshotEntry = new Entry(entry, item);
            entries.add(snapshotEntry);
            byIdentity.put(entry, snapshotEntry);
        }
        return new Snapshot(entries, byIdentity, chooser, orderSessionId);
    }

    static boolean isSimultaneousTriggerProfileCandidate(final List<SpellAbility> active) {
        try {
            Player effectiveOrderingPlayer = null;
            for (final SpellAbility entry : active) {
                if (!(entry instanceof WrappedAbility) || !entry.isTrigger()) {
                    return false;
                }
                final Trigger trigger = ((WrappedAbility) entry).getTrigger();
                if (trigger == null || trigger.isStatic()) {
                    return false;
                }
                final Player effective = effectiveOrderingPlayer(entry);
                if (effective == null || effectiveOrderingPlayer != null
                        && !effectiveOrderingPlayer.equals(effective)) {
                    return false;
                }
                if (effectiveOrderingPlayer == null) {
                    effectiveOrderingPlayer = effective;
                }
            }
            return true;
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    private static String admissionRejection(final SpellAbility entry, final Card source,
            final Trigger trigger, final ApiType effectApi, final Player chooser) {
        if (!entry.isTrigger()) {
            return "NOT_TRIGGER";
        }
        if (trigger == null) {
            return "NULL_TRIGGER";
        }
        if (trigger.isStatic()) {
            return "STATIC_TRIGGER";
        }
        if (source == null) {
            return "NULL_SOURCE";
        }
        if (source.isFaceDown()) {
            return "SOURCE_FACE_DOWN";
        }
        if (source.getView() == null || chooser.getView() == null
                || !source.getView().canBeShownTo(chooser.getView())) {
            return "SOURCE_NOT_VISIBLE";
        }
        if (trigger.getMode() == null) {
            return "NULL_TRIGGER_TYPE";
        }
        if (effectApi == null) {
            return "NULL_API";
        }
        return null;
    }

    private static Player effectiveOrderingPlayer(final SpellAbility entry) {
        Player player = entry.getActivatingPlayer();
        if (player == null && entry.getHostCard() != null) {
            player = entry.getHostCard().getController();
        }
        return player;
    }

    private List<SpellAbility> orderNative(final List<SpellAbility> active, final Player chooser,
            final SimultaneousTriggerOrderDecisionProvider provider, final Snapshot snapshot,
            final NativeOrderer nativeOrderer) {
        final DecisionRequest firstRequest = createRequest(provider, snapshot, 0, snapshot.entries);
        final DeterminismTrace.RequestHandle firstTrace = recordRequest(chooser, firstRequest);
        final List<SpellAbility> nativeResult;
        try {
            nativeResult = nativeOrderer.order(active);
        } catch (final RuntimeException ex) {
            SimultaneousTriggerOrderAuditDiagnostics.recordNativeCallbackFailure();
            firstTrace.recordNativeCallbackFailure();
            throw failure(SimultaneousTriggerOrderIntegrityException.Reason.NATIVE_CALLBACK_FAILURE);
        }

        if (!isFullPermutation(nativeResult, snapshot)) {
            SimultaneousTriggerOrderAuditDiagnostics.recordMappingFailure();
            firstTrace.recordMappingFailed();
            throw failure(SimultaneousTriggerOrderIntegrityException.Reason.MAPPING_FAILED);
        }

        final List<Entry> semanticOrder = new ArrayList<>(nativeResult.size());
        for (final SpellAbility nativeEntry : toSemanticResolveFirst(nativeResult)) {
            semanticOrder.add(snapshot.byIdentity.get(nativeEntry));
        }

        final List<Entry> remaining = new ArrayList<>(snapshot.entries);
        completeNativeStep(firstTrace, semanticOrder.get(0), firstRequest);
        remaining.remove(semanticOrder.get(0));
        for (int step = 1; step < semanticOrder.size() - 1; step++) {
            final DecisionRequest request = createRequest(provider, snapshot, step, remaining);
            final DeterminismTrace.RequestHandle trace = recordRequest(chooser, request);
            completeNativeStep(trace, semanticOrder.get(step), request);
            remaining.remove(semanticOrder.get(step));
        }
        return nativeResult;
    }

    private List<SpellAbility> orderExternal(final Player chooser,
            final SimultaneousTriggerOrderDecisionProvider provider, final Snapshot snapshot,
            final SimultaneousTriggerOrderDecisionProvider.Resolver resolver) {
        final List<Entry> remaining = new ArrayList<>(snapshot.entries);
        final List<Entry> semanticOrder = new ArrayList<>();
        int step = 0;
        while (remaining.size() > 1) {
            final DecisionRequest request = createRequest(provider, snapshot, step, remaining);
            final DeterminismTrace.RequestHandle trace = recordRequest(chooser, request);
            final LegalCandidate selected;
            try {
                selected = resolver.choose(request);
            } catch (final RuntimeException ex) {
                SimultaneousTriggerOrderAuditDiagnostics.recordInvalidExternalCandidate();
                trace.recordInvalidExternalCandidate();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.INVALID_EXTERNAL_CANDIDATE);
            }
            final Entry selectedEntry = findSelectedEntry(request, selected, remaining);
            if (selectedEntry == null) {
                SimultaneousTriggerOrderAuditDiagnostics.recordInvalidExternalCandidate();
                trace.recordInvalidExternalCandidate();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.INVALID_EXTERNAL_CANDIDATE);
            }
            trace.recordExternalChosenResult(selected);
            semanticOrder.add(selectedEntry);
            remaining.remove(selectedEntry);
            step++;
        }
        semanticOrder.add(remaining.get(0));
        final List<SpellAbility> semanticAbilities = semanticOrder.stream()
                .map(entry -> entry.nativeEntry).toList();
        return toNativeInsertion(semanticAbilities);
    }

    private static Entry findSelectedEntry(final DecisionRequest request, final LegalCandidate selected,
            final List<Entry> remaining) {
        if (selected == null || selected.getOrderKind() != OrderCandidateKind.SELECT_RESOLVE_FIRST
                || selected.getOrderItem() == null || !request.getCandidates().contains(selected)) {
            return null;
        }
        final long itemId = selected.getOrderItem().getItemId();
        return remaining.stream().filter(entry -> entry.item.getItemId() == itemId
                && selected.getSemanticKey().equals("RESOLVE_FIRST|" + itemId)).findFirst().orElse(null);
    }

    private static void completeNativeStep(final DeterminismTrace.RequestHandle trace,
            final Entry selected, final DecisionRequest request) {
        final LegalCandidate candidate = request.getCandidates().stream()
                .filter(value -> value.getOrderItem().getItemId() == selected.item.getItemId())
                .findFirst()
                .orElseThrow(() -> failure(SimultaneousTriggerOrderIntegrityException.Reason.MAPPING_FAILED));
        trace.recordNativeMappedResult(candidate);
    }

    private static DecisionRequest createRequest(final SimultaneousTriggerOrderDecisionProvider provider,
            final Snapshot snapshot, final int step, final List<Entry> remaining) {
        final List<LegalCandidate> candidates = new ArrayList<>(remaining.size());
        for (int index = 0; index < remaining.size(); index++) {
            candidates.add(LegalCandidate.order(index, OrderCandidateKind.SELECT_RESOLVE_FIRST,
                    remaining.get(index).item));
        }
        final SimultaneousTriggerOrderContext context = new SimultaneousTriggerOrderContext(
                SimultaneousTriggerOrderProfile.SIMULTANEOUS_TRIGGER_ORDER,
                OrderDirection.RESOLVE_FIRST, snapshot.orderSessionId, step,
                snapshot.entries.size(), snapshot.chooser.getId());
        return new DecisionRequest(provider.nextRequestId(), DecisionType.ORDER, candidates, context);
    }

    private static DeterminismTrace.RequestHandle recordRequest(final Player chooser,
            final DecisionRequest request) {
        SimultaneousTriggerOrderAuditDiagnostics.recordRequest(request.getCandidates().size(), request.isForced());
        return DeterminismTrace.recordRequest(chooser.getGame(), chooser.getId(), request,
                TRACE_STAGE, request.getOrderContext().getStepIndex(),
                DecisionTraceRequestRecord.Profile.SIMULTANEOUS_TRIGGER_ORDER,
                DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE);
    }

    private static boolean isFullPermutation(final List<SpellAbility> result, final Snapshot snapshot) {
        if (result == null || result.size() != snapshot.entries.size()) {
            return false;
        }
        final Map<SpellAbility, Boolean> seen = new IdentityHashMap<>();
        for (final SpellAbility entry : result) {
            if (entry == null || snapshot.byIdentity.get(entry) == null || seen.put(entry, Boolean.TRUE) != null) {
                return false;
            }
        }
        return seen.size() == snapshot.entries.size();
    }

    private static SimultaneousTriggerOrderIntegrityException failure(
            final SimultaneousTriggerOrderIntegrityException.Reason reason) {
        return new SimultaneousTriggerOrderIntegrityException(reason);
    }

    private static final class Snapshot {
        private final List<Entry> entries;
        private final Map<SpellAbility, Entry> byIdentity;
        private final Player chooser;
        private final long orderSessionId;

        private Snapshot(final List<Entry> entries, final Map<SpellAbility, Entry> byIdentity,
                final Player chooser, final long orderSessionId) {
            this.entries = List.copyOf(entries);
            this.byIdentity = byIdentity;
            this.chooser = chooser;
            this.orderSessionId = orderSessionId;
        }
    }

    private static final class Entry {
        private final SpellAbility nativeEntry;
        private final SimultaneousTriggerOrderItem item;

        private Entry(final SpellAbility nativeEntry, final SimultaneousTriggerOrderItem item) {
            this.nativeEntry = nativeEntry;
            this.item = item;
        }
    }
}
