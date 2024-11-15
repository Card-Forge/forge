package forge.util.collect;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentMultiMap<K, V> {
    private Map<K, Collection<V>> _MAP;
    private Map<K, Collection<V>> MAP() {
        Map<K, Collection<V>> result = _MAP;
        if (result == null) {
            synchronized (this) {
                result = _MAP;
                if (result == null) {
                    result = new ConcurrentHashMap<>();
                    _MAP = result;
                }
            }
        }
        return _MAP;
    }

    public int size() {
        return MAP().size();
    }


    public boolean isEmpty() {
        return MAP().isEmpty();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean containsKey(Object key) {
        if (key == null)
            return false;
        return MAP().containsKey(key);
    }

    public boolean put(K key, V value) {
        return safeGet(key).add(value);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean remove(Object key, Object value) {
        if (key == null || value == null)
            return false;
        return MAP().get(key).remove(value);
    }

    public boolean putAll(K key, Iterable<? extends V> iterable) {
        Collection<V> values = safeGet(key);
        for (V v : iterable) {
            if(!values.add(v)) {
                return false;
            }
        }
        return true;
    }

    public boolean putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            if(!safeGet(entry.getKey()).add(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        MAP().clear();
    }

    public Collection<V> safeGet(K key) {
        return MAP().computeIfAbsent(key, value -> new ConcurrentLinkedQueue<>());
    }

    public Collection<V> get(K key) {
        return MAP().get(key);
    }

    public Set<K> keySet() {
        return MAP().keySet();
    }

    public Multiset<K> keys() {
        Multiset<K> multiset = ConcurrentHashMultiset.create();
        multiset.addAll(MAP().keySet());
        return multiset;
    }

    public Collection<V> values() {
        Queue<V> values = new ConcurrentLinkedQueue<>();
        for (Map.Entry<K, Collection<V>> entry : MAP().entrySet()) {
            values.addAll(entry.getValue());
        }
        return values;
    }

    /*@SuppressWarnings("SuspiciousMethodCalls")
    public boolean containsValue(Object value) {
        if (value == null)
            return false;
        return storage.containsValue(value);
    }

    public boolean containsEntry(Object key, Object value) {
        if (key == null || value == null)
            return false;
        return storage.entrySet().contains(Maps.immutableEntry(key, value));
    }

    public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
        return null;
    }

    public Collection<V> removeAll(Object key) {
        if (key == null)
            return null;
        return storage.containsKey(key) ? storage.remove(key) : null;
    }

    public Collection<Map.Entry<K, V>> entries() {
        return null;
    }

    public Map<K, Collection<V>> asMap() {
        return null;
    }*/
}
