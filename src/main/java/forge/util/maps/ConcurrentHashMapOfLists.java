package forge.util.maps;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;

public class ConcurrentHashMapOfLists<K, V> extends ConcurrentHashMap<K, Collection<V>> implements MapOfLists<K, V> {

    private final Supplier<? extends Collection<V>> factory;
    public ConcurrentHashMapOfLists(Supplier<? extends Collection<V>> factory) {
        super();
        this.factory = factory;
    }

    public ConcurrentHashMapOfLists(int initialCapacity, float loadFactor, Supplier<? extends Collection<V>> factory) {
        super(initialCapacity, loadFactor);
        this.factory = factory;
    }

    public ConcurrentHashMapOfLists(int initialCapacity, Supplier<? extends Collection<V>> factory) {
        super(initialCapacity);
        this.factory = factory;
    }

    public ConcurrentHashMapOfLists(Map<? extends K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
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
