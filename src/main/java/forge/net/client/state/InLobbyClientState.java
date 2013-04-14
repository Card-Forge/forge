package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.incoming.Packet;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InLobbyClientState implements IClientState {

    private final INetClient client;
    protected InLobbyClientState(INetClient client) {
        this.client = client;
    }

    @Override
    public boolean processPacket(Packet data) {
        return false;
    }
}