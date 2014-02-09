package forge.util.maps;

import com.google.common.base.Supplier;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EnumMapOfLists<K extends Enum<K>, V> extends EnumMap<K, Collection<V>> implements MapOfLists<K, V> {
    private final Supplier<? extends Collection<V>> factory;
    
    private static final long serialVersionUID = 4107133987205594272L;

    public EnumMapOfLists(Class<K> keyType, Supplier<? extends Collection<V>> factory) {
        super(keyType);
        this.factory = factory;
    }


    public EnumMapOfLists(EnumMap<K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
        super(m);
        this.factory = factory;
    }

    public EnumMapOfLists(Map<K, ? extends List<V>> m, Supplier<? extends Collection<V>> factory) {
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
