package net.slightlymagic.braids.util;

import java.util.Iterator;

/**
 * Acts as both immutable Iterator and Iterable; remove method always throws
 * exception.
 * 
 * @param <T>
 *            the generic type
 */
public class ImmutableIterableFrom<T> implements Iterable<T>, Iterator<T> {
    private Iterator<T> iterator;

    /**
     * Wrap an iterable so that it cannot be changed via the remove method.
     * 
     * @param iterable
     *            the iterable to wrap
     */
    public ImmutableIterableFrom(final Iterable<T> iterable) {
        this.iterator = iterable.iterator();
    }

    /**
     * Wrap an iterator so that its container cannot be changed via the remove
     * method.
     * 
     * @param iterator
     *            the iterator to wrap
     */
    public ImmutableIterableFrom(final Iterator<T> iterator) {
        this.iterator = iterator;
    }

    /**
     * This class acts as both an Iterable and an Iterator.
     * 
     * @return the iterator
     */
    public final Iterator<T> iterator() {
        return this;
    }

    /**
     * Returns hasNext from the wrapped [object's] iterator.
     * 
     * @return true, if successful
     */
    public final boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Returns next from the wrapped [object's] iterator.
     * 
     * @return the t
     */
    public final T next() {
        return iterator.next();
    }

    /**
     * Never succeeeds.
     * 
     */
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
