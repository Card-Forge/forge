package forge.game.decision;

import java.util.Objects;

/** Stable, public identity for the defender attached to an attacker candidate. */
public final class AttackDeclarationDefender {
    private final int entityId;
    private final String visibleName;
    private final String entityKind;

    AttackDeclarationDefender(final int entityId, final String visibleName, final String entityKind) {
        this.entityId = entityId;
        this.visibleName = Objects.requireNonNull(visibleName);
        this.entityKind = Objects.requireNonNull(entityKind);
    }

    public int getEntityId() {
        return entityId;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public String getEntityKind() {
        return entityKind;
    }

    String identityKey() {
        return entityKind + "|" + entityId;
    }

    String semanticKey() {
        return entityKind + "|" + entityId;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AttackDeclarationDefender that)) {
            return false;
        }
        return entityId == that.entityId && entityKind.equals(that.entityKind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, entityKind);
    }
}
