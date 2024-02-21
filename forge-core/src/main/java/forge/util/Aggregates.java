package forge.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

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

    public static final <T> List<T> listWithMin(final Iterable<T> source, final Function<T, Integer> valueAccessor) {
        if (source == null) { return null; }
        int min = Integer.MAX_VALUE;
        List<T> result = Lists.newArrayList();
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if (value == min) {
                result.add(c);
            }
            if (value < min) {
                min = value;
                result.clear();
                result.add(c);
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

    public static final <T> T random(final T[] source) {
        if (source == null) { return null; }

        switch (source.length) {
            case 0: return null;
            case 1: return source[0];
            default: return source[MyRandom.getRandom().nextInt(source.length)];
        }
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
        if (source == null) { return null; }

        if (source instanceof List<?>) {
            List<T> src = (List<T>)source;
            int len = src.size();
            switch(len) {
                case 0: return null;
                case 1: return src.get(0);
                default: return src.get(MyRandom.getRandom().nextInt(len));
            }
        }

        T candidate = null;
        int lowest = Integer.MAX_VALUE;
        for (final T item : source) {
            int next = MyRandom.getRandom().nextInt();
            if(next < lowest) {
                lowest = next;
                candidate = item;
            }
        }
        return candidate;
    }

    public static final <T> List<T> random(final Iterable<T> source, final int count) {
        return random(source, count, new ArrayList<>());
    }
    public static final <T, L extends List<T>> L random(final Iterable<T> source, final int count, final L list) {
        // Using Reservoir Sampling to grab X random values from source
        int i = 0;
        for (T item : source) {
            i++;
            if (i <= count) {
                // Add the first count items into the result list
                list.add(item);
            } else {
                // Progressively reduce odds of item > count to get added into the reservoir
                int j = MyRandom.getRandom().nextInt(i);
                if (j < count) {
                    list.set(j, item);
                }
            }
        }
        return list;
    }

    public static final <T> T removeRandom(final List<T> source) {
        if (source == null || source.isEmpty()) { return null; }

        int index;
        if (source.size() > 1) {
            index = MyRandom.getRandom().nextInt(source.size());
        }
        else {
            index = 0;
        }
        return source.remove(index);
    }

    public static int randomInt(int min, int max) {
        return MyRandom.getRandom().nextInt(max - min + 1) + min;
    }

    public static final <K, U> Iterable<U> uniqueByLast(final Iterable<U> source, final Function<U, K> fnUniqueKey) { // this might be exotic
        final Map<K, U> uniques = new Hashtable<>();
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
        }
        else {
            for (final TItem c : source) {
                if (valueEquals.equals(valueAccessor.apply(c))) {
                    return c;
                }
            }
        }
        return null;
    }

    public static <T, U> Iterable<Entry<U, Integer>> groupSumBy(Iterable<Entry<T, Integer>> source, Function<T, U> fnGetField) {
        Map<U, Integer> result = new HashMap<>();
        for (Entry<T, Integer> kv : source) {
            U k = fnGetField.apply(kv.getKey());
            Integer v = kv.getValue();
            Integer sum = result.get(k);
            int n = v == null ? 0 : v.intValue();
            int s = sum == null ? 0 : sum.intValue();
            result.put(k, Integer.valueOf(s + n)); 
        }
        return result.entrySet();
    }
}
