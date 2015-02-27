package forge.net.game;

import forge.net.game.server.RemoteClient;

public class LogoutEvent implements NetEvent {
    private static final long serialVersionUID = -8262613254026625787L;

    private final String username;
    public LogoutEvent(final String username) {
        this.username = username;
    }

    public void updateForClient(final RemoteClient client) {
    }

    public String getUsername() {
        return username;
    }
}
