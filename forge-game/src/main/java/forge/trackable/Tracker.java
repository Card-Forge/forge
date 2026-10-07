package forge.trackable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import forge.trackable.TrackableTypes.TrackableType;

/**
 * Per-game lookup + change-coalescing ledger for {@link TrackableObject}s. Owned by the
 * game thread; every TrackableObject in an active game's view holds a reference to one
 * instance.
 *
 * <p><b>Object lookup.</b> Stores ({@link TrackableTypes.TrackableType}, id) → instance.
 * Used by deserialization to resolve {@code IdRef} stand-ins back to canonical objects.
 *
 * <p><b>Freeze model.</b> {@link #freeze()}/{@link #unfreeze()} bracket a region during
 * which {@link TrackableObject#set} queues changes rather than applying them. When the
 * freeze counter reaches zero, queued changes replay through {@code set} and may cascade
 * into consumer dirty-bit updates. Used to bundle the state changes of a multi-step
 * engine effect into a single coherent post-effect snapshot. {@link #flush()} drains the
 * queue without leaving the frozen state.
 *
 * <p><b>Thread safety.</b> Not thread-safe — game thread only. The {@code unfreeze}
 * replay walks TrackableObjects and triggers consumer notifications; running it from
 * another thread corrupts consumer dirty-bit state.
 */
public class Tracker {
    private int freezeCounter = 0;
    private final List<DelayedPropChange> delayedPropChanges = Lists.newArrayList();

    private final Table<TrackableType<?>, Integer, Object> objLookups = HashBasedTable.create();

    public final boolean isFrozen() {
        return freezeCounter > 0;
    }

    public void freeze() {
        freezeCounter++;
    }

    // Note: objLookups exist on the tracker and not on the TrackableType because
    // TrackableType is global and Tracker is per game.
    @SuppressWarnings("unchecked")
    public <T> T getObj(TrackableType<T> type, Integer id) {
        return (T)objLookups.get(type, id);
    }

    public boolean hasObj(TrackableType<?> type, Integer id) {
        return objLookups.contains(type, id);
    }

    public <T> void putObj(TrackableType<T> type, Integer id, T val) {
        objLookups.put(type, id, val);
    }

    public void unfreeze() {
        if (!isFrozen() || --freezeCounter > 0 || delayedPropChanges.isEmpty()) {
            return;
        }
        //after being unfrozen, ensure all changes delayed during freeze are now applied
        for (final DelayedPropChange change : delayedPropChanges) {
            change.object.set(change.prop, change.value);
        }
        delayedPropChanges.clear();
    }

    public void flush() {
        // unfreeze and refreeze the tracker in order to flush current pending properties
        if (!isFrozen()) {
            return;
        }
        unfreeze();
        freeze();
    }

    public void addDelayedPropChange(final TrackableObject object, final TrackableProperty prop, final Object value) {
        delayedPropChanges.add(new DelayedPropChange(object, prop, value));
    }

    public void clearDelayed() {
        delayedPropChanges.clear();
    }

    /**
     * Read-only peek at delayed property changes queued for a specific object.
     */
    public Map<TrackableProperty, Object> getDelayedPropsFor(TrackableObject obj) {
        if (delayedPropChanges.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<TrackableProperty, Object> result = new EnumMap<>(TrackableProperty.class);
        for (DelayedPropChange change : delayedPropChanges) {
            if (change.object == obj) {
                result.put(change.prop, change.value);
            }
        }
        return result;
    }

    private class DelayedPropChange {
        private final TrackableObject object;
        private final TrackableProperty prop;
        private final Object value;
        private DelayedPropChange(final TrackableObject object0, final TrackableProperty prop0, final Object value0) {
            object = object0;
            prop = prop0;
            value = value0;
        }
        @Override public String toString() {
            return "Set " + prop + " of " + object + " to " + value;
        }
    }
}
