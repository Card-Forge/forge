package forge.util.collect;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.TreeBasedTable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Snapshot Table: lock-free reads and iteration that never throw ConcurrentModificationException, synchronized
 * writes. Every write publishes a new immutable snapshot, so a reader (or an iterator it holds) always sees one
 * consistent state, exactly like a copy-on-write collection.
 *
 * <p><b>Tree tables</b> ({@link #createTree()}): same ordering as Guava's TreeBasedTable (rows ascending, columns
 * ascending inside a row). A snapshot is ONE flat array of (row, column, value) triples sorted by (row, column).
 * A write is a binary search plus a single array copy (no Guava table is copied, no per-row TreeMap is allocated),
 * a lookup is a binary search, and values()/cellSet()/row()/rowKeySet() read the array directly. That keeps the
 * hot pattern of continuous effects - "put one entry, read all values, repeat" for every lord on every creature -
 * linear instead of quadratic with a Guava copy per step. Guava-shaped views that are rarely needed
 * (rowMap(), columnMap(), toString()) are built lazily from the array, once per snapshot.</p>
 *
 * <p><b>Hash tables</b> ({@link #createHash()}): backed by a Guava HashBasedTable that is copied on write, as
 * before. They are small and written rarely, and their iteration order (hash order) is kept unchanged.</p>
 *
 * <p>values() is cached per snapshot (a shared empty list for an empty table). Views returned by readers are
 * read-only snapshots; mutate only through put/remove/putAll/clear or the removal methods of
 * rowKeySet()/columnKeySet() (remove, removeAll, retainAll, removeIf, clear).</p>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class FSnapshotTable<R, C, V> implements Table<R, C, V>, Serializable {
    private static final long serialVersionUID = 2L;

    // ======================================================================================================
    // snapshots
    // ======================================================================================================

    /** One immutable state of the table. Never modified after it was published; lazily cached views are benign races. */
    private abstract static class Snap<R, C, V> {
        private volatile List<R> rowKeys;
        private volatile List<C> columnKeys;

        abstract int size();
        abstract V get(Object r, Object c);
        abstract boolean contains(Object r, Object c);
        abstract boolean containsRow(Object r);
        abstract boolean containsColumn(Object c);
        abstract boolean containsValue(Object v);
        abstract Collection<V> values();
        abstract Set<Cell<R, C, V>> cellSet();
        abstract Map<C, V> row(R r);
        abstract Map<R, V> column(C c);
        /** The same content as a (never modified) Guava table; built lazily for the tree variant. */
        abstract Table<R, C, V> table();
        abstract List<R> computeRowKeys();
        abstract List<C> computeColumnKeys();

        final List<R> rowKeys() {
            List<R> k = rowKeys;
            if (k == null) {
                k = Collections.unmodifiableList(computeRowKeys());
                rowKeys = k;
            }
            return k;
        }

        final List<C> columnKeys() {
            List<C> k = columnKeys;
            if (k == null) {
                k = Collections.unmodifiableList(computeColumnKeys());
                columnKeys = k;
            }
            return k;
        }
    }

    /** Hash variant: an immutable-by-convention Guava HashBasedTable, copied on every write. */
    private static final class HashSnap<R, C, V> extends Snap<R, C, V> {
        private final Table<R, C, V> table;
        private volatile Collection<V> values;

        HashSnap(Table<R, C, V> table) {
            this.table = table;
        }

        @Override int size() { return table.size(); }
        @Override V get(Object r, Object c) { return table.get(r, c); }
        @Override boolean contains(Object r, Object c) { return table.contains(r, c); }
        @Override boolean containsRow(Object r) { return table.containsRow(r); }
        @Override boolean containsColumn(Object c) { return table.containsColumn(c); }
        @Override boolean containsValue(Object v) { return table.containsValue(v); }
        @Override Table<R, C, V> table() { return table; }
        @Override List<R> computeRowKeys() { return new ArrayList<>(table.rowKeySet()); }
        @Override List<C> computeColumnKeys() { return new ArrayList<>(table.columnKeySet()); }
        @Override Map<C, V> row(R r) { return Collections.unmodifiableMap(table.row(r)); }
        @Override Map<R, V> column(C c) { return Collections.unmodifiableMap(table.column(c)); }

        @Override
        Collection<V> values() {
            Collection<V> v = values;
            if (v == null) {
                v = table.isEmpty() ? Collections.<V>emptyList() : Collections.unmodifiableList(new ArrayList<>(table.values()));
                values = v;
            }
            return v;
        }

        @Override
        Set<Cell<R, C, V>> cellSet() {
            return table.isEmpty() ? Collections.<Cell<R, C, V>>emptySet() : Collections.unmodifiableSet(table.cellSet());
        }
    }

    /** Tree variant: sorted flat array of (row, column, value) triples. */
    private static final class SortedSnap<R, C, V> extends Snap<R, C, V> {
        static final Object[] NO_DATA = new Object[0];

        /** row0, column0, value0, row1, column1, value1, ... sorted by (row, column); exactly 3 * n elements. */
        final Object[] d;
        final int n;
        private volatile Table<R, C, V> table;
        private volatile Collection<V> values;

        SortedSnap(Object[] d, int n) {
            this.d = d;
            this.n = n;
        }

        static int cmp(Object a, Object b) {
            return ((Comparable<Object>) a).compareTo(b);
        }

        /** Index of the entry (r, c), or -(insertionPoint + 1). Keys must be comparable with the stored ones. */
        static int search(Object[] d, int n, Object r, Object c) {
            int lo = 0, hi = n - 1;
            while (lo <= hi) {
                final int mid = (lo + hi) >>> 1;
                final int k = 3 * mid;
                int x = cmp(d[k], r);
                if (x == 0) {
                    x = cmp(d[k + 1], c);
                }
                if (x < 0) {
                    lo = mid + 1;
                } else if (x > 0) {
                    hi = mid - 1;
                } else {
                    return mid;
                }
            }
            return -(lo + 1);
        }

        /** Lookup with arbitrary keys: a key of the wrong type is simply not found, like in Guava. */
        int find(Object r, Object c) {
            if (n == 0 || r == null || c == null) {
                return -1;
            }
            try {
                return search(d, n, r, c);
            } catch (ClassCastException e) {
                return -1;
            }
        }

        /** First index whose row is >= r (n if there is none); may throw ClassCastException. */
        private int rowLow(Object r) {
            int lo = 0, hi = n;
            while (lo < hi) {
                final int mid = (lo + hi) >>> 1;
                if (cmp(d[3 * mid], r) < 0) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            return lo;
        }

        /** First index whose row is > r; may throw ClassCastException. */
        private int rowHigh(Object r) {
            int lo = 0, hi = n;
            while (lo < hi) {
                final int mid = (lo + hi) >>> 1;
                if (cmp(d[3 * mid], r) <= 0) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            return lo;
        }

        SortedSnap<R, C, V> withValue(int index, Object value) {
            final Object[] nd = d.clone();
            nd[3 * index + 2] = value;
            return new SortedSnap<>(nd, n);
        }

        SortedSnap<R, C, V> inserted(int index, Object r, Object c, Object v) {
            final Object[] nd = new Object[3 * (n + 1)];
            System.arraycopy(d, 0, nd, 0, 3 * index);
            nd[3 * index] = r;
            nd[3 * index + 1] = c;
            nd[3 * index + 2] = v;
            System.arraycopy(d, 3 * index, nd, 3 * index + 3, 3 * (n - index));
            return new SortedSnap<>(nd, n + 1);
        }

        SortedSnap<R, C, V> without(int index) {
            if (n == 1) {
                return new SortedSnap<>(NO_DATA, 0);
            }
            final Object[] nd = new Object[3 * (n - 1)];
            System.arraycopy(d, 0, nd, 0, 3 * index);
            System.arraycopy(d, 3 * index + 3, nd, 3 * index, 3 * (n - index - 1));
            return new SortedSnap<>(nd, n - 1);
        }

        @Override int size() { return n; }

        @Override
        V get(Object r, Object c) {
            final int i = find(r, c);
            return i >= 0 ? (V) d[3 * i + 2] : null;
        }

        @Override boolean contains(Object r, Object c) { return find(r, c) >= 0; }

        @Override
        boolean containsRow(Object r) {
            if (n == 0 || r == null) {
                return false;
            }
            try {
                final int lo = rowLow(r);
                return lo < n && cmp(d[3 * lo], r) == 0;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        boolean containsColumn(Object c) {
            if (c == null) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (c.equals(d[3 * i + 1])) {
                    return true;
                }
            }
            return false;
        }

        @Override
        boolean containsValue(Object v) {
            if (v == null) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (v.equals(d[3 * i + 2])) {
                    return true;
                }
            }
            return false;
        }

        @Override
        Collection<V> values() {
            Collection<V> v = values;
            if (v == null) {
                v = n == 0 ? Collections.<V>emptyList() : new ValueList<V>(d, n);
                values = v;
            }
            return v;
        }

        @Override
        Set<Cell<R, C, V>> cellSet() {
            return n == 0 ? Collections.<Cell<R, C, V>>emptySet() : new CellSet<R, C, V>(this);
        }

        @Override
        Map<C, V> row(R r) {
            if (n == 0 || r == null) {
                return Collections.emptyMap();
            }
            try {
                final int lo = rowLow(r);
                if (lo >= n || cmp(d[3 * lo], r) != 0) {
                    return Collections.emptyMap();
                }
                return new RowView<C, V>(d, lo, rowHigh(r));
            } catch (ClassCastException e) {
                return Collections.emptyMap();
            }
        }

        @Override
        Map<R, V> column(C c) {
            if (n == 0 || c == null) {
                return Collections.emptyMap();
            }
            final Map<R, V> out = new LinkedHashMap<>();         // rows come out ascending, as in Guava
            for (int i = 0; i < n; i++) {
                if (c.equals(d[3 * i + 1])) {
                    out.put((R) d[3 * i], (V) d[3 * i + 2]);
                }
            }
            return out.isEmpty() ? Collections.<R, V>emptyMap() : Collections.unmodifiableMap(out);
        }

        @Override
        Table<R, C, V> table() {
            Table<R, C, V> t = table;
            if (t == null) {
                final TreeBasedTable raw = TreeBasedTable.create();
                for (int i = 0; i < n; i++) {
                    raw.put(d[3 * i], d[3 * i + 1], d[3 * i + 2]);
                }
                t = raw;
                table = t;
            }
            return t;
        }

        @Override
        List<R> computeRowKeys() {
            final List<R> out = new ArrayList<>();
            Object last = null;
            for (int i = 0; i < n; i++) {
                final Object row = d[3 * i];
                if (i == 0 || !row.equals(last)) {
                    out.add((R) row);
                    last = row;
                }
            }
            return out;
        }

        @Override
        List<C> computeColumnKeys() {
            final TreeSet<Object> set = new TreeSet<>();
            for (int i = 0; i < n; i++) {
                set.add(d[3 * i + 1]);
            }
            return new ArrayList<>((Set<C>) (Set) set);
        }
    }

    /** Read-only list over the value slots of a sorted array. */
    private static final class ValueList<V> extends AbstractList<V> implements RandomAccess {
        private final Object[] d;
        private final int n;

        ValueList(Object[] d, int n) {
            this.d = d;
            this.n = n;
        }

        @Override
        public V get(int index) {
            if (index < 0 || index >= n) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + n);
            }
            return (V) d[3 * index + 2];
        }

        @Override public int size() { return n; }

        /** Plain index iterator: AbstractList's one checks a modification counter that can never change here. */
        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                private int i;

                @Override public boolean hasNext() { return i < n; }

                @Override
                public V next() {
                    if (i >= n) {
                        throw new NoSuchElementException();
                    }
                    return (V) d[3 * i++ + 2];
                }
            };
        }
    }

    /** Read-only set of cells over a sorted array (cells are created on demand). */
    private static final class CellSet<R, C, V> extends AbstractSet<Cell<R, C, V>> {
        private final SortedSnap<R, C, V> s;

        CellSet(SortedSnap<R, C, V> s) {
            this.s = s;
        }

        @Override public int size() { return s.n; }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Cell)) {
                return false;
            }
            final Cell<?, ?, ?> cell = (Cell<?, ?, ?>) o;
            final int i = s.find(cell.getRowKey(), cell.getColumnKey());
            return i >= 0 && Objects.equals(s.d[3 * i + 2], cell.getValue());
        }

        @Override
        public Iterator<Cell<R, C, V>> iterator() {
            return new Iterator<Cell<R, C, V>>() {
                private int i;

                @Override public boolean hasNext() { return i < s.n; }

                @Override
                public Cell<R, C, V> next() {
                    if (i >= s.n) {
                        throw new NoSuchElementException();
                    }
                    final int k = 3 * i++;
                    return Tables.immutableCell((R) s.d[k], (C) s.d[k + 1], (V) s.d[k + 2]);
                }
            };
        }
    }

    /** Read-only map of one row: a window [lo, hi) into a sorted array. */
    private static final class RowView<C, V> extends AbstractMap<C, V> {
        private final Object[] d;
        private final int lo;
        private final int hi;

        RowView(Object[] d, int lo, int hi) {
            this.d = d;
            this.lo = lo;
            this.hi = hi;
        }

        private int indexOfColumn(Object c) {
            if (c == null) {
                return -1;
            }
            try {
                int a = lo, b = hi - 1;
                while (a <= b) {
                    final int mid = (a + b) >>> 1;
                    final int x = SortedSnap.cmp(d[3 * mid + 1], c);
                    if (x < 0) {
                        a = mid + 1;
                    } else if (x > 0) {
                        b = mid - 1;
                    } else {
                        return mid;
                    }
                }
            } catch (ClassCastException e) {
                // a column of another type is not in this row
            }
            return -1;
        }

        @Override public int size() { return hi - lo; }
        @Override public boolean containsKey(Object key) { return indexOfColumn(key) >= 0; }

        @Override
        public V get(Object key) {
            final int i = indexOfColumn(key);
            return i >= 0 ? (V) d[3 * i + 2] : null;
        }

        @Override
        public Set<Entry<C, V>> entrySet() {
            return new AbstractSet<Entry<C, V>>() {
                @Override public int size() { return hi - lo; }

                @Override
                public Iterator<Entry<C, V>> iterator() {
                    return new Iterator<Entry<C, V>>() {
                        private int i = lo;

                        @Override public boolean hasNext() { return i < hi; }

                        @Override
                        public Entry<C, V> next() {
                            if (i >= hi) {
                                throw new NoSuchElementException();
                            }
                            final int k = 3 * i++;
                            return new AbstractMap.SimpleImmutableEntry<>((C) d[k + 1], (V) d[k + 2]);
                        }
                    };
                }
            };
        }
    }

    // ======================================================================================================
    // table
    // ======================================================================================================

    private final boolean sorted;
    private transient volatile Snap<R, C, V> snap;

    private FSnapshotTable(boolean sorted, Snap<R, C, V> initial) {
        this.sorted = sorted;
        this.snap = initial;
    }

    /** Replacement for TreeBasedTable.create() */
    public static <R extends Comparable, C extends Comparable, V> FSnapshotTable<R, C, V> createTree() {
        return new FSnapshotTable<R, C, V>(true, new SortedSnap<R, C, V>(SortedSnap.NO_DATA, 0));
    }

    /** Replacement for TreeBasedTable.create(table) */
    public static <R extends Comparable, C extends Comparable, V> FSnapshotTable<R, C, V> createTree(Table<R, C, V> src) {
        FSnapshotTable<R, C, V> t = createTree();
        t.putAll(src);
        return t;
    }

    /** Replacement for HashBasedTable.create() */
    public static <R, C, V> FSnapshotTable<R, C, V> createHash() {
        return new FSnapshotTable<R, C, V>(false, new HashSnap<R, C, V>(HashBasedTable.<R, C, V>create()));
    }

    /** Replacement for HashBasedTable.create(table) */
    public static <R, C, V> FSnapshotTable<R, C, V> createHash(Table<? extends R, ? extends C, ? extends V> src) {
        return new FSnapshotTable<R, C, V>(false, new HashSnap<R, C, V>(HashBasedTable.<R, C, V>create(src)));
    }

    private SortedSnap<R, C, V> sortedSnap() {
        return (SortedSnap<R, C, V>) snap;
    }

    private Table<R, C, V> hashCopy() {
        return HashBasedTable.create(snap.table());
    }

    // ---------------- writes (synchronized, publish a new snapshot) ----------------

    @Override
    public synchronized V put(R rowKey, C columnKey, V value) {
        if (rowKey == null || columnKey == null || value == null) {
            throw new NullPointerException();
        }
        if (!sorted) {
            final V old = snap.table().get(rowKey, columnKey);
            if (old == value) {
                return old; // same instance already stored, skip the copy
            }
            final Table<R, C, V> next = hashCopy();
            next.put(rowKey, columnKey, value);
            snap = new HashSnap<>(next);
            return old;
        }
        final SortedSnap<R, C, V> s = sortedSnap();
        final int i = SortedSnap.search(s.d, s.n, rowKey, columnKey);
        if (i >= 0) {
            final V old = (V) s.d[3 * i + 2];
            if (old == value) {
                return old; // same instance already stored, nothing changes
            }
            snap = s.withValue(i, value);
            return old;
        }
        snap = s.inserted(-i - 1, rowKey, columnKey, value);
        return null;
    }

    @Override
    public synchronized V remove(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) return null;
        if (!sorted) {
            if (!snap.table().contains(rowKey, columnKey)) return null;
            final Table<R, C, V> next = hashCopy();
            final V old = next.remove(rowKey, columnKey);
            snap = new HashSnap<>(next);
            return old;
        }
        final SortedSnap<R, C, V> s = sortedSnap();
        final int i = s.find(rowKey, columnKey);
        if (i < 0) return null;
        final V old = (V) s.d[3 * i + 2];
        snap = s.without(i);
        return old;
    }

    @Override
    public synchronized void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        if (table.isEmpty()) return;
        if (!sorted) {
            final Table<R, C, V> next = hashCopy();
            next.putAll(table);
            snap = new HashSnap<>(next);
            return;
        }
        final SortedSnap<R, C, V> s = sortedSnap();
        Object[] d = s.d;
        int n = s.n;
        boolean copied = false;
        boolean changed = false;
        final int room = table.size();
        for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            final Object r = cell.getRowKey();
            final Object c = cell.getColumnKey();
            final Object v = cell.getValue();
            if (r == null || c == null || v == null) {
                throw new NullPointerException();
            }
            final int i = SortedSnap.search(d, n, r, c);
            if (i >= 0) {
                if (d[3 * i + 2] == v) {
                    continue;
                }
                if (!copied) {
                    d = Arrays.copyOf(d, 3 * (n + room));
                    copied = true;
                }
                d[3 * i + 2] = v;
            } else {
                if (!copied) {
                    d = Arrays.copyOf(d, 3 * (n + room));
                    copied = true;
                }
                final int pos = -i - 1;
                System.arraycopy(d, 3 * pos, d, 3 * pos + 3, 3 * (n - pos));
                d[3 * pos] = r;
                d[3 * pos + 1] = c;
                d[3 * pos + 2] = v;
                n++;
            }
            changed = true;
        }
        if (changed) {
            snap = new SortedSnap<>(d.length == 3 * n ? d : Arrays.copyOf(d, 3 * n), n);
        }
    }

    @Override
    public synchronized void clear() {
        if (snap.size() == 0) return;
        snap = sorted ? new SortedSnap<R, C, V>(SortedSnap.NO_DATA, 0) : new HashSnap<R, C, V>(HashBasedTable.<R, C, V>create());
    }

    /**
     * Removes every entry whose row key (rows == true) or column key matches. The predicate runs for all entries
     * before anything is published, so a throwing predicate leaves the table untouched.
     */
    private synchronized boolean removeKeys(boolean rows, Predicate<Object> remove) {
        if (!sorted) {
            final Table<R, C, V> next = hashCopy();
            final Set<?> keys = rows ? next.rowKeySet() : next.columnKeySet();
            boolean changed = false;
            for (Object key : new ArrayList<Object>(keys)) {
                if (remove.test(key)) {
                    keys.remove(key);
                    changed = true;
                }
            }
            if (!changed) return false;
            snap = new HashSnap<>(next);
            return true;
        }
        final SortedSnap<R, C, V> s = sortedSnap();
        final Object[] d = s.d;
        final int n = s.n;
        final int slot = rows ? 0 : 1;
        int first = -1;
        for (int i = 0; i < n; i++) {
            if (remove.test(d[3 * i + slot])) {
                first = i;
                break;
            }
        }
        if (first < 0) return false;
        final Object[] out = new Object[3 * n];
        System.arraycopy(d, 0, out, 0, 3 * first);
        int w = first;
        for (int i = first + 1; i < n; i++) {
            if (!remove.test(d[3 * i + slot])) {
                System.arraycopy(d, 3 * i, out, 3 * w, 3);
                w++;
            }
        }
        snap = w == 0 ? new SortedSnap<R, C, V>(SortedSnap.NO_DATA, 0) : new SortedSnap<R, C, V>(Arrays.copyOf(out, 3 * w), w);
        return true;
    }

    /**
     * Identity token of the current snapshot: a different object after every write that changed
     * the table, the same object otherwise. Lets callers validate caches derived from this table
     * with a plain reference comparison (no allocation, no locking).
     */
    public Object version() {
        return snap;
    }

    /** version() of the table, or null if it is not an FSnapshotTable (callers must then not cache). */
    public static Object versionOf(final Table<?, ?, ?> table) {
        return table instanceof FSnapshotTable ? ((FSnapshotTable<?, ?, ?>) table).version() : null;
    }

    // ---------------- reads (lock-free, snapshot) ----------------

    @Override public boolean contains(Object r, Object c) { return snap.contains(r, c); }
    @Override public boolean containsRow(Object r) { return snap.containsRow(r); }
    @Override public boolean containsColumn(Object c) { return snap.containsColumn(c); }
    @Override public boolean containsValue(Object v) { return snap.containsValue(v); }
    @Override public V get(Object r, Object c) { return snap.get(r, c); }
    @Override public boolean isEmpty() { return snap.size() == 0; }
    @Override public int size() { return snap.size(); }

    @Override public Map<C, V> row(R r) { return snap.row(r); }
    @Override public Map<R, V> column(C c) { return snap.column(c); }
    @Override public Map<R, Map<C, V>> rowMap() { return Collections.unmodifiableMap(snap.table().rowMap()); }
    @Override public Map<C, Map<R, V>> columnMap() { return Collections.unmodifiableMap(snap.table().columnMap()); }
    @Override public Set<R> rowKeySet() { return new KeyView<R>(true); }
    @Override public Set<C> columnKeySet() { return new KeyView<C>(false); }
    @Override public Collection<V> values() { return snap.values(); }
    @Override public Set<Cell<R, C, V>> cellSet() { return snap.cellSet(); }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof Table && cellSet().equals(((Table<?, ?, ?>) o).cellSet()));
    }

    @Override public int hashCode() { return cellSet().hashCode(); }
    @Override public String toString() { return snap.table().toString(); }

    // ---------------- serialization ----------------

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(snap.table());                       // the same format as before: a Guava table
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        final Table<R, C, V> t = (Table<R, C, V>) in.readObject();
        if (sorted) {
            snap = new SortedSnap<R, C, V>(SortedSnap.NO_DATA, 0);
            putAll(t);
        } else {
            snap = new HashSnap<>(t);
        }
    }

    /**
     * Key set view: reads come from the current snapshot, removals (remove, removeAll,
     * retainAll, removeIf, clear) are applied atomically. Iterator.remove is not supported.
     */
    private final class KeyView<K> extends AbstractSet<K> {
        private final boolean rows;

        KeyView(boolean rows) {
            this.rows = rows;
        }

        private List<K> keys() {
            return (List<K>) (rows ? snap.rowKeys() : snap.columnKeys());
        }

        @Override public Iterator<K> iterator() { return keys().iterator(); }
        @Override public int size() { return keys().size(); }
        @Override public boolean contains(Object o) { return rows ? snap.containsRow(o) : snap.containsColumn(o); }

        @Override
        public boolean remove(Object o) {
            return o != null && removeKeys(rows, o::equals);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return removeKeys(rows, c::contains);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            Objects.requireNonNull(c);
            return removeKeys(rows, k -> !c.contains(k));
        }

        @Override
        public boolean removeIf(Predicate<? super K> filter) {
            Objects.requireNonNull(filter);
            return removeKeys(rows, k -> filter.test((K) k));
        }

        @Override public void clear() { FSnapshotTable.this.clear(); }
    }
}