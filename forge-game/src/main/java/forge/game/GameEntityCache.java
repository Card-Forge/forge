package forge.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import forge.trackable.TrackableObject;

public class GameEntityCache<Entity extends IIdentifiable, View extends TrackableObject> {
    private HashMap<Integer, Entity> entityCache = new HashMap<>();
 
    public void put(Integer id, Entity entity) {
        entityCache.put(id, entity);
    }
    public void putAll(Iterable<Entity> entities) {
        for (Entity e : entities) {
            put(e.getId(), e);
        }
    }

    public void remove(Integer id) {
        entityCache.remove(id);
    }

    public Entity get(View entityView) {
        if (entityView == null) { return null; }
        return entityCache.get(entityView.getId());
    }

    public void addToList(Iterable<View> views, List<Entity> list) {
        for (View view : views) {
            Entity entity = get(view);
            if (entity != null) {
                list.add(entity);
            }
        }
    }

    public List<Entity> getList(Iterable<View> views) {
        List<Entity> list = new ArrayList<>();
        addToList(views, list);
        return list;
    }

    public void clear() {
        entityCache.clear();
    }

    public Collection<Entity> getValues() {
        return entityCache.values();
    }
}
