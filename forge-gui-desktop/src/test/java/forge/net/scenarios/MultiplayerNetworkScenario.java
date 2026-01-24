package forge.net.scenarios;

import forge.deck.Deck;
import forge.game.Game;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.net.HeadlessGuiDesktop;
import forge.net.HeadlessNetworkClient;
import forge.net.TestDeckLoader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import forge.net.PortAllocator;

/**
 * Multiplayer network scenario with actual remote clients.
 *
 * Unlike MultiplayerScenario which uses all local AI players,
 * this scenario connects real HeadlessNetworkClient instances
 * to test delta sync in multiplayer games.
 *
 * Setup:
 * - 1 local AI host player (slot 0)
 * - 2-3 remote network clients (slots 1-3)
 *
 * This ensures delta sync is tested with multiple concurrent clients,
 * catching bugs related to concurrent sends, shared state, etc.
 */
public class MultiplayerNetworkScenario {

    private static final String LOG_PREFIX = "[MultiplayerNetworkScenario]";
    private static final String[] PLAYER_NAMES = {"Alice (Host AI)", "Bob (Remote)", "Charlie (Remote)", "Diana (Remote)"};

    private static final long CONNECTION_TIMEOUT_MS = 30000;
    private static final long GAME_TIMEOUT_MS = 300000; // 5 minutes

    /**
     * Result of the multiplayer network scenario.
     */
    public static class ScenarioResult {
        public final boolean gameStarted;
        public final boolean gameCompleted;
        public final int playerCount;
        public final int remoteClientCount;
        public final int turnCount;
        public final String winner;
        public final String description;
        public final long totalDeltaPackets;
        public final long totalDeltaBytes;
        public final String errorMessage;
        public final List<String> deckNames;

        public ScenarioResult(boolean gameStarted, boolean gameCompleted,
                              int playerCount, int remoteClientCount,
                              int turnCount, String winner,
                              String description,
                              long totalDeltaPackets, long totalDeltaBytes,
                              String errorMessage) {
            this(gameStarted, gameCompleted, playerCount, remoteClientCount,
                 turnCount, winner, description, totalDeltaPackets, totalDeltaBytes,
                 errorMessage, Collections.emptyList());
        }

        public ScenarioResult(boolean gameStarted, boolean gameCompleted,
                              int playerCount, int remoteClientCount,
                              int turnCount, String winner,
                              String description,
                              long totalDeltaPackets, long totalDeltaBytes,
                              String errorMessage, List<String> deckNames) {
            this.gameStarted = gameStarted;
            this.gameCompleted = gameCompleted;
            this.playerCount = playerCount;
            this.remoteClientCount = remoteClientCount;
            this.turnCount = turnCount;
            this.winner = winner;
            this.description = description;
            this.totalDeltaPackets = totalDeltaPackets;
            this.totalDeltaBytes = totalDeltaBytes;
            this.errorMessage = errorMessage;
            this.deckNames = deckNames != null ? new ArrayList<>(deckNames) : Collections.emptyList();
        }

        public boolean passed() {
            // Test passes if game completed with delta packets received
            return gameStarted && gameCompleted && turnCount > 0 && totalDeltaPackets > 0;
        }

        @Override
        public String toString() {
            return String.format(
                "ScenarioResult[%s, started=%b, completed=%b, players=%d, remoteClients=%d, " +
                "turns=%d, winner=%s, deltas=%d, bytes=%d, error=%s]",
                description, gameStarted, gameCompleted, playerCount, remoteClientCount,
                turnCount, winner, totalDeltaPackets, totalDeltaBytes, errorMessage);
        }
    }

    // Configuration
    private int playerCount = 3;
    private long gameTimeoutMs = GAME_TIMEOUT_MS;
    private int specifiedPort = -1; // -1 means auto-allocate

    /**
     * Set the number of players (3-4 supported).
     */
    public MultiplayerNetworkScenario playerCount(int count) {
        if (count < 3 || count > 4) {
            throw new IllegalArgumentException("Player count must be 3 or 4, got: " + count);
        }
        this.playerCount = count;
        return this;
    }

    /**
     * Set a specific port to use instead of auto-allocating.
     */
    public MultiplayerNetworkScenario port(int port) {
        this.specifiedPort = port;
        return this;
    }

    /**
     * Set the game timeout in milliseconds.
     */
    public MultiplayerNetworkScenario gameTimeout(long timeoutMs) {
        this.gameTimeoutMs = timeoutMs;
        return this;
    }

    /**
     * Execute the multiplayer network scenario.
     */
    public ScenarioResult execute() {
        int port = (specifiedPort > 0) ? specifiedPort : PortAllocator.allocatePort();
        int remoteClientCount = playerCount - 1; // All except host are remote

        NetworkDebugLogger.log("%s Starting %d-player network game with %d remote clients on port %d",
                LOG_PREFIX, playerCount, remoteClientCount, port);

        FServerManager server = null;
        ServerGameLobby lobby = null;
        ExecutorService clientExecutor = null;
        List<HeadlessNetworkClient> remoteClients = new ArrayList<>();
        List<String> deckNames = new ArrayList<>();

        boolean gameStarted = false;
        boolean gameCompleted = false;
        int turnCount = 0;
        String winner = null;
        long totalDeltaPackets = 0;
        long totalDeltaBytes = 0;

        // Track successful connections
        AtomicInteger successfulConnections = new AtomicInteger(0);

        try {
            // 1. Start server
            NetworkDebugLogger.log("%s Step 1: Starting server on port %d...", LOG_PREFIX, port);
            server = FServerManager.getInstance();
            server.startServer(port);
            NetworkDebugLogger.log("%s Server started successfully", LOG_PREFIX);
            logServerInstanceBanner(playerCount, port);

            // 2. Create lobby with correct number of slots
            lobby = new ServerGameLobby();
            server.setLobby(lobby);

            // Set up lobby listener to log updates
            server.setLobbyListener(new forge.interfaces.ILobbyListener() {
                @Override
                public void update(forge.gamemodes.match.GameLobby.GameLobbyData state, int slot) {
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
                public forge.gamemodes.net.client.ClientGameLobby getLobby() {
                    return null; // Server-side, not client
                }
            });

            // Add extra slots for multiplayer
            for (int i = 2; i < playerCount; i++) {
                lobby.addSlot();
            }
            NetworkDebugLogger.log("%s Lobby has %d slots", LOG_PREFIX, lobby.getNumberOfSlots());

            // 3. Configure player slots on the server
            // Host slot (0) is AI, remote slots remain OPEN so connectPlayer() can assign clients
            // Decks are pre-loaded so clients don't need to send them
            NetworkDebugLogger.log("%s Configuring %d player slots on server...", LOG_PREFIX, playerCount);

            for (int i = 0; i < playerCount; i++) {
                Deck deck = TestDeckLoader.getRandomPrecon();
                deckNames.add(deck.getName());

                LobbySlot slot = lobby.getSlot(i);
                if (i == 0) {
                    // Host is local AI - configure fully
                    slot.setType(LobbySlotType.AI);
                    slot.setName(PLAYER_NAMES[0]);
                    slot.setDeck(deck);
                    slot.setIsReady(true);
                    NetworkDebugLogger.log("%s   Slot 0: %s (AI host) with %s",
                            LOG_PREFIX, PLAYER_NAMES[0], deck.getName());
                } else {
                    // Remote slots stay OPEN so clients can connect
                    // Pre-load deck but leave type as OPEN, name empty
                    slot.setType(LobbySlotType.OPEN);
                    slot.setDeck(deck);
                    slot.setIsReady(false);
                    NetworkDebugLogger.log("%s   Slot %d: OPEN (deck pre-loaded: %s)",
                            LOG_PREFIX, i, deck.getName());
                }
            }

            // Give server time to fully initialize before clients connect
            Thread.sleep(1000);

            // 5. Start remote clients in parallel threads
            // Clients connect and mark ready - decks are already on the server
            NetworkDebugLogger.log("%s Step 5: Starting %d remote client threads...", LOG_PREFIX, remoteClientCount);
            clientExecutor = Executors.newFixedThreadPool(remoteClientCount);
            CountDownLatch clientsAttemptedLatch = new CountDownLatch(remoteClientCount);
            CountDownLatch clientsFinishedLatch = new CountDownLatch(remoteClientCount);

            // Start client threads sequentially with stagger
            for (int i = 0; i < remoteClientCount; i++) {
                final int clientIndex = i;
                final String clientName = PLAYER_NAMES[i + 1];
                final int finalPort = port;
                final AtomicInteger successfulConnectionsRef = successfulConnections;

                NetworkDebugLogger.log("%s Submitting client thread %d (%s)", LOG_PREFIX, clientIndex, clientName);

                clientExecutor.submit(() -> {
                    HeadlessNetworkClient client = null;
                    try {
                        // Stagger based on client index to avoid race conditions
                        long staggerDelay = 500L + (clientIndex * 300L);
                        Thread.sleep(staggerDelay);

                        client = new HeadlessNetworkClient(clientName, "localhost", finalPort);
                        synchronized (remoteClients) {
                            remoteClients.add(client);
                        }

                        if (client.connect(CONNECTION_TIMEOUT_MS)) {
                            int assignedSlot = client.getAssignedSlot();
                            NetworkDebugLogger.log("%s Client %d (%s) connected to slot %d",
                                    LOG_PREFIX, clientIndex, clientName, assignedSlot);

                            // Pause for server to process connection
                            Thread.sleep(500);

                            // Mark ready (deck is already configured on server)
                            client.setReady();
                            NetworkDebugLogger.log("%s Client %d (%s) marked ready",
                                    LOG_PREFIX, clientIndex, clientName);
                            Thread.sleep(200);

                            // Track successful connection
                            int connectedCount = successfulConnectionsRef.incrementAndGet();
                            NetworkDebugLogger.log("%s Client %d ready (%d/%d connected)",
                                    LOG_PREFIX, clientIndex, connectedCount, remoteClientCount);

                            // Signal this client has finished setup
                            clientsAttemptedLatch.countDown();

                            // Wait for game to start and finish
                            boolean gameStartedForClient = client.waitForGameStart(gameTimeoutMs);
                            NetworkDebugLogger.log("%s Client %d game started: %s",
                                    LOG_PREFIX, clientIndex, gameStartedForClient);
                            client.waitForGameFinish(gameTimeoutMs);

                            NetworkDebugLogger.log("%s Client %d finished: %s",
                                    LOG_PREFIX, clientIndex, client.getMetricsSummary());
                        } else {
                            NetworkDebugLogger.error("%s Client %d (%s) FAILED to connect",
                                    LOG_PREFIX, clientIndex, clientName);
                            clientsAttemptedLatch.countDown();
                        }
                    } catch (Exception e) {
                        NetworkDebugLogger.error("%s Client %d error: %s", LOG_PREFIX, clientIndex, e.getMessage());
                        e.printStackTrace();
                        clientsAttemptedLatch.countDown();
                    } finally {
                        if (client != null) {
                            client.close();
                        }
                        clientsFinishedLatch.countDown();
                    }
                });

                // Small delay between submitting threads
                Thread.sleep(100);
            }

            // 6. Wait for all clients to attempt connection
            NetworkDebugLogger.log("%s Step 6: Waiting for %d clients to attempt connection (timeout: %dms)...",
                    LOG_PREFIX, remoteClientCount, CONNECTION_TIMEOUT_MS);
            boolean allClientsAttempted = clientsAttemptedLatch.await(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            int connectedCount = successfulConnections.get();
            NetworkDebugLogger.log("%s Connection attempts complete: %d/%d connected successfully",
                    LOG_PREFIX, connectedCount, remoteClientCount);

            if (!allClientsAttempted) {
                return new ScenarioResult(false, false, playerCount, remoteClientCount,
                        0, null, playerCount + "-player network",
                        0, 0, "Client connection timeout - only " + connectedCount + " connected");
            }

            if (connectedCount == 0) {
                return new ScenarioResult(false, false, playerCount, remoteClientCount,
                        0, null, playerCount + "-player network",
                        0, 0, "No clients connected successfully");
            }

            // Brief pause to ensure server has processed all ready states
            Thread.sleep(1000);

            // 7. Start the game
            NetworkDebugLogger.log("%s Step 7: %d clients connected, checking lobby state before starting game...",
                    LOG_PREFIX, connectedCount);

            // Log lobby slot states
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                LobbySlot slot = lobby.getSlot(i);
                NetworkDebugLogger.log("%s   Slot %d: type=%s, name=%s, deck=%s, ready=%s",
                        LOG_PREFIX, i,
                        slot.getType(),
                        slot.getName(),
                        slot.getDeck() != null ? slot.getDeck().getName() : "null",
                        slot.isReady());
            }

            NetworkDebugLogger.log("%s Calling lobby.startGame()...", LOG_PREFIX);
            Runnable startRunnable = lobby.startGame();
            if (startRunnable == null) {
                NetworkDebugLogger.error("%s startGame() returned null!", LOG_PREFIX);
                return new ScenarioResult(false, false, playerCount, remoteClientCount,
                        0, null, playerCount + "-player network",
                        0, 0, "startGame() returned null - deck validation likely failed");
            }

            // Create game session for network play
            NetworkDebugLogger.log("%s Creating game session...", LOG_PREFIX);
            server.createGameSession();
            NetworkDebugLogger.log("%s Running start runnable...", LOG_PREFIX);
            startRunnable.run();
            gameStarted = true;

            NetworkDebugLogger.log("%s Game started successfully! Waiting for completion...", LOG_PREFIX);

            // 8. Wait for game completion
            HostedMatch hostedMatch = HeadlessGuiDesktop.getLastMatch();
            if (hostedMatch != null) {
                long endTime = System.currentTimeMillis() + gameTimeoutMs;
                while (System.currentTimeMillis() < endTime) {
                    Game game = hostedMatch.getGame();
                    if (game != null && game.isGameOver()) {
                        gameCompleted = true;
                        turnCount = game.getPhaseHandler().getTurn();
                        if (game.getOutcome() != null && game.getOutcome().getWinningPlayer() != null) {
                            winner = game.getOutcome().getWinningPlayer().getPlayer().getName();
                        }
                        break;
                    }
                    Thread.sleep(500);
                }
            }

            // 9. Wait for clients to finish processing
            clientsFinishedLatch.await(10, TimeUnit.SECONDS);

            // 10. Collect metrics from all clients
            synchronized (remoteClients) {
                for (HeadlessNetworkClient client : remoteClients) {
                    totalDeltaPackets += client.getDeltaPacketsReceived();
                    totalDeltaBytes += client.getTotalDeltaBytes();
                }
            }

            NetworkDebugLogger.log("%s Game completed: %b, turns: %d, winner: %s, totalDeltas: %d, totalBytes: %d",
                    LOG_PREFIX, gameCompleted, turnCount, winner, totalDeltaPackets, totalDeltaBytes);

            return new ScenarioResult(
                    gameStarted, gameCompleted, playerCount, remoteClientCount,
                    turnCount, winner, playerCount + "-player network",
                    totalDeltaPackets, totalDeltaBytes, null, deckNames);

        } catch (Exception e) {
            NetworkDebugLogger.error("%s Error: %s", LOG_PREFIX, e.getMessage());
            e.printStackTrace();
            return new ScenarioResult(
                    gameStarted, gameCompleted, playerCount, remoteClientCount,
                    turnCount, winner, playerCount + "-player network",
                    totalDeltaPackets, totalDeltaBytes, e.getMessage(), deckNames);

        } finally {
            // Cleanup
            if (clientExecutor != null) {
                clientExecutor.shutdownNow();
                try {
                    clientExecutor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            synchronized (remoteClients) {
                for (HeadlessNetworkClient client : remoteClients) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }

            if (server != null) {
                try {
                    server.endGameSession();
                } catch (Exception e) {
                    // Ignore
                }
                if (server.isHosting()) {
                    server.stopServer();
                }
            }

            HeadlessGuiDesktop.clearLastMatch();
            NetworkDebugLogger.log("%s Cleanup complete", LOG_PREFIX);
        }
    }

    private void logServerInstanceBanner(int players, int serverPort) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String separator = "=".repeat(80);
        NetworkDebugLogger.log(separator);
        NetworkDebugLogger.log("MULTIPLAYER NETWORK SERVER STARTED");
        NetworkDebugLogger.log("  Players:       %d (1 host + %d remote clients)", players, players - 1);
        NetworkDebugLogger.log("  Port:          %d", serverPort);
        NetworkDebugLogger.log("  Time:          %s", timestamp);
        NetworkDebugLogger.log(separator);
    }

    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        System.out.println("Multiplayer Network Scenario Test");
        System.out.println("=".repeat(60));

        int playerCount = 3;
        if (args.length > 0) {
            try {
                playerCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid player count: " + args[0] + ", using default 3");
            }
        }

        MultiplayerNetworkScenario scenario = new MultiplayerNetworkScenario()
                .playerCount(playerCount)
                .gameTimeout(300000);

        ScenarioResult result = scenario.execute();

        System.out.println();
        System.out.println("Result: " + result);
        System.out.println("Test " + (result.passed() ? "PASSED" : "FAILED"));

        System.exit(result.passed() ? 0 : 1);
    }
}
