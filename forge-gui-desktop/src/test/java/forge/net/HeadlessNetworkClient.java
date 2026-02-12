package forge.net;

import forge.game.GameView;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
        /** Special value indicating the client hasn't been assigned a slot yet.
         * Must match RemoteClient.UNASSIGNED_SLOT on the server. */
        private static final int UNASSIGNED_SLOT = -1;

        @Override
        public void update(GameLobbyData state, int slot) {
            // Always update the lobby data
            lobby.setData(state);

            // Only update slot assignment if we received a valid slot.
            // The first LobbyUpdateEvent after connection may have slot=-1 (UNASSIGNED_SLOT)
            // because the server hasn't processed our LoginEvent yet.
            if (slot >= 0) {
                int previousSlot = assignedSlot.get();
                assignedSlot.set(slot);
                lobby.setLocalPlayer(slot);

                NetworkDebugLogger.log("%s Lobby update: assigned to slot %d (previous=%d)",
                        LOG_PREFIX, slot, previousSlot);

                // Signal connected once we have a valid slot assignment
                if (previousSlot == -1 && slot >= 0) {
                    connectedLatch.countDown();
                }
            } else {
                NetworkDebugLogger.log("%s Lobby update: slot not yet assigned (slot=%d)",
                        LOG_PREFIX, slot);
            }
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
     * Extends HeadlessNetworkGuiGame to get proper delta packet processing
     * (deserialization, tracker updates, object creation) while providing
     * auto-response behavior for headless testing.
     *
     * IMPORTANT: All auto-responses are serialized through a single-threaded executor
     * to prevent race conditions where multiple response threads interfere with each other.
     * Each new prompt cancels any pending auto-response from the previous prompt.
     */
    private static class DeltaLoggingGuiGame extends HeadlessNetworkGuiGame {
        private final HeadlessNetworkClient client;
        private IGameController gameController;
        // Track selectable cards for multi-selection prompts (e.g., "discard 2 cards")
        private final java.util.List<forge.game.card.CardView> pendingSelectables = new java.util.ArrayList<>();
        private int selectableIndex = 0;

        // Single-threaded executor to serialize all auto-responses and prevent race conditions
        private final ScheduledExecutorService autoResponseExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HeadlessClient-AutoResponse");
            t.setDaemon(true);
            return t;
        });
        // Track the current pending auto-response so we can cancel it when a new prompt arrives
        private volatile ScheduledFuture<?> pendingAutoResponse = null;
        // Lock for coordinating auto-response scheduling
        private final Object autoResponseLock = new Object();

        DeltaLoggingGuiGame(HeadlessNetworkClient client) {
            this.client = client;
        }

        /**
         * Cancel any pending auto-response. Called when a new prompt arrives
         * to prevent stale responses from interfering with the new prompt.
         */
        private void cancelPendingAutoResponse(String reason) {
            synchronized (autoResponseLock) {
                if (pendingAutoResponse != null && !pendingAutoResponse.isDone()) {
                    pendingAutoResponse.cancel(false);
                    NetworkDebugLogger.log("[DeltaLoggingGuiGame] Cancelled pending auto-response: %s", reason);
                }
                pendingAutoResponse = null;
            }
        }

        /**
         * Schedule an auto-response action with the given delay.
         * Cancels any previously pending auto-response first.
         */
        private void scheduleAutoResponse(Runnable action, long delayMs, String description) {
            synchronized (autoResponseLock) {
                // Cancel any pending response first
                cancelPendingAutoResponse("scheduling new: " + description);

                pendingAutoResponse = autoResponseExecutor.schedule(() -> {
                    try {
                        // Verify we still have a controller before executing
                        if (gameController != null) {
                            action.run();
                        } else {
                            NetworkDebugLogger.log("[DeltaLoggingGuiGame] Skipping auto-response (no controller): %s", description);
                        }
                    } catch (Exception e) {
                        NetworkDebugLogger.error("[DeltaLoggingGuiGame] Error in auto-response '%s': %s", description, e.getMessage());
                    }
                }, delayMs, TimeUnit.MILLISECONDS);

                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Scheduled auto-response in %dms: %s", delayMs, description);
            }
        }

        @Override
        public void applyDelta(DeltaPacket packet) {
            // First, process the delta packet (deserialize, update tracker, etc.)
            super.applyDelta(packet);

            // Then notify the client for logging/verification
            client.onDeltaPacketReceived(packet);
        }

        @Override
        public void fullStateSync(FullStatePacket packet) {
            // First, process the full state sync (tracker setup, etc.)
            super.fullStateSync(packet);

            // Then notify the client for logging/verification
            client.onFullStateSyncReceived(packet);
        }

        @Override
        public void setOriginalGameController(forge.game.player.PlayerView view, IGameController controller) {
            super.setOriginalGameController(view, controller);
            if (controller != null) {
                this.gameController = controller;
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Original game controller set for player: %s",
                        view != null ? view.getName() : "null");
            }
        }

        @Override
        public void setGameController(forge.game.player.PlayerView player, IGameController controller) {
            super.setGameController(player, controller);
            if (controller != null) {
                this.gameController = controller;
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Game controller set for player: %s",
                        player != null ? player.getName() : "null");
            }
        }

        @Override
        public void showPromptMessage(forge.game.player.PlayerView playerView, String message) {
            NetworkDebugLogger.log("[DeltaLoggingGuiGame] Prompt: %s", message);

            // Detect player selection prompts (like "who goes first")
            // These contain "Click on the portrait" in the message
            if (message != null && message.contains("Click on the portrait") && gameController != null) {
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Detected player selection prompt, auto-selecting...");
                scheduleAutoResponse(() -> {
                    // Get the game view and select the first player (or self)
                    GameView gv = getGameView();
                    if (gv != null && gv.getPlayers() != null && !gv.getPlayers().isEmpty()) {
                        forge.game.player.PlayerView toSelect = null;
                        // Prefer to select self if possible
                        for (forge.game.player.PlayerView pv : gv.getPlayers()) {
                            if (pv.getName().equals(client.username)) {
                                toSelect = pv;
                                break;
                            }
                        }
                        // Otherwise select first player
                        if (toSelect == null) {
                            toSelect = gv.getPlayers().iterator().next();
                        }
                        NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-selecting player: %s", toSelect.getName());
                        gameController.selectPlayer(toSelect, null);
                    } else {
                        NetworkDebugLogger.error("[DeltaLoggingGuiGame] Cannot auto-select player - no game view or players");
                    }
                }, 100, "player selection");
            }
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
            NetworkDebugLogger.log("[DeltaLoggingGuiGame] updateButtons(ok): okEnabled=%s, cancelEnabled=%s, controller=%s",
                    okEnabled, cancelEnabled, gameController != null ? "set" : "null");
            // Auto-respond to button prompts (mulligan, priority, etc.)
            if (gameController != null && okEnabled) {
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-clicking OK for player: %s",
                        owner != null ? owner.getName() : "unknown");
                scheduleAutoResponse(() -> gameController.selectButtonOk(), 50, "click OK button");
            }
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
            NetworkDebugLogger.log("[DeltaLoggingGuiGame] updateButtons(labels): '%s'/%s, '%s'/%s, controller=%s",
                    label1, enable1, label2, enable2, gameController != null ? "set" : "null");
            // Auto-respond to labeled button prompts - click first enabled button
            if (gameController != null && (enable1 || enable2)) {
                String clickTarget = enable1 ? label1 : label2;
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-clicking '%s' for player: %s",
                        clickTarget, owner != null ? owner.getName() : "unknown");
                if (enable1) {
                    scheduleAutoResponse(() -> gameController.selectButtonOk(), 50, "click '" + label1 + "'");
                } else {
                    scheduleAutoResponse(() -> gameController.selectButtonCancel(), 50, "click '" + label2 + "'");
                }
            } else if (gameController != null && !enable1 && selectableIndex < pendingSelectables.size()) {
                // OK is disabled but we have more cards to select (multi-selection prompt)
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] OK disabled, selecting next card (%d/%d remaining)",
                        pendingSelectables.size() - selectableIndex, pendingSelectables.size());
                selectNextCard();
            }
        }

        @Override
        public void setSelectables(Iterable<forge.game.card.CardView> cards) {
            super.setSelectables(cards);
            // Track selectable cards for multi-selection prompts
            pendingSelectables.clear();
            selectableIndex = 0;
            if (cards != null) {
                for (forge.game.card.CardView card : cards) {
                    pendingSelectables.add(card);
                }
            }

            // Auto-select the first selectable card when cards become selectable
            if (gameController != null && !pendingSelectables.isEmpty()) {
                selectNextCard();
            }
        }

        /**
         * Select the next card from the pending list.
         * Uses the serialized auto-response executor to prevent race conditions.
         */
        private void selectNextCard() {
            if (selectableIndex < pendingSelectables.size()) {
                forge.game.card.CardView card = pendingSelectables.get(selectableIndex);
                selectableIndex++;
                NetworkDebugLogger.log("[DeltaLoggingGuiGame] Auto-selecting card %d/%d: %s",
                        selectableIndex, pendingSelectables.size(), card.getName());
                scheduleAutoResponse(() -> gameController.selectCard(card, null, null),
                        100, "select card " + card.getName());
            }
        }

        /**
         * Shutdown the auto-response executor when the game ends.
         */
        @Override
        public void afterGameEnd() {
            super.afterGameEnd();
            // Shutdown the executor to clean up resources
            autoResponseExecutor.shutdownNow();
            client.onGameEnd();
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
