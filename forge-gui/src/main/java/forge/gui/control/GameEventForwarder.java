package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.interfaces.IGuiGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Buffers game events and flushes them to the GUI in batches.
 *
 * <p>Flush triggers (all on the game thread):
 * <ul>
 *   <li>Size threshold: 50+ buffered events in {@link #receiveGameEvent}</li>
 *   <li>Time threshold: 500ms+ since last flush in {@link #receiveGameEvent}</li>
 *   <li>Input queue change: registered as {@link Observer} on player InputQueues,
 *       ensuring events are delivered before the game thread blocks for input</li>
 *   <li>Sync points: explicit {@link #flush()} from {@code flushPendingEvents()}</li>
 * </ul>
 *
 * <p>No daemon thread — all delta collection runs on the game thread.
 */
public class GameEventForwarder implements Observer {
    private static final long FLUSH_INTERVAL_NS = 500_000_000L; // 500ms
    private static final int FLUSH_SIZE_THRESHOLD = 50;

    private final IGuiGame gui;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    private long lastFlushTime = System.nanoTime();

    public GameEventForwarder(IGuiGame gui) {
        this.gui = gui;
    }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) {
        synchronized (pendingEvents) {
            pendingEvents.add(ev);
        }
        long now = System.nanoTime();
        boolean timeThreshold = (now - lastFlushTime) >= FLUSH_INTERVAL_NS;
        boolean sizeThreshold;
        synchronized (pendingEvents) {
            sizeThreshold = pendingEvents.size() >= FLUSH_SIZE_THRESHOLD;
        }
        if (timeThreshold || sizeThreshold) {
            flush();
        }
    }

    public void flush() {
        List<GameEvent> batch;
        synchronized (pendingEvents) {
            if (pendingEvents.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(pendingEvents);
            pendingEvents.clear();
        }
        lastFlushTime = System.nanoTime();
        gui.handleGameEvents(batch);
    }

    /**
     * Called when an InputQueue changes (setInput/removeInput/clearInputs).
     * Flushes any pending events on the game thread before it blocks for input.
     */
    @Override
    public void update(Observable o, Object arg) {
        flush();
    }

    public void shutdown() {
        // No daemon thread to shut down — flush any remaining events
        flush();
    }
}
