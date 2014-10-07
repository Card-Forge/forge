package forge.trackable;

import java.util.EnumMap;
import java.util.EnumSet;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject implements IIdentifiable {
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
