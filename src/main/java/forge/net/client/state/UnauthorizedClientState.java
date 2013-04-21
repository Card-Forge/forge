package forge.net.client.state;

import org.apache.commons.lang3.StringUtils;

import forge.net.client.INetClient;
import forge.net.client.InvalidFieldInPacketException;
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

            if( StringUtils.isBlank(p.getUsername()))
                throw new InvalidFieldInPacketException("username is blank");

            // check credentials here!

            client.createPlayer(p.getUsername()); 

            client.send(new AuthResultPacketClt(client.getPlayer().getName(), true));
            client.replaceState(this, new InLobbyClientState(client));

            return true;
        }

        return false;
    }

}
