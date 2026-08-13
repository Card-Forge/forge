package forge.game.decision;

import java.util.Objects;

/** Immutable, player-perspective-safe context for an admitted confirmation slice. */
public final class ConfirmationDecisionContext {
    private final ConfirmationTriggerProfile profile;
    private final ConfirmationEventType event;
    private final CardSelectionCard sourcePublicIdentity;
    private final CardSelectionCard targetPublicIdentity;
    private final Integer triggeringPlayerId;
    private final int deciderPlayerId;

    ConfirmationDecisionContext(final ConfirmationTriggerProfile profile, final ConfirmationEventType event,
            final CardSelectionCard sourcePublicIdentity, final CardSelectionCard targetPublicIdentity,
            final Integer triggeringPlayerId, final int deciderPlayerId) {
        this.profile = Objects.requireNonNull(profile);
        this.event = Objects.requireNonNull(event);
        this.sourcePublicIdentity = Objects.requireNonNull(sourcePublicIdentity);
        this.targetPublicIdentity = targetPublicIdentity;
        this.triggeringPlayerId = triggeringPlayerId;
        this.deciderPlayerId = deciderPlayerId;
    }

    /** Compatibility constructor for the target-free Gelectrode context. */
    ConfirmationDecisionContext(final ConfirmationTriggerProfile profile, final ConfirmationEventType event,
            final CardSelectionCard sourcePublicIdentity, final Integer triggeringPlayerId,
            final int deciderPlayerId) {
        this(profile, event, sourcePublicIdentity, null, triggeringPlayerId, deciderPlayerId);
    }

    public ConfirmationTriggerProfile getProfile() {
        return profile;
    }

    public ConfirmationEventType getEvent() {
        return event;
    }

    public CardSelectionCard getSourcePublicIdentity() {
        return sourcePublicIdentity;
    }

    public CardSelectionCard getTargetPublicIdentity() {
        return targetPublicIdentity;
    }

    public Integer getTriggeringPlayerId() {
        return triggeringPlayerId;
    }

    public int getDeciderPlayerId() {
        return deciderPlayerId;
    }
}
