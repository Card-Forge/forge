package forge.game;

import java.io.Serializable;

import forge.game.card.CardView;

public record GameLogEntry(GameLogEntryType type, String message, CardView sourceCard) implements Serializable {
    GameLogEntry(final GameLogEntryType type, final String message) {
        this(type, message, null);
    }

    @Override
    public String toString() {
        return type.getCaption() + ": " + message;
    }
}
