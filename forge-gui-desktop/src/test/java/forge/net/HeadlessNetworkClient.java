package forge.net;

import forge.game.GameView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.ChatMessage;
import forge.util.IHasForgeLog;
import forge.gamemodes.match.GameLobby.GameLobbyData;
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
 * Headless TCP client that connects to a Forge server as a remote player.
 * Receives delta sync packets and auto-responds to game prompts, enabling
 * true network traffic measurement without a display.
 *
 * <p>Used by {@link UnifiedNetworkHarness} as the remote player implementation.
 * Internally extends {@link HeadlessNetworkGuiGame} via {@code DeltaLoggingGuiGame}
 * to process delta packets and provide auto-response behavior.
 */
public class HeadlessNetworkClient implements AutoCloseable, IHasForgeLog {

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
    private final AtomicLong eventStateMismatches = new AtomicLong(0);
    // Hidden information held by this client while the game is on: the most library cards the tracker ever held
    // with a name, and the most it held at all (so a zero cannot come from an empty library)
    private final AtomicInteger maxNamedLibraryCards = new AtomicInteger(0);
    private final AtomicInteger maxLibraryCards = new AtomicInteger(0);
    // After a reconnect (a second full state while the game is on): the same two over the deltas applied after the
    // reconnect's full state, and the named count right after that full state arrived
    private final AtomicBoolean afterReconnect = new AtomicBoolean(false);
    // Set once the first post-reconnect delta carrying new objects has been applied: the host's re-send of the graph,
    // which is where the library cards the full state carried in full are replaced by placeholders
    private final AtomicBoolean afterResend = new AtomicBoolean(false);
    private final AtomicInteger maxNamedLibraryCardsAfterReconnect = new AtomicInteger(0);
    private final AtomicInteger maxLibraryCardsAfterReconnect = new AtomicInteger(0);
    private final AtomicInteger namedLibraryCardsAfterFullState = new AtomicInteger(-1);

    public HeadlessNetworkClient(String username, String hostname, int port) {
        this.username = username;
        this.hostname = hostname;
        this.port = port;
    }

    public boolean connect(long timeoutMs) {
        netLog.info("Connecting to {}:{} as '{}'", hostname, port, username);

        try {
            guiGame = new DeltaLoggingGuiGame(this);
            client = new FGameClient(username, guiGame, hostname, port);
            lobby = new ClientGameLobby();
            client.addLobbyListener(new ClientLobbyListener());
            client.connect();

            boolean success = connectedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (success) {
                connected.set(true);
                netLog.info("Connected successfully, assigned slot {}",
                        assignedSlot.get());
            } else {
                netLog.error("Connection timeout after {}ms", timeoutMs);
            }
            return success;

        } catch (Exception e) {
            netLog.error("Connection failed: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean waitForGameStart(long timeoutMs) {
        try {
            return gameStartedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean waitForGameFinish(long timeoutMs) {
        try {
            return gameFinishedLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isGameInProgress() {
        return gameInProgress.get();
    }

    public int getAssignedSlot() {
        return assignedSlot.get();
    }

    public long getDeltaPacketsReceived() {
        return deltaPacketsReceived.get();
    }

    public long getFullStateSyncsReceived() {
        return fullStateSyncsReceived.get();
    }

    public long getTotalDeltaBytes() {
        return totalDeltaBytes.get();
    }

    public long getEventStateMismatches() {
        return eventStateMismatches.get();
    }

    /** The most library cards this client's tracker held with a name at any point while the game was on. */
    public int getMaxNamedLibraryCards() {
        return maxNamedLibraryCards.get();
    }

    /** The most library cards this client's tracker held at any point while the game was on. */
    public int getMaxLibraryCards() {
        return maxLibraryCards.get();
    }

    public String getUsername() {
        return username;
    }

    /** True once the client has come back from a disconnect: a second full state while the game is on, and connected again. */
    public boolean hasReconnected() {
        return afterReconnect.get() && client != null
                && client.getReconnectState() == FGameClient.ReconnectState.CONNECTED;
    }

    public int getMaxNamedLibraryCardsAfterReconnect() {
        return maxNamedLibraryCardsAfterReconnect.get();
    }

    public int getMaxLibraryCardsAfterReconnect() {
        return maxLibraryCardsAfterReconnect.get();
    }

    /** Named library cards this client may not see, counted right after the reconnect's full state; -1 without one. */
    public int getNamedLibraryCardsAfterFullState() {
        return namedLibraryCardsAfterFullState.get();
    }

    /**
     * What a cheat would read: after each applied delta, while the game is on, count the library cards the client
     * holds and how many of them carry a name (a current state with a name, or an oracle name on the card) although
     * this client may not see them: not shown to any of its players by the client's own rule (CardView.canBeShownToAny,
     * so a card revealed to it through PlayerMayLook is not counted) and not in the game's reveal history.
     */
    private void censusHiddenInfo(forge.game.GameView gameView) {
        int[] counts = countHiddenInfo(gameView);
        if (counts == null) {
            return;
        }
        maxLibraryCards.accumulateAndGet(counts[0], Math::max);
        maxNamedLibraryCards.accumulateAndGet(counts[1], Math::max);
        if (afterResend.get()) {
            maxLibraryCardsAfterReconnect.accumulateAndGet(counts[0], Math::max);
            maxNamedLibraryCardsAfterReconnect.accumulateAndGet(counts[1], Math::max);
        }
    }

    /** The reconnect's full state has just been applied: what the client holds before any delta follows it. */
    private void censusAfterFullState(forge.game.GameView gameView) {
        int[] counts = countHiddenInfo(gameView);
        if (counts != null) {
            namedLibraryCardsAfterFullState.compareAndSet(-1, counts[1]);
        }
    }

    /** {library cards held, library cards named that this client may not see}, or null when there is no game to count. */
    private int[] countHiddenInfo(forge.game.GameView gameView) {
        if (gameView == null || gameView.isGameOver() || gameView.getPlayers() == null || guiGame == null) {
            return null;
        }
        java.util.Set<forge.game.player.PlayerView> mine = guiGame.getLocalPlayers();
        java.util.Set<Integer> revealed = new java.util.HashSet<>();
        if (gameView.getRevealedCollection() != null) {
            for (forge.game.card.CardView cv : gameView.getRevealedCollection()) {
                revealed.add(cv.getId());
            }
        }
        int cards = 0, named = 0;
        for (forge.game.player.PlayerView pv : gameView.getPlayers()) {
            forge.util.collect.FCollectionView<forge.game.card.CardView> library = pv.getCards(forge.game.zone.ZoneType.Library);
            if (library == null) {
                continue;
            }
            for (forge.game.card.CardView cv : library) {
                cards++;
                boolean stateNamed = cv.getCurrentState() != null && !cv.getCurrentState().getName().isEmpty();
                boolean oracleNamed = ((java.util.Map<?, ?>) cv.getProps()).containsKey(forge.trackable.TrackableProperty.OracleName);
                boolean maySee = cv.canBeShownToAny(mine) || revealed.contains(cv.getId());
                if ((stateNamed || oracleNamed) && !maySee) {
                    named++;
                }
            }
        }
        return new int[] { cards, named };
    }

    public forge.game.GameView getGameView() {
        return guiGame != null ? guiGame.getGameView() : null;
    }

    public boolean isOpenViewCalled() {
        return guiGame != null && guiGame.isOpenViewCalled();
    }

    public int getSetGameViewCount() {
        return guiGame != null ? guiGame.getSetGameViewCount() : 0;
    }

    public ClientGameLobby getLobby() {
        return lobby;
    }

    public FGameClient getClient() {
        return client;
    }

    public void setReady() {
        if (client != null && connected.get()) {
            netLog.info("Sending ready status");
            UpdateLobbyPlayerEvent event = UpdateLobbyPlayerEvent.isReadyUpdate(true);
            // Apply to local lobby AND send to server
            int slot = assignedSlot.get();
            if (slot >= 0 && lobby != null) {
                lobby.applyToSlot(slot, event);
                netLog.info("Applied ready status to local lobby slot {}", slot);
            }
            client.send(event);
        } else {
            netLog.error("Cannot set ready - not connected");
        }
    }

    @Override
    public void close() {
        netLog.info("Disconnecting");
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
        connected.set(false);
    }

    void onDeltaPacketReceived(DeltaPacket packet) {
        deltaPacketsReceived.incrementAndGet();
        totalDeltaBytes.addAndGet(packet.getApproximateSize());

        netLog.info("Delta packet #{}: deltas={}, new={}, estimatedBytes={}",
                packet.getSequenceNumber(),
                packet.getObjectDeltas() != null ? packet.getObjectDeltas().size() : 0,
                packet.getNewObjects() != null ? packet.getNewObjects().size() : 0,
                packet.getApproximateSize());
    }

    void onFullStateSyncReceived(long sequenceNumber) {
        // A second full state while the game is on is a resync, which is how a reconnect resumes the client
        if (fullStateSyncsReceived.incrementAndGet() >= 2) {
            afterReconnect.set(true);
            connected.set(true);
        }
        gameInProgress.set(true);
        gameStartedLatch.countDown();

        netLog.info("Full state sync: seq={}", sequenceNumber);
    }

    void onGameEnd() {
        gameInProgress.set(false);
        gameFinishedLatch.countDown();
        netLog.info("Game ended. Deltas={}, FullSyncs={}, TotalBytes={}",
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
            lobby.setData(state);

            // First LobbyUpdateEvent may have slot=-1 before LoginEvent is processed
            if (slot >= 0) {
                int previousSlot = assignedSlot.get();
                assignedSlot.set(slot);
                lobby.setLocalPlayer(slot);

                netLog.info("Lobby update: assigned to slot {} (previous={})",
                        slot, previousSlot);

                // Signal connected once we have a valid slot assignment
                if (previousSlot == -1 && slot >= 0) {
                    connectedLatch.countDown();
                }
            } else {
                netLog.info("Lobby update: slot not yet assigned (slot={})",
                        slot);
            }
        }

        @Override
        public void message(String source, String message, ChatMessage.MessageType type) {
            netLog.info("Chat: {}: {}", source, message);
        }

        @Override
        public void close() {
            netLog.info("Connection closed by server");
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
                    netLog.info("Cancelled pending auto-response: {}", reason);
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
                            netLog.info("Skipping auto-response (no controller): {}", description);
                        }
                    } catch (Exception e) {
                        netLog.error("Error in auto-response '{}': {}", description, e.getMessage());
                    }
                }, delayMs, TimeUnit.MILLISECONDS);

                netLog.info("Scheduled auto-response in {}ms: {}", delayMs, description);
            }
        }

        @Override
        public void applyDelta(DeltaPacket packet) {
            // First, process the delta packet (deserialize, update tracker, etc.)
            super.applyDelta(packet);

            // Then notify the client for logging/verification
            client.onDeltaPacketReceived(packet);
            if (client.afterReconnect.get() && packet.getNewObjects() != null && !packet.getNewObjects().isEmpty()) {
                client.afterResend.set(true);
            }
            client.censusHiddenInfo(getGameView());
        }

        @Override
        public void setGameView(forge.game.GameView gameView, long sequenceNumber) {
            super.setGameView(gameView, sequenceNumber);

            // Notify the client when this is a full state sync (sequenceNumber >= 0)
            if (sequenceNumber >= 0) {
                client.onFullStateSyncReceived(sequenceNumber);
                if (client.afterReconnect.get()) {
                    client.censusAfterFullState(getGameView());
                }
            }
        }

        @Override
        public void handleGameEvents(java.util.List<forge.game.event.GameEvent> events) {
            // Validate event-delta consistency AFTER delta is applied but BEFORE
            // super dispatches events to FControlGameEventHandler.
            // Only check the LAST tap event per card in the batch — earlier events
            // may reference intermediate states that the delta (final-state-only)
            // correctly doesn't include.
            java.util.Map<Integer, forge.game.event.GameEventCardTapped> lastTapPerCard = new java.util.LinkedHashMap<>();
            for (forge.game.event.GameEvent event : events) {
                if (event instanceof forge.game.event.GameEventCardTapped tapEvent) {
                    forge.game.card.CardView card = tapEvent.card();
                    if (card != null) {
                        lastTapPerCard.put(card.getId(), tapEvent);
                    }
                }
            }
            for (forge.game.event.GameEventCardTapped tapEvent : lastTapPerCard.values()) {
                forge.game.card.CardView card = tapEvent.card();
                if (card != null && card.getZone() == forge.game.zone.ZoneType.Battlefield && card.isTapped() != tapEvent.tapped()) {
                    client.eventStateMismatches.incrementAndGet();
                    netLog.warn("[EventDeltaCheck] MISMATCH: GameEventCardTapped says tapped={} but CardView.isTapped()={} for {}",
                            tapEvent.tapped(), card.isTapped(), card);
                }
            }
            super.handleGameEvents(events);
        }

        @Override
        public void setOriginalGameController(forge.game.player.PlayerView view, IGameController controller) {
            super.setOriginalGameController(view, controller);
            if (controller != null) {
                this.gameController = controller;
                netLog.info("Original game controller set for player: {}",
                        view != null ? view.getName() : "null");
            }
        }

        @Override
        public void setGameController(forge.game.player.PlayerView player, IGameController controller) {
            super.setGameController(player, controller);
            if (controller != null) {
                this.gameController = controller;
                netLog.info("Game controller set for player: {}",
                        player != null ? player.getName() : "null");
            }
        }

        @Override
        public void showPromptMessage(forge.game.player.PlayerView playerView, String message, forge.game.card.CardView cv) {
            netLog.info("Prompt: {}", message);

            // Detect player selection prompts (like "who goes first")
            // These contain "Click on the portrait" in the message
            if (message != null && message.contains("Click on the portrait") && gameController != null) {
                netLog.info("Detected player selection prompt, auto-selecting...");
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
                        netLog.info("Auto-selecting player: {}", toSelect.getName());
                        gameController.selectPlayer(toSelect, null);
                    } else {
                        netLog.error("Cannot auto-select player - no game view or players");
                    }
                }, 100, "player selection");
            }
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, boolean okEnabled, boolean cancelEnabled, boolean focusOk) {
            netLog.info("updateButtons(ok): okEnabled={}, cancelEnabled={}, controller={}",
                    okEnabled, cancelEnabled, gameController != null ? "set" : "null");
            // Auto-respond to button prompts (mulligan, priority, etc.)
            if (gameController != null && okEnabled) {
                netLog.info("Auto-clicking OK for player: {}",
                        owner != null ? owner.getName() : "unknown");
                scheduleAutoResponse(() -> gameController.selectButtonOk(), 50, "click OK button");
            }
        }

        @Override
        public void updateButtons(forge.game.player.PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
            netLog.info("updateButtons(labels): '{}'/{}, '{}'/{}, controller={}",
                    label1, enable1, label2, enable2, gameController != null ? "set" : "null");
            // Auto-respond to labeled button prompts - click first enabled button
            if (gameController != null && (enable1 || enable2)) {
                String clickTarget = enable1 ? label1 : label2;
                netLog.info("Auto-clicking '{}' for player: {}",
                        clickTarget, owner != null ? owner.getName() : "unknown");
                if (enable1) {
                    scheduleAutoResponse(() -> gameController.selectButtonOk(), 50, "click '" + label1 + "'");
                } else {
                    scheduleAutoResponse(() -> gameController.selectButtonCancel(), 50, "click '" + label2 + "'");
                }
            } else if (gameController != null && !enable1) {
                // OK is disabled but we may have more cards to select (multi-selection prompt)
                synchronized (pendingSelectables) {
                    if (selectableIndex < pendingSelectables.size()) {
                        netLog.info("OK disabled, selecting next card ({}/{} remaining)",
                                pendingSelectables.size() - selectableIndex, pendingSelectables.size());
                        selectNextCard();
                    }
                }
            }
        }

        @Override
        public void setSelectables(Iterable<forge.game.card.CardView> cards, int min, int max) {
            super.setSelectables(cards, min, max);
            synchronized (pendingSelectables) {
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
        }

        /**
         * Select the next card from the pending list.
         * Uses the serialized auto-response executor to prevent race conditions.
         */
        private void selectNextCard() {
            synchronized (pendingSelectables) {
                if (selectableIndex < pendingSelectables.size()) {
                    forge.game.card.CardView card = pendingSelectables.get(selectableIndex);
                    selectableIndex++;
                    netLog.info("Auto-selecting card {}/{}: {}",
                            selectableIndex, pendingSelectables.size(), card.getName());
                    scheduleAutoResponse(() -> gameController.selectCard(card, null, null),
                            100, "select card " + card.getName());
                }
            }
        }

        @Override
        public void afterGameEnd() {
            super.afterGameEnd();
            autoResponseExecutor.shutdownNow();
            client.onGameEnd();
        }
    }

    public String getMetricsSummary() {
        return String.format("HeadlessClient[%s]: deltas=%d, fullSyncs=%d, bytes=%d, eventMismatches=%d, connected=%s, gameInProgress=%s",
                username,
                deltaPacketsReceived.get(),
                fullStateSyncsReceived.get(),
                totalDeltaBytes.get(),
                eventStateMismatches.get(),
                connected.get(),
                gameInProgress.get());
    }
}
