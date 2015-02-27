package forge.net.game;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

public class LobbyState implements Serializable {
    private static final long serialVersionUID = 3899410700896996173L;

    private final List<LobbyPlayerData> players = Lists.newArrayList();
    private int localPlayer = -1;
    public int getLocalPlayer() {
        return localPlayer;
    }
    public void setLocalPlayer(final int localPlayer) {
        this.localPlayer = localPlayer;
    }

    public void addPlayer(final LobbyPlayerData data) {
        players.add(data);
    }
    public List<LobbyPlayerData> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public final static class LobbyPlayerData implements Serializable {
        private static final long serialVersionUID = 8642923786206592216L;

        private final String name;
        private final int avatarIndex;
        private final LobbySlotType type;
        public LobbyPlayerData(final String name, final int avatarIndex, final LobbySlotType type) {
            this.name = name;
            this.avatarIndex = avatarIndex;
            this.type = type;
        }

        public String getName() {
            return name;
        }
        public int getAvatarIndex() {
            return avatarIndex;
        }
        public LobbySlotType getType() {
            return type;
        }

    }
}
