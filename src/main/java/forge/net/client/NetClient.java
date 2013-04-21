package forge.net.client;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import forge.Singletons;
import forge.game.player.LobbyPlayer;
import forge.net.IClientSocket;
import forge.net.IConnectionObserver;
import forge.net.client.state.ConnectedClientState;
import forge.net.client.state.UnauthorizedClientState;
import forge.net.client.state.IClientState;
import forge.net.protocol.ClientProtocol;
import forge.net.protocol.ClientProtocolJson;
import forge.net.protocol.toclient.IPacketClt;
import forge.net.protocol.toserver.IPacketSrv;

public class NetClient implements IConnectionObserver, INetClient{

    private final IClientSocket socket; 
    private final BlockingDeque<IClientState> state = new LinkedBlockingDeque<IClientState>();
    private LobbyPlayer player = null;
    private final ClientProtocol<IPacketSrv, IPacketClt> protocol;
    
    
    public NetClient(IClientSocket clientSocket) {
        socket = clientSocket;
        state.push(new ConnectedClientState(this));
        state.push(new UnauthorizedClientState(this));
        protocol = new ClientProtocolJson();
    }

    public void autorized() { 

    }
    
    /* (non-Javadoc)
     * @see forge.net.client.IConnectionObserver#onConnectionClosed()
     */
    @Override
    public void onConnectionClosed() {
        // Tell the game, the client is gone.
        if ( player != null ) Singletons.getControl().getLobby().disconnectPlayer(player);
    }


    @Override
    public final LobbyPlayer getPlayer() {
        return player;
    }

    /** Receives input from network client */ 
    @Override
    public void onMessage(String data) {
        IPacketSrv p = protocol.decodePacket(data);
        for(IClientState s : state) {
            if ( s.processPacket(p) )
                break;
        }
    }


    @Override
    public void send(IPacketClt message) {
        String rawData = protocol.encodePacket(message);
        socket.send(rawData);
    }

    /* (non-Javadoc)
     * @see forge.net.client.INetClient#setPlayer(forge.game.player.LobbyPlayer)
     */
    @Override
    public final void createPlayer(String name) {
        player = Singletons.getControl().getLobby().findOrCreateRemotePlayer(name, this);
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
