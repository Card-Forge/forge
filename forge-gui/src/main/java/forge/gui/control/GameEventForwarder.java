package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.interfaces.IGuiGame;

import java.util.ArrayList;
import java.util.List;

public class GameEventForwarder {
    private static final long FLUSH_INTERVAL_NS = 50_000_000L; // 50ms
    private static final int FLUSH_SIZE_THRESHOLD = 50;

    private final IGuiGame gui;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    private long lastFlushTime = System.nanoTime();

    public GameEventForwarder(IGuiGame gui) { this.gui = gui; }

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
}
