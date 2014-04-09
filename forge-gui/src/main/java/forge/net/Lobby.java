package forge.net;

import com.google.common.base.Supplier;

import forge.GuiBase;
import forge.ai.AiProfileUtil;
import forge.ai.LobbyPlayerAi;
import forge.control.ChatArea;
import forge.game.player.LobbyPlayer;
import forge.model.FModel;
import forge.net.client.INetClient;
import forge.properties.ForgePreferences.FPref;
import forge.util.GuiDisplayUtil;
import forge.util.MyRandom;
import forge.util.NameGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        int avatarCount = GuiBase.getInterface().getAvatarCount();
        return getAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount));
    }
    public final LobbyPlayer getAiPlayer(String name, int avatarIndex) {
        LobbyPlayerAi player = new LobbyPlayerAi(name);

        // TODO: implement specific AI profiles for quest mode.
        String lastProfileChosen = FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        player.setRotateProfileEachGame(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL));
        if(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH)) {
            lastProfileChosen = AiProfileUtil.getRandomProfile();
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", lastProfileChosen, player.getName()));
        }
        player.setAiProfile(lastProfileChosen);
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
