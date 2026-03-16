package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.interfaces.IGuiGame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameEventForwarder {
    private static final long FLUSH_INTERVAL_NS = 500_000_000L; // 500ms
    private static final int FLUSH_SIZE_THRESHOLD = 50;

    private final IGuiGame gui;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    private long lastFlushTime = System.nanoTime();
    /** True when flush() is running from receiveGameEvent (game thread). */
    private volatile boolean gameThreadFlush;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "GameEventForwarder-flush");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> scheduledFlush;

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
            cancelScheduledFlush();
            gameThreadFlush = true;
            try {
                flush();
            } finally {
                gameThreadFlush = false;
            }
        } else if (scheduledFlush == null || scheduledFlush.isDone()) {
            // Schedule a deferred flush for when the interval expires
            long delayNs = FLUSH_INTERVAL_NS - (now - lastFlushTime);
            scheduledFlush = scheduler.schedule(this::flush, delayNs, TimeUnit.NANOSECONDS);
        }
    }

    public void flush() {
        cancelScheduledFlush();
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

    private void cancelScheduledFlush() {
        if (scheduledFlush != null) {
            scheduledFlush.cancel(false);
            scheduledFlush = null;
        }
    }

    /** True when flush is running on the game thread (from receiveGameEvent or flushPendingEvents). */
    public boolean isGameThreadFlush() {
        return gameThreadFlush;
    }

    public void shutdown() {
        cancelScheduledFlush();
        scheduler.shutdownNow();
    }
}
