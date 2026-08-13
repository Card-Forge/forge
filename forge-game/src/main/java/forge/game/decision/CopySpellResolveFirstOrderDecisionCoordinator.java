package forge.game.decision;

import forge.game.ability.ApiType;
import forge.game.ability.SpellApiBased;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.WrappedAbility;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Strict admission and private identity capture for the exact L1C copy batch. */
public final class CopySpellResolveFirstOrderDecisionCoordinator {
    private static final String TRACE_STAGE = "COPY_SPELL_RESOLVE_FIRST_ORDER";

    @FunctionalInterface
    public interface NativeOrderer {
        List<SpellAbility> order(List<SpellAbility> input);
    }

    Admission admit(final List<SpellAbility> active, final Player chooser, final long orderSessionId) {
        if (active == null || active.size() < 2 || chooser == null || orderSessionId <= 0) {
            return null;
        }

        final Map<SpellAbility, Boolean> identities = new IdentityHashMap<>();
        for (final SpellAbility entry : active) {
            if (entry == null) {
                return null;
            }
            if (identities.put(entry, Boolean.TRUE) != null) {
                throw new SimultaneousTriggerOrderIntegrityException(
                        SimultaneousTriggerOrderIntegrityException.Reason.SESSION_INTEGRITY_FAILURE);
            }
        }

        if (PriorityActionDiagnostics.hasActiveActionContinuation()) {
            return null;
        }

        final List<CopySpellResolveFirstOrderItem> items = new ArrayList<>(active.size());
        final Map<SpellAbility, CopySpellResolveFirstOrderItem> itemByIdentity = new IdentityHashMap<>();
        Card commonSource = null;
        ApiType commonApi = null;
        for (final SpellAbility entry : active) {
            final Capture capture = capture(entry, chooser);
            if (capture == null) {
                return null;
            }
            if (commonSource == null) {
                commonSource = capture.source;
                commonApi = capture.effectApi;
            } else if (commonSource != capture.source || commonApi != capture.effectApi) {
                return null;
            }

            final CopySpellResolveFirstOrderItem item = new CopySpellResolveFirstOrderItem(
                    items.size() + 1L,
                    new CopySpellResolveFirstOrderSourceProjection(capture.visibleSourceName),
                    capture.effectApi, CopySpellResolveFirstOrderItemKind.COPIED_SPELL);
            items.add(item);
            itemByIdentity.put(entry, item);
        }
        return new Admission(active, items, itemByIdentity, chooser, orderSessionId);
    }

    public List<SpellAbility> order(final List<SpellAbility> active, final Player chooser,
            final CopySpellResolveFirstOrderDecisionProvider provider,
            final NativeOrderer nativeOrderer) {
        Objects.requireNonNull(provider);
        Objects.requireNonNull(nativeOrderer);
        if (active == null || active.size() < 2) {
            return active;
        }

        final Admission admission;
        try {
            admission = admit(active, chooser, provider.nextOrderSessionId());
        } catch (final SimultaneousTriggerOrderIntegrityException ex) {
            SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellProfileSession(active.size(), false);
            SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellIntegrityFailure();
            throw ex;
        }
        SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellProfileSession(active.size(), admission != null);
        if (admission == null) {
            if (provider.hasResolver()) {
                SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellUnsupportedFailure();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.UNSUPPORTED_ADMISSION);
            }
            return nativeOrderer.order(active);
        }

        final CopySpellResolveFirstOrderDecisionProvider.Resolver resolver = provider.getResolver();
        if (resolver == null) {
            return orderNative(active, admission, provider, nativeOrderer);
        }
        return orderExternal(admission, provider, resolver);
    }

    private List<SpellAbility> orderNative(final List<SpellAbility> active, final Admission admission,
            final CopySpellResolveFirstOrderDecisionProvider provider, final NativeOrderer nativeOrderer) {
        final List<CopySpellResolveFirstOrderItem> remaining = new ArrayList<>(admission.items);
        final DecisionRequest firstRequest = createRequest(provider, admission, 0, remaining);
        final DeterminismTrace.RequestHandle firstTrace = recordRequest(admission.chooser, firstRequest, remaining);
        final List<SpellAbility> nativeResult;
        try {
            SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellNativeTeacherCallback();
            nativeResult = nativeOrderer.order(active);
        } catch (final RuntimeException ex) {
            SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellNativeCallbackFailure();
            firstTrace.recordNativeCallbackFailure();
            throw failure(SimultaneousTriggerOrderIntegrityException.Reason.NATIVE_CALLBACK_FAILURE);
        }
        if (!isFullPermutation(nativeResult, admission)) {
            SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellMappingFailure();
            firstTrace.recordMappingFailed();
            throw failure(SimultaneousTriggerOrderIntegrityException.Reason.MAPPING_FAILED);
        }

        final List<SpellAbility> semanticOrder =
                OrderResolutionTranslation.toSemanticResolveFirst(nativeResult);
        for (int step = 0; step < semanticOrder.size() - 1; step++) {
            final DecisionRequest request;
            final DeterminismTrace.RequestHandle trace;
            if (step == 0) {
                request = firstRequest;
                trace = firstTrace;
            } else {
                request = createRequest(provider, admission, step, new ArrayList<>(remaining));
                trace = recordRequest(admission.chooser, request, remaining);
            }
            final CopySpellResolveFirstOrderItem selected = admission.getNativeItem(semanticOrder.get(step));
            if (selected == null || !remaining.contains(selected)) {
                SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellMappingFailure();
                trace.recordMappingFailed();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.MAPPING_FAILED);
            }
            final LegalCandidate candidate = candidateFor(request, selected);
            if (candidate == null) {
                SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellMappingFailure();
                trace.recordMappingFailed();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.MAPPING_FAILED);
            }
            trace.recordNativeMappedResult(candidate);
            remaining.remove(selected);
        }
        return nativeResult;
    }

    private List<SpellAbility> orderExternal(final Admission admission,
            final CopySpellResolveFirstOrderDecisionProvider provider,
            final CopySpellResolveFirstOrderDecisionProvider.Resolver resolver) {
        final List<CopySpellResolveFirstOrderItem> remaining = new ArrayList<>(admission.items);
        final List<SpellAbility> semanticOrder = new ArrayList<>(admission.items.size());
        int step = 0;
        while (remaining.size() > 1) {
            final DecisionRequest request = createRequest(provider, admission, step, remaining);
            final DeterminismTrace.RequestHandle trace = recordRequest(admission.chooser, request, remaining);
            final LegalCandidate selected;
            try {
                selected = resolver.choose(request);
            } catch (final RuntimeException ex) {
                SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellInvalidExternalCandidate();
                trace.recordInvalidExternalCandidate();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.INVALID_EXTERNAL_CANDIDATE);
            }
            final CopySpellResolveFirstOrderItem selectedItem = findSelectedItem(request, selected, remaining);
            if (selectedItem == null) {
                SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellInvalidExternalCandidate();
                trace.recordInvalidExternalCandidate();
                throw failure(SimultaneousTriggerOrderIntegrityException.Reason.INVALID_EXTERNAL_CANDIDATE);
            }
            trace.recordExternalChosenResult(selected);
            semanticOrder.add(admission.getNativeEntryForItem(selectedItem));
            remaining.remove(selectedItem);
            step++;
        }
        semanticOrder.add(admission.getNativeEntryForItem(remaining.get(0)));
        return OrderResolutionTranslation.toNativeInsertion(semanticOrder);
    }

    private static CopySpellResolveFirstOrderItem findSelectedItem(final DecisionRequest request,
            final LegalCandidate selected, final List<CopySpellResolveFirstOrderItem> remaining) {
        if (selected == null
                || selected.getCopySpellResolveFirstOrderKind()
                != CopySpellResolveFirstOrderItemKind.COPIED_SPELL
                || selected.getCopySpellResolveFirstOrderItem() == null
                || selected.getOrderKind() != null || selected.getOrderItem() != null
                || !request.getCandidates().contains(selected)
                || !selected.getSemanticKey().equals("RESOLVE_FIRST|"
                        + selected.getCopySpellResolveFirstOrderItem().getItemId())) {
            return null;
        }
        final long itemId = selected.getCopySpellResolveFirstOrderItem().getItemId();
        return remaining.stream().filter(item -> item.getItemId() == itemId).findFirst().orElse(null);
    }

    private static LegalCandidate candidateFor(final DecisionRequest request,
            final CopySpellResolveFirstOrderItem item) {
        return request.getCandidates().stream()
                .filter(candidate -> candidate.getCopySpellResolveFirstOrderItem() == item)
                .findFirst().orElse(null);
    }

    private static DecisionRequest createRequest(final CopySpellResolveFirstOrderDecisionProvider provider,
            final Admission admission, final int step,
            final List<CopySpellResolveFirstOrderItem> remaining) {
        final List<LegalCandidate> candidates = new ArrayList<>(remaining.size());
        for (int index = 0; index < remaining.size(); index++) {
            candidates.add(LegalCandidate.copySpellResolveFirstOrder(index,
                    CopySpellResolveFirstOrderItemKind.COPIED_SPELL, remaining.get(index)));
        }
        final CopySpellResolveFirstOrderContext context = new CopySpellResolveFirstOrderContext(
                CopySpellResolveFirstOrderProfile.COPY_SPELL_RESOLVE_FIRST_ORDER,
                OrderDirection.RESOLVE_FIRST, admission.orderSessionId, step,
                admission.items.size(), admission.chooser.getId());
        return new DecisionRequest(provider.nextRequestId(), DecisionType.ORDER, candidates, context);
    }

    private static DeterminismTrace.RequestHandle recordRequest(final Player chooser,
            final DecisionRequest request, final List<CopySpellResolveFirstOrderItem> remaining) {
        final DecisionTraceTeacherLabelEligibility eligibility = isPubliclySymmetric(remaining)
                ? DecisionTraceTeacherLabelEligibility.BC_EXCLUDED_PUBLIC_SYMMETRY
                : DecisionTraceTeacherLabelEligibility.BC_ELIGIBLE;
        SimultaneousTriggerOrderAuditDiagnostics.recordCopySpellRequest(request.getCandidates().size(),
                request.isForced());
        return DeterminismTrace.recordRequest(chooser.getGame(), chooser.getId(), request,
                TRACE_STAGE, request.getCopySpellResolveFirstOrderContext().getStepIndex(),
                DecisionTraceRequestRecord.Profile.COPY_SPELL_RESOLVE_FIRST_ORDER, eligibility);
    }

    private static boolean isPubliclySymmetric(final List<CopySpellResolveFirstOrderItem> items) {
        final Map<PublicProjectionKey, Boolean> seen = new java.util.HashMap<>();
        for (final CopySpellResolveFirstOrderItem item : items) {
            final PublicProjectionKey key = new PublicProjectionKey(
                    item.getSourceProjection().getVisibleOriginalSourceName(), item.getEffectApi(), item.getKind());
            if (seen.put(key, Boolean.TRUE) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFullPermutation(final List<SpellAbility> result, final Admission admission) {
        if (result == null || result.size() != admission.nativeEntries.size()) {
            return false;
        }
        final Map<SpellAbility, Boolean> seen = new IdentityHashMap<>();
        for (final SpellAbility entry : result) {
            if (entry == null || admission.getNativeItem(entry) == null
                    || seen.put(entry, Boolean.TRUE) != null) {
                return false;
            }
        }
        return seen.size() == admission.nativeEntries.size();
    }

    private static SimultaneousTriggerOrderIntegrityException failure(
            final SimultaneousTriggerOrderIntegrityException.Reason reason) {
        return new SimultaneousTriggerOrderIntegrityException(reason);
    }

    private static Capture capture(final SpellAbility entry, final Player chooser) {
        try {
            if (!(entry instanceof SpellApiBased) || !entry.isSpell() || !entry.isCopied()
                    || entry.isTrigger() || entry instanceof WrappedAbility) {
                return null;
            }
            final Card host = entry.getHostCard();
            if (host == null || !host.isCopiedSpell() || host.getCopiedPermanent() == null
                    || host.getCastSA() != entry) {
                return null;
            }
            final ApiType effectApi = entry.getApi();
            if (effectApi == null) {
                return null;
            }
            final Player activatingPlayer = entry.getActivatingPlayer();
            final Player hostController = host.getController();
            final Player hostOwner = host.getOwner();
            if (activatingPlayer == null || hostController == null || hostOwner == null
                    || activatingPlayer != chooser || hostController != chooser) {
                return null;
            }

            final Card source = host.getCopiedPermanent();
            if (source.isFaceDown() || source.getView() == null || chooser.getView() == null
                    || !source.getView().canBeShownTo(chooser.getView())) {
                return null;
            }
            final String visibleSourceName = source.getName();
            if (visibleSourceName == null || visibleSourceName.isEmpty()) {
                return null;
            }
            return new Capture(source, effectApi, visibleSourceName);
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    static final class Admission {
        private final List<SpellAbility> nativeEntries;
        private final List<CopySpellResolveFirstOrderItem> items;
        private final Map<SpellAbility, CopySpellResolveFirstOrderItem> itemByIdentity;
        private final Player chooser;
        private final long orderSessionId;

        private Admission(final List<SpellAbility> nativeEntries,
                final List<CopySpellResolveFirstOrderItem> items,
                final Map<SpellAbility, CopySpellResolveFirstOrderItem> itemByIdentity,
                final Player chooser, final long orderSessionId) {
            this.nativeEntries = List.copyOf(nativeEntries);
            this.items = List.copyOf(items);
            this.itemByIdentity = itemByIdentity;
            this.chooser = chooser;
            this.orderSessionId = orderSessionId;
        }

        List<CopySpellResolveFirstOrderItem> getItems() {
            return items;
        }

        CopySpellResolveFirstOrderItem getNativeItem(final SpellAbility nativeEntry) {
            return itemByIdentity.get(nativeEntry);
        }

        SpellAbility getNativeEntryForItem(final CopySpellResolveFirstOrderItem item) {
            return nativeEntries.stream()
                    .filter(entry -> itemByIdentity.get(entry) == item).findFirst().orElse(null);
        }

        SpellAbility getNativeEntry(final SpellAbility nativeEntry) {
            return nativeEntries.stream().filter(entry -> entry == nativeEntry).findFirst().orElse(null);
        }

        Player getChooser() {
            return chooser;
        }

        long getOrderSessionId() {
            return orderSessionId;
        }
    }

    private static final class Capture {
        private final Card source;
        private final ApiType effectApi;
        private final String visibleSourceName;

        private Capture(final Card source, final ApiType effectApi, final String visibleSourceName) {
            this.source = source;
            this.effectApi = effectApi;
            this.visibleSourceName = visibleSourceName;
        }
    }

    private record PublicProjectionKey(String visibleSourceName, ApiType effectApi,
            CopySpellResolveFirstOrderItemKind kind) {
    }
}
