package forge.util;

import java.util.List;

import forge.util.collect.FCollection;

public interface Visitor<T> {
    /**
     * visit the object
     * the Visitor should return true it can be visit again
     * returning false means the outer function can stop
     *
     * @param object
     * @return boolean
     */
    boolean visit(T object);

    default boolean visitAll(Iterable<? extends T> objects) {
        // Hot path (Game.forEachCardInGame): FCollection is ArrayList-backed, so index it
        // instead of allocating an Iterator for every zone visited.
        if (objects instanceof FCollection<?>) {
            final List<? extends T> list = (List<? extends T>) objects;
            for (int i = 0; i < list.size(); i++) {
                if (!visit(list.get(i))) {
                    return false;
                }
            }
            return true;
        }
        for (T obj : objects) {
            if (!visit(obj)) {
                return false;
            }
        }
        return true;
    }
}