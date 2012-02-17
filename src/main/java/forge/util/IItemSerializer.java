package forge.util;



/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IItemSerializer<T extends IHasName> extends IItemReader<T> {
    void save(T unit);
    void erase(T unit);
}


