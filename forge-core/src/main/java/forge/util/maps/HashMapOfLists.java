package forge.util.maps;

import com.google.common.base.Supplier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashMapOfLists<K, V> extends HashMap<K, Collection<V>> implements MapOfLists<K, V> {

    private final Supplier<? extends Collection<V>> factory;
    public HashMapOfLists(Supplier<? extends Collection<V>> factory) {
        super();
        this.factory = factory;
    }

    public HashMapOfLists(int initialCapacity, float loadFactor, Supplier<? extends Collection<V>> factory) {
        super(initialCapacity, loadFactor);
        this.factory = factory;
    }

    public HashMapOfLists(int initialCapacity, Supplier<? extends Collection<V>> factory) {
        super(initialCapacity);
        this.factory = factory;
    }

    public HashMapOfLists(Map<? extends K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
        super(m);
        this.factory = factory;
    }

    private static final long serialVersionUID = 3029089910183132930L;

    public Collection<V> ensureCollectionFor(K key) {
        Collection<V> value = get(key);
        if ( value == null ) {
            value = factory.get();
            put(key, value);
        }
        return value;
    }
    
    @Override
    public void add(K key, V element) {
        ensureCollectionFor(key).add(element);
    }


    @Override
    public void addAll(K key, Collection<V> elements) {
        ensureCollectionFor(key).addAll(elements);
        
    }
}
