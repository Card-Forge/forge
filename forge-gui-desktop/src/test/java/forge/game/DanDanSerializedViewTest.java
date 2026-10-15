package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DanDanSerializedViewTest extends AITest {
    @Test
    public void serializedConstructedDanDanKeepsSharedViewAndSeatZones() throws Exception {
        Game game = newGame(true);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);

        Card libraryTop = createCard("Island", first);
        Card libraryNext = createCard("Memory Lapse", second);
        Card graveyardCard = createCard("Mental Note", first);
        Card firstHandCard = createCard("Dandân", first);
        Card secondHandCard = createCard("Dandân", second);
        first.getZone(ZoneType.Library).add(libraryTop);
        second.getZone(ZoneType.Library).add(libraryNext);
        first.getZone(ZoneType.Graveyard).add(graveyardCard);
        first.getZone(ZoneType.Hand).add(firstHandCard);
        second.getZone(ZoneType.Hand).add(secondHandCard);
        Card exiled = createCard("Think Twice", first);
        exiled.setOwners(List.of(first, second));
        second.getZone(ZoneType.Exile).add(exiled);
        exiled.turnFaceDown(true);
        exiled.addMayLookAt(game.getNextTimestamp(), List.of(second));

        GameView snapshot = roundTrip(game.getView());

        assertNull(snapshot.getGame(), "the received view must not depend on its local Game");
        assertNull(snapshot.getMatch(), "the received view must not depend on its local Match");
        assertTrue(snapshot.isDanDan(), "the applied variant must survive serialization on Constructed");

        List<PlayerView> players = new ArrayList<>(snapshot.getPlayers());
        assertEquals(players.size(), 2);
        CardView sharedTop = players.get(0).getCards(ZoneType.Library).iterator().next();
        assertEquals(Set.copyOf(sharedTop.getOwners()), Set.copyOf(players));
        assertEquals(Set.copyOf(sharedTop.getZonePlayers()), Set.copyOf(players));
        CardView receivedExile = players.get(1).getCards(ZoneType.Exile).iterator().next();
        assertEquals(Set.copyOf(receivedExile.getOwners()), Set.copyOf(players));
        assertEquals(List.copyOf(receivedExile.getZonePlayers()), List.of(players.get(1)));
        assertFalse(receivedExile.canBeShownTo(players.get(0)), "joint ownership does not reveal face-down exile");
        assertTrue(receivedExile.canBeShownTo(players.get(1)), "explicit look grants survive serialization");
        assertEquals(ids(players.get(0).getCards(ZoneType.Library)),
                List.of(libraryTop.getId(), libraryNext.getId()));
        assertEquals(ids(players.get(1).getCards(ZoneType.Library)),
                List.of(libraryTop.getId(), libraryNext.getId()));
        assertEquals(ids(players.get(0).getCards(ZoneType.Graveyard)),
                List.of(graveyardCard.getId()));
        assertEquals(ids(players.get(1).getCards(ZoneType.Graveyard)),
                List.of(graveyardCard.getId()));
        assertEquals(ids(players.get(0).getCards(ZoneType.Hand)),
                List.of(firstHandCard.getId()), "the first seat's hand remains attached to its player view");
        assertEquals(ids(players.get(1).getCards(ZoneType.Hand)),
                List.of(secondHandCard.getId()), "the second seat's hand remains attached to its player view");

        game.setGameOver(GameEndReason.Draw);
        GameView terminal = roundTrip(game.getView());
        assertTrue(terminal.isDanDan(), "terminal updates must retain the serialized variant identity");
        assertTrue(terminal.isGameOver());
        assertEquals(ids(new ArrayList<>(terminal.getPlayers()).get(1).getCards(ZoneType.Library)),
                List.of(libraryTop.getId(), libraryNext.getId()));
    }

    @Test
    public void serializedOrdinaryViewKeepsItsSeparateSeatZones() throws Exception {
        Game game = newGame(false);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        Card firstLibraryCard = createCard("Island", first);
        Card secondLibraryCard = createCard("Memory Lapse", second);
        first.getZone(ZoneType.Library).add(firstLibraryCard);
        second.getZone(ZoneType.Library).add(secondLibraryCard);

        GameView snapshot = roundTrip(game.getView());
        assertFalse(snapshot.isDanDan());
        List<PlayerView> players = new ArrayList<>(snapshot.getPlayers());
        assertEquals(ids(players.get(0).getCards(ZoneType.Library)), List.of(firstLibraryCard.getId()));
        assertEquals(ids(players.get(1).getCards(ZoneType.Library)), List.of(secondLibraryCard.getId()));
    }

    @Test
    public void explicitLibraryLookGrantFollowsDecisionControlWithoutRevealingOtherCards() throws Exception {
        Game game = newGame(true);
        Player first = game.getRegisteredPlayers().get(0);
        Player second = game.getRegisteredPlayers().get(1);
        Card granted = createCard("Island", first);
        Card hidden = createCard("Memory Lapse", first);
        first.getZone(ZoneType.Library).add(granted);
        first.getZone(ZoneType.Library).add(hidden);
        granted.addMayLookAt(game.getNextTimestamp(), List.of(second));
        second.addController(game.getNextTimestamp(), first, second.getController(), false);

        GameView received = roundTrip(game.getView());
        List<PlayerView> players = new ArrayList<>(received.getPlayers());
        List<CardView> library = new ArrayList<>(players.get(0).getCards(ZoneType.Library));
        assertTrue(library.get(0).canBeShownTo(players.get(1)));
        assertTrue(library.get(0).canBeShownTo(players.get(0)),
                "the player controlling a granted decision can inspect that choice");
        assertFalse(library.get(1).canBeShownTo(players.get(0)));
        assertFalse(library.get(1).canBeShownTo(players.get(1)));
    }

    private Game newGame(boolean danDan) {
        return newGame(GameType.Constructed, danDan);
    }

    private Game newGame(GameType gameType, boolean danDanVariant) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("second", null)));
        GameRules rules = new GameRules(gameType);
        if (danDanVariant) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        return new Match(rules, players, "serialized view test").createGame();
    }

    @Test
    public void serializedDanDanGameTypeRemainsRecognized() throws Exception {
        Game game = newGame(GameType.DanDan, false);
        assertTrue(roundTrip(game.getView()).isDanDan());
    }

    private static GameView roundTrip(GameView source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(source);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (GameView) input.readObject();
        }
    }

    private static List<Integer> ids(Iterable<CardView> cards) {
        List<Integer> result = new ArrayList<>();
        for (CardView card : cards) {
            result.add(card.getId());
        }
        return result;
    }
}
