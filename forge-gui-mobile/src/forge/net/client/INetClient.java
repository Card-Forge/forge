package forge.net.client;

import forge.game.player.LobbyPlayer;
import forge.net.client.state.IClientState;
import forge.net.protocol.toclient.IPacketClt;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface INetClient {

    /**
     * TODO: Write javadoc for this method.
     * @param echoMessage
     */
    void send(IPacketClt message);


    void createPlayer(String playerName);
    LobbyPlayer getPlayer();


    void replaceState(IClientState old, IClientState newState);
}
