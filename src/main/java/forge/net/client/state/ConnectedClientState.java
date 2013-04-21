package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.toclient.EchoPacketClt;
import forge.net.protocol.toclient.ErrorIncorrectPacketClt;
import forge.net.protocol.toclient.ErrorUnknownPacketClt;
import forge.net.protocol.toserver.EchoPacketSrv;
import forge.net.protocol.toserver.IPacketSrv;
import forge.net.protocol.toserver.IncorrectPacketSrv;



public class ConnectedClientState implements IClientState {

    private final INetClient client;
    
    public ConnectedClientState(INetClient client) {
        this.client = client;
    }
    
    @Override
    public boolean processPacket(IPacketSrv packet ) {
        if( packet instanceof EchoPacketSrv) {
            EchoPacketSrv pe = (EchoPacketSrv)packet;
            client.send(new EchoPacketClt(pe.getMessage()));
            return true;
        }
        if( packet instanceof IncorrectPacketSrv) {
            IncorrectPacketSrv pi = (IncorrectPacketSrv)packet;
            client.send(new ErrorIncorrectPacketClt(pi.getMessage()));
            return true;
        }

        client.send(new ErrorUnknownPacketClt());
        return true;
    }
    
}
