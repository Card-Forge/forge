package forge.util.maps;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
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
            if (entry.getValue() > max) {
                max = entry.getValue();
                maxElement = entry.getKey();
            }
        }
        return Pair.of(maxElement, max);
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
            if (entry.getValue() < min) {
                min = entry.getValue();
                minElement = entry.getKey();
            }
        }
        return Pair.of(minElement, min);
    }

    public static <T> List<Pair<T, Integer>> sort(final Map<T, Integer> map) {
        final List<Pair<T, Integer>> entries = Lists.newArrayListWithCapacity(map.size());
        for (final Entry<T, Integer> entry : map.entrySet()) {
            entries.add(Pair.of(entry.getKey(), entry.getValue()));
        }
        entries.sort(Entry.comparingByValue());
        return entries;
    }

}
