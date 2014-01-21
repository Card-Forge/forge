package forge.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Supplier;

import forge.control.ChatArea;
import forge.game.player.LobbyPlayer;
import forge.game.player.LobbyPlayerAi;
import forge.game.player.LobbyPlayerRemote;
import forge.gui.GuiDisplayUtil;
import forge.gui.toolbox.FSkin;
import forge.net.client.INetClient;
import forge.util.MyRandom;
import forge.util.NameGenerator;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class Lobby {

    private final LobbyPlayer guiPlayer;
    public Lobby(Supplier<LobbyPlayer> humanFactory){
        guiPlayer = humanFactory.get();
    }
    
    
    private Map<String, LobbyPlayerRemote> remotePlayers = new ConcurrentHashMap<String, LobbyPlayerRemote>();
    
    private final LobbyPlayerAi system = new LobbyPlayerAi("System");

    public final LobbyPlayer getGuiPlayer() {
        return guiPlayer;
    }

    public final LobbyPlayer getAiPlayer() { return getAiPlayer(getRandomName()); }
    public final LobbyPlayer getAiPlayer(String name) {
        LobbyPlayer player = new LobbyPlayerAi(name);
        if(FSkin.isLoaded())
            player.setAvatarIndex(MyRandom.getRandom().nextInt(FSkin.getAvatars().size()));
        return player;
    }
    public final LobbyPlayer getAiPlayer(String name, int avatarIndex) {
        LobbyPlayer player = new LobbyPlayerAi(name);
        player.setAvatarIndex(avatarIndex);
        return player;
    }


    /** Returns a random name from the supplied list. */
    private String getRandomName() {
        String playerName = GuiDisplayUtil.getPlayerName();
        String aiName = NameGenerator.getRandomName("Any", "Generic", playerName);
        return aiName;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public LobbyPlayer getQuestPlayer() {
        return guiPlayer;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param name
     * @return
     */
    public synchronized LobbyPlayer findOrCreateRemotePlayer(String name, INetClient client) {
        if (remotePlayers.containsKey(name))
            return remotePlayers.get(name);

        LobbyPlayerRemote res = new LobbyPlayerRemote(name, client);
        speak(ChatArea.Room, system, res.getName()  + " has joined the server.");
        // have to load avatar from remote user's preferences here
        remotePlayers.put(name, res);

        return res;
    }

    public void disconnectPlayer(LobbyPlayer player) {
        // Should set up a timer here to discard player and all of his games after 20 minutes of being offline
    }


    public void speak(ChatArea room, LobbyPlayer player, String message) {
        getGuiPlayer().hear(player, message);
        for(LobbyPlayer remote : remotePlayers.values()) {
            remote.hear(player, message);
        }
    }
}
