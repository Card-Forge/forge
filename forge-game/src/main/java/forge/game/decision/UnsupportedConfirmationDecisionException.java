package forge.game.decision;

/** Raised when confirmation ownership encounters an unsupported profile or integrity failure. */
public final class UnsupportedConfirmationDecisionException extends IllegalStateException {
    private final ConfirmationDecisionProvider.Status status;
    private final String reason;

    public UnsupportedConfirmationDecisionException(final ConfirmationDecisionProvider.Status status0,
            final String reason0) {
        super("Confirmation decision unsupported or integrity-failed: " + status0 + " / " + reason0);
        status = status0;
        reason = reason0;
    }

    public ConfirmationDecisionProvider.Status getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
