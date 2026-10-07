package forge.gui.control;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.eventbus.Subscribe;

import forge.game.PlanarDice;
import forge.game.event.GameEventRollDie;

/**
 * Forwards dice-roll game events to an optional platform listener (set by the mobile UI).
 * If no listener is set (desktop, headless sims) this does nothing.
 *
 * Subscribed once per Game from HostedMatch.startGame().
 *
 * The event bus delivers events on the GAME thread, synchronously inside game.fireEvent().
 * That lets us hold the game thread until the animation has finished, so the result
 * message / dialog that the effect shows right afterwards appears AFTER the dice have landed.
 */
public final class DiceEventBridge {
    public static final DiceEventBridge instance = new DiceEventBridge();

    /** Safety net so a stalled render thread (app in background, etc.) can never freeze the game. */
    private static final long MAX_WAIT_MS = 4500;

    public interface Listener {
        /**
         * Called on the GAME thread.
         * @return a latch that is released when the animation is finished, or null if nothing will be shown.
         */
        CountDownLatch onDiceRoll(int sides, int result, PlanarDice planar);
    }

    private volatile Listener listener;

    private DiceEventBridge() { }

    public void setListener(Listener l) {
        this.listener = l;
    }

    @Subscribe
    public void onRollDie(final GameEventRollDie ev) {
        final Listener l = listener;
        if (l == null) {
            return;
        }
        final CountDownLatch done = l.onDiceRoll(ev.sides, ev.result, ev.planar);
        if (done != null) {
            try {
                done.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}