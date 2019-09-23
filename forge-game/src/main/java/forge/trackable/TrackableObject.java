package forge.trackable;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject implements IIdentifiable, Serializable {
    private static final long serialVersionUID = 7386836745378571056L;

    private final int id;
    protected transient Tracker tracker;
    private final Map<TrackableProperty, Object> props;
    private final Set<TrackableProperty> changedProps;
    private boolean copyingProps;

    protected TrackableObject(final int id0, final Tracker tracker) {
        id = id0;
        this.tracker = tracker;
        props = new EnumMap<>(TrackableProperty.class);
        changedProps = EnumSet.noneOf(TrackableProperty.class);
    }

    public final int getId() {
        return id;
    }

    // needed for multiplayer support
    public void setTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public final Tracker getTracker() {
        return tracker;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public final boolean equals(final Object o) {
        if (o == null) { return false; }
        return o.hashCode() == id && o.getClass().equals(getClass());
    }

    // don't know if this is really needed, but don't know a better way
    public <T> T getProps() {
        return (T)props;
    }

    @SuppressWarnings("unchecked")
    protected final <T> T get(final TrackableProperty key) {
        T value = (T)props.get(key);
        if (value == null) {
            value = key.getDefaultValue();
        }
        return value;
    }

    protected final <T> void set(final TrackableProperty key, final T value) {
        if (tracker != null && tracker.isFrozen()) { //if trackable objects currently frozen, queue up delayed prop change
            boolean respectsFreeze = false;
            if (key.getFreezeMode() == TrackableProperty.FreezeMode.RespectsFreeze) {
                respectsFreeze = true;
            } else if (key.getFreezeMode() == TrackableProperty.FreezeMode.IgnoresFreezeIfUnset) {
                respectsFreeze = (props.get(key) != null);
            }
            if (respectsFreeze) {
                tracker.addDelayedPropChange(this, key, value);
                return;
            }
        }
        if (value == null || value.equals(key.getDefaultValue())) {
            if (props.remove(key) != null) {
                changedProps.add(key);
                key.updateObjLookup(tracker, value);
            }
        }
        else if (!value.equals(props.put(key, value))) {
            changedProps.add(key);
            key.updateObjLookup(tracker, value);
        }
    }

    public final void updateObjLookup() {
        for (final Entry<TrackableProperty, Object> prop : props.entrySet()) {
            prop.getKey().updateObjLookup(tracker, prop.getValue());
        }
    }

    /**
     * Copy all change properties of another Trackable object to this object.
     */
    public final void copyChangedProps(final TrackableObject from) {
        if (copyingProps) { return; } //prevent infinite loop from circular reference
        copyingProps = true;
        for (final TrackableProperty prop : from.changedProps) {
            prop.copyChangedProps(from, this);
        }
        copyingProps = false;
    }

    //use when updating collection type properties with using set
    protected final void flagAsChanged(final TrackableProperty key) {
        changedProps.add(key);
        key.updateObjLookup(tracker, props.get(key));
    }

    public final void serialize(final TrackableSerializer ts) {
        ts.write(changedProps.size());
        for (TrackableProperty key : changedProps) {
            ts.write(TrackableProperty.serialize(key));
            key.serialize(ts, props.get(key));
        }
        changedProps.clear();
    }

    public final void deserialize(final TrackableDeserializer td) {
        int count = td.readInt();
        for (int i = 0; i < count; i++) {
            TrackableProperty key = TrackableProperty.deserialize(td.readInt());
            set(key, key.deserialize(td, props.get(key)));
        }
        changedProps.clear();
    }
}
