package forge.util.maps;

import com.google.common.collect.Lists;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;


public final class MapToAmountUtil {

    private MapToAmountUtil() {
    }

    /**
     * Get an element with the highest amount among elements in a
     * {@link MapToAmount}.
     * 
     * @param map
     *            a {@link MapToAmount}.
     * @return a key of the provided map.
     */
    public static <T> Pair<T, Integer> max(final MapToAmount<T> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (map.isEmpty()) {
            throw new NoSuchElementException();
        }

        int max = Integer.MIN_VALUE;
        T maxElement = null;
        for (final Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue().intValue() > max) {
                max = entry.getValue().intValue();
                maxElement = entry.getKey();
            }
        }
        return Pair.of(maxElement, Integer.valueOf(max));
    }

    /**
     * Get all elements with the highest amount among elements in a
     * {@link MapToAmount}.
     * 
     * @param map
     *            a {@link MapToAmount}.
     * @return a subset of keys of the provided map. This set is not backed by
     *         the map, so any changes in the map are not reflected in the
     *         returned set.
     */
    public static <T> FCollectionView<T> maxAll(final MapToAmount<T> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (map.isEmpty()) {
            return new FCollection<T>();
        }

        final int max = Collections.max(map.values());
        final FCollection<T> set = new FCollection<T>();
        for (final Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue().intValue() == max) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    /**
     * Get an element with the lowest amount among elements in a
     * {@link MapToAmount}.
     * 
     * @param map
     *            a {@link MapToAmount}.
     * @return a key of the provided map.
     */
    public static <T> Pair<T, Integer> min(final MapToAmount<T> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (map.isEmpty()) {
            throw new NoSuchElementException();
        }

        int min = Integer.MAX_VALUE;
        T minElement = null;
        for (final Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue().intValue() < min) {
                min = entry.getValue().intValue();
                minElement = entry.getKey();
            }
        }
        return Pair.of(minElement, Integer.valueOf(min));
    }

    /**
     * Get all elements with the lowest amount among elements in a
     * {@link MapToAmount}.
     * 
     * @param map
     *            a {@link MapToAmount}.
     * @return a subset of keys of the provided map. This set is not backed by
     *         the map, so any changes in the map are not reflected in the
     *         returned set.
     */
    public static <T> FCollectionView<T> minAll(final MapToAmount<T> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        if (map.isEmpty()) {
            return new FCollection<T>();
        }

        final int min = Collections.min(map.values());
        final FCollection<T> set = new FCollection<T>();
        for (final Entry<T, Integer> entry : map.entrySet()) {
            if (entry.getValue().intValue() == min) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    public static <T> List<Pair<T, Integer>> sort(final MapToAmount<T> map) {
        final List<Pair<T, Integer>> entries = Lists.newArrayListWithCapacity(map.size());
        for (final Entry<T, Integer> entry : map.entrySet()) {
            entries.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        Collections.sort(entries, new Comparator<Entry<T, Integer>>() {
            @Override
            public int compare(final Entry<T, Integer> o1, final Entry<T, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        return entries;
    }

    private static final MapToAmount<?> EMPTY_MAP = new LinkedHashMapToAmount<>(0);

    @SuppressWarnings("unchecked")
    public static final <T> MapToAmount<T> emptyMap() {
        return (MapToAmount<T>) EMPTY_MAP;
    }
}
