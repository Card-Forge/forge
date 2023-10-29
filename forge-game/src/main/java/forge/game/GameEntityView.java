package forge.game;

import com.google.common.collect.Iterables;

import forge.game.card.CardView;
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

    public Iterable<CardView> getAttachedCards() {
        if (hasAnyCardAttachments()) {
            Iterable<CardView> active = Iterables.filter(get(TrackableProperty.AttachedCards), c -> !c.isPhasedOut());
            if (!Iterables.isEmpty(active)) {
                return active;
            }
        }
        return null;
    }
    public boolean hasCardAttachments() {
        return getAttachedCards() != null;
    }
    public Iterable<CardView> getAllAttachedCards() {
        return get(TrackableProperty.AttachedCards);
    }
    public boolean hasAnyCardAttachments() {
        return getAllAttachedCards() != null;
    }

    protected void updateAttachedCards(GameEntity e) {
        if (!e.getAllAttachedCards().isEmpty()) {
            set(TrackableProperty.AttachedCards, CardView.getCollection(e.getAllAttachedCards()));
        } else {
            set(TrackableProperty.AttachedCards, null);
        }
    }
}
