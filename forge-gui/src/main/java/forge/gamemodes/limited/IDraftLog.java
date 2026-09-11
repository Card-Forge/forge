package forge.gamemodes.limited;

public interface IDraftLog {
    void addLogEntry(String message);
    default void addPrivateLogEntry(int seatIndex, String message) { }
}
