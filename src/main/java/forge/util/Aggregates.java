package forge.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Aggregates {

    // Returns the value matching predicate conditions with the maximum value of whatever valueAccessor returns.
    public static final <T> Integer max(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        if (source == null) { return null; }
        int max = Integer.MIN_VALUE;
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static final <T> Integer min(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        if (source == null) { return null; }
        int max = Integer.MAX_VALUE;
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if (value < max) {
                max = value;
            }
        }
        return max;
    }


    public static final <T> T itemWithMax(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        if (source == null) { return null; }
        int max = Integer.MIN_VALUE;
        T result = null;
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if (value > max) {
                max = value;
                result = c;
            }
        }
        return result;
    }

    public static final <T> int sum(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        int result = 0;
        if (source != null) {
            for (final T c : source) {
                result += valueAccessor.apply(c);
            }
        }
        return result;
    }


    // Random - algorithm adapted from Braid's GeneratorFunctions
    /**
     * Random.
     * 
     * @param source
     *            the source
     * @return the t
     */
    public static final <T> T random(final Iterable<T> source) {
        int n = 0;
        T candidate = null;
        for (final T item : source) {
            if ((Math.random() * ++n) < 1) {
                candidate = item;
            }
        }
        return candidate;
    }

    // Get several random values
    // should improve to make 1 pass over source and track N candidates at once
    public static final <T> List<T> random(final Iterable<T> source, final int count) {
        final List<T> result = new ArrayList<T>();
        for (int i = 0; i < count; ++i) {
            final T toAdd = Aggregates.random(source);
            if (toAdd == null) {
                break;
            }
            result.add(toAdd);
        }
        return result;
    }

    public static final <K, U> Iterable<U> uniqueByLast(final Iterable<U> source, final Function<U, K> fnUniqueKey) { // this might be exotic
        final Map<K, U> uniques = new Hashtable<K, U>();
        for (final U c : source) {
             uniques.put(fnUniqueKey.apply(c), c);
        }
        return uniques.values();
    }


    public static <T> T itemWithMin(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        if (source == null) { return null; }
        int max = Integer.MAX_VALUE;
        T result = null;
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if (value < max) {
                max = value;
                result = c;
            }
        }
        return result;
    }


    public static <TItem, TField> TItem firstFieldEquals(List<TItem> source, Function<TItem, TField> valueAccessor, TField valueEquals) {
        if (source == null) { return null; }
        if (valueEquals == null) {
            for (final TItem c : source) {
                if (null == valueAccessor.apply(c)) {
                    return c;
                }
            }
        } else {
            for (final TItem c : source) {
                if (valueEquals.equals(valueAccessor.apply(c))) {
                    return c;
                }
            }
        }
        return null;
    }

}
