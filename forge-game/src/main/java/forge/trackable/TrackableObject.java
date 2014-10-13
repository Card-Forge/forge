package forge.trackable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject implements IIdentifiable {
    private static int freezeCounter = 0;
    public static void freeze() {
        freezeCounter++;
    }
    public static void unfreeze() {
        if (freezeCounter == 0 || --freezeCounter > 0 || delayedPropChanges.isEmpty()) {
            return;
        }
        //after being unfrozen, ensure all changes delayed during freeze are now applied
        for (DelayedPropChange change : delayedPropChanges) {
            change.object.set(change.prop, change.value);
        }
        delayedPropChanges.clear();
    }
    private static class DelayedPropChange {
        private final TrackableObject object;
        private final TrackableProperty prop;
        private final Object value;
        private DelayedPropChange(TrackableObject object0, TrackableProperty prop0, Object value0) {
            object = object0;
            prop = prop0;
            value = value0;
        }
    }
    private static final ArrayList<DelayedPropChange> delayedPropChanges = new ArrayList<DelayedPropChange>();

    private final int id;
    private final EnumMap<TrackableProperty, Object> props;
    private final EnumSet<TrackableProperty> changedProps;

    protected TrackableObject(int id0) {
        id = id0;
        props = new EnumMap<TrackableProperty, Object>(TrackableProperty.class);
        changedProps = EnumSet.noneOf(TrackableProperty.class);
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(TrackableProperty key) {
        T value = (T)props.get(key);
        if (value == null) {
            value = key.getDefaultValue();
        }
        return value;
    }

    protected <T> void set(TrackableProperty key, T value) {
        if (freezeCounter > 0 && key.respectFreeze()) { //if trackable objects currently frozen, queue up delayed prop change
            delayedPropChanges.add(new DelayedPropChange(this, key, value));
            return;
        }
        if (value == null || value.equals(key.getDefaultValue())) {
            if (props.remove(key) != null) {
                changedProps.add(key);
            }
        }
        else if (!value.equals(props.put(key, value))) {
            changedProps.add(key);
        }
    }

    //use when updating collection type properties with using set
    protected void flagAsChanged(TrackableProperty key) {
        changedProps.add(key);
    }

    public void serialize(TrackableSerializer ts) {
        ts.write(changedProps.size());
        for (TrackableProperty key : changedProps) {
            ts.write(TrackableProperty.serialize(key));
            key.serialize(ts, props.get(key));
        }
        changedProps.clear();
    }

    public void deserialize(TrackableDeserializer td) {
        int count = td.readInt();
        for (int i = 0; i < count; i++) {
            TrackableProperty key = TrackableProperty.deserialize(td.readInt());
            set(key, key.deserialize(td, props.get(key)));
        }
        changedProps.clear();
    }
}
