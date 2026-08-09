package forge.game.decision;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.Objects;

/** Stable, value-only identity for a visible BLOCK-side battlefield creature. */
public final class BlockDeclarationCard {
    private final int cardId;
    private final long gameTimestamp;
    private final String visibleName;
    private final ZoneType zone;
    private final int controllerId;

    BlockDeclarationCard(final Card card) {
        this(card.getId(), card.getGameTimestamp(), card.getName(),
                card.getZone() == null ? null : card.getZone().getZoneType(),
                card.getController() == null ? -1 : card.getController().getId());
    }

    BlockDeclarationCard(final int cardId, final long gameTimestamp, final String visibleName,
            final ZoneType zone, final int controllerId) {
        this.cardId = cardId;
        this.gameTimestamp = gameTimestamp;
        this.visibleName = Objects.requireNonNull(visibleName);
        this.zone = zone;
        this.controllerId = controllerId;
    }

    public int getCardId() {
        return cardId;
    }

    public long getGameTimestamp() {
        return gameTimestamp;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public ZoneType getZone() {
        return zone;
    }

    public int getControllerId() {
        return controllerId;
    }

    String identityKey() {
        return cardId + "|" + gameTimestamp;
    }

    String semanticKey() {
        return "BLOCKER|" + identityKey();
    }

    String attackerSemanticKey() {
        return "ATTACKER|" + identityKey();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockDeclarationCard that)) {
            return false;
        }
        return cardId == that.cardId && gameTimestamp == that.gameTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, gameTimestamp);
    }
}
