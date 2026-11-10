package forge.gamemodes.limited;

import forge.item.PaperCard;

public interface IDraftLog {
    void addLogEntry(String message);
    default void addLogEntry(String message, PaperCard card) {
        addLogEntry(message);
    }
    default void addPrivateLogEntry(int seatIndex, String message, PaperCard card) { }
}
