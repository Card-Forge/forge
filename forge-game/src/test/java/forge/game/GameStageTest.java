package forge.game;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import forge.GuiDesktop;
import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.player.RegisteredPlayer;
import forge.game.player.Player;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

class GameStageTest {
    private static boolean initialized = false;
    private Game game;
    private Player player;

    @BeforeAll
    static void initialize() {
        if (!initialized) {
            GuiBase.setInterface(new GuiDesktop());
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                return null;
            });
            initialized = true;
        }
    }

    @BeforeEach
    void setUp() {
        game = resetGame();
        player = game.getPlayers().get(0);
    }

    private Game resetGame() {
        List<RegisteredPlayer> players = new ArrayList<>();
        Deck deck = new Deck();

        // Create AI players
        Set<AIOption> options = new HashSet<>();
        options.add(AIOption.USE_SIMULATION);
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p1", options)));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p2", null)));

        // Set up game rules and match
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");

        // Create the game
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = false;
        game.AI_TIMEOUT = FModel.getPreferences().getPrefInt(FPref.MATCH_AI_TIMEOUT);
        game.AI_CAN_USE_TIMEOUT = true;

        return game;
    }

    @Test
    void testStartGame() { // Transition to Mulligan
        game.setAge(GameStage.Mulligan);
        assertEquals(GameStage.Mulligan, game.getAge(), "Game should transition to Mulligan stage.");
    }

    @Test
    void testFinishMulligan() { // Mulligan to Play
        game.setAge(GameStage.Mulligan);
        game.setAge(GameStage.Play);
        assertEquals(GameStage.Play, game.getAge(), "Game should transition to Play stage.");
    }

    @Test // Mulligan to GameOver
    void testEndGame() {
        game.setAge(GameStage.Mulligan);
        game.setAge(GameStage.Play); 
        game.setAge(GameStage.GameOver);
        assertEquals(GameStage.GameOver, game.getAge(), "Game should transition to GameOver stage.");
    }

    @Test // Mulligan to GameOver reset to Mullgan
    void testRestartGame() {
        game.setAge(GameStage.Mulligan);
        game.setAge(GameStage.Play);
        game.setAge(GameStage.GameOver);
        game.setAge(GameStage.BeforeMulligan);
        assertEquals(GameStage.BeforeMulligan, game.getAge(), "Game should transition to BeforeMulligan stage.");
    }
}