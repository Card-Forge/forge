package forge.game.player;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.GameRules;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.deck.Deck;
import forge.util.Localizer;
import forge.game.player.IGameEntitiesFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

class DamageAfterPrevention {
    private Game game;
    private Player player;

    @BeforeAll
    static void initialize() throws IOException {
        // Create a temporary directory for resource bundles
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "localizerTest");
        tempDir.mkdir();

        // Create a mock resource bundle file (e.g., en-US.properties)
        File mockBundleFile = new File(tempDir, "en-US.properties");
        try (FileWriter writer = new FileWriter(mockBundleFile)) {
            writer.write("mockKey=Mocked message\n");
        }

        // Initialize the Localizer with the temporary directory
        Localizer.getInstance().initialize("en-US", tempDir.getAbsolutePath());
    }

    @BeforeEach
    void setUp() {
        // Initialize the game environment
        List<RegisteredPlayer> players = new ArrayList<>();
        Deck deck = new Deck();

        // Create a mock IGameEntitiesFactory
        IGameEntitiesFactory mockFactory = mock(IGameEntitiesFactory.class);
        Player mockInGamePlayer = mock(Player.class);

        // Mock the behavior of createIngamePlayer
        when(mockFactory.createIngamePlayer(any(Game.class), anyInt())).thenReturn(mockInGamePlayer);

        // Create a RegisteredPlayer with the mock IGameEntitiesFactory
        RegisteredPlayer registeredPlayer = new RegisteredPlayer(deck);
        players.add(registeredPlayer);

        // Set up game rules and match
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");

        // Create the game
        game = new Game(players, rules, match);
        game.setAge(GameStage.Play);

        // Get the first player for testing
        player = game.getPlayers().get(0);
    }

    @Test
    void testDamageAfterPrevention() {
        // Set up test data
        SpellAbility mockSpellAbility = mock(SpellAbility.class);
        Card mockCard = mock(Card.class);

        // Set player's life and apply damage
        player.setLife(5, mockSpellAbility);
        player.addDamageAfterPrevention(3, mockCard, mockSpellAbility, false, null);

        // Verify the result
        assertEquals(2, player.getLife(), "Life should be 2 after 3 damage and 5 prevention.");
    }
}