package forge.util;

import java.util.Collection;

/**
 * Provides helper methods for Iterables similar to the Guava library,
 * but supporting Java 8's implementation of Iterators instead.
 */
public class Iterables {
    private Iterables(){}

    //TODO: Restore everything below
    public static <T> Iterable<T> unmodifiableIterable(final Iterable<? extends T> iterable) {
        return com.google.common.collect.Iterables.unmodifiableIterable(iterable);
    }

    public static int size(Iterable<?> iterable) {
        return com.google.common.collect.Iterables.size(iterable);
    }

    public static boolean contains(Iterable<?> iterable, Object element) {
        return com.google.common.collect.Iterables.contains(iterable, element);
    }

    public static <T> boolean addAll_withReturn(Collection<T> collection, Iterable<? extends T> toAdd) {
        return com.google.common.collect.Iterables.addAll(collection, toAdd);
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
        return com.google.common.collect.Iterables.concat(a, b);
    }
    public static <T> Iterable<T> concat(Iterable<? extends Iterable<? extends T>> inputs) {
        return com.google.common.collect.Iterables.concat(inputs);
    }
    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c) {
        return com.google.common.collect.Iterables.concat(a, b, c);
    }
    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c, Iterable<? extends T> d) {
        return com.google.common.collect.Iterables.concat(a, b, c, d);
    }

    public static int frequency(Iterable<?> iterable, Object element) {
        return com.google.common.collect.Iterables.frequency(iterable, element);
    }

    public static <T> T get(Iterable<T> iterable, int position) {
        return com.google.common.collect.Iterables.get(iterable, position);
    }
    
    public static <T> T getFirst(Iterable<? extends T> iterable, T defaultValue) {
        return com.google.common.collect.Iterables.getFirst(iterable, defaultValue);
    }

    public static <T> T getLast(Iterable<T> iterable) {
        return com.google.common.collect.Iterables.getLast(iterable);
    }
    public static <T> T getLast(Iterable<? extends T> iterable, T defaultValue) {
        return com.google.common.collect.Iterables.getLast(iterable, defaultValue);
    }

    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
        return com.google.common.collect.Iterables.limit(iterable, limitSize);
    }

    public static boolean isEmpty(Iterable<?> iterable) {
        return com.google.common.collect.Iterables.isEmpty(iterable);
    }
}
