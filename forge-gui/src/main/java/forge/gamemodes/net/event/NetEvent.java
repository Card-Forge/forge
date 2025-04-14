package forge.gamemodes.net.event;

import forge.gamemodes.net.server.RemoteClient;

import java.io.Serializable;

public interface NetEvent extends Serializable {
    void updateForClient(RemoteClient client);
}
