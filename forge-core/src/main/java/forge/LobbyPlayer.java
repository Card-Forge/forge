package forge;

import org.apache.commons.lang3.StringUtils;

/** 
 * This means a player's part unchanged for all games.
 * 
 * May store player's assets here.
 *
 */
public abstract class LobbyPlayer {
    protected String name;
    private int avatarIndex = -1;
    private String avatarCardImageKey;

    public LobbyPlayer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name0) {
        if (StringUtils.isEmpty(name0)) { return; } //don't allow setting name to nothing
        name = name0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + getClass().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LobbyPlayer other = (LobbyPlayer) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }
    public void setAvatarIndex(int avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public String getAvatarCardImageKey() {
        return avatarCardImageKey;
    }
    public void setAvatarCardImageKey(String avatarImageKey0) {
        this.avatarCardImageKey = avatarImageKey0;
    }

    public abstract void hear(LobbyPlayer player, String message);
}
