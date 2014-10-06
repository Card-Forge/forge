package forge.trackable;

import java.util.EnumMap;
import java.util.EnumSet;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject<E extends Enum<E>> implements IIdentifiable {
    private final int id;
    private final EnumMap<E, Object> props;
    private final EnumSet<E> changedProps;

    protected TrackableObject(int id0, Class<E> propEnum0) {
        id = id0;
        props = new EnumMap<E, Object>(propEnum0);
        changedProps = EnumSet.noneOf(propEnum0);
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(E key) {
        return (T)props.get(key);
    }    

    protected <T> void set(E key, T value) {
        if (value == null) {
            if (props.remove(key) != null) {
                changedProps.add(key);
            }
        }
        else if (!value.equals(props.put(key, value))) {
            changedProps.add(key);
        }
    }

    //use when updating collection type properties with using set
    protected void flagAsChanged(E key) {
        changedProps.add(key);
    }

    public void serialize() {
        //TODO
    }

    public void deserialize() {
        //TODO
    }
}
