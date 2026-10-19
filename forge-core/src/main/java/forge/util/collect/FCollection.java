package forge.util.collect;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Ordered collection with unique elements.
 *
 * <p>This implementation is intentionally optimized for Forge/Android workloads:
 * small and short-lived collections stay as a plain ArrayList, while larger collections
 * use a compact open-addressed object table instead of java.util.HashSet/HashMap.
 * The latter avoids one HashMap.Node allocation per element.</p>
 *
 * <p>Uses a Dual-Threshold strategy:
 * 1. LAZY_HASH_THRESHOLD (64): Lookup operations (contains/get) build the hashtable on demand.
 * 2. AUTO_HASH_THRESHOLD (128): Growth operations (add/addAll) build the table automatically
 *    to prevent O(N^2) linear lookup cost on large collections.</p>
 *
 * <p>{@link #shareCopy()} returns a cheap copy-on-write copy: it shares the backing list with its source
 * until either side is modified, then the modifying side copies the list first. Used for cached instances
 * (CardState caches) so every caller gets a normal mutable collection without paying for a full copy.</p>
 *
 * @param <T> element type
 */
public class FCollection<T> implements List<T>, FCollectionView<T>, Cloneable, Serializable {
    private static final long serialVersionUID = -1664555336364294106L;

    private static final FCollection<?> EMPTY = new EmptyFCollection<>();
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /** Querying contains()/get() builds table above this size. */
    private static final int LAZY_HASH_THRESHOLD = 64;

    /** Continuous add()/addAll() auto-builds table past this size to prevent O(N^2) slowdown. */
    private static final int AUTO_HASH_THRESHOLD = 128;

    /** Keep the compact table below 75% occupancy. */
    private static final int MIN_HASH_CAPACITY = 32;

    /** Used by sort() without allocating a Comparator on every invocation. */
    private static final Comparator<Object> TO_STRING_COMPARATOR = new Comparator<Object>() {
        @Override
        public int compare(final Object a, final Object b) {
            return a.toString().compareTo(b.toString());
        }
    };

    /** Ordered representation. */
    private ArrayList<T> list;

    /**
     * Compact membership table. Null means the collection is currently using
     * linear ArrayList scans. Non-null entries are stored directly in this
     * array using linear probing; no HashMap.Node objects are created.
     */
    private transient Object[] hashTable;

    private transient int hashSize;
    private transient int hashMask;

    /** Optional Set facade, allocated only if asSet() is actually requested. */
    private transient Set<T> setView;

    /** true = {@link #list} may also be referenced by other FCollections (copy-on-write). */
    private transient boolean sharedList;

    private FCollection(final ArrayList<T> sharedBacking, final boolean shared) {
        this.list = sharedBacking;
        this.sharedList = shared;
    }

    /**
     * Cheap copy-on-write copy: the returned (fully mutable) collection shares this one's backing list until
     * either of them is modified; the modifying side then copies the list first, so neither ever sees the
     * other's changes. Costs one small object instead of a full copy.
     */
    public final FCollection<T> shareCopy() {
        sharedList = true;
        return new FCollection<>(list, true);
    }

    /** Must be called before every structural change of {@link #list}. */
    private void ownList() {
        if (sharedList) {
            list = new ArrayList<>(list);
            sharedList = false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> FCollection<T> getEmpty() {
        return (FCollection<T>) EMPTY;
    }

    /**
     * Returns whether an iterable contains at least one item without creating
     * a temporary array or collection.
     */
    public static boolean hasElements(final Iterable<?> iterable) {
        if (iterable == null) {
            return false;
        }
        if (iterable instanceof Collection<?>) {
            return !((Collection<?>) iterable).isEmpty();
        }
        return iterable.iterator().hasNext();
    }

    /**
     * Check whether a {@link Collection} contains a particular element, silently
     * returning {@code false} when {@code null} is passed as the first argument.
     *
     * @param collection a collection.
     * @param element a possible element of the collection.
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
     * Creates a shallow copy of this collection (always mutable, even if this one is frozen).
     */
    @Override
    public final FCollection<T> clone() {
        return new FCollection<>(this);
    }

    /**
     * Get the first object in this {@link FCollection}.
     */
    public T getFirst() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Get the last object in this {@link FCollection}.
     */
    public T getLast() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * Get the number of elements in this collection.
     */
    @Override
    public int size() {
        return list.size();
    }

    /**
     * Check whether this collection is empty.
     */
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Returns a live Set facade over this collection.
     */
    public Set<T> asSet() {
        if (setView == null) {
            setView = new SetView();
        }
        return setView;
    }

    /**
     * Check whether this collection contains a particular object.
     *
     * @param o an object.
     */
    @Override
    public boolean contains(final Object o) {
        // Preserve the original FCollection contract: contains(null) is false.
        if (o == null) {
            return false;
        }

        // Lazy threshold: build hashtable on query if size > LAZY_HASH_THRESHOLD
        if (hashTable == null && list.size() > LAZY_HASH_THRESHOLD) {
            buildHashTable(list.size());
        }

        if (hashTable != null) {
            return findHashIndex(o) >= 0;
        }

        return list.contains(o);
    }

    /**
     * Returns the actual stored object equal to obj, or obj when not found,
     * matching the original implementation.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T get(final T obj) {
        if (obj == null) {
            return null;
        }

        // Lazy threshold: build hashtable on query if size > LAZY_HASH_THRESHOLD
        if (hashTable == null && list.size() > LAZY_HASH_THRESHOLD) {
            buildHashTable(list.size());
        }

        if (hashTable != null) {
            final int index = findHashIndex(obj);
            if (index >= 0) {
                return (T) hashTable[index];
            }
            return obj;
        }

        final int targetSize = list.size();
        for (int i = 0; i < targetSize; i++) {
            final T x = list.get(i);
            if (x != null && x.equals(obj)) {
                return x;
            }
        }
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        // ArrayList.iterator() allocates even when empty; for-each over empty collections is very common
        if (list.isEmpty()) {
            return Collections.emptyIterator();
        }
        if (sharedList) {
            return new SharedIterator(list);       // ArrayList's own iterator.remove() would modify the shared list
        }
        if (hashTable != null || list.size() > LAZY_HASH_THRESHOLD) {
            return new HashSyncIterator(list.iterator());   // iterator.remove() must also update the hash table
        }
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
    public <E> E[] toArray(final E[] a) {
        return list.toArray(a);
    }

    /**
     * Add an element to this collection, if it isn't already present.
     *
     * @param e the object to add.
     *
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean add(final T e) {
        if (e == null) {
            return false;
        }
        ownList();

        if (hashTable == null) {
            if (list.contains(e)) {
                return false;
            }

            list.add(e);

            // Auto-detect threshold: build table if size exceeds safety limit
            if (list.size() > AUTO_HASH_THRESHOLD) {
                buildHashTable(list.size());
            }

            return true;
        }

        if (!addToHashTable(e)) {
            return false;
        }

        list.add(e);
        return true;
    }

    /**
     * Remove an element from this collection.
     *
     * @param o the object to remove.
     *
     * @return whether the collection changed as a result of this method call.
     */
    @Override
    public boolean remove(final Object o) {
        if (o == null) {
            return false;
        }
        ownList();

        if (hashTable == null) {
            final int index = list.indexOf(o);
            if (index < 0) {
                return false;
            }
            list.remove(index);
            return true;
        }

        final int hashIndex = findHashIndex(o);
        if (hashIndex < 0) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final T stored = (T) hashTable[hashIndex];
        removeHashEntryAt(hashIndex);

        final int listIndex = list.indexOf(stored);
        if (listIndex >= 0) {
            list.remove(listIndex);
        }

        return true;
    }

    @Override
    public boolean removeIf(final Predicate<? super T> filter) {
        if (filter == null) {
            throw new NullPointerException("filter");
        }
        if (list.isEmpty()) {
            return false;
        }
        ownList();

        final int originalSize = list.size();
        int writeIndex = 0;
        boolean changed = false;

        // Compact in place to avoid Iterator allocations
        for (int readIndex = 0; readIndex < originalSize; readIndex++) {
            final T value = list.get(readIndex);
            if (filter.test(value)) {
                changed = true;
            } else {
                if (writeIndex != readIndex) {
                    list.set(writeIndex, value);
                }
                writeIndex++;
            }
        }

        if (!changed) {
            return false;
        }

        for (int i = originalSize - 1; i >= writeIndex; i--) {
            list.remove(i);
        }

        if (hashTable != null) {
            rebuildHashTable();
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        if (c == null) {
            throw new NullPointerException("collection");
        }
        if (c.isEmpty()) {
            return true;
        }
        if (c == this) {
            return true;
        }
        if (c instanceof FCollection<?> || (c instanceof RandomAccess && c instanceof List<?>)) {
            final List<?> src = (List<?>) c;
            for (int i = 0; i < src.size(); i++) {
                if (!contains(src.get(i))) {
                    return false;
                }
            }
            return true;
        }
        for (final Object value : c) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(final Collection<? extends T> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }
        ownList();

        final int expectedSize = list.size() + c.size();
        list.ensureCapacity(expectedSize);

        // Indexed access avoids allocating an Iterator (hot path: Game.getCardsIn -> addAll).
        if (c instanceof FCollection<?> || (c instanceof RandomAccess && c instanceof List<?>)) {
            final List<? extends T> src = (List<? extends T>) c;
            boolean changed = false;

            // Fast path: target is empty and untabled, and the source is an FCollection
            // (unique elements), so no per-element duplicate check is needed.
            if (c instanceof FCollection<?> && list.isEmpty() && hashTable == null) {
                for (int i = 0; i < src.size(); i++) {
                    final T element = src.get(i);
                    if (element != null) {
                        list.add(element);
                        changed = true;
                    }
                }
                /*if (list.size() > AUTO_HASH_THRESHOLD) {
                    buildHashTable(list.size());
                }*///would be waste. Needs bench.
                return changed;
            }

            if (hashTable == null && expectedSize > AUTO_HASH_THRESHOLD) {
                buildHashTable(expectedSize);
            }
            for (int i = 0; i < src.size(); i++) {
                final T element = src.get(i);
                if (element != null && addIfAbsent(element)) {
                    changed = true;
                }
            }
            return changed;
        }

        // Auto-detect threshold for bulk additions
        if (hashTable == null && expectedSize > AUTO_HASH_THRESHOLD) {
            buildHashTable(expectedSize);
        }

        boolean changed = false;
        for (final T element : c) {
            if (element != null && addIfAbsent(element)) {
                changed = true;
            }
        }

        return changed;
    }

    private boolean addIfAbsent(final T element) {
        if (hashTable != null) {
            if (addToHashTable(element)) {
                list.add(element);
                return true;
            }
            return false;
        }
        if (!list.contains(element)) {
            list.add(element);
            return true;
        }
        return false;
    }

    public boolean addAll(final Iterable<? extends T> i) {
        if (i == null) {
            return false;
        }

        if (i instanceof Collection<?>) {
            @SuppressWarnings("unchecked")
            final Collection<? extends T> collection = (Collection<? extends T>) i;
            return addAll(collection);
        }

        boolean changed = false;
        for (final T element : i) {
            if (add(element)) {
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Add all the elements in the specified array to this collection,
     * respecting the ordering.
     *
     * @param c an array.
     *
     * @return whether this collection changed as a result of this method call.
     */
    public boolean addAll(final T[] c) {
        if (c == null || c.length == 0) {
            return false;
        }
        ownList();

        final int expectedSize = list.size() + c.length;
        list.ensureCapacity(expectedSize);

        // Auto-detect threshold for bulk array additions
        if (hashTable == null && expectedSize > AUTO_HASH_THRESHOLD) {
            buildHashTable(expectedSize);
        }

        boolean changed = false;
        for (final T element : c) {
            if (element == null) {
                continue;
            }

            if (hashTable != null) {
                if (addToHashTable(element)) {
                    list.add(element);
                    changed = true;
                }
            } else if (!list.contains(element)) {
                list.add(element);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(final int index, final Collection<? extends T> c) {
        if (c == null || c.isEmpty()) {
            return false;
        }

        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
        }

        if (c instanceof List<?>) {
            @SuppressWarnings("unchecked")
            final List<? extends T> source = (List<? extends T>) c;
            boolean changed = false;

            for (int i = source.size() - 1; i >= 0; i--) {
                changed |= insert(index, source.get(i));
            }
            return changed;
        }

        final ArrayList<T> source = new ArrayList<>(c.size());
        for (final T element : c) {
            @SuppressWarnings("unchecked")
            final T value = (T) element;
            source.add(value);
        }

        boolean changed = false;
        for (int i = source.size() - 1; i >= 0; i--) {
            changed |= insert(index, source.get(i));
        }
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        if (c == null || c.isEmpty() || list.isEmpty()) {
            return false;
        }

        if (c == this) {
            clear();
            return true;
        }
        ownList();

        final int originalSize = list.size();
        int writeIndex = 0;
        boolean changed = false;

        for (int readIndex = 0; readIndex < originalSize; readIndex++) {
            final T value = list.get(readIndex);
            if (c.contains(value)) {
                changed = true;
            } else {
                if (writeIndex != readIndex) {
                    list.set(writeIndex, value);
                }
                writeIndex++;
            }
        }

        if (!changed) {
            return false;
        }

        for (int i = originalSize - 1; i >= writeIndex; i--) {
            list.remove(i);
        }

        if (hashTable != null) {
            rebuildHashTable();
        }

        return true;
    }

    public boolean removeAll(final Iterable<?> c) {
        if (c == null || list.isEmpty()) {
            return false;
        }

        if (c instanceof Collection<?>) {
            return removeAll((Collection<?>) c);
        }

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
        if (c == null) {
            throw new NullPointerException("collection");
        }

        if (c == this || list.isEmpty()) {
            return false;
        }
        ownList();

        final int originalSize = list.size();
        int writeIndex = 0;
        boolean changed = false;

        for (int readIndex = 0; readIndex < originalSize; readIndex++) {
            final T value = list.get(readIndex);
            if (c.contains(value)) {
                if (writeIndex != readIndex) {
                    list.set(writeIndex, value);
                }
                writeIndex++;
            } else {
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        for (int i = originalSize - 1; i >= writeIndex; i--) {
            list.remove(i);
        }

        if (hashTable != null) {
            rebuildHashTable();
        }

        return true;
    }

    @Override
    public void clear() {
        if (list.isEmpty()) {
            return;
        }

        if (sharedList) {
            list = new ArrayList<>();      // detach instead of copying what is about to be thrown away
            sharedList = false;
        } else {
            list.clear();
        }
        clearHashTable();
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
        ownList();
        return list.set(index, element);
    }

    /**
     * Replace the element at the specified position, updating both the
     * internal list and set. Unlike {@link #set}, this keeps set membership
     * in sync with the list.
     */
    public T replace(final int index, final T element) {
        if (element == null) {
            throw new NullPointerException("FCollection does not allow null elements");
        }
        ownList();

        final T old = list.set(index, element);
        if (old != element && hashTable != null) {
            removeFromHashTable(old);
            addToHashTable(element);
        }
        return old;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final int index, final T element) {
        insert(index, element);
    }

    private boolean insert(final int index, final T element) {
        if (element == null) {
            return false;
        }

        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + list.size());
        }
        ownList();

        final boolean exists;
        if (hashTable == null) {
            exists = list.contains(element);
        } else {
            exists = findHashIndex(element) >= 0;
        }

        if (!exists) {
            list.add(index, element);
            if (hashTable == null && list.size() > AUTO_HASH_THRESHOLD) {
                buildHashTable(list.size());
            } else if (hashTable != null) {
                addToHashTable(element);
            }
            return true;
        }

        final int oldIndex = list.indexOf(element);
        if (index == oldIndex) {
            return false;
        }

        int targetIndex = index;
        if (targetIndex > oldIndex) {
            targetIndex--;
        }

        final T old = list.remove(oldIndex);
        list.add(targetIndex, element);
        if (hashTable != null && old != element) {
            removeFromHashTable(old);
            addToHashTable(element);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T remove(final int index) {
        ownList();
        final T removedItem = list.remove(index);
        if (removedItem != null && hashTable != null) {
            removeFromHashTable(removedItem);
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
        ownList();                                  // rarely used: detach so set/add/remove are safe...
        clearHashTable();                           // ...and drop the table (rebuilt lazily) since the iterator bypasses it
        return list.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<T> listIterator(final int index) {
        ownList();
        clearHashTable();
        return list.listIterator(index);
    }

    @Override
    public Iterable<T> threadSafeIterable() {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return Collections.unmodifiableList(new ArrayList<>(list.subList(fromIndex, toIndex)));
    }

    public void sort() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Comparator<? super T> comparator = (Comparator) TO_STRING_COMPARATOR;
        sort(comparator);
    }

    @Override
    public void sort(final Comparator<? super T> comparator) {
        ownList();
        try {
            list.sort(comparator);
        } catch (final Exception e) {
            System.err.println("FCollection failed to sort: \n" + comparator + "\n" + e.getMessage());
        }
    }

    @Override
    public Stream<T> stream() {
        return list.stream();
    }

    @Override
    public boolean anyMatch(final Predicate<? super T> test) {
        for (int i = 0; i < list.size(); i++) {
            if (test.test(list.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allMatch(final Predicate<? super T> test) {
        for (int i = 0; i < list.size(); i++) {
            if (!test.test(list.get(i))) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Compact open-addressed membership table
    // -------------------------------------------------------------------------

    private static int spreadHash(final Object value) {
        final int h = value.hashCode();
        return h ^ (h >>> 16);
    }

    private static int nextPowerOfTwo(final int requested) {
        int capacity = MIN_HASH_CAPACITY;
        while (capacity < requested && capacity < (1 << 30)) {
            capacity <<= 1;
        }
        return capacity;
    }

    private void buildHashTable(final int expectedSize) {
        final int requested = Math.max(
                MIN_HASH_CAPACITY,
                expectedSize + (expectedSize >>> 1) + 1
        );

        final int capacity = nextPowerOfTwo(requested);
        final Object[] table = new Object[capacity];

        this.hashTable = table;
        this.hashMask = capacity - 1;
        this.hashSize = 0;

        for (int i = 0, size = list.size(); i < size; i++) {
            final T value = list.get(i);
            if (value != null) {
                insertHashNoResize(value, table);
            }
        }
    }

    private void rebuildHashTable() {
        if (hashTable == null) {
            return;
        }

        final Object[] table = hashTable;
        Arrays.fill(table, null);
        hashSize = 0;

        for (int i = 0, size = list.size(); i < size; i++) {
            final T value = list.get(i);
            if (value != null) {
                insertHashNoResize(value, table);
            }
        }
    }

    private void clearHashTable() {
        hashTable = null;
        hashSize = 0;
        hashMask = 0;
    }

    private boolean hashContains(final Object value) {
        return findHashIndex(value) >= 0;
    }

    private int findHashIndex(final Object value) {
        if (hashTable == null || value == null) {
            return -1;
        }

        final Object[] table = hashTable;
        final int mask = hashMask;
        int index = spreadHash(value) & mask;

        while (true) {
            final Object current = table[index];
            if (current == null) {
                return -1;
            }
            if (current == value || value.equals(current)) {
                return index;
            }
            index = (index + 1) & mask;
        }
    }

    private boolean addToHashTable(final T value) {
        if (value == null) {
            return false;
        }

        if (hashTable == null) {
            buildHashTable(Math.max(list.size() + 1, LAZY_HASH_THRESHOLD + 1));
        }

        if (hashContains(value)) {
            return false;
        }

        if (hashSize + 1 > hashTable.length - (hashTable.length >>> 2)) {
            resizeHashTable(hashTable.length << 1);
        }

        insertHashNoResize(value, hashTable);
        return true;
    }

    private void insertHashNoResize(final Object value, final Object[] table) {
        final int mask = table.length - 1;
        int index = spreadHash(value) & mask;

        while (table[index] != null) {
            index = (index + 1) & mask;
        }

        table[index] = value;
        hashSize++;
    }

    private void resizeHashTable(final int requestedCapacity) {
        final int newCapacity = nextPowerOfTwo(Math.max(MIN_HASH_CAPACITY, requestedCapacity));
        final Object[] oldTable = hashTable;
        final Object[] newTable = new Object[newCapacity];

        hashTable = newTable;
        hashMask = newCapacity - 1;
        hashSize = 0;

        if (oldTable != null) {
            for (int i = 0; i < oldTable.length; i++) {
                final Object value = oldTable[i];
                if (value != null) {
                    insertHashNoResize(value, newTable);
                }
            }
        }
    }

    private boolean removeFromHashTable(final Object value) {
        if (hashTable == null || value == null) {
            return false;
        }

        final int index = findHashIndex(value);
        if (index < 0) {
            return false;
        }

        removeHashEntryAt(index);
        return true;
    }

    private void removeHashEntryAt(final int removeIndex) {
        final Object[] table = hashTable;
        table[removeIndex] = null;
        hashSize--;

        int index = (removeIndex + 1) & hashMask;
        while (table[index] != null) {
            final Object value = table[index];
            table[index] = null;
            hashSize--;
            insertHashNoResize(value, table);
            index = (index + 1) & hashMask;
        }
    }

    // -------------------------------------------------------------------------
    // Set facade
    // -------------------------------------------------------------------------

    /** Wraps ArrayList's fail-fast iterator and keeps the membership table in sync on remove(). */
    private final class HashSyncIterator implements Iterator<T> {
        private final Iterator<T> delegate;
        private T lastReturned;

        HashSyncIterator(final Iterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public T next() {
            lastReturned = delegate.next();
            return lastReturned;
        }

        @Override
        public void remove() {
            delegate.remove();
            if (lastReturned != null && hashTable != null) {
                removeFromHashTable(lastReturned);
            }
            lastReturned = null;
        }
    }

    /** Iterator used while the backing list is shared: walks the shared snapshot, detaches on remove(). */
    private final class SharedIterator implements Iterator<T> {
        private final ArrayList<T> snapshot;
        private int cursor;
        private int lastReturned = -1;
        private int removed;

        SharedIterator(final ArrayList<T> snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.size();
        }

        @Override
        public T next() {
            if (cursor >= snapshot.size()) {
                throw new NoSuchElementException();
            }
            lastReturned = cursor;
            return snapshot.get(cursor++);
        }

        @Override
        public void remove() {
            if (lastReturned < 0) {
                throw new IllegalStateException();
            }
            ownList();
            final T gone = list.remove(lastReturned - removed);
            removed++;
            if (gone != null && hashTable != null) {
                removeFromHashTable(gone);
            }
            lastReturned = -1;
        }
    }

    private final class SetIterator implements Iterator<T> {
        private int cursor;
        private int lastReturned = -1;

        @Override
        public boolean hasNext() {
            return cursor < list.size();
        }

        @Override
        public T next() {
            if (cursor >= list.size()) {
                throw new NoSuchElementException();
            }
            lastReturned = cursor;
            return list.get(cursor++);
        }

        @Override
        public void remove() {
            if (lastReturned < 0) {
                throw new IllegalStateException();
            }
            ownList();

            final T removed = list.remove(lastReturned);
            if (removed != null && hashTable != null) {
                removeFromHashTable(removed);
            }

            cursor = lastReturned;
            lastReturned = -1;
        }
    }

    private final class SetView extends AbstractSet<T> {
        @Override
        public int size() {
            return FCollection.this.size();
        }

        @Override
        public boolean isEmpty() {
            return FCollection.this.isEmpty();
        }

        @Override
        public boolean contains(final Object o) {
            return FCollection.this.contains(o);
        }

        @Override
        public Iterator<T> iterator() {
            return new SetIterator();
        }

        @Override
        public boolean add(final T value) {
            return FCollection.this.add(value);
        }

        @Override
        public boolean remove(final Object o) {
            return FCollection.this.remove(o);
        }

        @Override
        public void clear() {
            FCollection.this.clear();
        }

        @Override
        public Object[] toArray() {
            return FCollection.this.toArray();
        }

        @Override
        public <E> E[] toArray(final E[] a) {
            return FCollection.this.toArray(a);
        }
    }

    // -------------------------------------------------------------------------
    // Empty FCollection
    // -------------------------------------------------------------------------

    public static class EmptyFCollection<T> extends FCollection<T> {
        private static final long serialVersionUID = 8667965158891635997L;

        public EmptyFCollection() {
            super();
        }

        @Override public final void add(final int index, final T element) { }
        @Override public final boolean add(final T e) { return false; }
        @Override public final boolean addAll(final Collection<? extends T> c) { return false; }
        @Override public final boolean addAll(final int index, final Collection<? extends T> c) { return false; }
        @Override public final boolean addAll(final Iterable<? extends T> i) { return false; }
        @Override public final boolean addAll(final T[] c) { return false; }
        @Override public final void clear() { }
        @Override public final boolean contains(final Object o) { return false; }
        @Override public final boolean containsAll(final Collection<?> c) { return c != null && c.isEmpty(); }
        @Override public final T get(final int index) {

            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final T getFirst() { throw new NoSuchElementException("Collection is empty"); }
        @Override public final T getLast() { throw new NoSuchElementException("Collection is empty"); }
        @Override public final int indexOf(final Object o) { return -1; }
        @Override public final boolean isEmpty() { return true; }
        @Override public final Iterator<T> iterator() { return Collections.emptyIterator(); }
        @Override public final int lastIndexOf(final Object o) { return -1; }
        @Override public final ListIterator<T> listIterator() { return Collections.emptyListIterator(); }
        @Override public final ListIterator<T> listIterator(final int index) { return Collections.emptyListIterator(); }
        @Override public final T remove(final int index) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final boolean remove(final Object o) { return false; }
        @Override public final boolean removeAll(final Collection<?> c) { return false; }
        @Override public final boolean removeAll(final Iterable<?> c) { return false; }
        @Override public final boolean retainAll(final Collection<?> c) { return false; }
        @Override public final T set(final int index, final T element) {
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final int size() { return 0; }
        @Override public final void sort() { }
        @Override public final void sort(final Comparator<? super T> comparator) { }
        @Override public final List<T> subList(final int fromIndex, final int toIndex) {
            if (fromIndex == 0 && toIndex == 0) {
                return this;
            }
            throw new IndexOutOfBoundsException("Any index is out of bounds for an empty collection");
        }
        @Override public final Iterable<T> threadSafeIterable() { return this; }
        @Override public final Object[] toArray() { return EMPTY_OBJECT_ARRAY; }
        @Override public final <E> E[] toArray(final E[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        @Override public final Stream<T> stream() { return Stream.empty(); }
        @Override public final boolean anyMatch(final Predicate<? super T> test) { return false; }
        @Override public final boolean allMatch(final Predicate<? super T> test) { return true; }
        @Override public final Set<T> asSet() { return Collections.emptySet(); }

        @Override
        public final String toString() {
            return "[]";
        }
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public FCollection() {
        this.list = new ArrayList<>();
    }

    public FCollection(final int initialCapacity) {
        this.list = new ArrayList<>(initialCapacity);
    }

    public FCollection(final T e) {
        this.list = new ArrayList<>(1);
        add(e);
    }

    public FCollection(final T[] c) {
        this.list = new ArrayList<>(c != null ? c.length : 10);
        addAll(c);
    }

    @SuppressWarnings("unchecked")
    public FCollection(final Iterable<? extends T> i) {
        if (i instanceof FCollection<?>) {
            final FCollection<T> source = (FCollection<T>) i;
            this.list = new ArrayList<>(source.list);
            return;
        }

        if (i instanceof Collection<?>) {
            final Collection<?> collection = (Collection<?>) i;
            this.list = new ArrayList<>(collection.size());
            addAll((Iterable<? extends T>) i);
            return;
        }

        this.list = new ArrayList<>();
        addAll(i);
    }

    public FCollection(final FCollectionReader<T> reader) {
        this.list = new ArrayList<>();
        reader.readAll(this);
    }
    /**
     * Ensure the internal backing list can hold at least minCapacity elements
     * without further resizing.
     */
    public void ensureCapacity(final int minCapacity) {
        list.ensureCapacity(minCapacity);
    }
}