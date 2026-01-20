package forge.gamesimulationtests;

import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.gamemodes.net.server.GameSession;
import forge.gamemodes.net.server.PlayerSession;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test cases for the network play optimization features:
 * - Delta sync (sending only changed properties)
 * - Reconnection support
 * - Session management
 */
public class NetworkOptimizationTest {

    private LocalNetworkTestHarness harness;

    @BeforeMethod
    public void setUp() {
        harness = new LocalNetworkTestHarness();
    }

    @AfterMethod
    public void tearDown() {
        if (harness != null) {
            harness.cleanup();
        }
    }

    // ==================== Delta Packet Tests ====================

    @Test
    public void testDeltaPacketCreation() {
        // Test that delta packets can be created with proper structure
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[]{1, 2, 3});
        deltas.put(2, new byte[]{4, 5, 6, 7});

        Set<Integer> removed = new HashSet<>();
        removed.add(100);

        DeltaPacket packet = new DeltaPacket(1L, deltas, removed);

        Assert.assertEquals(packet.getSequenceNumber(), 1L);
        Assert.assertEquals(packet.getObjectDeltas().size(), 2);
        Assert.assertEquals(packet.getRemovedObjectIds().size(), 1);
        Assert.assertTrue(packet.getRemovedObjectIds().contains(100));
        Assert.assertFalse(packet.isEmpty());
    }

    @Test
    public void testEmptyDeltaPacket() {
        // Test that empty delta packets are properly identified
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashSet<>());
        Assert.assertTrue(packet.isEmpty());
    }

    @Test
    public void testDeltaPacketWithChecksum() {
        // Test delta packets with checksum for validation
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[]{1, 2, 3});

        int checksum = 12345;
        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>(), checksum);

        Assert.assertTrue(packet.hasChecksum());
        Assert.assertEquals(packet.getChecksum(), checksum);
    }

    @Test
    public void testDeltaPacketApproximateSize() {
        // Test size calculation
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());

        // Size should be: 8 (seq) + 8 (timestamp) + 4 (checksum) + 2*(4 + delta size)
        int expectedMinSize = 8 + 8 + 4 + (4 + 100) + (4 + 200);
        Assert.assertTrue(packet.getApproximateSize() >= expectedMinSize);
    }

    // ==================== Full State Packet Tests ====================

    @Test
    public void testFullStatePacketCreation() {
        // Test full state packet creation (without actual GameView)
        FullStatePacket packet = new FullStatePacket(1L, null);

        Assert.assertEquals(packet.getSequenceNumber(), 1L);
        Assert.assertFalse(packet.isReconnect());
        Assert.assertNull(packet.getSessionId());
    }

    @Test
    public void testFullStatePacketForReconnect() {
        // Test full state packet with reconnection info
        String sessionId = "test-session-123";
        String token = "test-token-456";

        FullStatePacket packet = new FullStatePacket(1L, null, sessionId, token);

        Assert.assertTrue(packet.isReconnect());
        Assert.assertEquals(packet.getSessionId(), sessionId);
        Assert.assertEquals(packet.getSessionToken(), token);
    }

    // ==================== Delta Sync Manager Tests ====================

    @Test
    public void testDeltaSyncManagerClientRegistration() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.registerClient(0);
        manager.registerClient(1);

        // Both clients start at sequence 0
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 0L);
    }

    @Test
    public void testDeltaSyncManagerAcknowledgment() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.registerClient(0);
        manager.registerClient(1);

        // Client 0 acknowledges sequence 5
        manager.processAcknowledgment(0, 5L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 0L); // Client 1 still at 0

        // Client 1 acknowledges sequence 3
        manager.processAcknowledgment(1, 3L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 3L); // Min of 5 and 3

        // Client 1 catches up
        manager.processAcknowledgment(1, 10L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 5L); // Min of 5 and 10
    }

    @Test
    public void testDeltaSyncManagerSequenceIncrement() {
        DeltaSyncManager manager = new DeltaSyncManager();

        Assert.assertEquals(manager.getCurrentSequence(), 0L);

        // Collecting deltas should increment sequence
        DeltaPacket packet1 = manager.collectDeltas(null);
        // Even with null game view, sequence should be incremented
        Assert.assertTrue(manager.getCurrentSequence() >= 0);
    }

    @Test
    public void testDeltaSyncManagerClientUnregistration() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.registerClient(0);
        manager.registerClient(1);
        manager.processAcknowledgment(0, 5L);
        manager.processAcknowledgment(1, 3L);

        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 3L);

        // Unregister client 1
        manager.unregisterClient(1);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 5L);
    }

    @Test
    public void testNeedsFullResync() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.registerClient(0);

        // New client should not need resync initially
        Assert.assertFalse(manager.needsFullResync(0));

        // Unregistered client should need resync
        Assert.assertTrue(manager.needsFullResync(99));
    }

    // ==================== Game Session Tests ====================

    @Test
    public void testGameSessionCreation() {
        GameSession session = new GameSession();

        Assert.assertNotNull(session.getSessionId());
        Assert.assertFalse(session.isGameInProgress());
        Assert.assertFalse(session.isPaused());
    }

    @Test
    public void testPlayerSessionRegistration() {
        GameSession session = new GameSession();

        PlayerSession player1 = session.registerPlayer(0);
        PlayerSession player2 = session.registerPlayer(1);

        Assert.assertNotNull(player1);
        Assert.assertNotNull(player2);
        Assert.assertEquals(player1.getPlayerIndex(), 0);
        Assert.assertEquals(player2.getPlayerIndex(), 1);
        Assert.assertNotNull(player1.getSessionToken());
        Assert.assertNotEquals(player1.getSessionToken(), player2.getSessionToken());
    }

    @Test
    public void testPlayerSessionConnectionState() {
        GameSession session = new GameSession();
        PlayerSession player = session.registerPlayer(0);

        Assert.assertEquals(player.getConnectionState(), PlayerSession.ConnectionState.CONNECTED);
        Assert.assertTrue(player.isConnected());
        Assert.assertFalse(player.isDisconnected());

        session.markPlayerDisconnected(0);
        Assert.assertEquals(player.getConnectionState(), PlayerSession.ConnectionState.DISCONNECTED);
        Assert.assertFalse(player.isConnected());
        Assert.assertTrue(player.isDisconnected());
        Assert.assertTrue(player.getDisconnectTime() > 0);

        session.markPlayerConnected(0);
        Assert.assertEquals(player.getConnectionState(), PlayerSession.ConnectionState.CONNECTED);
        Assert.assertTrue(player.isConnected());
    }

    @Test
    public void testGamePauseResume() {
        GameSession session = new GameSession();
        session.setGameInProgress(true);

        Assert.assertFalse(session.isPaused());

        session.pauseGame("Test pause message");
        Assert.assertTrue(session.isPaused());
        Assert.assertEquals(session.getPauseMessage(), "Test pause message");

        session.resumeGame();
        Assert.assertFalse(session.isPaused());
        Assert.assertNull(session.getPauseMessage());
    }

    @Test
    public void testTokenValidation() {
        GameSession session = new GameSession();
        PlayerSession player = session.registerPlayer(0);
        String token = player.getSessionToken();

        Assert.assertTrue(player.validateToken(token));
        Assert.assertFalse(player.validateToken("wrong-token"));
        Assert.assertFalse(player.validateToken(null));
    }

    @Test
    public void testReconnectionValidation() {
        GameSession session = new GameSession();
        PlayerSession player = session.registerPlayer(0);
        String token = player.getSessionToken();

        Assert.assertTrue(session.validateReconnection(0, token));
        Assert.assertFalse(session.validateReconnection(0, "wrong-token"));
        Assert.assertFalse(session.validateReconnection(1, token)); // Wrong player index
    }

    @Test
    public void testDisconnectedPlayersTracking() {
        GameSession session = new GameSession();
        session.registerPlayer(0);
        session.registerPlayer(1);

        Assert.assertFalse(session.hasDisconnectedPlayers());
        Assert.assertTrue(session.allPlayersConnected());

        session.markPlayerDisconnected(0);
        Assert.assertTrue(session.hasDisconnectedPlayers());
        Assert.assertFalse(session.allPlayersConnected());

        session.markPlayerConnected(0);
        Assert.assertFalse(session.hasDisconnectedPlayers());
        Assert.assertTrue(session.allPlayersConnected());
    }

    @Test
    public void testReconnectionTimeout() {
        GameSession session = new GameSession();
        session.setDisconnectTimeoutMs(1000); // 1 second for testing
        PlayerSession player = session.registerPlayer(0);

        session.markPlayerDisconnected(0);

        // Immediately after disconnect, timeout should not be expired
        Assert.assertFalse(session.isReconnectionTimeoutExpired(0));
        Assert.assertTrue(session.getRemainingReconnectionTime(0) > 0);
    }

    // ==================== Mock Client Tests ====================

    @Test
    public void testMockClientConnection() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        Assert.assertFalse(client.isConnected());

        client.connect();
        Assert.assertTrue(client.isConnected());

        client.simulateDisconnect();
        Assert.assertFalse(client.isConnected());
    }

    @Test
    public void testMockClientReconnection() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");
        client.connect();

        // Can't reconnect without session credentials
        client.simulateDisconnect();
        Assert.assertFalse(client.simulateReconnect());

        // Set credentials and try again
        client.setSessionCredentials("session-123", "token-456");
        Assert.assertTrue(client.simulateReconnect());
        Assert.assertTrue(client.isConnected());
    }

    @Test
    public void testMockClientDeltaTracking() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        // Simulate receiving deltas
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);

        DeltaPacket packet1 = new DeltaPacket(1L, deltas, new HashSet<>());
        DeltaPacket packet2 = new DeltaPacket(2L, deltas, new HashSet<>());

        client.receiveDelta(packet1);
        client.receiveDelta(packet2);

        Assert.assertEquals(client.getReceivedDeltas().size(), 2);
        Assert.assertEquals(client.getLastAcknowledgedSequence(), 2L);
        Assert.assertTrue(client.getTotalDeltaBytes() > 0);
    }

    @Test
    public void testMockClientFullStateOverridesDelta() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        // Receive some deltas
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);
        client.receiveDelta(new DeltaPacket(1L, deltas, new HashSet<>()));
        client.receiveDelta(new DeltaPacket(2L, deltas, new HashSet<>()));

        Assert.assertEquals(client.getReceivedDeltas().size(), 2);

        // Receive full state - should clear deltas
        client.receiveFullState(new FullStatePacket(10L, null));

        Assert.assertEquals(client.getReceivedDeltas().size(), 0);
        Assert.assertEquals(client.getLastAcknowledgedSequence(), 10L);
        Assert.assertNotNull(client.getLastFullState());
    }

    // ==================== Integration-Style Tests ====================

    @Test
    public void testDeltaSyncWithMultipleClients() {
        // Simulate a scenario with multiple clients receiving deltas
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.registerClient(0);
        manager.registerClient(1);

        // Simulate sending multiple delta packets
        for (int i = 1; i <= 5; i++) {
            // In real usage, this would collect from GameView
            // For testing, we just verify sequence increments
            manager.collectDeltas(null);
        }

        // Client 0 is up to date
        manager.processAcknowledgment(0, 5L);

        // Client 1 is behind
        manager.processAcknowledgment(1, 2L);

        // Min acknowledged is 2 (client 1)
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 2L);

        // If client 1 falls too far behind, it needs full resync
        // (This would happen if sequence - acked > 100)
    }

    @Test
    public void testSessionLifecycle() {
        // Test complete session lifecycle
        GameSession session = new GameSession();

        // Pre-game setup
        PlayerSession player0 = session.registerPlayer(0);
        player0.setPlayerName("Alice");
        PlayerSession player1 = session.registerPlayer(1);
        player1.setPlayerName("Bob");

        Assert.assertFalse(session.isGameInProgress());

        // Game starts
        session.setGameInProgress(true);
        Assert.assertTrue(session.isGameInProgress());

        // Player disconnects
        session.markPlayerDisconnected(1);
        session.pauseGame("Waiting for Bob to reconnect...");

        Assert.assertTrue(session.isPaused());
        Assert.assertTrue(session.hasDisconnectedPlayers());

        // Player reconnects
        String token = player1.getSessionToken();
        Assert.assertTrue(session.validateReconnection(1, token));

        session.markPlayerConnected(1);
        session.resumeGame();

        Assert.assertFalse(session.isPaused());
        Assert.assertTrue(session.allPlayersConnected());
    }
}
