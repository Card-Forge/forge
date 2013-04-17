package forge.game.player;

import forge.game.GameState;

public class LobbyPlayerAi extends LobbyPlayer {
    public LobbyPlayerAi(String name) {
        super(name);
    }

    private String aiProfile = "";
    
    public void setAiProfile(String profileName) {
        aiProfile = profileName;
    }

    public String getAiProfile() {
        return aiProfile;
    }

    @Override
    public PlayerType getType() {
        return PlayerType.COMPUTER;
    }

    /* (non-Javadoc)
     * @see forge.game.player.LobbyPlayer#getPlayer(forge.game.GameState)
     */
    @Override
    public Player getPlayer(GameState game) {
        return new AIPlayer(this, game);
    }
}