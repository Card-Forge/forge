package forge.performance;

import forge.ai.AITest;
import forge.card.CardType;
import forge.card.GamePieceType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.LobbyPlayerForTests;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Correctness checks for item 1g: token promotion must route through the zone
 * add() path and carry the prototype's zone-entry bookkeeping, without re-running
 * entry (which would double-fire ETB triggers).
 */
public class StackedTokenPromotionTest extends AITest {

    private Card makeToken(Game game, Player p1) {
        Card token = new Card(game.nextCardId(), p1.getGame());
        token.setName("Soldier");
        token.setOwner(p1);
        token.setController(p1, 0);
        token.setType(new CardType(Arrays.asList("Creature", "Token", "Soldier"), false));
        token.setBasePower(1);
        token.setBaseToughness(1);
        token.setGamePieceType(GamePieceType.TOKEN);
        return token;
    }

    @Test
    public void testPromotionCarriesEntryBookkeepingThroughZoneAdd() {
        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer p1Spec = new RegisteredPlayer(new Deck("Player 1"));
        p1Spec.setPlayer(new LobbyPlayerForTests("Player 1", new PlayerActions()));
        players.add(p1Spec);
        RegisteredPlayer p2Spec = new RegisteredPlayer(new Deck("Player 2"));
        p2Spec.setPlayer(new LobbyPlayerForTests("Player 2", new PlayerActions()));
        players.add(p2Spec);

        GameRules rules = new GameRules(GameType.Commander);
        Match match = new Match(rules, players, "Promotion Test Match");
        Game game = match.createGame();
        game.setAge(GameStage.Play);

        Player p1 = game.getPlayers().get(0);
        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) p1.getZone(ZoneType.Battlefield);

        Card token = makeToken(game, p1);
        battlefield.add(token);
        assertTrue("freshly added token should be sick", token.hasSickness());
        int enteredTurn = token.getTurnInZone();

        assertTrue(battlefield.tryStackToken(token));
        assertSame("prototype must be held by the stack once stacked", token, battlefield.getStackedTokens().get(0).getPrototype());

        battlefield.expandStacks();

        List<Card> promoted = new ArrayList<>(battlefield.getCards());
        assertEquals("promotion must materialize the token into the zone", 1, promoted.size());
        Card copy = promoted.get(0);
        assertNotSame("promoted copy must be a distinct Card from the prototype", token, copy);
        assertEquals("promoted copy must keep the prototype's zone-entry turn", enteredTurn, copy.getTurnInZone());
        assertTrue("promoted copy must inherit sickness", copy.hasSickness());
        assertSame("promoted copy must be zoned to the battlefield", battlefield, copy.getZone());
        assertSame("promoted copy must be controlled by the entering player", p1, copy.getController());
        assertEquals(GamePieceType.TOKEN, copy.getGamePieceType());
    }

    @Test
    public void testBatchPromotionKeepsBookkeepingOnEveryCopy() {
        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer p1Spec = new RegisteredPlayer(new Deck("Player 1"));
        p1Spec.setPlayer(new LobbyPlayerForTests("Player 1", new PlayerActions()));
        players.add(p1Spec);
        RegisteredPlayer p2Spec = new RegisteredPlayer(new Deck("Player 2"));
        p2Spec.setPlayer(new LobbyPlayerForTests("Player 2", new PlayerActions()));
        players.add(p2Spec);

        GameRules rules = new GameRules(GameType.Commander);
        Match match = new Match(rules, players, "Batch Promotion Test Match");
        Game game = match.createGame();
        game.setAge(GameStage.Play);

        Player p1 = game.getPlayers().get(0);
        PlayerZoneBattlefield battlefield = (PlayerZoneBattlefield) p1.getZone(ZoneType.Battlefield);

        int enteredTurn = 0;
        List<Card> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Card token = makeToken(game, p1);
            battlefield.add(token);
            enteredTurn = token.getTurnInZone();
            tokens.add(token);
        }
        // Stack after all adds: each add() fires a view update that expands any
        // pending stack, so the merge must happen in a separate phase.
        for (Card token : tokens) {
            assertTrue(battlefield.tryStackToken(token));
        }
        assertEquals("three identical tokens must merge into one stack", 1, battlefield.getStackedTokens().size());
        assertEquals(3, battlefield.getStackedTokens().get(0).getQuantity());

        battlefield.expandStacks();

        List<Card> promoted = new ArrayList<>(battlefield.getCards());
        assertEquals(3, promoted.size());
        assertNotSame("copies must be independent objects", promoted.get(0), promoted.get(1));
        assertNotSame("copies must be independent objects", promoted.get(1), promoted.get(2));
        assertNotSame("copies must be independent objects", promoted.get(0), promoted.get(2));
        for (Card copy : promoted) {
            assertEquals("each promoted copy keeps the entry turn", enteredTurn, copy.getTurnInZone());
            assertTrue("each promoted copy is sick", copy.hasSickness());
            assertSame("no control change", p1, copy.getController());
        }
    }
}
