package forge.interfaces;

import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;

public interface IPlayerChangeListener {
    void update(int index, UpdateLobbyPlayerEvent event);
}
