package forge.game.decision;

import forge.game.card.Card;
import forge.game.zone.ZoneType;

import java.util.Objects;

/** Stable, public identity for one visible battlefield attacker. */
public final class AttackDeclarationCard {
    private final int cardId;
    private final long gameTimestamp;
    private final String visibleName;
    private final ZoneType zone;
    private final int controllerId;
    private final Card liveCard;

    AttackDeclarationCard(final Card card) {
        this.cardId = card.getId();
        this.gameTimestamp = card.getGameTimestamp();
        this.visibleName = card.getName();
        this.zone = card.getZone() == null ? null : card.getZone().getZoneType();
        this.controllerId = card.getController() == null ? -1 : card.getController().getId();
        this.liveCard = card;
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

    public Card getLiveCard() {
        return liveCard;
    }

    String identityKey() {
        return cardId + "|" + gameTimestamp;
    }

    String semanticKey() {
        return "ADD_ATTACKER|" + identityKey();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AttackDeclarationCard that)) {
            return false;
        }
        return cardId == that.cardId && gameTimestamp == that.gameTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, gameTimestamp);
    }
}
