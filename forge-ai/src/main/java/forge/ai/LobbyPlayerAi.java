package forge.ai;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;

public class LobbyPlayerAi extends LobbyPlayer implements IGameEntitiesFactory {
    public LobbyPlayerAi(String name) {
        super(name);
    }

    private String aiProfile = "";
    private boolean rotateProfileEachGame;
    private boolean allowCheatShuffle;
    
    public boolean isAllowCheatShuffle() {
        return allowCheatShuffle;
    }


    public void setAllowCheatShuffle(boolean allowCheatShuffle) {
        this.allowCheatShuffle = allowCheatShuffle;
    }

    public void setAiProfile(String profileName) {
        aiProfile = profileName;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    public void setRotateProfileEachGame(boolean rotateProfileEachGame) {
        this.rotateProfileEachGame = rotateProfileEachGame;
    }

    @Override
    public PlayerControllerAi createControllerFor(Player ai) {
        PlayerControllerAi result = new PlayerControllerAi(ai.getGame(), ai, this);
        result.allowCheatShuffle(allowCheatShuffle);
        return result;
    }
    
    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player ai = new Player(getName(), game, id);
        ai.setFirstController(createControllerFor(ai));

        if (rotateProfileEachGame) {
            setAiProfile(AiProfileUtil.getRandomProfile());
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", getAiProfile(), getName()));
        }
        return ai;
    }

    @Override
    public void hear(LobbyPlayer player, String message) { /* Local AI is deaf. */ }
}