package forge.game.decision;

import java.util.Objects;

/** Visible, immutable projection of the original source of one copied spell. */
public final class CopySpellResolveFirstOrderSourceProjection {
    private final String visibleOriginalSourceName;

    CopySpellResolveFirstOrderSourceProjection(final String visibleOriginalSourceName) {
        this.visibleOriginalSourceName = Objects.requireNonNull(visibleOriginalSourceName);
        if (visibleOriginalSourceName.isEmpty()) {
            throw new IllegalArgumentException("Visible original source name must not be empty");
        }
    }

    public String getVisibleOriginalSourceName() {
        return visibleOriginalSourceName;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CopySpellResolveFirstOrderSourceProjection that)) {
            return false;
        }
        return visibleOriginalSourceName.equals(that.visibleOriginalSourceName);
    }

    @Override
    public int hashCode() {
        return visibleOriginalSourceName.hashCode();
    }
}
