package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.incoming.EchoPacket;
import forge.net.protocol.incoming.IncorrectPacket;
import forge.net.protocol.incoming.IPacket;
import forge.net.protocol.outcoming.EchoMessage;
import forge.net.protocol.outcoming.IncorrectPacketMessage;
import forge.net.protocol.outcoming.UnknownPacketMessage;



public class ConnectedClientState implements IClientState {

    private final INetClient client;
    
    public ConnectedClientState(INetClient client) {
        this.client = client;
    }
    
    @Override
    public boolean processPacket(IPacket packet ) {
        switch( packet.getOpCode() ) {
            case Echo:
                EchoPacket pe = (EchoPacket)packet;
                client.send(new EchoMessage(pe.getMessage()));
                return true;
                
            case Incorrect:
                IncorrectPacket pi = (IncorrectPacket)packet;
                client.send(new IncorrectPacketMessage(pi));
                return true;

            default:
                client.send(new UnknownPacketMessage());
                return true;
        }
    }
    
}
