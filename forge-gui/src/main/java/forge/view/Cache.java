package forge.view;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

public class Cache<K, V> {

    private final Map<K, V> cache;
    private final Map<V, K> inverseCache;
    public Cache() {
        this.cache = Maps.newHashMap();
        this.inverseCache = Maps.newHashMap();
    }

    public boolean containsKey(final K key) {
        return cache.containsKey(key);
    }

    /**
     * @param key
     * @return
     * @see java.util.Map#get(java.lang.Object)
     */
    public V get(final K key) {
        return cache.get(key);
    }

    public K getKey(final V value) {
        return inverseCache.get(value);
    }

    /**
     * Get a value as it is present in this Cache, compared using the equals
     * method.
     * 
     * @param value
     * @return a value equal to value, if such a value is present in the Cache.
     */
    public synchronized V getValue(final V value) {
        for (final V currentValue : inverseCache.keySet()) {
            if (currentValue.equals(value)) {
                return currentValue;
            }
        }
        return null;
    }

    /**
     * @param key
     * @param value
     * @return
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public void put(final K key, final V value) {
        if (key == null || value == null) {
            return;
        }

        synchronized (this) {
            if (inverseCache.containsKey(value)) {
                cache.remove(inverseCache.get(value));
                inverseCache.remove(value);
            }

            final V oldValue = cache.put(key, value);
            inverseCache.remove(oldValue);
            inverseCache.put(value, key);
        }
    }

    public synchronized void retainAllKeys(final Collection<K> keys) {
        cache.keySet().retainAll(keys);
        inverseCache.values().retainAll(keys);
    }
}
