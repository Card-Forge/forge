package forge.ai;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;

/**
 * Lobby player implementation that uses an LLM service to make game decisions.
 */
public class LobbyPlayerLLM extends LobbyPlayer implements IGameEntitiesFactory {
    
    private final LLMClient client;
    
    /**
     * Creates a new LLM-based lobby player.
     * 
     * @param name The name of the player
     * @param client The LLM client to use for decision-making
     */
    public LobbyPlayerLLM(String name, LLMClient client) {
        super(name);
        if (client == null) {
            throw new IllegalArgumentException("LLMClient cannot be null for LLM controller");
        }
        this.client = client;
        System.out.println("======= LobbyPlayerLLM created for: " + name + " =======");
        
        // Test LLM client immediately
        try {
            System.out.println("Verifying LLM client connection for player: " + name);
            com.google.gson.JsonObject testRequest = new com.google.gson.JsonObject();
            testRequest.addProperty("context", "debug");
            testRequest.addProperty("message", "Initializing LobbyPlayerLLM for " + name);
            client.ask(testRequest);
            System.out.println("LLM client verification successful for player: " + name);
        } catch (Exception e) {
            System.err.println("============================================================");
            System.err.println("CRITICAL ERROR: LLM client failed verification for player: " + name);
            System.err.println("============================================================");
            e.printStackTrace();
            System.err.println("============================================================");
            throw new RuntimeException("LLM client failed verification: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Player createIngamePlayer(Game game, int id) {
        System.out.println("======= Creating LLM player in game: " + game.getId() + " =======");
        Player player = new Player(getName(), game, id);
        PlayerControllerLLM controller = new PlayerControllerLLM(game, player, this, client);
        System.out.println("======= Created PlayerControllerLLM: " + controller + " =======");
        player.setFirstController(controller);
        return player;
    }
    
    @Override
    public PlayerController createMindSlaveController(Player master, Player slave) {
        return new PlayerControllerLLM(slave.getGame(), slave, this, client);
    }
    
    @Override
    public void hear(LobbyPlayer player, String message) {
        // LLM player doesn't need to process chat messages
    }
}