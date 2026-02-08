package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

public class LoginEvent implements NetEvent {
    private static final long serialVersionUID = -8865183377417377938L;

    private final String username;
    private final int avatarIndex, sleeveIndex;
    private final String version;
    public LoginEvent(final String username, final int avatarIndex, final int sleeveIndex, final String version) {
        this.username = username;
        this.avatarIndex = avatarIndex;
        this.sleeveIndex = sleeveIndex;
        this.version = version;
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

    public String getVersion() {
        return version;
    }
}
