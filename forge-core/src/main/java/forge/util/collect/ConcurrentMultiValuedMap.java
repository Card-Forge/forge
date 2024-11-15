package forge.util.collect;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.collections4.multiset.HashMultiSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

public class ConcurrentMultiValuedMap<K,V> implements MultiValuedMap<K,V> {

    private Map<K, Collection<V>> storage = new ConcurrentHashMap<>();

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        //noinspection SuspiciousMethodCalls
        return storage.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        //noinspection SuspiciousMethodCalls
        return storage.containsValue(o);
    }

    @Override
    public boolean containsMapping(Object key, Object value) {
        @SuppressWarnings("SuspiciousMethodCalls")
        Collection<V> values = storage.get(key);
        if(values==null) {
            return false;
        }
        for (V v : values) {
            if(v.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<V> get(K k) {
        return storage.get(k);
    }

    private Collection<V> safeGet(K key) {
        return storage.computeIfAbsent(key, value -> new CopyOnWriteArraySet<>());
    }

    @Override
    public boolean put(K k, V v) {
        return safeGet(k).add(v);
    }

    @Override
    public boolean putAll(K k, Iterable<? extends V> iterable) {
        Collection<V> values = safeGet(k);
        for (V v : iterable) {
            if(!values.add(v)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if(!safeGet(entry.getKey()).add(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean putAll(MultiValuedMap<? extends K, ? extends V> multiValuedMap) {
        MapIterator<? extends K, ? extends V> iterator = multiValuedMap.mapIterator();
        while(iterator.hasNext()) {
            if(!safeGet(iterator.getKey()).add(iterator.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Collection<V> remove(Object o) {
        //noinspection SuspiciousMethodCalls
        return storage.remove(o);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean removeMapping(Object o, Object v) {
        Collection<V> values = storage.get(o);
        return values != null && values.remove(v);
    }

    @Override
    public void clear() {
        storage.clear();
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        Collection<Map.Entry<K, V>> collection = new LinkedList<>();
        collectValues((k,v)->{
            Map.Entry<K,V> mapEntry = new TempMapEntry(k, v);
            collection.add(mapEntry);
        });
        return collection;
    }

    @Override
    public MultiSet<K> keys() {
        MultiSet<K> multiSet = new HashMultiSet<>();
        multiSet.addAll(storage.keySet());
        return multiSet;
    }

    @Override
    public Set<K> keySet() {
        return storage.keySet();
    }

    @Override
    public Collection<V> values() {
        List<V> values = new LinkedList<>();
        for (Map.Entry<K, Collection<V>> entry : storage.entrySet()) {
            for (V v : entry.getValue()) {
                values.add(v);
            }
        }
        return values;
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return Collections.unmodifiableMap(storage);
    }

    @Override
    public MapIterator<K, V> mapIterator() {
        HashedMap<K,V> hashedMap = new HashedMap<>();
        collectValues(hashedMap::put);
        return hashedMap.mapIterator();
    }

    private void collectValues(BiConsumer<K,V> consumer) {
        for (Map.Entry<K, Collection<V>> entry : storage.entrySet()) {
            for (V v : entry.getValue()) {
                consumer.accept(entry.getKey(), v);
            }
        }
    }

    private final class TempMapEntry implements Map.Entry<K,V> {

        private V val;
        private final K key;

        TempMapEntry(K key, V value) {
            this.val = value;
            this.key = key;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return val;
        }

        @Override
        public V setValue(V value) {
            V old = val;
            val = value;
            return old;
        }
    }
}
