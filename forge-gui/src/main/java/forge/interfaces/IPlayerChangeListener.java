package forge.interfaces;

import forge.net.event.UpdateLobbyPlayerEvent;

public interface IPlayerChangeListener {
    void update(int index, UpdateLobbyPlayerEvent event);
}
