package forge.net.client;

import forge.game.player.LobbyPlayer;
import forge.net.client.state.IClientState;
import forge.net.protocol.outcoming.IMessage;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface INetClient {

    /**
     * TODO: Write javadoc for this method.
     * @param echoMessage
     */
    void send(IMessage message);


    void setPlayer(LobbyPlayer lobbyPlayer);
    LobbyPlayer getPlayer();


    void replaceState(IClientState old, IClientState newState);
}
