package forge.net.client;

import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerType;
import forge.net.IClientSocket;
import forge.net.IConnectionObserver;
import forge.net.client.state.ClientStateUnauthorized;
import forge.net.client.state.IClientState;
import forge.net.protocol.incoming.Packet;
import forge.net.protocol.incoming.PacketOpcode;
import forge.net.protocol.outcoming.Message;

public class NetClient implements IConnectionObserver, INetClient{

    private final IClientSocket socket; 
    private IClientState state = new ClientStateUnauthorized(this);
    private LobbyPlayer player = null;
    
    public NetClient(IClientSocket clientSocket) {
        socket = clientSocket;
    }

    public void autorized() { 
        player = new LobbyPlayer(PlayerType.REMOTE, "Guest");
    }
    
    /* (non-Javadoc)
     * @see forge.net.client.IConnectionObserver#onConnectionClosed()
     */
    @Override
    public void onConnectionClosed() {
        // Tell the game, the client is gone.
    }


    @Override
    public void onMessage(String data) {
        Packet p = PacketOpcode.decode(data);
        state.onPacket(p);
    }


    @Override
    public void send(Message message) {
        socket.send(message.toNetString());
    }

}
