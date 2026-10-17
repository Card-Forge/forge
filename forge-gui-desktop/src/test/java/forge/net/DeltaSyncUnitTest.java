package forge.net;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameView;
import forge.game.Match;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for delta sync components.
 *
 * Tests individual classes used by the delta sync system:
 * - DeltaPacket - size calculation formula
 * - NetworkByteTracker - enable/disable behavior
 * - DeltaSyncManager - library cards sent to a filtered client as shells
 * - HeadlessNetworkClient.countHiddenInfo - the hidden-info count the network tests assert on
 *
 * These are fast unit tests that don't involve actual network I/O.
 * For integration tests with real network traffic, see NetworkPlayIntegrationTest.
 */
public class DeltaSyncUnitTest {

    @Test
    public void testDeltaSizeCalculationAccuracy() {
        Map<Integer, Map<TrackableProperty, Object>> deltas = new HashMap<>();
        Map<TrackableProperty, Object> props1 = new HashMap<>();
        props1.put(TrackableProperty.Name, "Test");
        props1.put(TrackableProperty.Power, 3);
        deltas.put(1, props1);

        Map<TrackableProperty, Object> props2 = new HashMap<>();
        props2.put(TrackableProperty.Life, 20);
        props2.put(TrackableProperty.Toughness, 5);
        props2.put(TrackableProperty.MaxHandSize, 7);
        deltas.put(2, props2);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashMap<>(), 0, null);

        // Header: 8 (seq) + 4 (checksum) = 12 bytes
        // Delta key=1: 4 + 2*50 = 104 bytes
        // Delta key=2: 4 + 3*50 = 154 bytes
        // Total: 270 bytes
        int expectedSize = 12 + (4 + 2 * 50) + (4 + 3 * 50);

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta size calculation should match expected value");
    }

    @Test
    public void testDeltaSizeWithNewObjects() {
        Map<Integer, Map<TrackableProperty, Object>> deltas = new HashMap<>();
        Map<TrackableProperty, Object> deltaProps = new HashMap<>();
        deltaProps.put(TrackableProperty.Name, "Test");
        deltas.put(1, deltaProps);

        Map<Integer, Map<TrackableProperty, Object>> newObjects = new HashMap<>();
        Map<TrackableProperty, Object> newProps1 = new HashMap<>();
        newProps1.put(TrackableProperty.Name, "Card1");
        newProps1.put(TrackableProperty.Power, 2);
        newProps1.put(TrackableProperty.Toughness, 3);
        newObjects.put(100, newProps1);

        Map<TrackableProperty, Object> newProps2 = new HashMap<>();
        newProps2.put(TrackableProperty.Life, 20);
        newProps2.put(TrackableProperty.Toughness, 0);
        newProps2.put(TrackableProperty.MaxHandSize, 7);
        newProps2.put(TrackableProperty.IsAI, false);
        newObjects.put(101, newProps2);

        DeltaPacket packet = new DeltaPacket(1L, deltas, newObjects, 0, null);

        // Header: 12, Delta: (4+1*50)=54, New 100: (4+3*50)=154, New 101: (4+4*50)=204
        int expectedSize = 12 + 54 + 154 + 204;

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta packet with new objects should match expected size");
    }

    @Test
    public void testEmptyDeltaPacketSize() {
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashMap<>(), 0, null);
        int size = packet.getApproximateSize();

        // Empty packet should just have header: 8 + 4 = 12 bytes
        Assert.assertEquals(size, 12, "Empty delta packet should be exactly 12 bytes (header only)");
    }

    @Test
    public void testNetworkByteTrackerEnableDisable() {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        // Initially enabled
        Assert.assertTrue(tracker.isEnabled());

        tracker.recordBytesSent(100, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 100L);

        // Disable tracking
        tracker.setEnabled(false);
        Assert.assertFalse(tracker.isEnabled());

        // Should not record when disabled
        tracker.recordBytesSent(200, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 100L); // Still 100, not 300

        // Re-enable
        tracker.setEnabled(true);
        tracker.recordBytesSent(300, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 400L); // 100 + 300
    }

    // Library cards reach a filtered consumer as shells. Hand-built graph: players A and B, a card in B's library
    // with an alternate state, a card on B's battlefield. Only the root GameView needs a Game, mocked.

    // A separate copy of DeltaSyncManager's shell keys: a key added there and not here fails the key-set assertions
    private static final Set<TrackableProperty> SHELL_KEYS = EnumSet.of(
            TrackableProperty.Owner, TrackableProperty.Controller, TrackableProperty.Zone);
    private static final int LIBRARY_CARD = 10;
    private static final int BATTLEFIELD_CARD = 20;

    private static final class Board {
        final Tracker tracker = new Tracker();
        final PlayerView a = new PlayerView(1, tracker);
        final PlayerView b = new PlayerView(2, tracker);
        final CardView libraryCard = card(LIBRARY_CARD, ZoneType.Library, "Grizzly Bears");
        final CardView battlefieldCard = card(BATTLEFIELD_CARD, ZoneType.Battlefield, "Forest");
        final GameView gameView;
        final DeltaSyncManager manager = new DeltaSyncManager();

        Board() {
            CardView.CardStateView backside = libraryCard.createAlternateState(CardStateName.Backside);
            backside.set(TrackableProperty.Name, "Bear Form");
            libraryCard.set(TrackableProperty.AlternateState, backside);
            b.set(TrackableProperty.Library, new TrackableCollection<>(libraryCard));
            b.set(TrackableProperty.Battlefield, new TrackableCollection<>(battlefieldCard));

            Game game = Mockito.mock(Game.class);
            Mockito.when(game.getId()).thenReturn(1);
            Mockito.when(game.getTracker()).thenReturn(tracker);
            Mockito.when(game.getMatch()).thenReturn(Mockito.mock(Match.class));
            Mockito.when(game.getRules()).thenReturn(Mockito.mock(GameRules.class));
            gameView = new GameView(game);
            gameView.set(TrackableProperty.Players, new TrackableCollection<>(List.of(a, b)));
            // as ProtocolGuiGame does at GameStarted, before its first collect
            manager.registerNewObjects(gameView);
        }

        private CardView card(int id, ZoneType zone, String name) {
            CardView cv = new CardView(id, tracker);
            cv.set(TrackableProperty.Owner, b);
            cv.set(TrackableProperty.Controller, b);
            cv.set(TrackableProperty.Zone, zone);
            cv.set(TrackableProperty.Name, name);
            cv.set(TrackableProperty.OracleName, name);
            cv.getCurrentState().set(TrackableProperty.Name, name);
            return cv;
        }
    }

    private static int cardKey(int id) {
        return DeltaPacket.makeDeltaKey(DeltaPacket.TYPE_CARD_VIEW, id);
    }

    private static Map<Integer, Map<TrackableProperty, Object>> stateViewsOf(Map<Integer, Map<TrackableProperty, Object>> entries, int cardId) {
        Map<Integer, Map<TrackableProperty, Object>> views = new HashMap<>();
        for (Map.Entry<Integer, Map<TrackableProperty, Object>> e : entries.entrySet()) {
            if (DeltaPacket.getTypeFromDeltaKey(e.getKey()) == DeltaPacket.TYPE_CSV
                    && DeltaPacket.getIdFromDeltaKey(e.getKey()) / 16 == cardId) {
                views.put(e.getKey(), e.getValue());
            }
        }
        return views;
    }

    private static void assertShell(DeltaPacket packet, String what) {
        Map<TrackableProperty, Object> sent = packet.getNewObjects().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(sent, what + ": card not sent");
        Assert.assertEquals(sent.keySet(), SHELL_KEYS, what + ": the shell's keys");
        Assert.assertEquals(sent.get(TrackableProperty.Zone), ZoneType.Library, what);
        Assert.assertTrue(stateViewsOf(packet.getNewObjects(), LIBRARY_CARD).isEmpty(), what + ": a shell's state views were sent");
    }

    @Test
    public void testLibraryCardGoesAsShellToEveryFilteredViewer() {
        Board opponent = new Board();
        assertShell(opponent.manager.collectDeltas(opponent.gameView, List.of(opponent.a)), "viewer A, B's library");
        // The shell's state views were never sent, so nothing holds this consumer on them
        Assert.assertFalse(opponent.libraryCard.getCurrentState().hasConsumer(opponent.manager.getConsumerId()),
                "a shell's state view holds the consumer");
        // On a held shell a withheld property that changes stays withheld, and a shell property goes out
        opponent.libraryCard.set(TrackableProperty.Name, "Giant Growth");
        opponent.libraryCard.set(TrackableProperty.Tapped, true);
        opponent.libraryCard.set(TrackableProperty.Controller, opponent.a);
        Map<TrackableProperty, Object> changed = opponent.manager.collectDeltas(opponent.gameView, List.of(opponent.a))
                .getObjectDeltas().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(changed, "the shell's change not sent");
        Assert.assertEquals(changed.keySet(), Set.of(TrackableProperty.Controller));

        // A library is hidden from its owner too
        Board owner = new Board();
        assertShell(owner.manager.collectDeltas(owner.gameView, List.of(owner.b)), "viewer B, own library");

        // An empty viewer set sees nothing, and only the library is filtered: the battlefield card goes in full
        Board nobody = new Board();
        DeltaPacket packet = nobody.manager.collectDeltas(nobody.gameView, List.of());
        assertShell(packet, "no viewers");
        Assert.assertEquals(packet.getNewObjects().get(cardKey(BATTLEFIELD_CARD)).get(TrackableProperty.Name), "Forest");
        Assert.assertEquals(stateViewsOf(packet.getNewObjects(), BATTLEFIELD_CARD).size(), 1, "current state view");
    }

    @Test
    public void testRevealFillsInTheShellAndTheCardStaysFull() {
        Board board = new Board();
        List<PlayerView> viewers = List.of(board.a);
        board.manager.collectDeltas(board.gameView, viewers);
        board.libraryCard.set(TrackableProperty.PlayerMayLook, new TrackableCollection<>(board.a));
        DeltaPacket revealed = board.manager.collectDeltas(board.gameView, viewers);

        Assert.assertFalse(revealed.getNewObjects().containsKey(cardKey(LIBRARY_CARD)), "same instance, so no replacement");
        Map<TrackableProperty, Object> filled = revealed.getObjectDeltas().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(filled, "revealed card not sent");
        Map<TrackableProperty, Object> held = board.libraryCard.getProps();
        Assert.assertEquals(filled.keySet(), held.keySet(), "the full map");
        Assert.assertEquals(filled.get(TrackableProperty.Name), "Grizzly Bears");
        Assert.assertEquals(stateViewsOf(revealed.getNewObjects(), LIBRARY_CARD).size(), 2, "state views arrive as new objects");

        // The reveal ends and the card stays FULL: the change to PlayerMayLook goes out, nothing clears the card
        board.libraryCard.set(TrackableProperty.PlayerMayLook, null);
        DeltaPacket ended = board.manager.collectDeltas(board.gameView, viewers);
        Map<TrackableProperty, Object> change = ended.getObjectDeltas().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(change, "the end of the reveal not sent");
        Assert.assertEquals(change.keySet(), Set.of(TrackableProperty.PlayerMayLook), "more than the reveal's end sent");
        Assert.assertTrue(stateViewsOf(ended.getObjectDeltas(), LIBRARY_CARD).isEmpty(), "state views changed");
        Assert.assertTrue(stateViewsOf(ended.getNewObjects(), LIBRARY_CARD).isEmpty(), "state views sent again");

        // The next clean walk sends nothing for the card or its state views
        DeltaPacket clean = board.manager.collectDeltas(board.gameView, viewers);
        Assert.assertFalse(clean.getObjectDeltas().containsKey(cardKey(LIBRARY_CARD)), "card sent on a clean walk");
        Assert.assertFalse(clean.getNewObjects().containsKey(cardKey(LIBRARY_CARD)), "card sent on a clean walk");
        Assert.assertTrue(stateViewsOf(clean.getObjectDeltas(), LIBRARY_CARD).isEmpty(), "state views sent on a clean walk");
        Assert.assertTrue(stateViewsOf(clean.getNewObjects(), LIBRARY_CARD).isEmpty(), "state views sent on a clean walk");
    }

    @Test
    public void testRevealHistoryCardGoesInFull() {
        Board board = new Board();
        // A card in the game's reveal history goes in full to every consumer
        board.gameView.updateRevealedCards(new TrackableCollection<>(board.libraryCard));
        DeltaPacket packet = board.manager.collectDeltas(board.gameView, List.of(board.a));

        Map<TrackableProperty, Object> sent = packet.getNewObjects().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(sent, "card not sent");
        Map<TrackableProperty, Object> held = board.libraryCard.getProps();
        Assert.assertEquals(sent.keySet(), held.keySet(), "the full map");
        Assert.assertEquals(sent.get(TrackableProperty.Name), "Grizzly Bears");
        Assert.assertEquals(stateViewsOf(packet.getNewObjects(), LIBRARY_CARD).size(), 2, "current and alternate state views");
    }

    @Test
    public void testCleanShellSendsNothingUntilItsLevelChanges() {
        Board board = new Board();
        // B, the controller, may look at it; nobody on this proxy is B
        board.libraryCard.set(TrackableProperty.PlayerMayLook, new TrackableCollection<>(board.b));
        board.manager.collectDeltas(board.gameView, List.of());

        DeltaPacket again = board.manager.collectDeltas(board.gameView, List.of());
        Assert.assertFalse(again.getObjectDeltas().containsKey(cardKey(LIBRARY_CARD)), "clean shell sent again");
        Assert.assertFalse(again.getNewObjects().containsKey(cardKey(LIBRARY_CARD)), "clean shell sent again");

        // A becomes B's MindSlaveMaster and this proxy's viewer. MindSlaveMaster is on B's view, so the card is dirty on
        // nothing: the level change alone sends it
        board.b.set(TrackableProperty.MindSlaveMaster, board.a);
        DeltaPacket mastered = board.manager.collectDeltas(board.gameView, List.of(board.a));
        Map<TrackableProperty, Object> delta = mastered.getObjectDeltas().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(delta, "clean held shell not sent when its level changed");
        Assert.assertEquals(delta.get(TrackableProperty.Name), "Grizzly Bears");
    }

    @Test
    public void testZoneChangeUnderAFreezeIsClassifiedByTheZoneItShips() {
        Board board = new Board();
        List<PlayerView> viewers = List.of(board.a);
        board.manager.collectDeltas(board.gameView, viewers);
        // Library to battlefield keeps the instance. Under a freeze the live zone still reads Library while the packet
        // carries the delayed Battlefield, and the level must follow the zone the packet carries
        board.tracker.freeze();
        board.libraryCard.set(TrackableProperty.Zone, ZoneType.Battlefield);
        DeltaPacket packet = board.manager.collectDeltas(board.gameView, viewers);
        board.tracker.unfreeze();

        Map<TrackableProperty, Object> delta = packet.getObjectDeltas().get(cardKey(LIBRARY_CARD));
        Assert.assertNotNull(delta, "card not sent");
        Assert.assertEquals(delta.get(TrackableProperty.Zone), ZoneType.Battlefield);
        Assert.assertEquals(delta.get(TrackableProperty.Name), "Grizzly Bears", "shipped to the battlefield as a shell");
    }

    @Test
    public void testReplacementOfAFullCardIsClassifiedAfresh() {
        Board board = new Board();
        List<PlayerView> viewers = List.of(board.a);
        board.libraryCard.set(TrackableProperty.PlayerMayLook, new TrackableCollection<>(board.a));
        board.manager.collectDeltas(board.gameView, viewers);
        // Another instance of the card at the same key, the copy a zone change makes, with no look on it
        CardView copy = board.card(LIBRARY_CARD, ZoneType.Library, "Grizzly Bears");
        board.b.set(TrackableProperty.Library, new TrackableCollection<>(copy));
        assertShell(board.manager.collectDeltas(board.gameView, viewers), "a new instance of a card sent in full");
    }

    private static void assertCensus(Board board, Set<CardView> shown, int named, String what) {
        int[] counts = HeadlessNetworkClient.countHiddenInfo(board.gameView, List.of(board.a), shown);
        Assert.assertNotNull(counts, what + ": nothing counted");
        Assert.assertEquals(counts[0], 1, what + ": library cards held");
        Assert.assertEquals(counts[1], named, what + ": named library cards A may not see");
    }

    @Test
    public void testHiddenInfoCensusExemptsOnlyTheInstancesShown() {
        // The count the network tests assert on, for viewer A and B's named library card. An instance A may see joins
        // the shown set and is not counted after the look ends; another set, or another instance, does not exempt it
        Board board = new Board();
        Set<CardView> shown = Collections.newSetFromMap(new IdentityHashMap<>());
        board.libraryCard.set(TrackableProperty.PlayerMayLook, new TrackableCollection<>(board.a));
        assertCensus(board, shown, 0, "while A may look");
        board.libraryCard.set(TrackableProperty.PlayerMayLook, null);
        assertCensus(board, shown, 0, "after the look, with the set that saw it");
        assertCensus(board, Collections.newSetFromMap(new IdentityHashMap<>()), 1, "after the look, with a set that did not");
        CardView copy = board.card(LIBRARY_CARD, ZoneType.Library, "Grizzly Bears");
        board.b.set(TrackableProperty.Library, new TrackableCollection<>(copy));
        assertCensus(board, shown, 1, "another instance of the card, with the set that saw the first");
    }

}
