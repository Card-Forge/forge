package forge.game.decision;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.Objects;

/** Public, immutable and player-visible description of one card identity in a selection callback. */
public final class CardSelectionCard {
    private final int cardId;
    private final long gameTimestamp;
    private final String visibleName;
    private final ZoneType zone;
    private final int ownerId;
    private final int controllerId;

    CardSelectionCard(final Card card) {
        this.cardId = card.getId();
        this.gameTimestamp = card.getGameTimestamp();
        this.visibleName = card.getName();
        this.zone = card.getZone() == null ? null : card.getZone().getZoneType();
        this.ownerId = card.getOwner() == null ? -1 : card.getOwner().getId();
        this.controllerId = card.getController() == null ? -1 : card.getController().getId();
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

    public int getOwnerId() {
        return ownerId;
    }

    public int getControllerId() {
        return controllerId;
    }

    String identityKey() {
        return cardId + "|" + gameTimestamp;
    }

    String selectionSemanticKey() {
        return "SELECT_CARD|" + (zone == null ? "" : zone.ordinal()) + "|" + identityKey();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CardSelectionCard that)) {
            return false;
        }
        return cardId == that.cardId && gameTimestamp == that.gameTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, gameTimestamp);
    }
}
