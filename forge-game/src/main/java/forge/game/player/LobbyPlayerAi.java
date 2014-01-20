package forge.game.player;

import forge.ai.AiProfileUtil;
import forge.game.Game;

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
    protected PlayerType getType() {
        return PlayerType.COMPUTER;
    }

    @Override
    public PlayerController createControllerFor(Player ai) {
        return new PlayerControllerAi(ai.getGame(), ai, this);
    }
    
    @Override
    public Player createIngamePlayer(Game game) {
        Player ai = new Player(getName(), game);
        ai.setFirstController(createControllerFor(ai));

        String currentAiProfile = Singletons.getModel().getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        String lastProfileChosen = game.getMatch().getPlayedGames().isEmpty() ? currentAiProfile : getAiProfile();

        // TODO: implement specific AI profiles for quest mode.
        boolean wantRandomProfile = currentAiProfile.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL) 
             || game.getMatch().getPlayedGames().isEmpty() && currentAiProfile.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH); 

        setAiProfile(wantRandomProfile ? AiProfileUtil.getRandomProfile() : lastProfileChosen);
        System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", getAiProfile(), getName()));
        return ai;
    }

    @Override
    public void hear(LobbyPlayer player, String message) { /* Local AI is deaf. */ }
}