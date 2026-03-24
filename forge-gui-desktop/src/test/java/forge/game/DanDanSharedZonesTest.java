package forge.game;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class DanDanSharedZonesTest extends AITest {

    @Test
    public void dandanPlayersShareLibraryAndGraveyardZones() {
        // Ensure model/card DB initialization is done for match setup.
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        firstDeck.getMain().add("Wastes", 60);
        secondDeck.getMain().add("Wastes", 60);

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared zones");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        AssertJUnit.assertSame("DanDan should use one shared library zone",
                p1.getZone(ZoneType.Library), p2.getZone(ZoneType.Library));
        AssertJUnit.assertSame("DanDan should use one shared graveyard zone",
                p1.getZone(ZoneType.Graveyard), p2.getZone(ZoneType.Graveyard));
        AssertJUnit.assertSame("DanDan should use one shared registered deck object",
                game.getMatch().getPlayers().get(0).getDeck(), game.getMatch().getPlayers().get(1).getDeck());
    }

    @Test
    public void dandanTopOfLibraryAffectsBothPlayersDraws() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        firstDeck.getMain().add("Wastes", 10);
        secondDeck.getMain().add("Wastes", 10);

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared draw");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card cardToTop = p1.getZone(ZoneType.Library).get(3);
        p1.getGame().getAction().moveToLibrary(cardToTop, 0, null, null);

        final Card drawnByP2 = p2.drawCard();
        AssertJUnit.assertEquals("Player 2 should draw the card player 1 put on top of shared library",
                cardToTop, drawnByP2);
    }

    @Test
    public void dandanShuffleAndTopManipulationStaySharedAcrossPlayers() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        firstDeck.getMain().add("Wastes", 12);
        secondDeck.getMain().add("Wastes", 12);

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared shuffle and draw");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        // Pick a specific card object from the shared library, shuffle, then force it to top via the other player.
        final Card marker = p1.getZone(ZoneType.Library).get(5);
        p1.shuffle(null);
        p2.getGame().getAction().moveToLibrary(marker, 0, null, null);

        final Card drawnByP1 = p1.drawCard();
        AssertJUnit.assertEquals("Player 1 should draw the marker card player 2 moved to top after shuffle",
                marker, drawnByP1);
    }
}
