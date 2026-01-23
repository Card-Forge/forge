package forge.net;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventGameFinished;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnBegan;
import forge.gamemodes.net.NetworkDebugLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Helper class for listening to game events in automated tests.
 * Uses Guava EventBus subscription pattern.
 *
 * Logs all game actions to NetworkDebugLogger for test interpretability.
 *
 * Usage:
 * <pre>
 * GameEventListener listener = new GameEventListener();
 * listener.onTurnBegan(event -> {
 *     if (event.turnNumber() == 3) {
 *         // Do something on turn 3
 *     }
 * });
 * game.subscribeToEvents(listener);
 * </pre>
 */
public class GameEventListener {

    private static final String LOG_PREFIX = "[GameEvent]";

    private Consumer<GameEventTurnBegan> turnBeganHandler;
    private Consumer<GameEventGameFinished> gameFinishedHandler;
    private Consumer<GameEventGameOutcome> gameOutcomeHandler;

    private final CountDownLatch gameFinishedLatch = new CountDownLatch(1);
    private volatile int currentTurn = 0;
    private volatile boolean gameFinished = false;
    private volatile String winner = null;

    // Verbose logging flag - when true, logs all game actions
    private boolean verboseLogging = true;

    /**
     * Enable or disable verbose action logging.
     *
     * @param verbose true to log all game actions, false for minimal logging
     * @return this for method chaining
     */
    public GameEventListener setVerboseLogging(boolean verbose) {
        this.verboseLogging = verbose;
        return this;
    }

    /**
     * Set handler for turn began events.
     *
     * @param handler Consumer that receives GameEventTurnBegan
     * @return this for method chaining
     */
    public GameEventListener onTurnBegan(Consumer<GameEventTurnBegan> handler) {
        this.turnBeganHandler = handler;
        return this;
    }

    /**
     * Set handler for game finished events.
     *
     * @param handler Consumer that receives GameEventGameFinished
     * @return this for method chaining
     */
    public GameEventListener onGameFinished(Consumer<GameEventGameFinished> handler) {
        this.gameFinishedHandler = handler;
        return this;
    }

    /**
     * Set handler for game outcome events.
     *
     * @param handler Consumer that receives GameEventGameOutcome
     * @return this for method chaining
     */
    public GameEventListener onGameOutcome(Consumer<GameEventGameOutcome> handler) {
        this.gameOutcomeHandler = handler;
        return this;
    }

    // ====== Turn Events ======

    /**
     * EventBus subscriber method for turn began events.
     */
    @Subscribe
    public void handleTurnBegan(GameEventTurnBegan event) {
        currentTurn = event.turnNumber();
        NetworkDebugLogger.log("%s Turn %d began - %s's turn", LOG_PREFIX, currentTurn, event.turnOwner().getName());

        if (turnBeganHandler != null) {
            try {
                turnBeganHandler.accept(event);
            } catch (Exception e) {
                NetworkDebugLogger.error(LOG_PREFIX + " Error in turn handler: " + e.getMessage(), e);
            }
        }
    }

    // ====== Game Action Events ======

    /**
     * EventBus subscriber for spell/ability cast events.
     */
    @Subscribe
    public void handleSpellCast(GameEventSpellAbilityCast event) {
        if (verboseLogging) {
            NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
        }
    }

    /**
     * EventBus subscriber for spell resolved events.
     */
    @Subscribe
    public void handleSpellResolved(GameEventSpellResolved event) {
        if (verboseLogging) {
            NetworkDebugLogger.log("%s Resolved: %s", LOG_PREFIX, event.spell().getHostCard().getName());
        }
    }

    /**
     * EventBus subscriber for land played events.
     */
    @Subscribe
    public void handleLandPlayed(GameEventLandPlayed event) {
        if (verboseLogging) {
            NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
        }
    }

    /**
     * EventBus subscriber for attackers declared events.
     */
    @Subscribe
    public void handleAttackersDeclared(GameEventAttackersDeclared event) {
        if (verboseLogging && !event.attackersMap().isEmpty()) {
            NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
        }
    }

    /**
     * EventBus subscriber for blockers declared events.
     */
    @Subscribe
    public void handleBlockersDeclared(GameEventBlockersDeclared event) {
        if (verboseLogging && !event.blockers().isEmpty()) {
            NetworkDebugLogger.log("%s %s declared blockers: %s", LOG_PREFIX,
                event.defendingPlayer().getName(), event.blockers());
        }
    }

    /**
     * EventBus subscriber for life total changes.
     */
    @Subscribe
    public void handleLifeChanged(GameEventPlayerLivesChanged event) {
        if (verboseLogging) {
            NetworkDebugLogger.log("%s %s life: %d -> %d", LOG_PREFIX,
                event.player().getName(), event.oldLives(), event.newLives());
        }
    }

    // ====== Game End Events ======

    /**
     * EventBus subscriber method for game finished events.
     */
    @Subscribe
    public void handleGameFinished(GameEventGameFinished event) {
        gameFinished = true;
        NetworkDebugLogger.log("%s Game finished", LOG_PREFIX);

        if (gameFinishedHandler != null) {
            try {
                gameFinishedHandler.accept(event);
            } catch (Exception e) {
                NetworkDebugLogger.error(LOG_PREFIX + " Error in game finished handler: " + e.getMessage(), e);
            }
        }

        gameFinishedLatch.countDown();
    }

    /**
     * EventBus subscriber method for game outcome events.
     */
    @Subscribe
    public void handleGameOutcome(GameEventGameOutcome event) {
        if (event.result() != null && event.result().getWinningPlayer() != null) {
            winner = event.result().getWinningPlayer().getPlayer().getName();
            NetworkDebugLogger.log("%s Game outcome: winner = %s", LOG_PREFIX, winner);
        }

        if (gameOutcomeHandler != null) {
            try {
                gameOutcomeHandler.accept(event);
            } catch (Exception e) {
                NetworkDebugLogger.error(LOG_PREFIX + " Error in game outcome handler: " + e.getMessage(), e);
            }
        }
    }

    // ====== Query Methods ======

    /**
     * Wait for the game to finish.
     *
     * @param timeout Maximum time to wait
     * @param unit Time unit for timeout
     * @return true if game finished, false if timeout
     */
    public boolean waitForGameFinished(long timeout, TimeUnit unit) throws InterruptedException {
        return gameFinishedLatch.await(timeout, unit);
    }

    /**
     * Get the current turn number.
     */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /**
     * Check if the game has finished.
     */
    public boolean isGameFinished() {
        return gameFinished;
    }

    /**
     * Get the winner's name, or null if no winner yet.
     */
    public String getWinner() {
        return winner;
    }

    /**
     * Reset the listener state for reuse.
     */
    public void reset() {
        currentTurn = 0;
        gameFinished = false;
        winner = null;
    }
}
