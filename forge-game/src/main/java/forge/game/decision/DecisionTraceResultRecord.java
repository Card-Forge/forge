package forge.game.decision;

/** Immutable training-contract view of one terminal decision trace result. */
public final class DecisionTraceResultRecord {
    private final long traceRequestIndex;
    private final DecisionTraceResultKind kind;
    private final String selectedCandidateSemanticKey;
    private final boolean nativeCallbackCompleted;
    private final boolean mappingAttempted;
    private final boolean engineRollbackObserved;
    private final boolean engineForcedBypass;
    private final boolean traceFinalization;

    DecisionTraceResultRecord(final long traceRequestIndex, final DecisionTraceResultKind kind,
            final String selectedCandidateSemanticKey, final boolean nativeCallbackCompleted,
            final boolean mappingAttempted, final boolean engineRollbackObserved,
            final boolean engineForcedBypass, final boolean traceFinalization) {
        this.traceRequestIndex = traceRequestIndex;
        this.kind = kind;
        this.selectedCandidateSemanticKey = selectedCandidateSemanticKey;
        this.nativeCallbackCompleted = nativeCallbackCompleted;
        this.mappingAttempted = mappingAttempted;
        this.engineRollbackObserved = engineRollbackObserved;
        this.engineForcedBypass = engineForcedBypass;
        this.traceFinalization = traceFinalization;
    }

    public long getTraceRequestIndex() {
        return traceRequestIndex;
    }

    public DecisionTraceResultKind getKind() {
        return kind;
    }

    public String getSelectedCandidateSemanticKey() {
        return selectedCandidateSemanticKey;
    }

    public boolean isNativeCallbackCompleted() {
        return nativeCallbackCompleted;
    }

    public boolean isMappingAttempted() {
        return mappingAttempted;
    }

    public boolean isEngineRollbackObserved() {
        return engineRollbackObserved;
    }

    public boolean isEngineForcedBypass() {
        return engineForcedBypass;
    }

    public boolean isTraceFinalization() {
        return traceFinalization;
    }
}
