package forge.net.event;

import forge.net.server.RemoteClient;

public class LoginEvent implements NetEvent {
    private static final long serialVersionUID = -8865183377417377938L;

    private final String username;
    private final int avatarIndex;
    public LoginEvent(final String username, final int avatarIndex) {
        this.username = username;
        this.avatarIndex = avatarIndex;
    }

    @Override
    public void updateForClient(final RemoteClient client) {
    }

    public String getUsername() {
        return username;
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }
}
