package forge.gamemodes.net.event;

public final class LobbyAlertEvent implements NetEvent {
    private static final long serialVersionUID = 1L;

    private final String title;
    private final String message;

    public LobbyAlertEvent(final String title, final String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
