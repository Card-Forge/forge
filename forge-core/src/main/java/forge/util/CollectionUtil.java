package forge.util;

import java.util.Collection;

public class CollectionUtil {

    public static <T> Boolean addNoNull(Collection<T> coll, T obj) {
        if (obj != null) {
            coll.add(obj);
            return true;
        }
        return false;
    }
}
