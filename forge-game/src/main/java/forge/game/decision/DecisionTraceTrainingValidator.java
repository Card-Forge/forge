package forge.game.decision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Structural validation for future trajectory/history and policy-label consumers. */
public final class DecisionTraceTrainingValidator {
    private DecisionTraceTrainingValidator() {
    }

    public static boolean isHistoryValid(final DecisionTraceRequestRecord request,
            final DecisionTraceResultRecord result) {
        if (request == null || result == null
                || request.getTraceRequestIndex() != result.getTraceRequestIndex()) {
            return false;
        }
        final String selected = result.getSelectedCandidateSemanticKey();
        switch (result.getKind()) {
        case CHOSEN:
            final boolean nativeMapped = result.isNativeCallbackCompleted() && result.isMappingAttempted();
            final boolean externalChosen = request.getDecisionType() == DecisionType.CONFIRMATION
                    && !result.isNativeCallbackCompleted() && !result.isMappingAttempted();
            return !request.isForced() && (nativeMapped || externalChosen)
                    && request.getLegalCandidates().contains(selected);
        case FORCED:
            return request.isForced() && request.getLegalCandidates().size() == 1
                    && request.getLegalCandidates().get(0).equals(selected)
                    && (result.isEngineForcedBypass()
                            || result.isNativeCallbackCompleted() && result.isMappingAttempted());
        case UNOBSERVED:
            return selected.isEmpty() && result.isNativeCallbackCompleted() && !result.isMappingAttempted();
        case ENGINE_ROLLBACK:
            return selected.isEmpty() && result.isEngineRollbackObserved();
        case MAPPING_FAILED:
            return selected.isEmpty() && result.isNativeCallbackCompleted() && result.isMappingAttempted();
        case TRACE_INCOMPLETE:
            return selected.isEmpty() && result.isTraceFinalization();
        default:
            return false;
        }
    }

    public static boolean isBCPolicySample(final DecisionTraceRequestRecord request,
            final DecisionTraceResultRecord result) {
        return isHistoryValid(request, result) && result.getKind() == DecisionTraceResultKind.CHOSEN
                && !request.isForced()
                && result.isNativeCallbackCompleted()
                && result.isMappingAttempted()
                && request.getLegalCandidates().contains(result.getSelectedCandidateSemanticKey());
    }

    public static void validateRecords(final List<DecisionTraceRequestRecord> requests,
            final List<DecisionTraceResultRecord> results) {
        final Map<Long, DecisionTraceRequestRecord> byIndex = new HashMap<>();
        for (final DecisionTraceRequestRecord request : requests) {
            if (byIndex.put(request.getTraceRequestIndex(), request) != null) {
                throw new IllegalArgumentException("Duplicate decision trace request index: "
                        + request.getTraceRequestIndex());
            }
        }
        final Set<Long> terminalIndices = new HashSet<>();
        for (final DecisionTraceResultRecord result : results) {
            final DecisionTraceRequestRecord request = byIndex.get(result.getTraceRequestIndex());
            if (request == null) {
                throw new IllegalArgumentException("Decision trace result references unknown request: "
                        + result.getTraceRequestIndex());
            }
            if (!terminalIndices.add(result.getTraceRequestIndex())) {
                throw new IllegalArgumentException("Duplicate decision trace result: "
                        + result.getTraceRequestIndex());
            }
            if (!isHistoryValid(request, result)) {
                throw new IllegalArgumentException("Invalid decision trace result: "
                        + result.getTraceRequestIndex());
            }
        }
        if (terminalIndices.size() != byIndex.size()) {
            throw new IllegalArgumentException("Decision trace contains request without terminal result");
        }
    }
}
