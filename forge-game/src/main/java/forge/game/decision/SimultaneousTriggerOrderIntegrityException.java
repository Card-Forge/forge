package forge.game.decision;

import java.util.Objects;

/** Sanitized fail-closed error for the exact simultaneous-trigger ORDER boundary. */
public final class SimultaneousTriggerOrderIntegrityException extends IllegalStateException {
    public enum Reason {
        UNSUPPORTED_ADMISSION,
        SESSION_INTEGRITY_FAILURE,
        INVALID_EXTERNAL_CANDIDATE,
        NATIVE_CALLBACK_FAILURE,
        MAPPING_FAILED
    }

    private final Reason reason;

    public SimultaneousTriggerOrderIntegrityException(final Reason reason0) {
        super(Objects.requireNonNull(reason0).name());
        reason = reason0;
    }

    public String getReason() {
        return reason.name();
    }

    public Reason getReasonValue() {
        return reason;
    }
}
