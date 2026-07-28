package forge.game;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Multiset;

import forge.game.card.CardView;
import forge.game.card.CounterType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;

public abstract class GameEntityView extends TrackableObject {
    private static final long serialVersionUID = -5129089945124455670L;

    public static GameEntityView get(GameEntity e) {
        return e == null ? null : e.getView();
    }

    public static TrackableCollection<GameEntityView> getEntityCollection(Iterable<? extends GameEntity> entities) {
        if (entities == null) {
            return null;
        }
        TrackableCollection<GameEntityView> collection = new TrackableCollection<>();
        for (GameEntity e : entities) {
            collection.add(e.getView());
        }
        return collection;
    }

    public static <T extends GameEntity, V extends GameEntityView> GameEntityViewMap<T, V> getMap(Iterable<T> spabs) {
        GameEntityViewMap<T, V> gameViewCache = new GameEntityViewMap<T, V>();
        gameViewCache.putAll(spabs);
        return gameViewCache;
    }

    protected GameEntityView(final int id0, final Tracker tracker) {
        super(id0, tracker);
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return get(TrackableProperty.Name);
    }
    protected void updateName(GameEntity e) {
        set(TrackableProperty.Name, e.getName());
    }

    public int getPreventNextDamage() {
        return get(TrackableProperty.PreventNextDamage);
    }
    public void updatePreventNextDamage(GameEntity e) {
        set(TrackableProperty.PreventNextDamage, e.getPreventNextDamageTotalShields());
    }

    public List<CardView> getAttachedCards() {
        return getAllAttachedCards().stream().filter(c -> !c.isPhasedOut()).collect(Collectors.toList());
    }
    public boolean hasCardAttachments() {
        return !getAttachedCards().isEmpty();
    }
    public List<CardView> getAllAttachedCards() {
        return Objects.requireNonNullElse(get(TrackableProperty.AttachedCards), List.of());
    }
    public boolean hasAnyCardAttachments() {
        return getAllAttachedCards().isEmpty();
    }

    public Multiset<CounterType> getCounters() {
        return get(TrackableProperty.Counters);
    }
    public int getCounters(CounterType counterType) {
        final Multiset<CounterType> counters = getCounters();
        if (counters != null) {
            return counters.count(counterType);
        }
        return 0;
    }
    public boolean hasSameCounters(GameEntityView other) {
        Multiset<CounterType> counters = getCounters();
        if (counters == null) {
            return other.getCounters() == null;
        }
        return counters.equals(other.getCounters());
    }

    public void updateCounters(GameEntity e) {
        set(TrackableProperty.Counters, e.getCounters());
        flagAsChanged(TrackableProperty.Counters);
    }
}
