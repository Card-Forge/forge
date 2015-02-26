package forge.net.game;

public class LogoutEvent implements NetEvent {
    private static final long serialVersionUID = -8262613254026625787L;

    private final String username;
    public LogoutEvent(final String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
