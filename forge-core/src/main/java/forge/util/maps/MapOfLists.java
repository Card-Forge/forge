package forge.util.maps;

import java.util.Collection;
import java.util.Map;

public interface MapOfLists<K, V> extends Map<K, Collection<V>> {
    void add(K key, V element);
    void addAll(K key, Collection<V> element);
    Collection<V> ensureCollectionFor(K key);
}