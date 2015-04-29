package forge.net.event;

import forge.net.server.RemoteClient;

public class LogoutEvent implements NetEvent {
    private static final long serialVersionUID = -8262613254026625787L;

    private final String username;
    public LogoutEvent(final String username) {
        this.username = username;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    public String getUsername() {
        return username;
    }
}
