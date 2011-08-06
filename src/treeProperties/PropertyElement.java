/**
 * PropertyElement.java
 * 
 * Created on 19.08.2009
 */

package treeProperties;




/**
 * The class PropertyElement.
 * 
 * @version V0.0 19.08.2009
 * @author Clemens Koza
 */
public interface PropertyElement {
    /**
     * Returns the key of the property in the TreeProperties.
     */
    public String getKey();
    
    /**
     * Returns the type of the element.
     */
    public Class<?> getType();
    
    /**
     * Returns the value of the element.
     */
    public Object getValue();
    
    /**
     * Sets the property value as a string.
     */
    public void setValue(String value);
}
