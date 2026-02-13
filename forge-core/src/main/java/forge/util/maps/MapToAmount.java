package forge.util.maps;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface MapToAmount<K> extends Map<K, Integer> {
    void add(K item);
    void add(K item, int amount);
    void addAll(Iterable<K> items);
    int countAll();
    int count(K item); // just unboxes and returns zero instead of null
}
