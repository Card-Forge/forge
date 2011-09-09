/**
 * PropertyElement.java
 *
 * Created on 19.08.2009
 */

package treeProperties;


/**
 * The class PropertyElement.
 *
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 */
public interface PropertyElement {
    /**
     * Returns the key of the property in the TreeProperties.
     *
     * @return a {@link java.lang.String} object.
     */
    String getKey();

    /**
     * Returns the type of the element.
     *
     * @return a {@link java.lang.Class} object.
     */
    Class<?> getType();

    /**
     * Returns the value of the element.
     *
     * @return a {@link java.lang.Object} object.
     */
    Object getValue();

    /**
     * Sets the property value as a string.
     *
     * @param value a {@link java.lang.String} object.
     */
    void setValue(String value);
}
