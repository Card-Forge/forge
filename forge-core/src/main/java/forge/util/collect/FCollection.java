package forge.util.collect;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import forge.util.Lazy;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * Collection with unique elements ({@link Set}) that maintains the order in
 * which the elements are added to it ({@link List}).
 * <p>
 * This object is serializable if all elements it contains are.
 *
 * @param <T> the type of the elements this collection contains.
 * @see FCollectionView
 */
public class FCollection<T> implements List<T>, /*Set<T>,*/ FCollectionView<T>, Cloneable, Serializable {
    private static final long serialVersionUID = -1664555336364294106L;
    private final Object lock = new Object();

    private static final FCollection<?> EMPTY = new EmptyFCollection<>();

    @SuppressWarnings("unchecked")
    public static <T> FCollection<T> getEmpty() {
        return (FCollection<T>) EMPTY;
    }

    /**
     * The {@link Set} representation of this collection.
     */
    private final Lazy<Set<T>> set = Lazy.of(Sets::newHashSet);

    /**
     * The {@link List} representation of this collection.
     */
    private final Lazy<LinkedList<T>> list = Lazy.of(Lists::newLinkedList);

    /**
     * Create an empty {@link FCollection}.
     */
    public FCollection() {
    }

    /**
     * Create an {@link FCollection} containing a single element.
     *
     * @param e the single element the new collection contains.
     */
    public FCollection(final T e) {
        add(e);
    }

    /**
     * Create an {@link FCollection} from an array. The order of the elements in
     * the array is preserved in the new collection.
     *
     * @param c an array, whose elements will be in the collection upon its
     *          creation.
     */
    public FCollection(final T[] c) {
        this.addAll(Arrays.asList(c));
    }

    /**
     * Create an {@link FCollection} from an {@link Iterable}. The order of the
     * elements in the iterable is preserved in the new collection.
     *
     * @param i an iterable, whose elements will be in the collection upon its
     *          creation.
     */
    public FCollection(final Iterable<? extends T> i) {
        this.addAll(i);
    }

    /**
     * Create an {@link FCollection} from an {@link FCollectionReader}.
     *
     * @param reader a reader used to populate collection
     */
    public FCollection(final FCollectionReader<T> reader) {
        synchronized (lock) {
            reader.readAll(this);
        }
    }

    /**
     * Check whether an {@link Iterable} contains any iterable, silently
     * returning {@code false} when {@code null} is passed as an argument.
     *
     * @param iterable a card collection.
     */
    public static boolean hasElements(final Iterable<?> iterable) {
        return iterable != null && !Iterables.isEmpty(iterable);
    }

    /**
     * Check whether a {@link Collection} contains a particular element, silently
     * returning {@code false} when {@code null} is passed as the first argument.
     *
     * @param collection a collection.
     * @param element    a possible element of the collection.
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
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return list.get().hashCode();
    }

    /**
     * Return a string representation of this {@link FCollection}, by
     * concatenating the elements, in order, using a comma {@code ,}, and
     * wrapping it in brackets {@code [ ]}.
     */
    @Override
    public String toString() {
        return list.get().toString();
    }

    /**
     * Create a new {@link FCollection} containing the same objects as this
     * instance, in the same order. Note that objects are shallowly copied.
     */
    @Override
    public final FCollection<T> clone() {
        synchronized (lock) {
            return new FCollection<>(list.get());
        }
    }

    /**
     * Get the first object in this {@link FCollection}.
     *
     * @throws NoSuchElementException if the collection is empty.
     */
    @Override
    public T getFirst() {
        synchronized (lock) {
            if (list.get().isEmpty())
                return null;
            return list.get().getFirst();
        }
    }

    /**
     * Get the last object in this {@link FCollection}.
     *
     * @throws NoSuchElementException if the collection is empty.
     */
    @Override
    public T getLast() {
        synchronized (lock) {
            if (list.get().isEmpty())
                return null;
            return list.get().getLast();
        }
    }

    /**
     * Get the number of elements in this collection.
     */
    @Override
    public int size() {
        synchronized (lock) {
            return set.get().size();
        }
    }

    /**
     * Check whether this collection is empty.
     */
    @Override
    public boolean isEmpty() {
        return set.get().isEmpty();
    }

    public Set<T> asSet() {
        return set.get();
    }

    /**
     * Check whether this collection contains a particular object.
     *
     * @param o an object.
     */
    @Override
    public boolean contains(final Object o) {
        synchronized (lock) {
            if (o == null)
                return false;
            return set.get().contains(o);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return new Itr();
        //return list.get().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        synchronized (lock) {
            return list.get().toArray();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("hiding")
    public <T> T[] toArray(final T[] a) {
        synchronized (lock) {
            return list.get().toArray(a);
        }
    }

    /**
     * Add an element to this collection, if it isn't already present.
     *
     * @param e the object to add.
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean add(final T e) {
        synchronized (lock) {
            if (e == null)
                return false;
            if (set.get().add(e)) {
                list.get().add(e);
                return true;
            }
            return false;
        }
    }

    /**
     * Remove an element from this collection.
     *
     * @param o the object to remove.
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean remove(final Object o) {
        synchronized (lock) {
            if (o == null)
                return false;
            if (set.get().remove(o)) {
                list.get().remove(o);
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        synchronized (lock) {
            if (list.get().removeIf(filter)) {
                set.get().removeIf(filter);
                return true;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        synchronized (lock) {
            return set.get().containsAll(c);
        }
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
     * @param i an iterator.
     * @return whether this collection changed as a result of this method call.
     * @see #addAll(Collection)
     */
    public boolean addAll(final Iterable<? extends T> i) {
        synchronized (lock) {
            boolean changed = false;
            if (i == null)
                return false;
            for (final T e : i) {
                changed |= add(e);
            }
            return changed;
        }
    }

    /**
     * Add all the elements in the specified array to this collection,
     * respecting the ordering.
     *
     * @param c an array.
     * @return whether this collection changed as a result of this method call.
     */
    public boolean addAll(final T[] c) {
        synchronized (lock) {
            boolean changed = false;
            for (final T e : c) {
                changed |= add(e);
            }
            return changed;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        synchronized (lock) {
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
     * @param c an iterable.
     * @return whether this collection changed as a result of this method call.
     */
    public boolean removeAll(final Iterable<?> c) {
        synchronized (lock) {
            boolean changed = false;
            if (c == null)
                return false;
            for (final Object o : c) {
                changed |= remove(o);
            }
            return changed;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        synchronized (lock) {
            if (set.get().retainAll(c)) {
                list.get().retainAll(c);
                return true;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        synchronized (lock) {
            if (set.get().isEmpty()) {
                return;
            }
            set.get().clear();
            list.get().clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T get(final int index) {
        synchronized (lock) {
            return list.get().get(index);
        }
    }

    /**
     * Set the element at an index to a value. WARNING: this method doesn't
     * update the set and should only be used in a situation where the set of
     * elements in this collection is invariant.
     */
    @Override
    public T set(final int index, final T element) { //assume this isn't called except when changing list order, so don't worry about updating set
        synchronized (lock) {
            return list.get().set(index, element);
        }
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
     * @param index   the index to insert the element at.
     * @param element the element to insert.
     * @return whether this collection changed as a result of this method call.
     */
    private boolean insert(int index, final T element) {
        synchronized (lock) {
            if (set.get().add(element)) {
                list.get().add(index, element);
                return true;
            }
            //re-position in list if needed
            final int oldIndex = list.get().indexOf(element);
            if (index == oldIndex) {
                return false;
            }

            if (index > oldIndex) {
                index--; //account for being removed
            }
            list.get().remove(oldIndex);
            list.get().add(index, element);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T remove(final int index) {
        synchronized (lock) {
            final T removedItem = list.get().remove(index);
            if (removedItem != null) {
                set.get().remove(removedItem);
            }
            return removedItem;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(final Object o) {
        synchronized (lock) {
            return list.get().indexOf(o);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(final Object o) {
        synchronized (lock) {
            return list.get().lastIndexOf(o);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator() {
        return new ListItr(0);
        //return list.get().listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator(final int index) {
        return new ListItr(index);
        //return list.get().listIterator(index);
    }

    /**
     * <p>
     * <b>Note</b> This method breaks the contract of {@link List#subList(int, int)}
     * by returning a static collection, rather than a view, of the sublist.
     * </p>
     * <p>
     * {@inheritDoc}
     */
    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        synchronized (lock) {
            return ImmutableList.copyOf(list.get().subList(fromIndex, toIndex));
        }
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
        synchronized (lock) {
            try {
                list.get().sort(comparator);
            } catch (Exception e) {
                System.err.println("FCollection failed to sort: \n" + comparator + "\n" + e.getMessage());
            }
        }
    }

    @Override
    public T get(final T obj) {
        synchronized (lock) {
            if (obj == null) {
                return null;
            }
            for (T x : this) {
                if (x.equals(obj)) {
                    return x;
                }
            }
            return obj;
        }
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

        @Override
        public final void add(final int index, final T element) {
        }

        @Override
        public final boolean add(final T e) {
            return false;
        }

        @Override
        public final boolean addAll(final Collection<? extends T> c) {
            return false;
        }

        @Override
        public final boolean addAll(final int index, final Collection<? extends T> c) {
            return false;
        }

        @Override
        public final boolean addAll(final Iterable<? extends T> i) {
            return false;
        }

        @Override
        public final boolean addAll(final T[] c) {
            return false;
        }

        @Override
        public final void clear() {
        }

        @Override
        public final boolean contains(final Object o) {
            return false;
        }

        @Override
        public final boolean containsAll(final Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public final T get(final int index) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }

        @Override
        public final T getFirst() {
            throw new NoSuchElementException("Collection is empty");
        }

        @Override
        public final T getLast() {
            throw new NoSuchElementException("Collection is empty");
        }

        @Override
        public final int indexOf(final Object o) {
            return -1;
        }

        @Override
        public final boolean isEmpty() {
            return true;
        }

        @Override
        public final Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public final int lastIndexOf(final Object o) {
            return -1;
        }

        @Override
        public final ListIterator<T> listIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public final ListIterator<T> listIterator(final int index) {
            return Collections.emptyListIterator();
        }

        @Override
        public final T remove(final int index) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }

        @Override
        public final boolean remove(final Object o) {
            return false;
        }

        @Override
        public boolean removeAll(final Collection<?> c) {
            return false;
        }

        @Override
        public final boolean removeAll(final Iterable<?> c) {
            return false;
        }

        @Override
        public final boolean retainAll(final Collection<?> c) {
            return false;
        }

        @Override
        public final T set(final int index, final T element) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }

        @Override
        public final int size() {
            return 0;
        }

        @Override
        public final void sort() {
        }

        @Override
        public final void sort(final Comparator<? super T> comparator) {
        }

        @Override
        public final List<T> subList(final int fromIndex, final int toIndex) {
            if (fromIndex == 0 && toIndex == 0) {
                return this;
            }
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }

        @Override
        public final Object[] toArray() {
            return ArrayUtils.EMPTY_OBJECT_ARRAY;
        }

        @Override
        @SuppressWarnings("hiding")
        public final <T> T[] toArray(final T[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        @Override
        public final String toString() {
            return "[]";
        }
    }

    private class Itr implements Iterator<T> {
        protected int cursor;
        protected int lastRet;
        final FCollection l;

        public Itr() {
            cursor = 0;
            lastRet = -1;
            l = FCollection.this.clone();
        }


        @Override
        public boolean hasNext() {
            return cursor < l.size();
        }

        @Override
        public T next() {
            int i = cursor;
            if (i >= l.size()) {
                throw new NoSuchElementException();
            }
            cursor = i + 1;
            return (T) l.get(lastRet = i);
        }

        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }

            l.remove(lastRet);
            FCollection.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }

    public class ListItr extends Itr implements ListIterator<T> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        @Override
        public boolean hasPrevious() {
            return cursor > 0;
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public T previous() {
            int i = cursor - 1;
            if (i < 0) {
                throw new NoSuchElementException();
            }
            cursor = i;
            return (T) l.get(lastRet = i);
        }

        @Override
        public void set(T e) {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }

            l.set(lastRet, e);
            FCollection.this.set(lastRet, e);
        }

        @Override
        public void add(T e) {
            int i = cursor;
            l.add(i, e);
            FCollection.this.add(i, e);
            cursor = i + 1;
            lastRet = -1;
        }
    }
}
