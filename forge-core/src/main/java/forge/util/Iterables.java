package forge.util;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * Provides helper methods for Iterables similar to the Guava library,
 * but supporting Java 8's implementation of Iterators instead.
 */
public class Iterables {
    private Iterables(){}

    //TODO: Migrate everything below.

    public static <T> Iterable<T> filter(Iterable<T> iterable, Predicate<? super T> filter) {
        return () -> StreamSupport.stream(iterable.spliterator(), false).filter(filter).iterator();
    }
    public static <T> Iterable<T> filter(final Iterable<?> iterable, final Class<T> desiredType) {
        return () -> StreamSupport.stream(iterable.spliterator(), false)
                .filter(desiredType::isInstance)
                .map(desiredType::cast)
                .iterator();
    }

    public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> test) {
        return StreamSupport.stream(iterable.spliterator(), false).anyMatch(test);
    }

    public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> test) {
        return StreamSupport.stream(iterable.spliterator(), false).allMatch(test);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
        return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).findFirst().get();
    }
    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate, T defaultValue) {
        return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).findFirst().orElse(defaultValue);
    }
    public static <T> Optional<T> tryFind(Iterable<T> iterable, Predicate<? super T> predicate) {
        return StreamSupport.stream(iterable.spliterator(), false).filter(predicate).findFirst();
    }

    public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
        int index = 0;
        for(T i : iterable) {
            if(predicate.test(i))
                return index;
            index++;
        }
        return -1;
    }

    public static <F, T> Iterable<T> transform(final Iterable<F> iterable, final Function<? super F, T> function) {
        //TODO: Collection input variant. Some usages use Lists.newArrayList on output which could be made part of the stream.
        //Should probably also be ? extends T in the function type
        return () -> StreamSupport.stream(iterable.spliterator(), false).map(function).iterator();
    }

    //TODO: Inline everything below.

    public static <T> Iterable<T> filter(Collection<T> iterable, Predicate<? super T> filter) {
        return () -> iterable.stream().filter(filter).iterator();
    }
    public static <T> boolean any(Collection<T> iterable, Predicate<? super T> test) {
        return iterable.stream().anyMatch(test);
    }
    public static <T> boolean all(Collection<T> iterable, Predicate<? super T> test) {
        return iterable.stream().allMatch(test);
    }
    public static <T> T find(Collection<T> iterable, Predicate<? super T> predicate) {
        return iterable.stream().filter(predicate).findFirst().get();
    }
    public static <T> T find(Collection<T> iterable, Predicate<? super T> predicate, T defaultValue) {
        return iterable.stream().filter(predicate).findFirst().orElse(defaultValue);
    }

    public static <T> boolean removeIf(Iterable<T> iterable, Predicate<T> test) {
        //TODO: Convert parameter type
        return ((Collection<T>) iterable).removeIf(test);
    }

    public static boolean removeAll(Collection<?> removeFrom, Collection<?> toRemove) {
        return removeFrom.removeAll(toRemove);
    }

    public static int size(Collection<?> collection) {
        return collection.size();
    }

    public static boolean contains(Collection<?> collection, Object element) {
        return collection.contains(element);
    }

    public static <T> boolean addAll(Collection<T> collection, Collection<? extends T> toAdd) {
        return collection.addAll(toAdd);
    }

    public static <T> void addAll(Collection<T> collection, Iterable<? extends T> toAdd) {
        toAdd.forEach(collection::add);
    }

    public static <T> T getFirst(List<? extends T> iterable, T defaultValue) {
        return iterable.isEmpty() ? defaultValue : iterable.getFirst();
    }

    public static <T> T getLast(List<T> iterable) {
        return iterable.getLast();
    }
    public static <T> T getLast(List<? extends T> iterable, T defaultValue) {
        return iterable.isEmpty() ? defaultValue : iterable.getLast();
    }
    public static boolean isEmpty(Collection<?> iterable) {
        return iterable.isEmpty();
    }

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
