package forge.game;

import java.io.Serializable;

import forge.game.card.CardView;

public class GameLogEntry implements Serializable {
    private static final long serialVersionUID = -5322859985172769630L;

    public final String message;
    public final GameLogEntryType type;
    public final transient CardView sourceCard;

    GameLogEntry(final GameLogEntryType type0, final String messageIn) {
        this(type0, messageIn, null);
    }

    GameLogEntry(final GameLogEntryType type0, final String messageIn, final CardView sourceCard) {
        type = type0;
        message = messageIn;
        this.sourceCard = sourceCard;
    }

    @Override
    public String toString() {
        return type.getCaption() + ": " + message;
    }
}