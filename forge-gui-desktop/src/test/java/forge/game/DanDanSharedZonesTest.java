package forge.game;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
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
    }
}
