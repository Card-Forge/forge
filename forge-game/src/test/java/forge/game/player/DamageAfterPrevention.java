package forge.game.player;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import forge.ai.AIOption;
import forge.ai.LobbyPlayerAi;
import forge.game.Game;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.GameRules;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.deck.Deck;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.GuiDesktop;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class DamageAfterPrevention {
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
    void testDamageAfterPrevention() {
        // Create a real card instead of mocking it
        IPaperCard paperCard = FModel.getMagicDb().getCommonCards().getCard("Lightning Bolt");
        Card card = Card.fromPaperCard(paperCard, player);

        // Add the card to the battlefield to ensure it has a valid state
        card.setGameTimestamp(game.getNextTimestamp());
        player.getZone(ZoneType.Battlefield).add(card);

        // Create a real SpellAbility instead of mocking it
        SpellAbility spellAbility = card.getFirstSpellAbility();

        // Set player's life and apply damage
        player.setLife(5, spellAbility);
        int result = player.addDamageAfterPrevention(3, card, spellAbility, true, null);

        // Verify the result
        assertEquals(3, result, "Life should be 3 after 3 damage and 5 prevention.");
    }
}