package forge.gamesimulationtests;

import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Test harness for testing network play features on a single computer.
 * Provides utilities to simulate client connections, disconnections, and reconnections.
 */
public class LocalNetworkTestHarness {
    private static final int DEFAULT_TEST_PORT = 36750; // Different from default to avoid conflicts

    private FServerManager server;
    private final List<MockNetworkClient> clients = new ArrayList<>();
    private int port;
    private boolean serverStarted = false;

    public LocalNetworkTestHarness() {
        this(DEFAULT_TEST_PORT);
    }

    public LocalNetworkTestHarness(int port) {
        this.port = port;
    }

    /**
     * Start the test server.
     */
    public void startServer() {
        if (serverStarted) {
            return;
        }
        server = FServerManager.getInstance();
        // Note: In a real test, you'd need to handle the server lifecycle properly
        // For unit tests, you may need to mock certain aspects
        serverStarted = true;
    }

    /**
     * Stop the test server and clean up all clients.
     */
    public void stopServer() {
        cleanup();
        if (server != null && serverStarted) {
            server.stopServer();
            serverStarted = false;
        }
    }

    /**
     * Create a mock network client for testing.
     * @param playerName the player name
     * @return the mock client
     */
    public MockNetworkClient createClient(String playerName) {
        MockNetworkClient client = new MockNetworkClient(playerName, port);
        clients.add(client);
        return client;
    }

    /**
     * Simulate a client disconnect.
     * @param client the client to disconnect
     */
    public void simulateDisconnect(MockNetworkClient client) {
        client.simulateDisconnect();
    }

    /**
     * Clean up all test resources.
     */
    public void cleanup() {
        for (MockNetworkClient client : clients) {
            client.close();
        }
        clients.clear();
    }

    /**
     * Get the test port.
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the server instance.
     * @return the FServerManager instance
     */
    public FServerManager getServer() {
        return server;
    }

    /**
     * Mock network client for testing.
     * Simulates client-side network behavior without actual network connections.
     */
    public static class MockNetworkClient {
        private final String playerName;
        private final int port;
        private boolean connected = false;
        private final List<DeltaPacket> receivedDeltas = new ArrayList<>();
        private FullStatePacket lastFullState;
        private long lastAcknowledgedSequence = 0;

        public MockNetworkClient(String playerName, int port) {
            this.playerName = playerName;
            this.port = port;
        }

        /**
         * Simulate connecting to the server.
         */
        public void connect() {
            connected = true;
        }

        /**
         * Simulate disconnecting from the server.
         */
        public void simulateDisconnect() {
            connected = false;
        }

        /**
         * Close the client connection.
         */
        public void close() {
            connected = false;
            receivedDeltas.clear();
            lastFullState = null;
        }

        /**
         * Simulate receiving a delta packet.
         * @param packet the delta packet
         */
        public void receiveDelta(DeltaPacket packet) {
            receivedDeltas.add(packet);
            lastAcknowledgedSequence = packet.getSequenceNumber();
        }

        /**
         * Simulate receiving a full state packet.
         * @param packet the full state packet
         */
        public void receiveFullState(FullStatePacket packet) {
            lastFullState = packet;
            lastAcknowledgedSequence = packet.getSequenceNumber();
            receivedDeltas.clear(); // Clear deltas since we have full state
        }

        /**
         * Get all received delta packets.
         * @return list of delta packets
         */
        public List<DeltaPacket> getReceivedDeltas() {
            return new ArrayList<>(receivedDeltas);
        }

        /**
         * Get the last full state packet.
         * @return the last full state packet
         */
        public FullStatePacket getLastFullState() {
            return lastFullState;
        }

        /**
         * Get the last acknowledged sequence number.
         * @return the sequence number
         */
        public long getLastAcknowledgedSequence() {
            return lastAcknowledgedSequence;
        }

        /**
         * Calculate total size of received deltas.
         * @return total bytes received via delta packets
         */
        public int getTotalDeltaBytes() {
            int total = 0;
            for (DeltaPacket delta : receivedDeltas) {
                total += delta.getApproximateSize();
            }
            return total;
        }

        /**
         * Check if client is connected.
         * @return true if connected
         */
        public boolean isConnected() {
            return connected;
        }

        /**
         * Get the player name.
         * @return the player name
         */
        public String getPlayerName() {
            return playerName;
        }

    }
}
