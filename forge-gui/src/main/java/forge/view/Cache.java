package forge.view;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Implementation of a two-way cache, containing bidirectional mappings from
 * keys of type {@code K} to values of type {@code V}.
 * 
 * Keys and values are checked for equality using the
 * {@link Object#equals(Object)} method.
 * 
 * @author elcnesh
 *
 * @param <K>
 *            the type of this Cache's keys.
 * @param <V>
 *            the type of this Cache's values.
 */
public class Cache<K, V> {

    private final Map<K, V> cache;
    private final Map<V, K> inverseCache;

    /**
     * Create a new, empty Cache instance.
     */
    public Cache() {
        this.cache = Maps.newHashMap();
        this.inverseCache = Maps.newHashMap();
    }

    /**
     * @param key
     *            a key.
     * @return {@code true} if and only if this Cache contains the specified
     *         key.
     */
    public boolean containsKey(final K key) {
        return cache.containsKey(key);
    }

    /**
     * Get the value associated to a key.
     * 
     * @param key
     *            a key.
     * @return the value associated to key, or {@code null} if no such value
     *         exists.
     */
    public V get(final K key) {
        return cache.get(key);
    }

    /**
     * Get the key associated to a value.
     * 
     * @param value
     *            a value.
     * @return the key associated to value, or {@code null} if no such key
     *         exists.
     */
    public K getKey(final V value) {
        return inverseCache.get(value);
    }

    /**
     * Get a value as it is present in this Cache, compared using the equals
     * method.
     * 
     * @param value
     * @return a value equal to value, if such a value is present in the Cache;
     *         {@code null} otherwise.
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
     * Add a mapping to this Cache.
     * 
     * If either argument is {@code null}, this method silently returns.
     * Otherwise, any existing mapping from the provided key to the provided
     * value is removed, as well as its inverse. After that, the provided
     * mapping and its inverse are added to this Cache.
     * 
     * @param key
     *            the key of the new mapping.
     * @param value
     *            the value to map to.
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

    /**
     * Clear all the mappings in this Cache, except for any key that exists in
     * the argument.
     * 
     * @param keys
     *            the keys to retain.
     */
    public synchronized void retainAllKeys(final Collection<K> keys) {
        cache.keySet().retainAll(keys);
        inverseCache.values().retainAll(keys);
    }
}
