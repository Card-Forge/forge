package tree.properties;

/**
 * PropertyTypeHandler.java
 *
 * Created on 19.08.2009
 */

/**
 * The class PropertyType. A property type is used to process special, suffixed
 * entries in a {@link TreeProperties} ' properties-file
 * 
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 * 
 * @param <T>
 * 
 */
public interface PropertyType<T> {
    /**
     * The suffix, not including "--", that identifies this content type.
     * 
     * @return a {@link java.lang.String} object.
     */
    String getSuffix();

    /**
     * The class that identifies this content type.
     * 
     * @return a {@link java.lang.Class} object.
     */
    Class<T> getType();

    /**
     * Returns an object for the specified value, in the context of a
     * TreeProperties.
     * 
     * @param p
     *            a {@link tree.properties.TreeProperties} object.
     * @param s
     *            a {@link java.lang.String} object.
     * @return a T object.
     */
    T toObject(TreeProperties p, String s);
}
