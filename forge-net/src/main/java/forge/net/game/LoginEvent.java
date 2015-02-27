package forge.net.game;

import forge.net.game.server.RemoteClient;

public class LoginEvent implements NetEvent {
    private static final long serialVersionUID = -8865183377417377938L;

    private final String username;
    public LoginEvent(final String username) {
        this.username = username;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    public String getUsername() {
        return username;
    }
}
