package forge.net.event;

import java.io.Serializable;

import forge.net.server.RemoteClient;

public interface NetEvent extends Serializable {
    void updateForClient(RemoteClient client);
}
