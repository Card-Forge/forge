package forge.interfaces;

import forge.net.game.LobbyState.LobbyPlayerData;

public interface IPlayerChangeListener {
    void update(LobbyPlayerData data);
}
