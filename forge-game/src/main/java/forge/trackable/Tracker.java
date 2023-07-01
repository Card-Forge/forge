package forge.trackable;

import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import forge.trackable.TrackableTypes.TrackableType;

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
