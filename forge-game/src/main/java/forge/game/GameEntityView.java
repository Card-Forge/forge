package forge.game;

import forge.game.card.CardCollectionView;
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
    protected void updatePreventNextDamage(GameEntity e) {
        set(TrackableProperty.PreventNextDamage, e.getPreventNextDamageTotalShields());
    }

    public Iterable<CardView> getAttachedCards() {
        return get(TrackableProperty.AttachedCards);
    }
    public boolean hasCardAttachments() {
        return getAttachedCards() != null;
    }
    public Iterable<CardView> getAllAttachedCards() {
        return get(TrackableProperty.AllAttachedCards);
    }
    public boolean hasAnyCardAttachments() {
        return getAllAttachedCards() != null;
    }

    protected void updateAttachedCards(GameEntity e) {
        if (e.hasCardAttachments()) {
            set(TrackableProperty.AttachedCards, CardView.getCollection(e.getAttachedCards()));
        }
        else {
            set(TrackableProperty.AttachedCards, null);
        }
        CardCollectionView all = e.getAllAttachedCards();
        if (all.isEmpty()) {
            set(TrackableProperty.AllAttachedCards, null);
        } else {
            set(TrackableProperty.AllAttachedCards, CardView.getCollection(all));
        }
    }
}
