package forge.game.event;

import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardView;

public record GameEventAddLog(GameLogEntryType type, String message, CardView sourceCard) implements GameEvent {

    public GameEventAddLog(GameLogEntryType type, String message) {
        this(type, message, (CardView) null);
    }

    public GameEventAddLog(GameLogEntryType type, String message, Card card) {
        this(type, message, card != null ? CardView.get(card) : null);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
