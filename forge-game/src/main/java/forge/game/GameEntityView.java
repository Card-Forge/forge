package forge.game;

import forge.game.card.CardView;
import forge.trackable.TrackableObject;

public abstract class GameEntityView<E extends Enum<E>> extends TrackableObject<E> {
    public static GameEntityView<?> get(GameEntity e) {
        return e == null ? null : e.getView();
    }

    protected GameEntityView(int id0, Class<E> propEnum0) {
        super(id0, propEnum0);
    }

    protected abstract E preventNextDamageProp();

    public int getPreventNextDamage() {
        return get(preventNextDamageProp());
    }
    void updatePreventNextDamage(GameEntity e) {
        set(preventNextDamageProp(), e.getPreventNextDamageTotalShields());
    }

    protected abstract E enchantedByProp();

    public Iterable<CardView> getEnchantedBy() {
        return get(enchantedByProp());
    }
    void updateEnchantedBy(GameEntity e) {
        if (e.isEnchanted()) {
            set(enchantedByProp(), CardView.getCollection(e.getEnchantedBy(false)));
        }
        else {
            set(enchantedByProp(), null);
        }
    }
    public boolean isEnchanted() {
        return getEnchantedBy() != null;
    }
}
