package forge.game.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import org.testng.annotations.Test;

import java.io.*;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Tests that GameEvent records survive Java serialization round-trips.
 * This validates the core assumption of the event refactor: events use
 * view-typed fields (CardView, PlayerView, etc.) that are Serializable,
 * so they can be sent over the network via the existing Netty pipeline.
 *
 * Note: Events using ZoneType (e.g. GameEventCardChangeZone) cannot be
 * tested here because ZoneType's static initializer requires the Localizer,
 * which is not available in unit tests without full GUI bootstrapping.
 */
public class GameEventSerializationTest {

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    private <T extends Serializable> T roundTrip(T obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }

    private CardView makeCard(int id, String name) {
        return new CardView(id, null, name);
    }

    private PlayerView makePlayer(int id) {
        return new PlayerView(id, null);
    }

    // --- Simple event: CardView + PlayerView fields ---

    @Test
    public void testGameEventLandPlayed() throws Exception {
        PlayerView player = makePlayer(1);
        CardView land = makeCard(10, "Forest");

        GameEventLandPlayed event = new GameEventLandPlayed(player, land);
        GameEventLandPlayed deserialized = roundTrip(event);

        assertEquals(deserialized.player().getId(), 1);
        assertEquals(deserialized.land().getId(), 10);
        assertEquals(deserialized.land().getName(), "Forest");
    }

    // --- Simple event: multiple CardViews + enum + primitive ---

    @Test
    public void testGameEventCardDamaged() throws Exception {
        CardView target = makeCard(20, "Grizzly Bears");
        CardView source = makeCard(30, "Lightning Bolt");

        GameEventCardDamaged event = new GameEventCardDamaged(target, source, 3, GameEventCardDamaged.DamageType.Normal);
        GameEventCardDamaged deserialized = roundTrip(event);

        assertEquals(deserialized.card().getId(), 20);
        assertEquals(deserialized.card().getName(), "Grizzly Bears");
        assertEquals(deserialized.source().getId(), 30);
        assertEquals(deserialized.source().getName(), "Lightning Bolt");
        assertEquals(deserialized.amount(), 3);
        assertEquals(deserialized.type(), GameEventCardDamaged.DamageType.Normal);
    }

    // --- Null-safety: null CardView field ---

    @Test
    public void testGameEventCardDamagedNullSource() throws Exception {
        CardView target = makeCard(21, "Shivan Dragon");

        GameEventCardDamaged event = new GameEventCardDamaged(target, null, 2, GameEventCardDamaged.DamageType.M1M1Counters);
        GameEventCardDamaged deserialized = roundTrip(event);

        assertEquals(deserialized.card().getId(), 21);
        assertNull(deserialized.source());
        assertEquals(deserialized.amount(), 2);
        assertEquals(deserialized.type(), GameEventCardDamaged.DamageType.M1M1Counters);
    }

    // --- No-field event ---

    @Test
    public void testGameEventTokenCreated() throws Exception {
        GameEventTokenCreated event = new GameEventTokenCreated();
        GameEventTokenCreated deserialized = roundTrip(event);

        assertNotNull(deserialized);
    }

    // --- Collection event: Multimap<GameEntityView, CardView> ---

    @Test
    public void testGameEventAttackersDeclared() throws Exception {
        PlayerView player = makePlayer(2);
        GameEntityView defender = makePlayer(3);
        CardView attacker1 = makeCard(50, "Goblin Guide");
        CardView attacker2 = makeCard(51, "Monastery Swiftspear");

        Multimap<GameEntityView, CardView> attackersMap = HashMultimap.create();
        attackersMap.put(defender, attacker1);
        attackersMap.put(defender, attacker2);

        GameEventAttackersDeclared event = new GameEventAttackersDeclared(player, attackersMap);
        GameEventAttackersDeclared deserialized = roundTrip(event);

        assertEquals(deserialized.player().getId(), 2);
        assertEquals(deserialized.attackersMap().size(), 2);
    }

    // --- Pre-computed fields: primitives + strings only ---

    @Test
    public void testGameEventGameOutcome() throws Exception {
        List<String> outcomes = List.of("Player1 wins", "Player2 loses");
        GameEventGameOutcome event = new GameEventGameOutcome(5, outcomes, "Player1", "Player1: 2 Player2: 1");
        GameEventGameOutcome deserialized = roundTrip(event);

        assertEquals(deserialized.lastTurnNumber(), 5);
        assertEquals(deserialized.outcomeStrings(), outcomes);
        assertEquals(deserialized.winningPlayerName(), "Player1");
        assertEquals(deserialized.matchSummary(), "Player1: 2 Player2: 1");
    }

    // --- String-replacement fields + boolean ---

    @Test
    public void testGameEventPlayerControl() throws Exception {
        PlayerView player = makePlayer(4);

        GameEventPlayerControl event = new GameEventPlayerControl(player, "OldPlayer", "NewPlayer", true);
        GameEventPlayerControl deserialized = roundTrip(event);

        assertEquals(deserialized.player().getId(), 4);
        assertEquals(deserialized.oldLobbyPlayerName(), "OldPlayer");
        assertEquals(deserialized.newLobbyPlayerName(), "NewPlayer");
        assertTrue(deserialized.newControllerIsHuman());
    }

    // --- Null-safety: null String fields ---

    @Test
    public void testGameEventPlayerControlNullNames() throws Exception {
        PlayerView player = makePlayer(5);

        GameEventPlayerControl event = new GameEventPlayerControl(player, null, null, false);
        GameEventPlayerControl deserialized = roundTrip(event);

        assertEquals(deserialized.player().getId(), 5);
        assertNull(deserialized.oldLobbyPlayerName());
        assertNull(deserialized.newLobbyPlayerName());
        assertFalse(deserialized.newControllerIsHuman());
    }

    // --- Single-field event ---

    @Test
    public void testGameEventMulligan() throws Exception {
        PlayerView player = makePlayer(6);

        GameEventMulligan event = new GameEventMulligan(player);
        GameEventMulligan deserialized = roundTrip(event);

        assertEquals(deserialized.player().getId(), 6);
    }
}
