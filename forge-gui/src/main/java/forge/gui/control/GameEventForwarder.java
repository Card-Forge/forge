package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.event.GameEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;

import java.util.ArrayList;
import java.util.List;

public class GameEventForwarder {
    private final IGuiGame gui;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    private volatile boolean flushQueued = false;

    public GameEventForwarder(IGuiGame gui) { this.gui = gui; }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) {
        synchronized (pendingEvents) {
            pendingEvents.add(ev);
        }
        if (!flushQueued) {
            flushQueued = true;
            GuiBase.getInterface().invokeInEdtLater(this::flush);
        }
    }

    private void flush() {
        flushQueued = false;
        List<GameEvent> batch;
        synchronized (pendingEvents) {
            batch = new ArrayList<>(pendingEvents);
            pendingEvents.clear();
        }
        if (!batch.isEmpty()) {
            gui.handleGameEvents(batch);
        }
    }
}
