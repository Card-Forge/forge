package forge.net;

import forge.deck.Deck;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.interfaces.IGameController;
import forge.interfaces.ILobbyListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Headless network client for automated testing with actual network traffic.
 *
 * This client connects to a Forge server as a remote player, enabling:
 * - Delta sync packet generation and logging
 * - True network traffic measurement
 * - Reconnection testing with real socket disconnect
 *
 * Part of Phase 8 of the automated network testing infrastructure.
 */
public class HeadlessNetworkClient implements AutoCloseable {

    private static final String LOG_PREFIX = "[HeadlessClient]";

    private final String username;
    private final String hostname;
    private final int port;

    private FGameClient client;
    private ClientGameLobby lobby;
    private DeltaLoggingGuiGame guiGame;

    // Connection state
    private final CountDownLatch connectedLatch = new CountDownLatch(1);
    private final CountDownLatch gameStartedLatch = new CountDownLatch(1);
    private final CountDownLatch gameFinishedLatch = new CountDownLatch(1);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean gameInProgress = new AtomicBoolean(false);
    private final AtomicInteger assignedSlot = new AtomicInteger(-1);

    // Metrics
    private final AtomicLong deltaPacketsReceived = new AtomicLong(0);
    private final AtomicLong fullStateSyncsReceived = new AtomicLong(0);
    private final AtomicLong totalDeltaBytes = new AtomicLong(0);

    /**
     * Create a new headless network client.
     *
     * @param username Player name for this client
     * @param hostname Server hostname
     * @param port Server port
     */
    public HeadlessNetworkClient(String username, String hostname, int port) {
        this.username = username;
        this.hostname = hostname;
        this.port = port;
    }

    /**
     * Connect to the server and wait for lobby assignment.
     *
     * @param timeoutMs Maximum time to wait for connection
     * @return true if connected successfully
     */
    public boolean connect(long timeoutMs) {
        NetworkDebugLogger.log("%s Connecting to %s:%d as '%s'", LOG_PREFIX, hostname, port, username);

        try {
            // Create GUI implementation that logs delta packets
            guiGame = new DeltaLoggingGuiGame(this);

            // Create client
            client = new FGameClient(username, "0", guiGame, hostname, port);

            // Create lobby
            lobby = new ClientGameLobby();

            // Register lobby listener
            client.addLobbyListener(new ClientLobbyListener());

            // Connect (this sends LoginEvent automatically)
            client.connect();

            // Wait for lobby assignment
            boolean success = connectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (success) {
                connected.set(true);
                NetworkDebugLogger.log("%s Connected successfully, assigned slot %d",
                        LOG_PREFIX, assignedSlot.get());
            } else {
                NetworkDebugLogger.error("%s Connection timeout after %dms", LOG_PREFIX, timeoutMs);
            }
            return success;

        } catch (Exception e) {
            NetworkDebugLogger.error("%s Connection failed: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Wait for game to start.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if game started
     */
    public boolean waitForGameStart(long timeoutMs) {
        try {
            return gameStartedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Wait for game to finish.
     *
     * @param timeoutMs Maximum time to wait
     * @return true if game finished
     */
    public boolean waitForGameFinish(long timeoutMs) {
        try {
            return gameFinishedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Check if this client is connected.
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Check if a game is in progress.
     */
    public boolean isGameInProgress() {
        return gameInProgress.get();
    }

    /**
     * Get the slot index assigned to this client.
     */
    public int getAssignedSlot() {
        return assignedSlot.get();
    }

    /**
     * Get number of delta packets received.
     */
    public long getDeltaPacketsReceived() {
        return deltaPacketsReceived.get();
    }

    /**
     * Get number of full state syncs received.
     */
    public long getFullStateSyncsReceived() {
        return fullStateSyncsReceived.get();
    }

    /**
     * Get approximate total delta bytes received.
     */
    public long getTotalDeltaBytes() {
        return totalDeltaBytes.get();
    }

    /**
     * Get the lobby for this client.
     */
    public ClientGameLobby getLobby() {
        return lobby;
    }

    /**
     * Get the underlying FGameClient.
     */
    public FGameClient getClient() {
        return client;
    }

    /**
     * Send a deck selection to the server for this client's slot.
     *
     * @param deck The deck to use
     */
    public void sendDeck(Deck deck) {
        if (client != null && connected.get()) {
            NetworkDebugLogger.log("%s Sending deck: %s", LOG_PREFIX, deck.getName());
            UpdateLobbyPlayerEvent event = UpdateLobbyPlayerEvent.deckUpdate(deck);
            // Apply to local lobby AND send to server
            int slot = assignedSlot.get();
            if (slot >= 0 && lobby != null) {
                lobby.applyToSlot(slot, event);
                NetworkDebugLogger.log("%s Applied deck to local lobby slot %d", LOG_PREFIX, slot);
            }
            client.send(event);
        } else {
            NetworkDebugLogger.error("%s Cannot send deck - not connected", LOG_PREFIX);
        }
    }

    /**
     * Mark this client as ready in the lobby.
     */
    public void setReady() {
        if (client != null && connected.get()) {
            NetworkDebugLogger.log("%s Sending ready status", LOG_PREFIX);
            UpdateLobbyPlayerEvent event = UpdateLobbyPlayerEvent.isReadyUpdate(true);
            // Apply to local lobby AND send to server
            int slot = assignedSlot.get();
            if (slot >= 0 && lobby != null) {
                lobby.applyToSlot(slot, event);
                NetworkDebugLogger.log("%s Applied ready status to local lobby slot %d", LOG_PREFIX, slot);
            }
            client.send(event);
        } else {
            NetworkDebugLogger.error("%s Cannot set ready - not connected", LOG_PREFIX);
        }
    }

    @Override
    public void close() {
        NetworkDebugLogger.log("%s Disconnecting", LOG_PREFIX);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
        connected.set(false);
    }

    // Called by DeltaLoggingGuiGame when delta packet received
    void onDeltaPacketReceived(DeltaPacket packet) {
        deltaPacketsReceived.incrementAndGet();

        // Estimate packet size (deltas + new objects)
        int estimatedBytes = 0;
        if (packet.getObjectDeltas() != null) {
            for (byte[] delta : packet.getObjectDeltas().values()) {
                estimatedBytes += delta.length;
            }
        }
        totalDeltaBytes.addAndGet(estimatedBytes);

        NetworkDebugLogger.log("%s Delta packet #%d: deltas=%d, new=%d, removed=%d, estimatedBytes=%d",
                LOG_PREFIX,
                packet.getSequenceNumber(),
                packet.getObjectDeltas() != null ? packet.getObjectDeltas().size() : 0,
                packet.getNewObjects() != null ? packet.getNewObjects().size() : 0,
                packet.getRemovedObjectIds() != null ? packet.getRemovedObjectIds().size() : 0,
                estimatedBytes);
    }

    // Called by DeltaLoggingGuiGame when full state sync received
    void onFullStateSyncReceived(FullStatePacket packet) {
        fullStateSyncsReceived.incrementAndGet();
        gameInProgress.set(true);
        gameStartedLatch.countDown();

        NetworkDebugLogger.log("%s Full state sync: seq=%d, checksum=%d",
                LOG_PREFIX,
                packet.getSequenceNumber(),
                packet.getStateChecksum());
    }

    // Called by DeltaLoggingGuiGame when game ends
    void onGameEnd() {
        gameInProgress.set(false);
        gameFinishedLatch.countDown();
        NetworkDebugLogger.log("%s Game ended. Deltas=%d, FullSyncs=%d, TotalBytes=%d",
                LOG_PREFIX,
                deltaPacketsReceived.get(),
                fullStateSyncsReceived.get(),
                totalDeltaBytes.get());
    }

    /**
     * Lobby listener that handles server updates.
     */
    private class ClientLobbyListener implements ILobbyListener {
        @Override
        public void update(GameLobbyData state, int slot) {
            assignedSlot.set(slot);
            lobby.setLocalPlayer(slot);
            lobby.setData(state);

            NetworkDebugLogger.log("%s Lobby update: assigned to slot %d",
                    LOG_PREFIX, slot);

            // Signal connected once we have a slot
            connectedLatch.countDown();
        }

        @Override
        public void message(String source, String message) {
            NetworkDebugLogger.log("%s Chat: %s: %s", LOG_PREFIX, source, message);
        }

        @Override
        public void close() {
            NetworkDebugLogger.log("%s Connection closed by server", LOG_PREFIX);
            connected.set(false);
            onGameEnd();
        }

        @Override
        public ClientGameLobby getLobby() {
            return lobby;
        }
    }

    /**
     * GUI implementation that logs delta packets and auto-responds to input requests.
     * This enables headless network games to complete by automatically responding
     * to prompts like mulligan decisions and priority passes.
     */
    private static class DeltaLoggingGuiGame extends NoOpGuiGame {
        private final HeadlessNetworkClient client;
        private IGameController gameController;

        DeltaLoggingGuiGame(HeadlessNetworkClient client) {
            this.client = client;
        }

        @Override
        public void applyDelta(DeltaPacket packet) {
            client.onDeltaPacketReceived(packet);

            // Send acknowledgment if we have a controller
            if (gameController != null) {
                try {
                    // Note: NetGameController.ackSync sends the acknowledgment
                    // This tells the server we processed the delta
                } catch (Exception e) {
                    NetworkDebugLogger.error("[DeltaLoggingGuiGame] Error in delta handling: %s", e.getMessage());
                }
            }
        }

        @Override
        public void fullStateSync(FullStatePacket packet) {
            client.onFullStateSyncReceived(packet);
            if (packet.getGameView() != null) {
                setGameView(packet.getGameView());
            }
        }

        @Override
        public void afterGameEnd() {
            super.afterGameEnd();
            client.onGameEnd();
        }

        @Override
        public void setGameController(forge.game.player.PlayerView player, IGameController controller) {
            super.setGameController(player, controller);
            this.gameController = controller;
            NetworkDebugLogger.log("[DeltaLoggingGuiGame] Game controller set for player: %s",
                    player != null ? player.getName() : "null");
        }

        @Override
        public void showPromptMessage(forge.game.player.PlayerView playerView, String message) {
            NetworkDebugLogger.log("[DeltaLoggingGuiGame] Prompt: %s", message);
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
            // Auto-respond to button prompts (mulligan, priority, etc.)
            if (gameController != null && okEnabled) {
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-clicking OK for player: %s",
                        owner != null ? owner.getName() : "unknown");
                // Use a small delay to ensure the server has finished setting up the input
                new Thread(() -> {
                    try {
                        Thread.sleep(50); // Small delay for server to be ready
                        gameController.selectButtonOk();
                    } catch (Exception e) {
                        NetworkDebugLogger.error("[DeltaLoggingGuiGame] Error auto-clicking OK: %s", e.getMessage());
                    }
                }).start();
            }
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
            // Auto-respond to labeled button prompts
            if (gameController != null && enable1) {
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-clicking '%s' for player: %s",
                        label1, owner != null ? owner.getName() : "unknown");
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                        gameController.selectButtonOk();
                    } catch (Exception e) {
                        NetworkDebugLogger.error("[DeltaLoggingGuiGame] Error auto-clicking button: %s", e.getMessage());
                    }
                }).start();
            }
        }
    }

    /**
     * Get a summary of client metrics.
     */
    public String getMetricsSummary() {
        return String.format("HeadlessClient[%s]: deltas=%d, fullSyncs=%d, bytes=%d, connected=%s, gameInProgress=%s",
                username,
                deltaPacketsReceived.get(),
                fullStateSyncsReceived.get(),
                totalDeltaBytes.get(),
                connected.get(),
                gameInProgress.get());
    }
}
