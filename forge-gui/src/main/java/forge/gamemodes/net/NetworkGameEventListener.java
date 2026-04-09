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
 * Logs all significant game actions to NetworkLogConfig for debugging
 * and analysis of network play sessions.
 *
 * Log output uses the [GAME EVENT] prefix for easy filtering and
 * clear visual separation from network categories like [DeltaSync].
 */
public class NetworkGameEventListener implements IHasNetLog {

    private static final String LOG_PREFIX = "[GAME EVENT]";

    public NetworkGameEventListener() {
        netLog.info("{} Network game event listener initialized", LOG_PREFIX);
    }

    @Subscribe
    public void handleTurnBegan(GameEventTurnBegan event) {
        netLog.info("{} Turn {} began - {}'s turn",
                LOG_PREFIX, event.turnNumber(), event.turnOwner().getName());
    }

    @Subscribe
    public void handlePhaseChange(GameEventTurnPhase event) {
        netLog.info("{} {}", LOG_PREFIX, event.toString());
    }

    @Subscribe
    public void handleSpellCast(GameEventSpellAbilityCast event) {
        netLog.info("{} {}", LOG_PREFIX, event.toString());
    }

    @Subscribe
    public void handleSpellResolved(GameEventSpellResolved event) {
        netLog.info("{} Resolved: {}", LOG_PREFIX, event.spell().getHostCard().getName());
    }

    @Subscribe
    public void handleLandPlayed(GameEventLandPlayed event) {
        netLog.info("{} {}", LOG_PREFIX, event.toString());
    }

    @Subscribe
    public void handleAttackersDeclared(GameEventAttackersDeclared event) {
        if (!event.attackersMap().isEmpty()) {
            netLog.info("{} {}", LOG_PREFIX, event.toString());
        }
    }

    @Subscribe
    public void handleBlockersDeclared(GameEventBlockersDeclared event) {
        if (!event.blockers().isEmpty()) {
            netLog.info("{} {} declared blockers: {}", LOG_PREFIX,
                    event.defendingPlayer().getName(), event.blockers());
        }
    }

    @Subscribe
    public void handleLifeChanged(GameEventPlayerLivesChanged event) {
        netLog.info("{} {} life: {} -> {}", LOG_PREFIX,
                event.player().getName(), event.oldLives(), event.newLives());
    }

    @Subscribe
    public void handleGameFinished(GameEventGameFinished event) {
        netLog.info("{} Game finished", LOG_PREFIX);
    }

    @Subscribe
    public void handleGameOutcome(GameEventGameOutcome event) {
        if (event.winningPlayerName() != null) {
            netLog.info("{} Game outcome: winner = {}", LOG_PREFIX, event.winningPlayerName());
        } else {
            netLog.info("{} Game outcome: draw or no winner", LOG_PREFIX);
        }
    }

}
