package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.incoming.EchoPacket;
import forge.net.protocol.incoming.Packet;
import forge.net.protocol.outcoming.EchoMessage;
import forge.net.protocol.outcoming.UnknownPacketMessage;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ClientState implements IClientState {

    private final INetClient client;

    protected ClientState(INetClient client)
    {
        this.client = client;
    }
    
    @Override
    public void onPacket(Packet packet ) {
        switch( packet.getOpCode() ) {
            case Echo:
                EchoPacket p = (EchoPacket)packet;
                client.send(new EchoMessage(p.getMessage()));
                break;

            default:
                client.send(new UnknownPacketMessage());
                break;
        
        }
    }
    
}
