package forge.game.player;

import forge.game.GameState;

/** 
 * This means a player's part unchanged for all games.
 * 
 * May store player's assets here.
 *
 */
public abstract class LobbyPlayer implements IHasIcon {

    public abstract PlayerType getType();

    protected final String name;
    protected String imageKey;
    private int avatarIndex = -1;

    /** The AI profile. */


    public LobbyPlayer(String name) {
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
        result = prime * result + name.hashCode();
        result = prime * result + getType().hashCode();
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
        return getType() == other.getType();
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }

    public void setAvatarIndex(int avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public abstract Player getPlayer(GameState gameState); // factory method to create player

    public abstract void hear(LobbyPlayer player, String message);
}
