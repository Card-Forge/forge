package forge.game;

import forge.game.card.CardView;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

public abstract class GameEntityView extends TrackableObject {
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

    protected GameEntityView(int id0) {
        super(id0);
    }

    public String getName() {
        return get(TrackableProperty.Name);
    }
    void updateName(GameEntity e) {
        set(TrackableProperty.Name, e.getName());
    }

    public int getPreventNextDamage() {
        return get(TrackableProperty.PreventNextDamage);
    }
    void updatePreventNextDamage(GameEntity e) {
        set(TrackableProperty.PreventNextDamage, e.getPreventNextDamageTotalShields());
    }

    public Iterable<CardView> getEnchantedBy() {
        return get(TrackableProperty.EnchantedBy);
    }
    void updateEnchantedBy(GameEntity e) {
        if (e.isEnchanted()) {
            set(TrackableProperty.EnchantedBy, CardView.getCollection(e.getEnchantedBy(false)));
        }
        else {
            set(TrackableProperty.EnchantedBy, null);
        }
    }
    public boolean isEnchanted() {
        return getEnchantedBy() != null;
    }
}
