package forge.game.player;

/** 
 * This means a player's part unchanged for all games.
 * 
 * May store player's assets here.
 *
 */
public class LobbyPlayer implements IHasIcon {

    protected final PlayerType type;
    public final PlayerType getType() {
        return type;
    }

    protected final String name;
    protected String imageKey;
    private int avatarIndex = -1;

    public LobbyPlayer(PlayerType type, String name) {

        this.type = type;
        this.name = name;
    }

    @Override
    public final String getIconImageKey() {
        return imageKey;
    }

    @Override
    public final void setIconImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        if (type != other.type) {
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
}
