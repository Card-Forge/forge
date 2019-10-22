package forge.net.event;

import forge.net.server.RemoteClient;

public class LoginEvent implements NetEvent {
    private static final long serialVersionUID = -8865183377417377938L;

    private final String username;
    private final int avatarIndex, sleeveIndex;
    public LoginEvent(final String username, final int avatarIndex, final int sleeveIndex) {
        this.username = username;
        this.avatarIndex = avatarIndex;
        this.sleeveIndex = sleeveIndex;
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

    public int getSleeveIndex() {
        return sleeveIndex;
    }
}
