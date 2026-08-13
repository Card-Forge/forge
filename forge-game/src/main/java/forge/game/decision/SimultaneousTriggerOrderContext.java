package forge.game.decision;

import java.util.Objects;

/** Immutable context for one atomic ORDER step in one simultaneous-trigger session. */
public final class SimultaneousTriggerOrderContext {
    private final SimultaneousTriggerOrderProfile profile;
    private final OrderDirection direction;
    private final long orderSessionId;
    private final int stepIndex;
    private final int originalItemCount;
    private final int choosingPlayerId;

    SimultaneousTriggerOrderContext(final SimultaneousTriggerOrderProfile profile,
            final OrderDirection direction, final long orderSessionId, final int stepIndex,
            final int originalItemCount, final int choosingPlayerId) {
        this.profile = Objects.requireNonNull(profile);
        this.direction = Objects.requireNonNull(direction);
        if (orderSessionId < 0 || stepIndex < 0 || originalItemCount < 0 || choosingPlayerId < 0) {
            throw new IllegalArgumentException("ORDER context values must be non-negative");
        }
        this.orderSessionId = orderSessionId;
        this.stepIndex = stepIndex;
        this.originalItemCount = originalItemCount;
        this.choosingPlayerId = choosingPlayerId;
    }

    public SimultaneousTriggerOrderProfile getProfile() {
        return profile;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    public long getOrderSessionId() {
        return orderSessionId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public int getOriginalItemCount() {
        return originalItemCount;
    }

    public int getChoosingPlayerId() {
        return choosingPlayerId;
    }

    public Long getDecisionSequenceId() {
        return null;
    }

    public Integer getSubdecisionIndex() {
        return null;
    }
}
