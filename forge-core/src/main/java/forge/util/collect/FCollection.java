package forge.util.collect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Collection with unique elements ({@link Set}) that maintains the order in
 * which the elements are added to it ({@link List}).
 *
 * This object is serializable if all elements it contains are.
 *
 * @param <T> the type of the elements this collection contains.
 * @see FCollectionView
 */
public class FCollection<T> implements List<T>, Set<T>, FCollectionView<T>, Cloneable, Serializable {
    private static final long serialVersionUID = -1664555336364294106L;

    private static final FCollection<?> EMPTY = new EmptyFCollection<Object>();

    @SuppressWarnings("unchecked")
    public static <T> FCollection<T> getEmpty() {
        return (FCollection<T>) EMPTY;
    }

    /**
     * The {@link Set} representation of this collection.
     */
    private final Set<T> set = Sets.newHashSet();

    /**
     * The {@link List} representation of this collection.
     */
    private final LinkedList<T> list = Lists.newLinkedList();

    /**
     * Create an empty {@link FCollection}.
     */
    public FCollection() {
    }

    /**
     * Create an {@link FCollection} containing a single element.
     *
     * @param e
     *            the single element the new collection contains.
     */
    public FCollection(final T e) {
        add(e);
    }

    /**
     * Create an {@link FCollection} from an array. The order of the elements in
     * the array is preserved in the new collection.
     *
     * @param c
     *            an array, whose elements will be in the collection upon its
     *            creation.
     */
    public FCollection(final T[] c) {
        for (final T e : c) {
            add(e);
        }
    }

    /**
     * Create an {@link FCollection} from an {@link Iterable}. The order of the
     * elements in the iterable is preserved in the new collection.
     *
     * @param i
     *            an iterable, whose elements will be in the collection upon its
     *            creation.
     */
    public FCollection(final Iterable<? extends T> i) {
        for (final T e : i) {
            add(e);
        }
    }

    /**
     * Create an {@link FCollection} from an {@link FCollectionReader}.
     *
     * @param reader
     *            a reader used to populate collection
     */
    public FCollection(final FCollectionReader<T> reader) {
        reader.readAll(this);
    }

    /**
     * Check whether an {@link Iterable} contains any iterable, silently
     * returning {@code false} when {@code null} is passed as an argument.
     *
     * @param iterable
     *            a card collection.
     */
    public static boolean hasElements(final Iterable<?> iterable) {
        return iterable != null && !Iterables.isEmpty(iterable);
    }

    /**
     * Check whether a {@link Collection} contains a particular element, silently
     * returning {@code false} when {@code null} is passed as the first argument.
     *
     * @param collection
     *            a collection.
     * @param element
     *            a possible element of the collection.
     */
    public static <T> boolean hasElement(final Collection<T> collection, final T element) {
        return collection != null && collection.contains(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof FCollection && hashCode() == obj.hashCode();
    }

    /**
     * <p>This implementation uses the hash code of the backing list.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return list.hashCode();
    }

    /**
     * Return a string representation of this {@link FCollection}, by
     * concatenating the elements, in order, using a comma {@code ,}, and
     * wrapping it in brackets {@code [ ]}.
     */
    @Override
    public String toString() {
        return list.toString();
    }

    /**
     * Create a new {@link FCollection} containing the same objects as this
     * instance, in the same order. Note that objects are shallowly copied.
     */
    @Override
    public final FCollection<T> clone() {
        return new FCollection<>(list);
    }

    /**
     * Get the first object in this {@link FCollection}.
     *
     * @throws NoSuchElementException
     *             if the collection is empty.
     */
    @Override
    public T getFirst() {
        return list.getFirst();
    }

    /**
     * Get the last object in this {@link FCollection}.
     *
     * @throws NoSuchElementException
     *             if the collection is empty.
     */
    @Override
    public T getLast() {
        return list.getLast();
    }

    /**
     * Get the number of elements in this collection.
     */
    @Override
    public int size() {
        return set.size();
    }

    /**
     * Check whether this collection is empty.
     */
    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * Check whether this collection contains a particular object.
     *
     * @param o
     *            an object.
     */
    @Override
    public boolean contains(final Object o) {
        return set.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("hiding")
    public <T> T[] toArray(final T[] a) {
        return list.toArray(a);
    }

    /**
     * Add an element to this collection, if it isn't already present.
     *
     * @param e
     *            the object to add.
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean add(final T e) {
        if (set.add(e)) {
            list.add(e);
            return true;
        }
        return false;
    }

    /**
     * Remove an element from this collection.
     *
     * @param o
     *            the object to remove.
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean remove(final Object o) {
        if (set.remove(o)) {
            list.remove(o);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        return set.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(final Collection<? extends T> c) {
        return addAll((Iterable<? extends T>) c);
    }

    /**
     * Add all the elements in the specified {@link Iterator} to this
     * collection, in the order in which they appear.
     *
     * @param i
     *            an iterator.
     * @return whether this collection changed as a result of this method call.
     * @see #addAll(Collection)
     */
    public boolean addAll(final Iterable<? extends T> i) {
        boolean changed = false;
        for (final T e : i) {
            changed |= add(e);
        }
        return changed;
    }

    /**
     * Add all the elements in the specified array to this collection,
     * respecting the ordering.
     *
     * @param c
     *            an array.
     * @return whether this collection changed as a result of this method call.
     */
    public boolean addAll(final T[] c) {
        boolean changed = false;
        for (final T e : c) {
            changed |= add(e);
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        if (c == null) {
            return false;
        }

        final List<? extends T> list;
        if (c instanceof List) {
            list = (List<T>) c;
        } else {
            list = Lists.newArrayList(c);
        }

        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) { //must add in reverse order so they show up in the right place
            changed |= insert(index, list.get(i));
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        return removeAll((Iterable<?>) c);
    }

    /**
     * Remove all objects appearing in an {@link Iterable}.
     *
     * @param c
     *            an iterable.
     * @return whether this collection changed as a result of this method call.
     */
    public boolean removeAll(final Iterable<?> c) {
        boolean changed = false;
        for (final Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (set.retainAll(c)) {
            list.retainAll(c);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        if (set.isEmpty()) { return; }
        set.clear();
        list.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(final int index) {
        return list.get(index);
    }

    /**
     * Set the element at an index to a value. WARNING: this method doesn't
     * update the set and should only be used in a situation where the set of
     * elements in this collection is invariant.
     */
    @Override
    public T set(final int index, final T element) { //assume this isn't called except when changing list order, so don't worry about updating set
        return list.set(index, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final int index, final T element) {
        insert(index, element);
    }

    /**
     * Helper method to insert an element at a particular index.
     *
     * @param index
     *            the index to insert the element at.
     * @param element
     *            the element to insert.
     * @return whether this collection changed as a result of this method call.
     */
    private boolean insert(int index, final T element) {
        if (set.add(element)) {
            list.add(index, element);
            return true;
        }
        //re-position in list if needed
        final int oldIndex = list.indexOf(element);
        if (index == oldIndex) {
            return false;
        }

        if (index > oldIndex) {
            index--; //account for being removed
        }
        list.remove(oldIndex);
        list.add(index, element);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T remove(final int index) {
        final T removedItem = list.remove(index);
        if (removedItem != null) {
            set.remove(removedItem);
        }
        return removedItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(final Object o) {
        return list.indexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(final Object o) {
        return list.lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator(final int index) {
        return list.listIterator(index);
    }

    /**
     * <p>
     * <b>Note</b> This method breaks the contract of {@link List#subList(int, int)}
     * by returning a static collection, rather than a view, of the sublist.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return ImmutableList.copyOf(list.subList(fromIndex, toIndex));
    }

    /**
     * Sort this collection on the string representations of the resepctive
     * elements.
     *
     * @see Object#toString()
     * @see #sort(Comparator)
     * @see Ordering#usingToString()
     */
    public void sort() {
        sort(Ordering.usingToString());
    }

    /**
     * {@inheritDoc}
     */
    public void sort(final Comparator<? super T> comparator) {
        Collections.sort(list, comparator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<T> threadSafeIterable() {
        //create a new linked list for iterating to make it thread safe and avoid concurrent modification exceptions
        return Iterables.unmodifiableIterable(new LinkedList<T>(list));
    }

    /**
     * An unmodifiable, empty {@link FCollection}. Overrides all methods with
     * default implementations suitable for an empty collection, to improve
     * performance.
     */
    public static class EmptyFCollection<T> extends FCollection<T> {
        private static final long serialVersionUID = 8667965158891635997L;
        public EmptyFCollection() {
            super();
        }
        @Override public final void add(final int index, final T element) {
        }
        @Override public final boolean add(final T e) {
            return false;
        }
        @Override public final boolean addAll(final Collection<? extends T> c) {
            return false;
        }
        @Override public final boolean addAll(final int index, final Collection<? extends T> c) {
            return false;
        }
        @Override public final boolean addAll(final Iterable<? extends T> i) {
            return false;
        }
        @Override public final boolean addAll(final T[] c) {
            return false;
        }
        @Override public final void clear() {
        }
        @Override public final boolean contains(final Object o) {
            return false;
        }
        @Override public final boolean containsAll(final Collection<?> c) {
            return c.isEmpty();
        }
        @Override public final T get(final int index) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final T getFirst() {
            throw new NoSuchElementException("Collection is empty");
        }
        @Override public final T getLast() {
            throw new NoSuchElementException("Collection is empty");
        }
        @Override public final int indexOf(final Object o) {
            return -1;
        }
        @Override public final boolean isEmpty() {
            return true;
        }
        @Override public final Iterator<T> iterator() {
            return Collections.emptyIterator();
        }
        @Override public final int lastIndexOf(final Object o) {
            return -1;
        }
        @Override public final ListIterator<T> listIterator() {
            return Collections.emptyListIterator();
        }
        @Override public final ListIterator<T> listIterator(final int index) {
            return Collections.emptyListIterator();
        }
        @Override public final T remove(final int index) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final boolean remove(final Object o) {
            return false;
        }
        @Override public boolean removeAll(final Collection<?> c) {
            return false;
        }
        @Override public final boolean removeAll(final Iterable<?> c) {
            return false;
        }
        @Override public final boolean retainAll(final Collection<?> c) {
            return false;
        }
        @Override public final T set(final int index, final T element) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final int size() {
            return 0;
        }
        @Override public final void sort() {
        }
        @Override public final void sort(final Comparator<? super T> comparator) {
        }
        @Override public final List<T> subList(final int fromIndex, final int toIndex) {
            if (fromIndex == 0 && toIndex == 0) {
                return this;
            }
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final Iterable<T> threadSafeIterable() {
            return this;
        }
        @Override public final Object[] toArray() { return ArrayUtils.EMPTY_OBJECT_ARRAY; }
        @Override
        @SuppressWarnings("hiding")
        public final <T> T[] toArray(final T[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        @Override public final String toString() {
            return "[]";
        }
    }
}
