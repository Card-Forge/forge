package forge.net.client.state;

import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.net.client.INetClient;
import forge.net.protocol.incoming.AuthorizePacket;
import forge.net.protocol.incoming.IPacket;
import forge.net.protocol.incoming.PacketOpcode;
import forge.net.protocol.outcoming.AuthorizationSuccessfulMessage;

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
    public boolean processPacket(IPacket packet) {
        if( packet.getOpCode() == PacketOpcode.Authorize ) {
            AuthorizePacket p = (AuthorizePacket)packet;
            if( true ) { // check credentials here!
                client.send(new AuthorizationSuccessfulMessage(p.getUsername()));
                
                
                client.setPlayer(new LobbyPlayer(PlayerType.REMOTE, p.getUsername()));
                client.replaceState(this, new InLobbyClientState(client));
                return true;
            }
        }

        return false;
    }

}
