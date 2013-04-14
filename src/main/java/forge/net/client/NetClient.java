package forge.net.client;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import forge.game.player.LobbyPlayer;
import forge.net.IClientSocket;
import forge.net.IConnectionObserver;
import forge.net.client.state.ConnectedClientState;
import forge.net.client.state.UnauthorizedClientState;
import forge.net.client.state.IClientState;
import forge.net.protocol.incoming.IPacket;
import forge.net.protocol.incoming.PacketOpcode;
import forge.net.protocol.outcoming.IMessage;

public class NetClient implements IConnectionObserver, INetClient{

    private final IClientSocket socket; 
    private BlockingDeque<IClientState> state = new LinkedBlockingDeque<IClientState>();
    private LobbyPlayer player = null;
    
    public NetClient(IClientSocket clientSocket) {
        socket = clientSocket;
        state.push(new ConnectedClientState(this));
        state.push(new UnauthorizedClientState(this));
    }

    public void autorized() { 

    }
    
    /* (non-Javadoc)
     * @see forge.net.client.IConnectionObserver#onConnectionClosed()
     */
    @Override
    public void onConnectionClosed() {
        // Tell the game, the client is gone.
    }


    @Override
    public final LobbyPlayer getPlayer() {
        return player;
    }

    /** Receives input from network client */ 
    @Override
    public void onMessage(String data) {
        IPacket p = PacketOpcode.decode(data);
        for(IClientState s : state) {
            if ( s.processPacket(p) )
                break;
        }
    }


    @Override
    public void send(IMessage message) {
        socket.send(message.toNetString());
    }

    /* (non-Javadoc)
     * @see forge.net.client.INetClient#setPlayer(forge.game.player.LobbyPlayer)
     */
    @Override
    public final void setPlayer(LobbyPlayer lobbyPlayer) {
        player = lobbyPlayer;
    }

    /* (non-Javadoc)
     * @see forge.net.client.INetClient#replaceState(forge.net.client.state.IClientState, forge.net.client.state.IClientState)
     */
    @Override
    public synchronized void replaceState(IClientState old, IClientState newState) {
        state.removeFirstOccurrence(old);
        state.push(newState);
    }

}
