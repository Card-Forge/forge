package forge.game.decision;

/** Terminal lifecycle result for one {@code DECISION_TRACE_V2} request. */
public enum DecisionTraceResultKind {
    CHOSEN,
    FORCED,
    UNOBSERVED,
    ENGINE_ROLLBACK,
    MAPPING_FAILED,
    TRACE_INCOMPLETE
}
