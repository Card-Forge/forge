package forge.game;

import forge.game.card.CardView;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

public abstract class GameEntityView extends TrackableObject {
    public static GameEntityView get(GameEntity e) {
        return e == null ? null : e.getView();
    }

    protected GameEntityView(int id0) {
        super(id0);
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
