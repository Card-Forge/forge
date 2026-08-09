package forge.game.decision;

import forge.game.GameEntity;
import forge.game.player.Player;

import java.util.Objects;

/** Stable, public identity for the defender attached to an attacker candidate. */
public final class AttackDeclarationDefender {
    private final int entityId;
    private final String visibleName;
    private final String entityKind;
    private final GameEntity liveEntity;

    AttackDeclarationDefender(final GameEntity entity) {
        this.entityId = entity.getId();
        this.visibleName = entity.getName();
        this.entityKind = entity instanceof Player ? "PLAYER" : entity.getClass().getSimpleName();
        this.liveEntity = entity;
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

    public GameEntity getLiveEntity() {
        return liveEntity;
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
