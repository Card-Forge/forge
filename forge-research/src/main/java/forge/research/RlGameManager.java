package forge.research;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.onnxruntime.OrtException;

import java.util.Random;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameOutcome;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.research.observation.ObservationBuilder;
import forge.research.onnx.OnnxInferenceEngine;
import forge.research.proto.GameResult;
import forge.research.proto.Observation;
import forge.util.MyRandom;

/**
 * Manages game lifecycle for RL training.
 * Creates matches, runs games on daemon threads, and coordinates
 * decision exchange between the game thread and the gRPC service.
 */
public class RlGameManager {

    private static final long DECISION_TIMEOUT_SECONDS = 30;

    private final SynchronousQueue<DecisionContext> decisionQueue = new SynchronousQueue<>();
    private final SynchronousQueue<ActionResponse> responseQueue = new SynchronousQueue<>();
    private final ObservationBuilder observationBuilder = new ObservationBuilder();

    private volatile Thread gameThread;
    private volatile Game currentGame;
    private volatile Match currentMatch;
    private volatile Player agentPlayer;
    private volatile Player opponentPlayer;
    private volatile boolean gameRunning;
    private final AtomicBoolean gameEnded = new AtomicBoolean(false);
    private volatile GameOutcome lastOutcome;
    private OnnxInferenceEngine onnxEngine;
    private String currentModelPath;

    public SynchronousQueue<DecisionContext> getDecisionQueue() {
        return decisionQueue;
    }

    public SynchronousQueue<ActionResponse> getResponseQueue() {
        return responseQueue;
    }

    /**
     * Reset: start a new game and block until the first decision point.
     */
    public DecisionContext resetGame(String deckPathA, String deckPathB, int agentIndex) {
        return resetGame(deckPathA, deckPathB, agentIndex, false, "", 0);
    }

    /**
     * Reset: start a new game and block until the first decision point.
     * When dualRl is true, both players are RL-controlled via shared queues.
     */
    public DecisionContext resetGame(String deckPathA, String deckPathB, int agentIndex, boolean dualRl) {
        return resetGame(deckPathA, deckPathB, agentIndex, dualRl, "", 0);
    }

    /**
     * Reset: start a new game and block until the first decision point.
     * When dualRl is true and opponentModelPath is non-empty, the opponent uses ONNX inference
     * on the game thread instead of gRPC queues.
     */
    public DecisionContext resetGame(String deckPathA, String deckPathB, int agentIndex,
            boolean dualRl, String opponentModelPath) {
        return resetGame(deckPathA, deckPathB, agentIndex, dualRl, opponentModelPath, 0);
    }

    /**
     * Reset: start a new game with optional seed for reproducibility.
     * seed=0 means no seeding (uses default SecureRandom).
     */
    public DecisionContext resetGame(String deckPathA, String deckPathB, int agentIndex,
            boolean dualRl, String opponentModelPath, long seed) {
        // Clean up previous game if running
        stopCurrentGame();

        // Seed the RNG for reproducibility (0 = no seeding)
        if (seed != 0) {
            MyRandom.setRandom(new Random(seed));
        }

        Deck deckA = loadDeck(deckPathA);
        Deck deckB = loadDeck(deckPathB);

        GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(EnumSet.of(GameType.Constructed));
        rules.setGamesPerMatch(1);

        List<RegisteredPlayer> players = new ArrayList<>();

        if (dualRl && opponentModelPath != null && !opponentModelPath.isEmpty()) {
            // ONNX opponent: training agent uses gRPC queues, opponent runs inference locally
            loadOnnxEngine(opponentModelPath);

            int opponentIndex = 1 - agentIndex;
            RlLobbyPlayer rlLobby = new RlLobbyPlayer("RL_Agent", decisionQueue, responseQueue, agentIndex);
            OnnxLobbyPlayer onnxLobby = new OnnxLobbyPlayer("ONNX_Opponent", onnxEngine, opponentIndex);

            if (agentIndex == 0) {
                players.add(new RegisteredPlayer(deckA).setPlayer(rlLobby));
                players.add(new RegisteredPlayer(deckB).setPlayer(onnxLobby));
            } else {
                players.add(new RegisteredPlayer(deckA).setPlayer(onnxLobby));
                players.add(new RegisteredPlayer(deckB).setPlayer(rlLobby));
            }
        } else if (dualRl) {
            // Both players are RL-controlled, sharing the same queues (fallback)
            RlLobbyPlayer rlLobby0 = new RlLobbyPlayer("RL_Player_0", decisionQueue, responseQueue, 0);
            RlLobbyPlayer rlLobby1 = new RlLobbyPlayer("RL_Player_1", decisionQueue, responseQueue, 1);
            players.add(new RegisteredPlayer(deckA).setPlayer(rlLobby0));
            players.add(new RegisteredPlayer(deckB).setPlayer(rlLobby1));
        } else {
            // One RL player + one AI opponent
            RlLobbyPlayer rlLobby = new RlLobbyPlayer("RL_Agent", decisionQueue, responseQueue, agentIndex);
            forge.ai.LobbyPlayerAi aiLobby = new forge.ai.LobbyPlayerAi("AI_Opponent", null);

            RegisteredPlayer rpAgent;
            RegisteredPlayer rpOpponent;

            if (agentIndex == 0) {
                rpAgent = new RegisteredPlayer(deckA).setPlayer(rlLobby);
                rpOpponent = new RegisteredPlayer(deckB).setPlayer(aiLobby);
                players.add(rpAgent);
                players.add(rpOpponent);
            } else {
                rpOpponent = new RegisteredPlayer(deckA).setPlayer(aiLobby);
                rpAgent = new RegisteredPlayer(deckB).setPlayer(rlLobby);
                players.add(rpOpponent);
                players.add(rpAgent);
            }
        }

        currentMatch = new Match(rules, players, "RL_Match");
        currentGame = currentMatch.createGame();
        gameEnded.set(false);
        gameRunning = true;

        // Identify agent and opponent Player objects
        if (dualRl) {
            // In dual RL mode, use registration order (player 0 = index 0, player 1 = index 1)
            List<Player> gamePlayers = currentGame.getPlayers();
            agentPlayer = gamePlayers.get(agentIndex);
            opponentPlayer = gamePlayers.get(1 - agentIndex);
        } else {
            for (Player p : currentGame.getPlayers()) {
                if (p.getLobbyPlayer() instanceof RlLobbyPlayer) {
                    agentPlayer = p;
                } else {
                    opponentPlayer = p;
                }
            }
        }

        // Start game on daemon thread
        gameThread = new Thread(() -> {
            try {
                currentMatch.startGame(currentGame);
            } catch (Exception e) {
                System.err.println("Game thread exception: " + e.getMessage());
                e.printStackTrace();
            } finally {
                gameRunning = false;
                gameEnded.set(true);
                lastOutcome = currentGame.getOutcome();

                // Signal game over to anyone waiting
                try {
                    float reward = computeReward();
                    Observation obs = observationBuilder.buildObservation(currentGame, agentPlayer, opponentPlayer);
                    decisionQueue.offer(DecisionContext.gameOver(obs, reward), 5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Ignore if nobody is listening
                }
            }
        }, "ForgeGameThread");
        gameThread.setDaemon(true);
        gameThread.start();

        // Wait for first decision point (or game over)
        try {
            DecisionContext ctx = decisionQueue.poll(DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (ctx == null) {
                // Timeout — force game over
                forceGameOver();
                Observation obs = observationBuilder.buildObservation(currentGame, agentPlayer, opponentPlayer);
                return DecisionContext.gameOver(obs, 0f);
            }
            return ctx;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Step: send the action to the game thread and wait for the next decision point.
     */
    public DecisionContext step(int actionIndex) {
        if (gameEnded.get()) {
            // Game already over, return terminal state
            Observation obs = observationBuilder.buildObservation(currentGame, agentPlayer, opponentPlayer);
            return DecisionContext.gameOver(obs, computeReward());
        }

        try {
            // Send action to game thread
            responseQueue.put(new ActionResponse(actionIndex));

            // Wait for next decision or game over
            DecisionContext ctx = decisionQueue.poll(DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (ctx == null) {
                forceGameOver();
                Observation obs = observationBuilder.buildObservation(currentGame, agentPlayer, opponentPlayer);
                return DecisionContext.gameOver(obs, 0f);
            }
            return ctx;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public GameResult buildGameResult() {
        GameResult.Builder result = GameResult.newBuilder();
        if (lastOutcome == null) {
            result.setIsDraw(true);
            return result.build();
        }

        result.setTurnsPlayed(lastOutcome.getLastTurnNumber());
        if (lastOutcome.isDraw()) {
            result.setIsDraw(true);
            result.setWinnerIndex(-1);
        } else {
            result.setIsDraw(false);
            result.setWinnerIndex(
                    lastOutcome.isWinner(agentPlayer.getLobbyPlayer()) ? agentPlayer.getId() : opponentPlayer.getId());
        }
        result.setWinCondition(lastOutcome.getWinCondition() != null
                ? lastOutcome.getWinCondition().name() : "Unknown");
        return result.build();
    }

    private float computeReward() {
        if (lastOutcome == null || currentGame == null) {
            return 0f;
        }
        if (!currentGame.isGameOver()) {
            return 0f;
        }
        if (lastOutcome.isDraw()) {
            return 0f;
        }
        return lastOutcome.isWinner(agentPlayer.getLobbyPlayer()) ? 1f : -1f;
    }

    private void forceGameOver() {
        if (currentGame != null && !currentGame.isGameOver()) {
            currentGame.setGameOver(GameEndReason.Draw);
        }
    }

    private void stopCurrentGame() {
        if (gameThread != null && gameThread.isAlive()) {
            forceGameOver();
            // Unblock game thread if it's waiting on responseQueue
            responseQueue.offer(new ActionResponse(0));
            try {
                gameThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Drain any leftover items
            decisionQueue.poll();
        }
        gameThread = null;
        currentGame = null;
        currentMatch = null;
        agentPlayer = null;
        opponentPlayer = null;
        lastOutcome = null;
    }

    private void loadOnnxEngine(String modelPath) {
        if (onnxEngine != null && modelPath.equals(currentModelPath)) {
            return; // reuse cached engine
        }
        if (onnxEngine != null) {
            onnxEngine.close();
        }
        try {
            onnxEngine = new OnnxInferenceEngine(modelPath);
            currentModelPath = modelPath;
        } catch (OrtException e) {
            throw new RuntimeException("Failed to load ONNX model: " + modelPath + " - " + e.getMessage(), e);
        }
    }

    private static Deck loadDeck(String deckPath) {
        File f = new File(deckPath);
        if (!f.exists()) {
            throw new IllegalArgumentException("Deck file not found: " + deckPath);
        }
        Deck d = DeckSerializer.fromFile(f);
        if (d == null) {
            throw new IllegalArgumentException("Failed to parse deck: " + deckPath);
        }
        return d;
    }
}
