package forge.game.player;

import forge.ai.AiProfileUtil;
import forge.game.Game;

public class LobbyPlayerAi extends LobbyPlayer {
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
    protected PlayerType getType() {
        return PlayerType.COMPUTER;
    }

    @Override
    public PlayerControllerAi createControllerFor(Player ai) {
        PlayerControllerAi result = new PlayerControllerAi(ai.getGame(), ai, this);
        result.allowCheatShuffle(allowCheatShuffle);
        return result;
    }
    
    @Override
    public Player createIngamePlayer(Game game) {
        Player ai = new Player(getName(), game);
        ai.setFirstController(createControllerFor(ai));

        if( rotateProfileEachGame ) {
            setAiProfile(AiProfileUtil.getRandomProfile());
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", getAiProfile(), getName()));
        }
        return ai;
    }

    @Override
    public void hear(LobbyPlayer player, String message) { /* Local AI is deaf. */ }
}