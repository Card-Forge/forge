package forge.game.decision;

import java.util.Objects;

public final class SurveilPartitionCard {
    private final long itemId;
    private final String visibleName;

    SurveilPartitionCard(final long itemId, final String visibleName) {
        this.itemId = itemId;
        this.visibleName = Objects.requireNonNull(visibleName, "visibleName");
    }

    public long getItemId() {
        return itemId;
    }

    public String getVisibleName() {
        return visibleName;
    }
}
