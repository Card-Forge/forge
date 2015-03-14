package forge.game;

import java.io.Serializable;

public class GameLogEntry implements Serializable {
    private static final long serialVersionUID = -5322859985172769630L;

    public final String message;
    public final GameLogEntryType type;
    // might add here date and some other fields

    GameLogEntry(final GameLogEntryType type0, final String messageIn) {
        type = type0;
        message = messageIn;
    }

    @Override
    public String toString() {
        return type.getCaption() + ": " + message;
    }
}