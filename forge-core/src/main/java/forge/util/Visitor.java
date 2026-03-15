package forge.util;

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
        for (T obj : objects) {
            if (!visit(obj)) {
                return false;
            }
        }
        return true;
    }
}
