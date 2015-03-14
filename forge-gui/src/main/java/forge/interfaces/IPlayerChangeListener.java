package forge.interfaces;

import forge.net.game.UpdateLobbyPlayerEvent;

public interface IPlayerChangeListener {
    void update(int index, UpdateLobbyPlayerEvent event);
}
