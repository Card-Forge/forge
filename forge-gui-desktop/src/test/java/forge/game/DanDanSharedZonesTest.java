package forge.game;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.card.CardProperty;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.toolbox.special.PlayerDetailsPanel;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class DanDanSharedZonesTest extends AITest {

    private void seedSharedLibrary(final Player p, final int n) {
        for (int i = 0; i < n; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        // Ensure PlayerViews are updated for assertions and UI display helpers.
        p.updateZoneForView(p.getZone(ZoneType.Library));
    }

    @Test
    public void dandanPlayersShareLibraryAndGraveyardZones() {
        // Ensure model/card DB initialization is done for match setup.
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared zones");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);
        seedSharedLibrary(p1, 20);

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

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared draw");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);
        seedSharedLibrary(p1, 20);

        final Card cardToTop = p1.getZone(ZoneType.Library).get(3);
        p1.getGame().getAction().moveToLibrary(cardToTop, 0, null, null);

        final Card drawnByP2 = p2.drawCard().getFirst();
        AssertJUnit.assertEquals("Player 2 should draw the card player 1 put on top of shared library",
                cardToTop, drawnByP2);
        AssertJUnit.assertEquals("Drawn card should be controlled by the drawing player in DanDan shared library",
                p2, drawnByP2.getController());
        AssertJUnit.assertEquals("Drawn card should be owned by the drawing player in DanDan shared library",
                p2, drawnByP2.getOwner());
    }

    @Test
    public void dandanShuffleAndTopManipulationStaySharedAcrossPlayers() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared shuffle and draw");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);
        seedSharedLibrary(p1, 30);

        // Pick a specific card object from the shared library, shuffle, then force it to top via the other player.
        final Card marker = p1.getZone(ZoneType.Library).get(5);
        p1.shuffle(null);
        p2.getGame().getAction().moveToLibrary(marker, 0, null, null);

        final Card drawnByP1 = p1.drawCard().getFirst();
        AssertJUnit.assertEquals("Player 1 should draw the marker card player 2 moved to top after shuffle",
                marker, drawnByP1);
    }

    @Test
    public void dandanPlayerViewsStayInSyncForSharedZonesAfterMixedEvents() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared view parity");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);
        seedSharedLibrary(p1, 40);

        final Card topCandidate = p1.getZone(ZoneType.Library).get(4);
        game.getAction().moveToLibrary(topCandidate, 0, null, null);
        p2.drawCard();
        // Avoid Player.mill here (requires a non-null SpellAbility in newer engine versions).
        game.getAction().moveTo(ZoneType.Graveyard, p1.getZone(ZoneType.Library).get(0), null, null);
        game.getAction().moveTo(ZoneType.Graveyard, p1.getZone(ZoneType.Library).get(0), null, null);
        p1.shuffle(null);

        // Ensure both PlayerViews receive shared-zone updates for assertions below.
        p1.updateZoneForView(p1.getZone(ZoneType.Library));
        p1.updateZoneForView(p1.getZone(ZoneType.Graveyard));

        final PlayerView pv1 = p1.getView();
        final PlayerView pv2 = p2.getView();

        // UI display source parity: both players should render from one canonical sequence.
        final Iterable<CardView> displayedLibraryP1 = DanDanViewZones.cardsForZoneDisplay(game.getView(), pv1, ZoneType.Library);
        final Iterable<CardView> displayedLibraryP2 = DanDanViewZones.cardsForZoneDisplay(game.getView(), pv2, ZoneType.Library);
        final Iterable<CardView> displayedGraveyardP1 = DanDanViewZones.cardsForZoneDisplay(game.getView(), pv1, ZoneType.Graveyard);
        final Iterable<CardView> displayedGraveyardP2 = DanDanViewZones.cardsForZoneDisplay(game.getView(), pv2, ZoneType.Graveyard);

        assertSameOrderAndIds("displayed library", displayedLibraryP1, displayedLibraryP2);
        assertSameOrderAndIds("displayed graveyard", displayedGraveyardP1, displayedGraveyardP2);
        AssertJUnit.assertEquals("displayed library count should match for both players",
                count(displayedLibraryP1), count(displayedLibraryP2));
        AssertJUnit.assertEquals("displayed graveyard count should match for both players",
                count(displayedGraveyardP1), count(displayedGraveyardP2));

        // Label-layer count must match displayed zone list size (PlayerDetailsPanel zone badges).
        for (final ZoneType z : new ZoneType[] { ZoneType.Library, ZoneType.Graveyard }) {
            final int displayed = count(DanDanViewZones.cardsForZoneDisplay(game.getView(), pv1, z));
            AssertJUnit.assertEquals("label count vs displayed list (P1) " + z, displayed,
                    PlayerDetailsPanel.zoneCountForDisplay(game.getView(), pv1, z));
            AssertJUnit.assertEquals("label count vs displayed list (P2) " + z, displayed,
                    PlayerDetailsPanel.zoneCountForDisplay(game.getView(), pv2, z));
        }
    }

    @Test
    public void dandanSharedGraveyardTreatsYouOwnAsSharedAccess() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");

        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final GameRules rules = new GameRules(GameType.DanDan);
        final Match match = new Match(rules, players, "DanDan shared graveyard ownership checks");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card c = addCardToZone("Opt", p1, ZoneType.Graveyard);
        c.setOwner(p2);

        AssertJUnit.assertTrue("DanDan shared graveyard should allow YouOwn checks from either player (p1)",
                CardProperty.cardHasProperty(c, "YouOwn", p1, c, null));
        AssertJUnit.assertTrue("DanDan shared graveyard should still allow YouOwn checks for owner (p2)",
                CardProperty.cardHasProperty(c, "YouOwn", p2, c, null));
    }

    private static void assertSameOrderAndIds(final String zoneName, final Iterable<CardView> left, final Iterable<CardView> right) {
        final java.util.Iterator<CardView> li = left.iterator();
        final java.util.Iterator<CardView> ri = right.iterator();
        int index = 0;
        while (li.hasNext() && ri.hasNext()) {
            final CardView l = li.next();
            final CardView r = ri.next();
            AssertJUnit.assertEquals(zoneName + " mismatch at index " + index, l.getId(), r.getId());
            index++;
        }
        AssertJUnit.assertEquals(zoneName + " size mismatch", li.hasNext(), ri.hasNext());
    }

    private static int count(final Iterable<CardView> cards) {
        int c = 0;
        for (final CardView ignored : cards) {
            c++;
        }
        return c;
    }
}
