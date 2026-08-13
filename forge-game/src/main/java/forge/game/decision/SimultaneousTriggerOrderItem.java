package forge.game.decision;

import forge.game.ability.ApiType;
import forge.game.trigger.TriggerType;

import java.util.Objects;

/** Immutable public projection of one admitted simultaneous trigger entry. */
public final class SimultaneousTriggerOrderItem {
    private final long itemId;
    private final CardSelectionCard source;
    private final TriggerType triggerType;
    private final ApiType effectApi;

    SimultaneousTriggerOrderItem(final long itemId, final CardSelectionCard source,
            final TriggerType triggerType, final ApiType effectApi) {
        if (itemId <= 0) {
            throw new IllegalArgumentException("ORDER item ID must be positive");
        }
        this.itemId = itemId;
        this.source = Objects.requireNonNull(source);
        this.triggerType = Objects.requireNonNull(triggerType);
        this.effectApi = Objects.requireNonNull(effectApi);
    }

    public long getItemId() {
        return itemId;
    }

    public CardSelectionCard getSource() {
        return source;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public ApiType getEffectApi() {
        return effectApi;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimultaneousTriggerOrderItem that)) {
            return false;
        }
        return itemId == that.itemId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(itemId);
    }
}
