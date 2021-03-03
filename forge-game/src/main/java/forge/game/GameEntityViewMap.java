package forge.game;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import forge.trackable.TrackableCollection;

public class GameEntityViewMap<Entity extends GameEntity, View extends GameEntityView> extends ForwardingMap<View, Entity> {
    private Map<View, Entity> dataMap = Maps.newLinkedHashMap();

    @Override
    protected Map<View, Entity> delegate() {
        return dataMap;
    }

    @SuppressWarnings("unchecked")
    public void put(Entity e) {
        this.put((View) e.getView(), e);
    }

    public void putAll(Iterable<Entity> entities) {
        for (Entity e : entities) {
            put(e);
        }
    }

    public void remove(Entity e) {
        this.remove(e.getView());
    }

    public void removeAll(Iterable<Entity> entities) {
        for (Entity e : entities) {
            remove(e);
        }
    }

    public List<Entity> addToList(Iterable<View> views, List<Entity> list) {
        if (views == null) {
            return list;
        }
        for (View view : views) {
            Entity entity = get(view);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }

    public TrackableCollection<View> getTrackableKeys() {
        return new TrackableCollection<View>(this.keySet());
    }
}
