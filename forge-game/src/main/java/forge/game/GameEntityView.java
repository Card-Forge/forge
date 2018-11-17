package forge.game;

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
        TrackableCollection<GameEntityView> collection = new TrackableCollection<GameEntityView>();
        for (GameEntity e : entities) {
            collection.add(e.getView());
        }
        return collection;
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

    public Iterable<CardView> getAttachedBy() {
        return get(TrackableProperty.AttachedBy);
    }
    public boolean isAttached() {
        return getAttachedBy() != null;
    }

    protected void updateAttachedBy(GameEntity e) {
        if (e.isAttachedBy()) {
            set(TrackableProperty.AttachedBy, CardView.getCollection(e.getAttachedBy()));
        }
        else {
            set(TrackableProperty.AttachedBy, null);
        }
    }
}
