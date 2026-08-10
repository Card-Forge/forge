package forge.game.decision;

/** Semantic alternatives for the atomic KEEP/REDRAW mulligan callback. */
public enum MulliganCandidateKind {
    KEEP("MULLIGAN|KEEP"),
    REDRAW("MULLIGAN|REDRAW");

    private final String semanticKey;

    MulliganCandidateKind(final String semanticKey) {
        this.semanticKey = semanticKey;
    }

    public String semanticKey() {
        return semanticKey;
    }

    static MulliganCandidateKind fromSemanticKey(final String semanticKey) {
        for (final MulliganCandidateKind kind : values()) {
            if (kind.semanticKey.equals(semanticKey)) {
                return kind;
            }
        }
        return null;
    }
}
