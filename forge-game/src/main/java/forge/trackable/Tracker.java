package forge.trackable;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;

import forge.trackable.TrackableTypes.TrackableType;

public class Tracker {
    private int freezeCounter = 0;
    private final List<DelayedPropChange> delayedPropChanges = Lists.newArrayList();
    private final HashMap<TrackableType<?>, Object> objLookups = new HashMap<>();

    public final boolean isFrozen() {
        return freezeCounter > 0;
    }

    public void freeze() {
        freezeCounter++;
    }

    // Note: objLookups exist on the tracker and not on the TrackableType because
    // TrackableType is global and Tracker is per game.
    public <T> HashMap<Integer, T> getObjLookupForType(TrackableType<T> type) {
        @SuppressWarnings("unchecked")
        HashMap<Integer, T> result = (HashMap<Integer, T>) objLookups.get(type);
        if (result == null) {
            result = new HashMap<>();
            objLookups.put(type, result);
        }
        return result;
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

    public void addDelayedPropChange(final TrackableObject object, final TrackableProperty prop, final Object value) {
        delayedPropChanges.add(new DelayedPropChange(object, prop, value));
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
