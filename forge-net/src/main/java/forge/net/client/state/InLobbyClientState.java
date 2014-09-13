package forge.net.client.state;

import forge.net.protocol.toserver.ChatPacketSrv;
import forge.net.protocol.toserver.IPacketSrv;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class InLobbyClientState implements IClientState {

    /*private final INetClient client;
    protected InLobbyClientState(INetClient client) {
        this.client = client;
    }*/

    @Override
    public boolean processPacket(IPacketSrv data) {
        if( data instanceof ChatPacketSrv) {
            //ChatPacketSrv cp = (ChatPacketSrv) data;
            // if ( not muted ) 
            // FServer.getLobby().speak(ChatArea.Room, client.getPlayer(), cp.getMessage());
            // else 
            //   client.send("You are banned and cannot speak");
            return true;
        }
        return false;
    }
}