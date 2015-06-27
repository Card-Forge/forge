package forge.trackable;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject implements IIdentifiable, Serializable {
    private static final long serialVersionUID = 7386836745378571056L;

    private final int id;
    protected final transient Tracker tracker;
    private final Map<TrackableProperty, Object> props;
    private final Set<TrackableProperty> changedProps;

    protected TrackableObject(final int id0, final Tracker tracker) {
        id = id0;
        this.tracker = tracker;
        props = new EnumMap<TrackableProperty, Object>(TrackableProperty.class);
        changedProps = EnumSet.noneOf(TrackableProperty.class);
    }

    public final int getId() {
        return id;
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

    @SuppressWarnings("unchecked")
    protected final <T> T get(final TrackableProperty key) {
        T value = (T)props.get(key);
        if (value == null) {
            value = key.getDefaultValue();
        }
        return value;
    }

    protected final <T> void set(final TrackableProperty key, final T value) {
        if (tracker != null && tracker.isFrozen() && key.respectFreeze()) { //if trackable objects currently frozen, queue up delayed prop change
            tracker.addDelayedPropChange(this, key, value);
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

    /**
     * Copy all change properties of another Trackable object to this object.
     */
    public final void copyChangedProps(final TrackableObject from) {
        for (final TrackableProperty prop : from.changedProps) {
            prop.copyChangedProps(from, this);
        }
    }

    //use when updating collection type properties with using set
    protected final void flagAsChanged(final TrackableProperty key) {
        changedProps.add(key);
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
