package forge.game.decision;

import java.util.Objects;

/** Raised when external ownership cannot safely preserve triggered-target integrity. */
public final class TriggeredTargetIntegrityException extends IllegalStateException {
    public enum Reason {
        UNSUPPORTED_TARGETED_TRIGGER,
        UNSUPPORTED_PROFILE,
        LIVE_EFFECT_MISMATCH,
        NON_EMPTY_INITIAL_TARGETS,
        INVALID_EXTERNAL_CANDIDATE,
        TARGET_APPLICATION_INCOMPLETE,
        MAPPING_FAILED,
        UNSUPPORTED_ACTION_CONTINUATION
    }

    private final Reason reason;

    public TriggeredTargetIntegrityException(final Reason reason0) {
        super(Objects.requireNonNull(reason0).name());
        reason = reason0;
    }

    public String getReason() {
        return reason.name();
    }
}
