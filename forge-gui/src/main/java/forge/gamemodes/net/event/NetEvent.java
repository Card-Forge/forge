package forge.gamemodes.net.event;

import java.io.Serializable;

import forge.gamemodes.net.server.RemoteClient;

public interface NetEvent extends Serializable {
    void updateForClient(RemoteClient client);
}
