package forge.ai;

import java.util.Set;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

public class LobbyPlayerAi extends LobbyPlayer implements IGameEntitiesFactory {

    private String aiProfile = "";
    private String aiEndpoint;
    private boolean rotateProfileEachGame;
    private boolean allowCheatShuffle;
    private boolean useSimulation;

    public LobbyPlayerAi(String name, Set<AIOption> options) {
        this(name, options, null);
    }

    public LobbyPlayerAi(String name, Set<AIOption> options, String aiEndpoint) {
        super(name);
        this.aiEndpoint = aiEndpoint;
        if (options != null && options.contains(AIOption.USE_SIMULATION)) {
            this.useSimulation = true;
        }
    }

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

    public String getAiEndpoint() {
        return aiEndpoint;
    }

    public void setRotateProfileEachGame(boolean rotateProfileEachGame) {
        this.rotateProfileEachGame = rotateProfileEachGame;
    }

    private PlayerController createControllerFor(Player ai) {
        System.out.println("LobbyPlayerAi.createControllerFor called for: " + ai.getName());
        System.out.println("aiEndpoint is: " + aiEndpoint);
        if (aiEndpoint != null && !aiEndpoint.isEmpty()) {
            System.out.println("Creating PlayerControllerRemote...");
            AIAgentClient client = new AIAgentClient(aiEndpoint);
            return new PlayerControllerRemote(ai.getGame(), ai, this, client);
        }
        System.out.println("Creating default PlayerControllerAi...");
        PlayerControllerAi result = new PlayerControllerAi(ai.getGame(), ai, this);
        result.setUseSimulation(useSimulation);
        result.allowCheatShuffle(allowCheatShuffle);
        return result;
    }

    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return createControllerFor(slave);
    }

    @Override
    public Player createIngamePlayer(Game game, final int id) {
        Player ai = new Player(getName(), game, id);
        ai.setFirstController(createControllerFor(ai));

        if (rotateProfileEachGame) {
            setAiProfile(AiProfileUtil.getRandomProfile());
            /*
             * System.out.println(String.
             * format("AI profile %s was chosen for the lobby player %s.", getAiProfile(),
             * getName()));
             */
        }
        return ai;
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        /* Local AI is deaf. */ }
}