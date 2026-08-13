package forge.game.decision;

import java.util.Objects;

/** Immutable context for one atomic L1C copied-spell ORDER step. */
public final class CopySpellResolveFirstOrderContext {
    private final CopySpellResolveFirstOrderProfile profile;
    private final OrderDirection direction;
    private final long orderSessionId;
    private final int stepIndex;
    private final int originalItemCount;
    private final int choosingPlayerId;

    CopySpellResolveFirstOrderContext(final CopySpellResolveFirstOrderProfile profile,
            final OrderDirection direction, final long orderSessionId, final int stepIndex,
            final int originalItemCount, final int choosingPlayerId) {
        this.profile = Objects.requireNonNull(profile);
        this.direction = Objects.requireNonNull(direction);
        if (direction != OrderDirection.RESOLVE_FIRST) {
            throw new IllegalArgumentException("L1C ORDER direction must be RESOLVE_FIRST");
        }
        if (orderSessionId <= 0 || stepIndex < 0 || originalItemCount < 2 ||
                stepIndex >= originalItemCount - 1 || choosingPlayerId < 0) {
            throw new IllegalArgumentException("Invalid L1C ORDER context values");
        }
        this.orderSessionId = orderSessionId;
        this.stepIndex = stepIndex;
        this.originalItemCount = originalItemCount;
        this.choosingPlayerId = choosingPlayerId;
    }

    public CopySpellResolveFirstOrderProfile getProfile() {
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
}
