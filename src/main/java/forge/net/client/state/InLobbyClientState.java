package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.incoming.ChatPacket;
import forge.net.protocol.incoming.IPacket;
import forge.net.protocol.incoming.PacketOpcode;
import forge.net.protocol.outcoming.EchoMessage;

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
    public boolean processPacket(IPacket data) {
        if( data.getOpCode() == PacketOpcode.Chat) 
        {
            ChatPacket cp = (ChatPacket) data;
            // should actually find all players in a lobby and send it to them
            client.send(new EchoMessage("chat - " + cp.getMessage()));
            return true;
        }
        return false;
    }
}