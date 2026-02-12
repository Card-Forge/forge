package forge.gamemodes.net;

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
import forge.game.event.GameEventTurnPhase;

/**
 * Listener for game events during network play.
 * Logs all significant game actions to NetworkDebugLogger for debugging
 * and analysis of network play sessions.
 *
 * Uses Guava EventBus subscription pattern - subscribe to Game.events via:
 * <pre>
 * game.subscribeToEvents(new NetworkGameEventListener());
 * </pre>
 *
 * Log output uses the [GAME EVENT] prefix for easy filtering and
 * clear visual separation from network categories like [DeltaSync].
 */
public class NetworkGameEventListener {

    private static final String LOG_PREFIX = "[GAME EVENT]";

    // Track current turn for context
    private volatile int currentTurn = 0;

    /**
     * Create a new listener for network game events.
     */
    public NetworkGameEventListener() {
        NetworkDebugLogger.log("%s Network game event listener initialized", LOG_PREFIX);
    }

    // ====== Turn Events ======

    /**
     * EventBus subscriber method for turn began events.
     */
    @Subscribe
    public void handleTurnBegan(GameEventTurnBegan event) {
        currentTurn = event.turnNumber();
        NetworkDebugLogger.log("%s Turn %d began - %s's turn",
                LOG_PREFIX, currentTurn, event.turnOwner().getName());
    }

    /**
     * EventBus subscriber method for phase change events.
     */
    @Subscribe
    public void handlePhaseChange(GameEventTurnPhase event) {
        NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
    }

    // ====== Game Action Events ======

    /**
     * EventBus subscriber for spell/ability cast events.
     */
    @Subscribe
    public void handleSpellCast(GameEventSpellAbilityCast event) {
        NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
    }

    /**
     * EventBus subscriber for spell resolved events.
     */
    @Subscribe
    public void handleSpellResolved(GameEventSpellResolved event) {
        NetworkDebugLogger.log("%s Resolved: %s", LOG_PREFIX, event.spell().getHostCard().getName());
    }

    /**
     * EventBus subscriber for land played events.
     */
    @Subscribe
    public void handleLandPlayed(GameEventLandPlayed event) {
        NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
    }

    /**
     * EventBus subscriber for attackers declared events.
     */
    @Subscribe
    public void handleAttackersDeclared(GameEventAttackersDeclared event) {
        if (!event.attackersMap().isEmpty()) {
            NetworkDebugLogger.log("%s %s", LOG_PREFIX, event.toString());
        }
    }

    /**
     * EventBus subscriber for blockers declared events.
     */
    @Subscribe
    public void handleBlockersDeclared(GameEventBlockersDeclared event) {
        if (!event.blockers().isEmpty()) {
            NetworkDebugLogger.log("%s %s declared blockers: %s", LOG_PREFIX,
                    event.defendingPlayer().getName(), event.blockers());
        }
    }

    /**
     * EventBus subscriber for life total changes.
     */
    @Subscribe
    public void handleLifeChanged(GameEventPlayerLivesChanged event) {
        NetworkDebugLogger.log("%s %s life: %d -> %d", LOG_PREFIX,
                event.player().getName(), event.oldLives(), event.newLives());
    }

    // ====== Game End Events ======

    /**
     * EventBus subscriber method for game finished events.
     */
    @Subscribe
    public void handleGameFinished(GameEventGameFinished event) {
        NetworkDebugLogger.log("%s Game finished", LOG_PREFIX);
    }

    /**
     * EventBus subscriber method for game outcome events.
     */
    @Subscribe
    public void handleGameOutcome(GameEventGameOutcome event) {
        if (event.result() != null && event.result().getWinningPlayer() != null) {
            String winner = event.result().getWinningPlayer().getPlayer().getName();
            NetworkDebugLogger.log("%s Game outcome: winner = %s", LOG_PREFIX, winner);
        } else {
            NetworkDebugLogger.log("%s Game outcome: draw or no winner", LOG_PREFIX);
        }
    }

}
