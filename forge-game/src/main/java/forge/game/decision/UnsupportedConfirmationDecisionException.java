package forge.game.decision;

import forge.game.trigger.WrappedAbility;

/** Raised when external confirmation ownership encounters a profile outside the admitted B1 slice. */
public final class UnsupportedConfirmationDecisionException extends IllegalStateException {
    private final ConfirmationDecisionProvider.Status status;
    private final String reason;

    public UnsupportedConfirmationDecisionException(final WrappedAbility wrapper,
            final ConfirmationDecisionProvider.Generation generation) {
        this(wrapper, generation.getStatus(), generation.getReason());
    }

    public UnsupportedConfirmationDecisionException(final WrappedAbility wrapper,
            final ConfirmationDecisionProvider.Status status0, final String reason0) {
        super("Unsupported FRL-02K-B1 CONFIRMATION decision for "
                + (wrapper == null || wrapper.getHostCard() == null
                        ? "<unknown>" : wrapper.getHostCard().getName())
                + ": " + status0 + " / " + reason0);
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
