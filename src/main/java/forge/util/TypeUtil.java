package forge.util;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TypeUtil {

    /**
     * Cast object to a given type if possible, returning null if not possible
     * @param obj
     * @param type
     */
    @SuppressWarnings("unchecked")
    public static <T> T safeCast(Object obj, Class<T> type) {
        if (type.isInstance(obj)) {
            return (T) obj;
        }
        return null;
    }
}
