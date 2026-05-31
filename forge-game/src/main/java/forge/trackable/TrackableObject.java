package forge.trackable;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import forge.game.IIdentifiable;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;

/**
 * Base for objects that mirror engine state into a serialized view consumed by GUI(s).
 * Each subclass exposes its mutable state as {@link TrackableProperty} entries; the engine
 * writes via {@link #set} and consumers (GUIs) read via {@link #get}.
 *
 * <p><b>Consumer dirty bits.</b> Each GUI client that uses delta-sync registers as a
 * consumer; the object keeps a per-consumer set of properties dirty since the consumer's
 * last drain. {@link forge.gamemodes.net.server.DeltaSyncManager#collectDeltas} reads and
 * clears them. Offline games never register consumers, so {@code set} does no tracking work.
 *
 * <p><b>Freeze interaction.</b> When the owning {@link Tracker} is frozen, {@code set}
 * queues the change rather than applying it; the queued change replays at unfreeze. Do not
 * read a property during a frozen window expecting a freshly-set value — {@code get}
 * returns the pre-freeze value, not the queued one.
 */
public abstract class TrackableObject implements IIdentifiable, Serializable {
    private static final long serialVersionUID = 7386836745378571056L;

    private final int id;
    protected transient Tracker tracker;
    private final Map<TrackableProperty, Object> props;
    private int version;
    // Per-consumer dirty tracking. Lazy-init: null until first registerConsumer.
    private transient Map<Integer, EnumSet<TrackableProperty>> consumers;
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
        if (consumers == null) {
            return;
        }
        version++;
        for (EnumSet<TrackableProperty> dirtySet : consumers.values()) {
            dirtySet.add(key);
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
            prop.getType().copyChangedProps(from, this, prop);
        }
        // Remove properties that reverted to default on the source.
        // set() removes props that equal their default value, so they won't
        // appear in from.props — but they may still be in our props with a
        // stale non-default value.
        props.keySet().retainAll(from.props.keySet());
        copyingProps = false;
    }

    // use when updating collection type properties without using set (or assigning the same object)
    protected final void flagAsChanged(final TrackableProperty key) {
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
     * Check whether a consumer is currently registered on this object.
     * <p>
     * Used by network serialization to gate IdRef substitution: the server
     * registers a consumer on every TrackableObject it has included in a
     * delta packet for a given client. An object without that consumer is
     * one the client hasn't been told about (typically an ephemeral such as
     * a {@code Card.fromPaperCard} choice copy that never enters a tracked
     * zone), and protocol-method args holding it must serialize inline.
     */
    public boolean hasConsumer(int consumerId) {
        return consumers != null && consumers.containsKey(consumerId);
    }

    /**
     * Register a consumer for per-consumer dirty tracking.
     */
    public void registerConsumer(int consumerId) {
        if (consumers == null) {
            consumers = new HashMap<>();
        }
        consumers.putIfAbsent(consumerId, EnumSet.noneOf(TrackableProperty.class));
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
        if (consumers == null) {
            return EnumSet.noneOf(TrackableProperty.class);
        }
        EnumSet<TrackableProperty> dirtySet = consumers.get(consumerId);
        if (dirtySet == null || dirtySet.isEmpty()) {
            return EnumSet.noneOf(TrackableProperty.class);
        }
        EnumSet<TrackableProperty> copy = EnumSet.copyOf(dirtySet);
        dirtySet.clear();
        return copy;
    }

    //special methods for updating card and player properties as needed and returning the new collection
    public Card setCard(Card oldCard, Card newCard, TrackableProperty key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    public CardCollection setCards(CardCollection oldCards, CardCollection newCards, TrackableProperty key) {
        if (newCards == null || newCards.isEmpty()) { //avoid storing empty collections
            set(key, null);
            return null;
        }
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    public CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, TrackableProperty key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    public CardCollection addCard(CardCollection oldCards, Card cardToAdd, TrackableProperty key) {
        if (cardToAdd == null) { return oldCards; }

        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        if (oldCards.add(cardToAdd)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                views = new TrackableCollection<>();
                views.add(cardToAdd.getView());
                set(key, views);
            }
            else if (views.add(cardToAdd.getView())) {
                flagAsChanged(key);
            }
        }
        return oldCards;
    }
    public CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, TrackableProperty key) {
        if (cardsToAdd == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        boolean needFlagAsChanged = false;
        for (Card c : cardsToAdd) {
            if (c != null && oldCards.add(c)) {
                if (views == null) {
                    views = new TrackableCollection<>();
                    views.add(c.getView());
                    set(key, views);
                }
                else if (views.add(c.getView())) {
                    needFlagAsChanged = true;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    public CardCollection removeCard(CardCollection oldCards, Card cardToRemove, TrackableProperty key) {
        if (cardToRemove == null || oldCards == null) { return oldCards; }

        if (oldCards.remove(cardToRemove)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                set(key, null);
            } else if (views.remove(cardToRemove.getView())) {
                if (views.isEmpty()) {
                    set(key, null); //avoid keeping around an empty collection
                }
                else {
                    flagAsChanged(key);
                }
            }
            if (oldCards.isEmpty()) {
                oldCards = null; //avoid keeping around an empty collection
            }
        }
        return oldCards;
    }
    public CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, TrackableProperty key) {
        if (cardsToRemove == null || oldCards == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        boolean needFlagAsChanged = false;
        for (Card c : cardsToRemove) {
            if (oldCards.remove(c)) {
                if (views == null) {
                    set(key, null);
                } else if (views.remove(c.getView())) {
                    if (views.isEmpty()) {
                        views = null;
                        set(key, null); //avoid keeping around an empty collection
                        needFlagAsChanged = false; //doesn't need to be flagged a second time
                    }
                    else {
                        needFlagAsChanged = true;
                    }
                }
                if (oldCards.isEmpty()) {
                    oldCards = null; //avoid keeping around an empty collection
                    break;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    public CardCollection clearCards(CardCollection oldCards, TrackableProperty key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }
}
