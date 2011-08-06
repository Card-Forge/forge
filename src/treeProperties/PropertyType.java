
package treeProperties;


/**
 * PropertyTypeHandler.java
 * 
 * Created on 19.08.2009
 */


/**
 * The class PropertyType. A property type is used to process special, suffixed entries in a {@link TreeProperties}
 * ' properties-file
 * 
 * @version V0.0 19.08.2009
 * @author Clemens Koza
 */
public interface PropertyType<T> {
    /**
     * The suffix, not including "--", that identifies this content type.
     */
    public String getSuffix();
    
    /**
     * The class that identifies this content type.
     */
    public Class<T> getType();
    
    /**
     * Returns an object for the specified value, in the context of a TreeProperties.
     */
    public T toObject(TreeProperties p, String s);
}
