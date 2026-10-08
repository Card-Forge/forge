package forge.gui.control;

import com.google.common.eventbus.Subscribe;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventPlayerPriority;
import forge.gui.interfaces.IGuiGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.stream.Collectors;

/**
 * Buffers game events and flushes them to the GUI in batches.
 *
 * <p>Flush triggers (all on the game thread):
 * <ul>
 *   <li>Priority: a player receives it once the game has settled, so a batch holds whole actions
 *       and what one player did is sent before anyone chooses what to do next. A pass with nothing
 *       but redraw hints since the last batch sends nothing</li>
 *   <li>Input queue change: registered as {@link Observer} on player InputQueues,
 *       ensuring events are delivered before the game thread blocks for input</li>
 *   <li>Sync points: explicit {@link #flush()} from {@code flushPendingEvents()}</li>
 * </ul>
 *
 * <p>No daemon thread — all delta collection runs on the game thread to avoid race issues.
 */
public class GameEventForwarder implements Observer {
    private final IGuiGame gui;
    private final List<GameEvent> pendingEvents = new ArrayList<>();
    /** Whether anything but a redraw hint is waiting, which is what makes a pass of priority worth a batch. */
    private boolean somethingHappened;

    public GameEventForwarder(IGuiGame gui) {
        this.gui = gui;
    }

    @Subscribe
    public void receiveGameEvent(GameEvent ev) {
        pendingEvents.add(ev);
        if (!(ev instanceof GameEventPlayerPriority)) {
            somethingHappened |= !ev.isRedrawHint();
        } else if (somethingHappened) {
            flush();
        }
    }

    public void flush() {
        if (pendingEvents.isEmpty()) {
            return;
        }
        List<GameEvent> batch = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        somethingHappened = false;
        gui.handleGameEvents(batch);
    }

    public boolean hasPendingEvents() {
        return !pendingEvents.isEmpty();
    }
    public boolean hasPendingZoneChange(Object... args) {
        List<Integer> zoneChangers = pendingEvents.stream().filter(GameEventCardChangeZone.class::isInstance).map(ev -> ((GameEventCardChangeZone) ev).card().getId()).collect(Collectors.toList());
        if (zoneChangers.isEmpty()) {
            return false;
        }
        for (Object obj : args) {
            if (obj instanceof CardView cv && zoneChangers.contains(cv.getId())) {
                return true;
            }
            if (obj instanceof Iterable<?> it) {
                for (Object e : it) {
                    if (e instanceof CardView cv && zoneChangers.contains(cv.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Called when an InputQueue changes (setInput/removeInput/clearInputs).
     * Flushes any pending events on the game thread before it blocks for input.
     */
    @Override
    public void update(Observable o, Object arg) {
        flush();
    }

}
