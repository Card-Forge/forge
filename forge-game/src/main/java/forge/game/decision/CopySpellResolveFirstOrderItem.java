package forge.game.decision;

import forge.game.ability.ApiType;

import java.util.Objects;

/** Immutable public projection of one copied spell in an L1C session. */
public final class CopySpellResolveFirstOrderItem {
    private final long itemId;
    private final CopySpellResolveFirstOrderSourceProjection sourceProjection;
    private final ApiType effectApi;
    private final CopySpellResolveFirstOrderItemKind kind;

    CopySpellResolveFirstOrderItem(final long itemId,
            final CopySpellResolveFirstOrderSourceProjection sourceProjection,
            final ApiType effectApi, final CopySpellResolveFirstOrderItemKind kind) {
        if (itemId <= 0) {
            throw new IllegalArgumentException("ORDER item ID must be positive");
        }
        this.itemId = itemId;
        this.sourceProjection = Objects.requireNonNull(sourceProjection);
        this.effectApi = Objects.requireNonNull(effectApi);
        this.kind = Objects.requireNonNull(kind);
    }

    public long getItemId() {
        return itemId;
    }

    public CopySpellResolveFirstOrderSourceProjection getSourceProjection() {
        return sourceProjection;
    }

    public ApiType getEffectApi() {
        return effectApi;
    }

    public CopySpellResolveFirstOrderItemKind getKind() {
        return kind;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CopySpellResolveFirstOrderItem that)) {
            return false;
        }
        return itemId == that.itemId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(itemId);
    }
}
