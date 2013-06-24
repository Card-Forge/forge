package forge.util.maps;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Supplier;

public class TreeMapOfLists<K, V> extends TreeMap<K, Collection<V>> implements MapOfLists<K, V> {
    private static final long serialVersionUID = -5881288393640446185L;
    private final Supplier<? extends Collection<V>> factory;
    
    public TreeMapOfLists(Supplier<? extends Collection<V>> factory) {
        super();
        this.factory = factory;
    }

    public TreeMapOfLists(Comparator<? super K> comparator, Supplier<? extends Collection<V>> factory) {
        super(comparator);
        this.factory = factory;
    }

    public TreeMapOfLists(Map<? extends K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
        super(m);
        this.factory = factory;
    }

    public TreeMapOfLists(SortedMap<K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
        super(m);
        this.factory = factory;
    }

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
