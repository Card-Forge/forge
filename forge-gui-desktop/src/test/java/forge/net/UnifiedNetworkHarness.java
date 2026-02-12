package forge.net;

import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameOutcome;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.NetworkByteTracker;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.client.ClientGameLobby;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.GuiBase;
import forge.interfaces.ILobbyListener;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;

import forge.net.analysis.GameLogMetrics;
import forge.net.analysis.NetworkLogAnalyzer;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified network test harness supporting 2-4 players with configurable remote clients.
 *
 * Consolidates functionality from:
 * - AutomatedGameTestHarness (2p local AI)
 * - NetworkClientTestHarness (2p with 1 remote client)
 * - MultiplayerNetworkScenario (3-4p with remote clients)
 *
 * Configuration via builder pattern:
 * <pre>
 * // 2-player local AI (like NETWORK_LOCAL mode)
 * GameResult result = new UnifiedNetworkHarness()
 *     .playerCount(2)
 *     .remoteClients(0)
 *     .execute();
 *
 * // 2-player with remote client (like NETWORK_REMOTE mode)
 * GameResult result = new UnifiedNetworkHarness()
 *     .playerCount(2)
 *     .remoteClients(1)
 *     .execute();
 *
 * // 3-player multiplayer with remote clients
 * GameResult result = new UnifiedNetworkHarness()
 *     .playerCount(3)
 *     .remoteClients(2)
 *     .useAiForRemotePlayers(true)
 *     .execute();
 * </pre>
 */
public class UnifiedNetworkHarness {

    private static final String LOG_PREFIX = "[UnifiedNetworkHarness]";
    private static final String[] PLAYER_NAMES = {"Alice (Host AI)", "Bob (Remote)", "Charlie (Remote)", "Diana (Remote)"};

    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30000;
    private static final long DEFAULT_GAME_TIMEOUT_MS = 300000; // 5 minutes

    // Configuration
    private int playerCount = 2;
    private int remoteClientCount = 0;
    private long connectionTimeoutMs = DEFAULT_CONNECTION_TIMEOUT_MS;
    private long gameTimeoutMs = DEFAULT_GAME_TIMEOUT_MS;
    private int specifiedPort = -1; // -1 means auto-allocate
    private boolean useAiForRemotePlayers = true;
    private List<Deck> decks = null; // null means use random precons

    // Runtime state
    private FServerManager server;
    private ServerGameLobby lobby;
    private List<HeadlessNetworkClient> remoteClients = new ArrayList<>();
    private ExecutorService clientExecutor;
    private final AtomicBoolean serverRunning = new AtomicBoolean(false);

    // ==================== Builder Methods ====================

    /**
     * Set the number of players (2-4 supported).
     */
    public UnifiedNetworkHarness playerCount(int count) {
        if (count < 2 || count > 4) {
            throw new IllegalArgumentException("Player count must be 2-4, got: " + count);
        }
        this.playerCount = count;
        return this;
    }

    /**
     * Set the number of remote clients (0 to playerCount-1).
     * When 0, all players are local AI (no actual network traffic).
     * When > 0, remote HeadlessNetworkClients connect via TCP.
     */
    public UnifiedNetworkHarness remoteClients(int count) {
        this.remoteClientCount = count;
        return this;
    }

    /**
     * Set a specific port to use instead of auto-allocating.
     */
    public UnifiedNetworkHarness port(int port) {
        this.specifiedPort = port;
        return this;
    }

    /**
     * Set the game timeout in milliseconds.
     */
    public UnifiedNetworkHarness gameTimeout(long timeoutMs) {
        this.gameTimeoutMs = timeoutMs;
        return this;
    }

    /**
     * Set the connection timeout in milliseconds.
     */
    public UnifiedNetworkHarness connectionTimeout(long timeoutMs) {
        this.connectionTimeoutMs = timeoutMs;
        return this;
    }

    /**
     * Enable server-side AI for remote players.
     *
     * When enabled, remote player controllers are swapped to AI after the game starts.
     * This allows realistic gameplay with all players making strategic decisions.
     * Remote clients still receive delta updates for verification.
     */
    public UnifiedNetworkHarness useAiForRemotePlayers(boolean enable) {
        this.useAiForRemotePlayers = enable;
        return this;
    }

    /**
     * Set specific decks for players.
     * If not set, random precon decks are used.
     */
    public UnifiedNetworkHarness decks(List<Deck> decks) {
        this.decks = decks;
        return this;
    }

    /**
     * Set decks for a 2-player game.
     */
    public UnifiedNetworkHarness decks(Deck deck1, Deck deck2) {
        this.decks = new ArrayList<>();
        this.decks.add(deck1);
        this.decks.add(deck2);
        return this;
    }

    // ==================== Execution ====================

    /**
     * Execute the network test with configured settings.
     *
     * @return GameResult with all test metrics
     */
    public GameResult execute() {
        // Validate configuration
        if (remoteClientCount < 0 || remoteClientCount >= playerCount) {
            throw new IllegalArgumentException(
                    String.format("Remote client count must be 0 to %d, got: %d", playerCount - 1, remoteClientCount));
        }

        // Select execution path based on configuration
        if (remoteClientCount == 0) {
            return executeLocalGame();
        } else {
            return executeRemoteGame();
        }
    }

    // ==================== Local Game Execution (No Remote Clients) ====================

    /**
     * Execute a game with all local AI players (no actual network traffic).
     */
    private GameResult executeLocalGame() {
        GameResult result = new GameResult();
        result.playerCount = playerCount;
        result.remoteClientCount = 0;
        result.description = playerCount + "-player local AI";
        long startTime = System.currentTimeMillis();

        try {
            ensureFModelInitialized();

            int port = (specifiedPort > 0) ? specifiedPort : PortAllocator.allocatePort();
            result.port = port;

            // 1. Start server
            server = FServerManager.getInstance();
            server.startServer(port);
            serverRunning.set(true);
            logServerInstanceBanner("LocalAI", playerCount, port);

            // 2. Create lobby
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // Add slots for multiplayer
            for (int i = 2; i < playerCount; i++) {
                lobby.addSlot();
            }

            // 3. Configure all slots as AI with decks
            List<Deck> gameDecks = getDecks(playerCount);
            result.deckNames = new ArrayList<>();

            for (int i = 0; i < playerCount; i++) {
                Deck deck = gameDecks.get(i);
                result.deckNames.add(deck.getName());

                LobbySlot slot = lobby.getSlot(i);
                slot.setType(LobbySlotType.AI);
                slot.setName(PLAYER_NAMES[i]);
                slot.setDeck(deck);
                slot.setIsReady(true);

                NetworkDebugLogger.log("%s Configured slot %d: %s with deck: %s",
                        LOG_PREFIX, i, PLAYER_NAMES[i], deck.getName());
            }

            // 4. Start game
            NetworkDebugLogger.log("%s Starting local game...", LOG_PREFIX);
            result.gameStarted = true;

            Runnable startRunnable = lobby.startGame();
            if (startRunnable == null) {
                result.gameStarted = false;
                result.errorMessage = "startGame() returned null - deck validation likely failed";
                return result;
            }

            startRunnable.run();
            registerEventListener();

            // 5. Wait for completion
            waitForLocalGameCompletion(result);

            // 6. Collect metrics
            collectNetworkMetrics(result);

            result.success = result.gameCompleted && result.turnCount > 0;
            result.gameDurationMs = System.currentTimeMillis() - startTime;

        } catch (Exception e) {
            result.errorMessage = e.getMessage();
            result.exception = e;
            NetworkDebugLogger.error("%s Local game failed: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }

        NetworkDebugLogger.log("%s Local game result: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    // ==================== Remote Game Execution (With Remote Clients) ====================

    /**
     * Execute a game with remote clients connecting via TCP.
     */
    private GameResult executeRemoteGame() {
        GameResult result = new GameResult();
        result.playerCount = playerCount;
        result.remoteClientCount = remoteClientCount;
        result.description = playerCount + "-player network";
        long startTime = System.currentTimeMillis();

        AtomicInteger successfulConnections = new AtomicInteger(0);

        try {
            ensureFModelInitialized();

            int port = (specifiedPort > 0) ? specifiedPort : PortAllocator.allocatePort();
            result.port = port;

            NetworkDebugLogger.log("%s Starting %d-player game with %d remote clients on port %d",
                    LOG_PREFIX, playerCount, remoteClientCount, port);

            // 1. Start server
            server = FServerManager.getInstance();
            server.startServer(port);
            serverRunning.set(true);
            logServerInstanceBanner("RemoteNetwork", playerCount, port);

            // 2. Create lobby
            lobby = new ServerGameLobby();
            server.setLobby(lobby);
            setupLobbyListener();

            // Add slots for multiplayer
            for (int i = 2; i < playerCount; i++) {
                lobby.addSlot();
            }

            // 3. Configure player slots
            List<Deck> gameDecks = getDecks(playerCount);
            result.deckNames = new ArrayList<>();

            for (int i = 0; i < playerCount; i++) {
                Deck deck = gameDecks.get(i);
                result.deckNames.add(deck.getName());

                LobbySlot slot = lobby.getSlot(i);
                if (i == 0) {
                    // Host is always local AI
                    slot.setType(LobbySlotType.AI);
                    slot.setName(PLAYER_NAMES[0]);
                    slot.setDeck(deck);
                    slot.setIsReady(true);
                    NetworkDebugLogger.log("%s Slot 0: %s (AI host) with %s",
                            LOG_PREFIX, PLAYER_NAMES[0], deck.getName());
                } else if (i <= remoteClientCount) {
                    // Remote client slots - stay OPEN, deck pre-loaded
                    slot.setType(LobbySlotType.OPEN);
                    slot.setDeck(deck);
                    slot.setIsReady(false);
                    NetworkDebugLogger.log("%s Slot %d: OPEN for remote client (deck: %s)",
                            LOG_PREFIX, i, deck.getName());
                } else {
                    // Additional local AI slots
                    slot.setType(LobbySlotType.AI);
                    slot.setName(PLAYER_NAMES[i]);
                    slot.setDeck(deck);
                    slot.setIsReady(true);
                    NetworkDebugLogger.log("%s Slot %d: %s (AI) with %s",
                            LOG_PREFIX, i, PLAYER_NAMES[i], deck.getName());
                }
            }

            // Brief pause for server initialization
            Thread.sleep(1000);

            // 4. Start remote clients
            clientExecutor = Executors.newFixedThreadPool(remoteClientCount);
            CountDownLatch clientsAttemptedLatch = new CountDownLatch(remoteClientCount);
            CountDownLatch clientsFinishedLatch = new CountDownLatch(remoteClientCount);

            for (int i = 0; i < remoteClientCount; i++) {
                final int clientIndex = i;
                final String clientName = PLAYER_NAMES[i + 1];
                final int finalPort = port;

                clientExecutor.submit(() -> runRemoteClientThread(
                        clientIndex, clientName, finalPort,
                        successfulConnections, clientsAttemptedLatch, clientsFinishedLatch));

                Thread.sleep(100); // Small stagger between submissions
            }

            // 5. Wait for client connections
            boolean allClientsAttempted = clientsAttemptedLatch.await(connectionTimeoutMs, TimeUnit.MILLISECONDS);
            int connectedCount = successfulConnections.get();

            NetworkDebugLogger.log("%s Connection attempts: %d/%d connected successfully",
                    LOG_PREFIX, connectedCount, remoteClientCount);

            if (!allClientsAttempted) {
                result.errorMessage = "Client connection timeout - only " + connectedCount + " connected";
                return result;
            }

            if (connectedCount == 0) {
                result.errorMessage = "No clients connected successfully";
                return result;
            }

            // Brief pause for server to process ready states
            Thread.sleep(1000);

            // 6. Start game
            logLobbyState();

            Runnable startRunnable = lobby.startGame();
            if (startRunnable == null) {
                result.errorMessage = "startGame() returned null - deck validation likely failed";
                return result;
            }

            startRunnable.run();
            result.gameStarted = true;
            NetworkDebugLogger.log("%s Game started successfully!", LOG_PREFIX);

            // Swap remote players to AI if enabled
            HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
            if (useAiForRemotePlayers && hostedMatch != null && hostedMatch.getGame() != null) {
                swapRemotePlayersToAi(hostedMatch.getGame());
            }

            // 7. Wait for game completion
            waitForRemoteGameCompletion(result, hostedMatch);

            // 8. Wait for clients to finish
            clientsFinishedLatch.await(10, TimeUnit.SECONDS);

            // 9. Collect metrics from clients
            collectRemoteClientMetrics(result);

            // Validate result
            result.success = result.gameCompleted &&
                    result.deltaPacketsReceived > 0 &&
                    result.turnCount > 0;

            if (!result.success && result.errorMessage == null) {
                if (result.deltaPacketsReceived == 0) {
                    result.errorMessage = "No delta packets received - network path not exercised";
                } else if (!result.gameCompleted) {
                    result.errorMessage = "Game did not complete";
                }
            }

            result.gameDurationMs = System.currentTimeMillis() - startTime;

        } catch (Exception e) {
            result.errorMessage = e.getMessage();
            result.exception = e;
            NetworkDebugLogger.error("%s Remote game failed: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }

        NetworkDebugLogger.log("%s Remote game result: %s", LOG_PREFIX, result.toSummary());
        return result;
    }

    private void runRemoteClientThread(int clientIndex, String clientName, int port,
                                        AtomicInteger successfulConnections,
                                        CountDownLatch attemptedLatch,
                                        CountDownLatch finishedLatch) {
        HeadlessNetworkClient client = null;
        try {
            // Stagger connections — must be long enough that each client's LoginEvent
            // is fully processed (including updateLobbyState broadcast) before the next
            // client connects, to avoid a Netty event loop deadlock in RemoteClient.send()
            // when two broadcast() calls run concurrently on different event loop threads.
            long staggerDelay = 500L + (clientIndex * 3000L);
            Thread.sleep(staggerDelay);

            client = new HeadlessNetworkClient(clientName, "localhost", port);
            synchronized (remoteClients) {
                remoteClients.add(client);
            }

            if (client.connect(connectionTimeoutMs)) {
                int assignedSlot = client.getAssignedSlot();
                NetworkDebugLogger.log("%s Client %d (%s) connected to slot %d",
                        LOG_PREFIX, clientIndex, clientName, assignedSlot);

                Thread.sleep(500);
                client.setReady();
                NetworkDebugLogger.log("%s Client %d (%s) marked ready", LOG_PREFIX, clientIndex, clientName);
                Thread.sleep(200);

                int connectedCount = successfulConnections.incrementAndGet();
                NetworkDebugLogger.log("%s Client %d ready (%d/%d connected)",
                        LOG_PREFIX, clientIndex, connectedCount, remoteClientCount);

                attemptedLatch.countDown();

                // Wait for game
                client.waitForGameStart(gameTimeoutMs);
                client.waitForGameFinish(gameTimeoutMs);

                NetworkDebugLogger.log("%s Client %d finished: %s",
                        LOG_PREFIX, clientIndex, client.getMetricsSummary());
            } else {
                NetworkDebugLogger.error("%s Client %d (%s) FAILED to connect",
                        LOG_PREFIX, clientIndex, clientName);
                attemptedLatch.countDown();
            }

        } catch (Exception e) {
            NetworkDebugLogger.error("%s Client %d error: %s", LOG_PREFIX, clientIndex, e.getMessage());
            e.printStackTrace();
            attemptedLatch.countDown();
        } finally {
            if (client != null) {
                client.close();
            }
            finishedLatch.countDown();
        }
    }

    // ==================== Helper Methods ====================

    private void ensureFModelInitialized() {
        if (GuiBase.getInterface() == null) {
            NetworkDebugLogger.log("%s Initializing FModel with HeadlessGuiDesktop", LOG_PREFIX);
            GuiBase.setInterface(new HeadlessGuiDesktop());
            FModel.initialize(null, preferences -> {
                preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
                preferences.setPref(FPref.UI_LANGUAGE, "en-US");
                return null;
            });
        }
    }

    private List<Deck> getDecks(int count) {
        if (decks != null && decks.size() >= count) {
            return decks.subList(0, count);
        }
        List<Deck> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(TestDeckLoader.getRandomPrecon());
        }
        return result;
    }

    private void setupLobbyListener() {
        server.setLobbyListener(new ILobbyListener() {
            @Override
            public void update(GameLobbyData state, int slot) {
                NetworkDebugLogger.log("%s [LobbyListener] Update for slot %d", LOG_PREFIX, slot);
            }

            @Override
            public void message(String source, String message) {
                NetworkDebugLogger.log("%s [LobbyListener] Message from %s: %s", LOG_PREFIX, source, message);
            }

            @Override
            public void close() {
                NetworkDebugLogger.log("%s [LobbyListener] Lobby closed", LOG_PREFIX);
            }

            @Override
            public ClientGameLobby getLobby() {
                return null;
            }
        });
    }

    private void registerEventListener() {
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch != null && hostedMatch.getGame() != null) {
            GameEventListener listener = new GameEventListener().setVerboseLogging(false);
            hostedMatch.getGame().subscribeToEvents(listener);
        }
    }

    private void logLobbyState() {
        NetworkDebugLogger.log("%s Lobby state before game start:", LOG_PREFIX);
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            LobbySlot slot = lobby.getSlot(i);
            NetworkDebugLogger.log("%s   Slot %d: type=%s, name=%s, deck=%s, ready=%s",
                    LOG_PREFIX, i,
                    slot.getType(),
                    slot.getName(),
                    slot.getDeck() != null ? slot.getDeck().getName() : "null",
                    slot.isReady());
        }
    }

    private void waitForLocalGameCompletion(GameResult result) {
        HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
        if (hostedMatch == null) {
            result.errorMessage = "No hosted match found";
            return;
        }

        int maxWaitSeconds = (int) (gameTimeoutMs / 1000);
        int waitedMs = 0;
        int pollIntervalMs = 100;

        while (waitedMs < maxWaitSeconds * 1000) {
            Match match = hostedMatch.getMatch();
            if (match != null) {
                Collection<GameOutcome> outcomes = match.getOutcomes();
                if (outcomes != null && !outcomes.isEmpty()) {
                    NetworkDebugLogger.log("%s Game completed after %dms", LOG_PREFIX, waitedMs);
                    extractLocalGameResults(result, hostedMatch);
                    result.gameCompleted = true;
                    return;
                }
            }

            try {
                Thread.sleep(pollIntervalMs);
                waitedMs += pollIntervalMs;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (waitedMs % 5000 == 0) {
                NetworkDebugLogger.log("%s Still waiting... %ds", LOG_PREFIX, waitedMs / 1000);
            }
        }

        result.errorMessage = "Game did not complete within " + maxWaitSeconds + " seconds";
    }

    private void extractLocalGameResults(GameResult result, HostedMatch hostedMatch) {
        Match match = hostedMatch.getMatch();
        if (match == null) return;

        Collection<GameOutcome> outcomesCollection = match.getOutcomes();
        if (outcomesCollection == null || outcomesCollection.isEmpty()) return;

        List<GameOutcome> outcomes = new ArrayList<>(outcomesCollection);
        GameOutcome outcome = outcomes.get(outcomes.size() - 1);

        result.turnCount = outcome.getLastTurnNumber();
        RegisteredPlayer winner = outcome.getWinningPlayer();
        if (winner != null) {
            result.winner = winner.getPlayer().getName();
        }
    }

    private void waitForRemoteGameCompletion(GameResult result, HostedMatch hostedMatch) {
        if (hostedMatch == null) return;

        long endTime = System.currentTimeMillis() + gameTimeoutMs;
        while (System.currentTimeMillis() < endTime) {
            Game game = hostedMatch.getGame();
            if (game != null && game.isGameOver()) {
                result.gameCompleted = true;
                result.turnCount = game.getPhaseHandler().getTurn();
                if (game.getOutcome() != null && game.getOutcome().getWinningPlayer() != null) {
                    result.winner = game.getOutcome().getWinningPlayer().getPlayer().getName();
                }
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void collectNetworkMetrics(GameResult result) {
        if (server == null) return;

        NetworkByteTracker tracker = server.getNetworkByteTracker();
        if (tracker != null) {
            result.totalBytesSent = tracker.getTotalBytesSent();
            result.deltaBytesSent = tracker.getDeltaBytesSent();
            result.fullStateBytesSent = tracker.getFullStateBytesSent();
            result.deltaPacketCount = tracker.getDeltaPacketCount();
            result.fullStatePacketCount = tracker.getFullStatePacketCount();
            NetworkDebugLogger.log("%s Network stats: %s", LOG_PREFIX, tracker.getStatsSummary());
        }
    }

    private void collectRemoteClientMetrics(GameResult result) {
        synchronized (remoteClients) {
            for (HeadlessNetworkClient client : remoteClients) {
                result.deltaPacketsReceived += client.getDeltaPacketsReceived();
                result.fullStateSyncsReceived += client.getFullStateSyncsReceived();
                result.totalDeltaBytes += client.getTotalDeltaBytes();
            }
        }
        NetworkDebugLogger.log("%s Client metrics: deltas=%d, fullSyncs=%d, bytes=%d",
                LOG_PREFIX, result.deltaPacketsReceived, result.fullStateSyncsReceived, result.totalDeltaBytes);
    }

    private void swapRemotePlayersToAi(Game game) {
        NetworkDebugLogger.log("%s Swapping remote player controllers to AI...", LOG_PREFIX);

        for (Player player : game.getPlayers()) {
            // The instanceof check naturally skips the AI host (which has PlayerControllerAi).
            // Do NOT skip by player ID — HostedMatch sorts LobbyPlayerHuman before LobbyPlayerAi,
            // so the first remote player may have ID 0 instead of the host.
            if (player.getController() instanceof PlayerControllerHuman) {
                String originalControllerType = player.getController().getClass().getSimpleName();
                LobbyPlayerAi aiLobbyPlayer = new LobbyPlayerAi(player.getName(), null);
                PlayerControllerAi aiController = new PlayerControllerAi(game, player, aiLobbyPlayer);
                player.dangerouslySetController(aiController);

                NetworkDebugLogger.log("%s   Player %d (%s): swapped %s -> PlayerControllerAi",
                        LOG_PREFIX, player.getId(), player.getName(), originalControllerType);
            } else {
                NetworkDebugLogger.log("%s   Player %d (%s): kept %s (not PlayerControllerHuman)",
                        LOG_PREFIX, player.getId(), player.getName(), player.getController().getClass().getSimpleName());
            }
        }
    }

    private void logServerInstanceBanner(String scenarioName, int players, int serverPort) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String separator = "=".repeat(80);
        NetworkDebugLogger.log(separator);
        NetworkDebugLogger.log("SERVER INSTANCE STARTED");
        NetworkDebugLogger.log("  Scenario: %s", scenarioName);
        NetworkDebugLogger.log("  Players:  %d (remote clients: %d)", players, remoteClientCount);
        NetworkDebugLogger.log("  Port:     %d", serverPort);
        NetworkDebugLogger.log("  Time:     %s", timestamp);
        NetworkDebugLogger.log(separator);
    }

    private void cleanup() {
        NetworkDebugLogger.log("%s Cleaning up...", LOG_PREFIX);

        // Close clients
        synchronized (remoteClients) {
            for (HeadlessNetworkClient client : remoteClients) {
                try {
                    client.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            remoteClients.clear();
        }

        // Shutdown client executor
        if (clientExecutor != null) {
            clientExecutor.shutdownNow();
            try {
                clientExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            clientExecutor = null;
        }

        // Clear player GUIs between games
        if (server != null) {
            try {
                server.clearPlayerGuis();
            } catch (Exception e) {
                // Ignore
            }
        }

        // Stop server
        if (server != null && serverRunning.get()) {
            try {
                server.stopServer();
            } catch (Exception e) {
                // Ignore
            }
            serverRunning.set(false);
        }

        // Clear last match reference
        HeadlessGuiDesktop.clearLastMatch();
        lobby = null;

        NetworkDebugLogger.log("%s Cleanup complete", LOG_PREFIX);
    }

    // ==================== GameResult Inner Class ====================

    /**
     * Unified result class for all network test configurations.
     *
     * Unified result class replacing the separate result types from
     * the previously distinct harness classes.
     */
    public static class GameResult {
        // Configuration
        public int playerCount;
        public int remoteClientCount;
        public int port;
        public String description;

        // Game outcome
        public boolean success;
        public boolean gameStarted;
        public boolean gameCompleted;
        public int turnCount;
        public String winner;
        public long gameDurationMs;

        // Network metrics (from server tracker)
        public long totalBytesSent;
        public long deltaBytesSent;
        public long fullStateBytesSent;
        public long deltaPacketCount;
        public long fullStatePacketCount;

        // Network metrics (from remote clients)
        public long deltaPacketsReceived;
        public long fullStateSyncsReceived;
        public long totalDeltaBytes;

        // Deck information
        public List<String> deckNames = new ArrayList<>();

        // Error information
        public String errorMessage;
        public Exception exception;

        /**
         * Check if this test passed all criteria.
         * For remote client tests: game completed + delta packets received + turns > 0
         * For local tests: game completed + turns > 0
         */
        public boolean passed() {
            if (remoteClientCount > 0) {
                return gameStarted && gameCompleted && turnCount > 0 && deltaPacketsReceived > 0;
            } else {
                return gameStarted && gameCompleted && turnCount > 0;
            }
        }

        public String toSummary() {
            if (remoteClientCount > 0) {
                return String.format(
                        "GameResult[%s, success=%s, started=%s, completed=%s, players=%d, remoteClients=%d, " +
                                "turns=%d, winner=%s, deltas=%d, bytes=%d, error=%s]",
                        description, success, gameStarted, gameCompleted, playerCount, remoteClientCount,
                        turnCount, winner, deltaPacketsReceived, totalDeltaBytes, errorMessage);
            } else {
                return String.format(
                        "GameResult[%s, success=%s, completed=%s, turns=%d, winner=%s, duration=%.1fs, " +
                                "bytes=%d, error=%s]",
                        description, success, gameCompleted, turnCount, winner,
                        gameDurationMs / 1000.0, totalBytesSent, errorMessage);
            }
        }

        @Override
        public String toString() {
            return toSummary();
        }

        /**
         * Get a concise bandwidth summary from log analysis.
         * Shows Approximate/ActualNetwork/FullState comparison without full report.
         *
         * @return Bandwidth summary string, or null if log file not available
         */
        public String getBandwidthSummary() {
            File logFile = NetworkDebugLogger.getCurrentLogFile();
            if (logFile == null || !logFile.exists()) {
                return null;
            }

            try {
                NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
                GameLogMetrics metrics = analyzer.analyzeLogFile(logFile);

                long approx = metrics.getTotalApproximateBytes();
                long actual = metrics.getTotalDeltaBytes();
                long full = metrics.getTotalFullStateBytes();

                double approxSavings = full > 0 ? 100.0 * (1.0 - (double) approx / full) : 0;
                double actualSavings = full > 0 ? 100.0 * (1.0 - (double) actual / full) : 0;

                return String.format("Bandwidth: Approximate=%s (%.1f%% savings), ActualNetwork=%s (%.1f%% savings), FullState=%s",
                        formatBytes(approx), approxSavings,
                        formatBytes(actual), actualSavings,
                        formatBytes(full));
            } catch (Exception e) {
                return "Bandwidth analysis failed: " + e.getMessage();
            }
        }

        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }

        /**
         * Analyze the log file for this game and return a detailed analysis report.
         * This provides bandwidth savings metrics, checksum validation, and error context
         * that aren't available from the basic GameResult metrics.
         *
         * @return Analysis report string, or null if log file not available
         */
        public String analyzeLogFile() {
            File logFile = NetworkDebugLogger.getCurrentLogFile();
            if (logFile == null || !logFile.exists()) {
                return null;
            }

            try {
                NetworkLogAnalyzer analyzer = new NetworkLogAnalyzer();
                GameLogMetrics metrics = analyzer.analyzeLogFile(logFile);
                return metrics.toSummaryReport();
            } catch (Exception e) {
                return "Log analysis failed: " + e.getMessage();
            }
        }

        /**
         * Get the combined summary and analysis report.
         * Shows both the basic GameResult info and the detailed log analysis.
         * Also saves the report to the network logs directory.
         */
        public String toDetailedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append(toSummary());

            String analysis = analyzeLogFile();
            if (analysis != null) {
                sb.append("\n");
                sb.append(analysis);
            }

            // Save report alongside the log file
            saveAnalysisReport(sb.toString());

            return sb.toString();
        }

        /**
         * Save the analysis report to the network logs directory.
         * Report filename matches the log file with "-analysis.md" suffix.
         */
        private void saveAnalysisReport(String report) {
            File logFile = NetworkDebugLogger.getCurrentLogFile();
            if (logFile == null) {
                return;
            }

            // Create report filename based on log filename
            String logName = logFile.getName();
            String reportName = logName.replace(".log", "-analysis.md");
            File reportFile = new File(logFile.getParentFile(), reportName);

            try (java.io.FileWriter writer = new java.io.FileWriter(reportFile)) {
                writer.write("# Delta Sync Analysis Report\n\n");
                writer.write("**Source Log:** `" + logName + "`\n\n");
                writer.write("```\n");
                writer.write(report);
                writer.write("\n```\n");
                NetworkDebugLogger.log("[GameResult] Analysis report saved to: %s", NetworkDebugLogger.sanitizePath(reportFile.getAbsolutePath()));
            } catch (java.io.IOException e) {
                NetworkDebugLogger.warn("[GameResult] Failed to save analysis report: %s", e.getMessage());
            }
        }
    }
}
