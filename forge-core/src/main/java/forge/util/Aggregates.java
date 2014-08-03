package forge.util;

import com.google.common.base.Function;

import java.util.*;
import java.util.Map.Entry;

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

        Random rnd = MyRandom.getRandom(); 
        if (source instanceof List<?>) {
            List<T> src = (List<T>)source;
            int len = src.size();
            switch(len) {
                case 0: return null;
                case 1: return src.get(0);
                default: return src.get(rnd.nextInt(len));
            }
        }
        
        T candidate = null;
        int lowest = Integer.MAX_VALUE;
        for (final T item : source) {
            int next = rnd.nextInt();
            if(next < lowest) {
                lowest = next;
                candidate = item;
            }
        }
        return candidate;
    }

    public static final <T> List<T> random(final Iterable<T> source, final int count) {
        final List<T> result = new ArrayList<T>();
        final int[] randoms = new int[count];
        for (int i = 0; i < count; i++) {
            randoms[i] = Integer.MAX_VALUE;
            result.add(null);
        }

        Random rnd = MyRandom.getRandom();
        for (T item : source) {
            int next = rnd.nextInt();
            for (int i = 0; i < count; i++) {
                if (next < randoms[i]) {
                    randoms[i] = next;
                    result.set(i, item);
                    break;
                }
            }
        }
        return result;
    }

    public static int randomInt(int min, int max) {
        Random rnd = MyRandom.getRandom();
        return rnd.nextInt(max - min + 1) + min;
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
        Map<U, Integer> result = new HashMap<U, Integer>();
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
