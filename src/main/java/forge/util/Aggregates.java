package forge.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import forge.util.closures.Lambda1;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Aggregates {

    // Returns the value matching predicate conditions with the maximum value of whatever valueAccessor returns. 
    public final static <T> Integer max(final Iterable<T> source, final Lambda1<Integer, T> valueAccessor) {
        if (source == null) { return null; }  
        int max = Integer.MIN_VALUE;
        for (final T c : source) {
            int value = valueAccessor.apply(c);
            if ( value > max ) {
                max = value;
            }
        }
        return max;
    }

    public final static <T> int sum(final Iterable<T> source, final Lambda1<Integer, T> valueAccessor) {
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
    public final static <T> T random(final Iterable<T> source) {
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
    /**
     * Random.
     * 
     * @param source
     *            the source
     * @param count
     *            the count
     * @return the list
     */
    public final static <T> List<T> random(final Iterable<T> source, final int count) {
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
    
    /**
     * Unique by last.
     * 
     * @param <K>
     *            the key type
     * @param <U>
     *            the generic type
     * @param source
     *            the source
     * @param fnUniqueKey
     *            the fn unique key
     * @param accessor
     *            the accessor
     * @return the iterable
     */
    public static final <K, U> Iterable<U> uniqueByLast(final Iterable<U> source, final Lambda1<K, U> fnUniqueKey) { // this might be exotic
        final Map<K, U> uniques = new Hashtable<K, U>();
        for (final U c : source) {
             uniques.put(fnUniqueKey.apply(c), c);
        }
        return uniques.values();
    }    

}
