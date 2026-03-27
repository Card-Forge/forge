package forge.trackable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import forge.game.IIdentifiable;

//base class for objects that can be tracked and synced between game server and GUI
public abstract class TrackableObject implements IIdentifiable, Serializable {
    private static final long serialVersionUID = 7386836745378571056L;

    private final int id;
    protected transient Tracker tracker;
    private final Map<TrackableProperty, Object> props;
    private volatile int version;
    // Per-consumer dirty tracking. Lazy-init: null until first registerConsumer
    // In offline games (no consumers), set() does no tracking work at all
    // Volatile: multiple DeltaSyncManager threads call registerConsumer concurrently
    private transient volatile Map<Integer, EnumSet<TrackableProperty>> consumers;
    private boolean copyingProps;

    protected TrackableObject(final int id0, final Tracker tracker) {
        id = id0;
        this.tracker = tracker;
        props = new EnumMap<>(TrackableProperty.class);
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
        return o.hashCode() == hashCode() && o.getClass().equals(getClass());
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

    public final <T> void set(final TrackableProperty key, final T value) {
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
                // TODO: A property changing A->B->A between consumer reads would still be marked dirty.
                // A checksum or version-per-property approach could skip this, but A->B->A is uncommon
                // in typical Magic game flow. Revisit if profiling shows excessive no-op deltas.
                markDirtyForConsumers(key);
                key.updateObjLookup(tracker, value);
            }
        }
        else if (!value.equals(props.put(key, value))) {
            markDirtyForConsumers(key);
            key.updateObjLookup(tracker, value);
        }
    }

    /**
     * Mark a property as dirty for all registered consumers and increment version.
     */
    private void markDirtyForConsumers(final TrackableProperty key) {
        Map<Integer, EnumSet<TrackableProperty>> c = consumers;
        if (c == null) {
            return;
        }
        version++;
        for (EnumSet<TrackableProperty> dirtySet : c.values()) {
            synchronized (dirtySet) {
                dirtySet.add(key);
            }
        }
    }

    public final void updateObjLookup() {
        for (final Entry<TrackableProperty, Object> prop : props.entrySet()) {
            prop.getKey().updateObjLookup(tracker, prop.getValue());
        }
    }

    /**
     * Copy all properties of another TrackableObject to this object.
     * Used in network full-state scenarios where all properties should be synced.
     */
    public final void copyChangedProps(final TrackableObject from) {
        if (copyingProps) { return; } //prevent infinite loop from circular reference
        copyingProps = true;
        for (final TrackableProperty prop : from.props.keySet()) {
            prop.copyChangedProps(from, this);
        }
        // Remove properties that reverted to default on the source.
        // set() removes props that equal their default value, so they won't
        // appear in from.props — but they may still be in our props with a
        // stale non-default value.
        props.keySet().retainAll(from.props.keySet());
        copyingProps = false;
    }

    // use when updating collection type properties without using set (or assigning the same object)
    @SuppressWarnings("unchecked")
    protected final void flagAsChanged(final TrackableProperty key) {
        // In network games, replace the mutable value with an immutable defensive copy.
        // Callers mutate Map/List/Collection in-place then call this method. Without the
        // copy, the daemon thread's toNetworkValue or walkAndCollect could iterate the
        // same mutable object while the game thread mutates it on the next update.
        if (consumers != null) { // volatile read — safe even if concurrently nulled
            Object value = props.get(key);
            if (value instanceof TrackableCollection tc) {
                props.put(key, new TrackableCollection<>(tc));
            } else if (value instanceof Map) {
                props.put(key, new HashMap<>((Map<?, ?>) value));
            } else if (value instanceof List) {
                props.put(key, new ArrayList<>((List<?>) value));
            }
        }
        markDirtyForConsumers(key);
        key.updateObjLookup(tracker, props.get(key));
    }

    /**
     * Get the monotonic version counter. Incremented on every actual property change.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Register a consumer for per-consumer dirty tracking.
     * Creates an EnumSet for this consumer; lazy-inits the consumer map.
     */
    public void registerConsumer(int consumerId) {
        Map<Integer, EnumSet<TrackableProperty>> c = consumers;
        if (c == null) {
            synchronized (this) {
                c = consumers;
                if (c == null) {
                    c = new ConcurrentHashMap<>();
                    consumers = c;
                }
            }
        }
        c.putIfAbsent(consumerId, EnumSet.noneOf(TrackableProperty.class));
    }

    /**
     * Unregister a consumer. Removes its dirty set.
     * Nulls the map if empty to avoid overhead in offline games.
     */
    public void unregisterConsumer(int consumerId) {
        if (consumers != null) {
            consumers.remove(consumerId);
            if (consumers.isEmpty()) {
                consumers = null;
            }
        }
    }

    /**
     * Get and clear dirty properties for a specific consumer.
     * Returns a snapshot copy; the consumer's dirty set is cleared.
     */
    public EnumSet<TrackableProperty> getAndClearDirtyProps(int consumerId) {
        Map<Integer, EnumSet<TrackableProperty>> c = consumers;
        if (c == null) {
            return EnumSet.noneOf(TrackableProperty.class);
        }
        EnumSet<TrackableProperty> dirtySet = c.get(consumerId);
        if (dirtySet == null) {
            return EnumSet.noneOf(TrackableProperty.class);
        }
        synchronized (dirtySet) {
            if (dirtySet.isEmpty()) {
                return EnumSet.noneOf(TrackableProperty.class);
            }
            EnumSet<TrackableProperty> copy = EnumSet.copyOf(dirtySet);
            dirtySet.clear();
            return copy;
        }
    }

}
