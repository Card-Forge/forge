package forge.game.decision;

/** The only two candidates admitted by the FRL-02K-B1 confirmation slice. */
public enum ConfirmationCandidateKind {
    ACCEPT,
    DECLINE;

    String semanticKey() {
        return name();
    }
}
