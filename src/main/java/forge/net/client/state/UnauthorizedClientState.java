package forge.net.client.state;

import forge.net.client.INetClient;
import forge.net.protocol.toclient.AuthResultPacketClt;
import forge.net.protocol.toserver.AuthorizePacketSrv;
import forge.net.protocol.toserver.IPacketSrv;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnauthorizedClientState  implements IClientState {

    /**
     * TODO: Write javadoc for Constructor.
     * @param client
     */
    private final INetClient client;
    public UnauthorizedClientState(INetClient client) {
        this.client = client;
    }
    

    @Override
    public boolean processPacket(IPacketSrv packet) {
        if( packet instanceof AuthorizePacketSrv ) {
            AuthorizePacketSrv p = (AuthorizePacketSrv)packet;
            if( true ) { // check credentials here!
                client.send(new AuthResultPacketClt(p.getUsername(), true));
                
                
                client.createPlayer(p.getUsername());
                client.replaceState(this, new InLobbyClientState(client));
                return true;
            }
        }

        return false;
    }

}
