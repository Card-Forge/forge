package forge.game.decision;

import java.util.List;
import java.util.Objects;

public final class SurveilPartitionContext {
    private final SurveilPartitionProfile profile;
    private final long surveilSessionId;
    private final int decisionStepIndex;
    private final int choosingPlayerId;
    private final int originalItemCount;
    private final List<SurveilPartitionCard> visibleItems;
    private final long currentItemId;

    SurveilPartitionContext(final SurveilPartitionProfile profile, final long surveilSessionId,
            final int decisionStepIndex, final int choosingPlayerId, final int originalItemCount,
            final List<SurveilPartitionCard> visibleItems, final long currentItemId) {
        this.profile = Objects.requireNonNull(profile, "profile");
        if (originalItemCount < 0) {
            throw new IllegalArgumentException("originalItemCount must not be negative");
        }
        if (decisionStepIndex < 0 || decisionStepIndex >= originalItemCount) {
            throw new IllegalArgumentException("decisionStepIndex must be within the item range");
        }
        this.surveilSessionId = surveilSessionId;
        this.decisionStepIndex = decisionStepIndex;
        this.choosingPlayerId = choosingPlayerId;
        this.originalItemCount = originalItemCount;
        final List<SurveilPartitionCard> copiedItems =
                List.copyOf(Objects.requireNonNull(visibleItems, "visibleItems"));
        if (copiedItems.size() != originalItemCount) {
            throw new IllegalArgumentException("visibleItems size must match originalItemCount");
        }
        final long currentItemCount = copiedItems.stream()
                .filter(item -> item.getItemId() == currentItemId)
                .count();
        if (currentItemCount != 1L) {
            throw new IllegalArgumentException("currentItemId must occur exactly once");
        }
        this.visibleItems = copiedItems;
        this.currentItemId = currentItemId;
    }

    public SurveilPartitionProfile getProfile() {
        return profile;
    }

    public long getSurveilSessionId() {
        return surveilSessionId;
    }

    public int getDecisionStepIndex() {
        return decisionStepIndex;
    }

    public int getChoosingPlayerId() {
        return choosingPlayerId;
    }

    public int getOriginalItemCount() {
        return originalItemCount;
    }

    public List<SurveilPartitionCard> getVisibleItems() {
        return visibleItems;
    }

    public long getCurrentItemId() {
        return currentItemId;
    }
}
