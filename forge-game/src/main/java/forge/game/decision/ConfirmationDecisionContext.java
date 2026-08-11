package forge.game.decision;

import java.util.Objects;

/** Immutable, player-perspective-safe context for the Gelectrode confirmation slice. */
public final class ConfirmationDecisionContext {
    private final ConfirmationTriggerProfile profile;
    private final ConfirmationEventType event;
    private final CardSelectionCard sourcePublicIdentity;
    private final int triggeringPlayerId;
    private final int deciderPlayerId;

    ConfirmationDecisionContext(final ConfirmationTriggerProfile profile, final ConfirmationEventType event,
            final CardSelectionCard sourcePublicIdentity, final int triggeringPlayerId,
            final int deciderPlayerId) {
        this.profile = Objects.requireNonNull(profile);
        this.event = Objects.requireNonNull(event);
        this.sourcePublicIdentity = Objects.requireNonNull(sourcePublicIdentity);
        this.triggeringPlayerId = triggeringPlayerId;
        this.deciderPlayerId = deciderPlayerId;
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

    public int getTriggeringPlayerId() {
        return triggeringPlayerId;
    }

    public int getDeciderPlayerId() {
        return deciderPlayerId;
    }
}
