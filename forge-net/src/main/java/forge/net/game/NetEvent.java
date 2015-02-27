package forge.net.game;

import java.io.Serializable;

import forge.net.game.server.RemoteClient;

public interface NetEvent extends Serializable {
    void updateForClient(RemoteClient client);
}
