package forge.net;

import forge.LobbyPlayer;
import forge.net.client.INetClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class Lobby {

     
    private Map<String, LobbyPlayerRemote> remotePlayers = new ConcurrentHashMap<String, LobbyPlayerRemote>();

    /**
     * TODO: Write javadoc for this method.
     * @param name
     * @return
     */
    public synchronized LobbyPlayer findOrCreateRemotePlayer(String name, INetClient client) {
        if (remotePlayers.containsKey(name))
            return remotePlayers.get(name);

        LobbyPlayerRemote res = new LobbyPlayerRemote(name, client);
        // speak(ChatArea.Room, system, res.getName()  + " has joined the server.");
        // have to load avatar from remote user's preferences here
        remotePlayers.put(name, res);

        return res;
    }

    public void disconnectPlayer(LobbyPlayer player) {
        // Should set up a timer here to discard player and all of his games after 20 minutes of being offline
    }
}
