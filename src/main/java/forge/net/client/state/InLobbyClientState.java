package forge.net.client.state;

import forge.Singletons;
import forge.control.ChatArea;
import forge.net.client.INetClient;
import forge.net.protocol.toserver.ChatPacketSrv;
import forge.net.protocol.toserver.IPacketSrv;

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
    public boolean processPacket(IPacketSrv data) {
        if( data instanceof ChatPacketSrv) {
            ChatPacketSrv cp = (ChatPacketSrv) data;
            // if ( not muted ) 
            Singletons.getControl().getLobby().speak(ChatArea.Room, client.getPlayer(), cp.getMessage());
            // else 
            //   client.send("You are banned and cannot speak");
            return true;
        }
        return false;
    }
}