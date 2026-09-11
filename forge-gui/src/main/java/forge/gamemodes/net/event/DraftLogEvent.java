package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

/** Server -> every seat when {@code seatIndex} is -1, otherwise to that seat only: one draft log line. */
public final class DraftLogEvent implements NetEvent {
    private static final long serialVersionUID = 1L;
    private final int seatIndex;
    private final String message;

    public DraftLogEvent(int seatIndex, String message) {
        this.seatIndex = seatIndex;
        this.message = message;
    }

    public int getSeatIndex() { return seatIndex; }
    public String getMessage() { return message; }

    @Override
    public void updateForClient(RemoteClient client) { }
}
