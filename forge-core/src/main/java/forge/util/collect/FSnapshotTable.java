package forge.util.collect;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Copy-on-write Table: lock-free reads/iteration (no ConcurrentModificationException),
 * synchronized writes. Backed by Guava's TreeBasedTable or HashBasedTable, so ordering
 * and semantics are identical to the originals. Best for small, read-heavy tables.
 *
 * Reads are served from an immutable-by-convention snapshot. values() is cached per snapshot
 * (and is a shared empty list for an empty table), so hot loops like
 * "for (X x : table.values())" allocate (almost) nothing.
 *
 * Views returned by readers (cellSet, values, row, column, ...) are read-only snapshots;
 * mutate only through put/remove/putAll/clear or the removal methods of
 * rowKeySet()/columnKeySet() (remove, removeAll, retainAll, removeIf, clear).
 */
public final class FSnapshotTable<R, C, V> implements Table<R, C, V>, Serializable {
    private static final long serialVersionUID = 2L;

    /** Immutable-by-convention Guava table plus lazily cached read-only views. */
    private static final class Snap<R, C, V> {
        final Table<R, C, V> table;
        private volatile Collection<V> values;

        Snap(Table<R, C, V> table) {
            this.table = table;
        }

        Collection<V> values() {
            Collection<V> v = values;
            if (v == null) {
                if (table.isEmpty()) {
                    v = Collections.emptyList();
                } else {
                    List<V> list = new ArrayList<>(table.values());
                    v = Collections.unmodifiableList(list);
                }
                values = v; // benign race: every thread computes the same content
            }
            return v;
        }
    }

    private final boolean sorted;
    private transient volatile Snap<R, C, V> snap;

    private FSnapshotTable(boolean sorted, Table<R, C, V> initial) {
        this.sorted = sorted;
        this.snap = new Snap<>(initial);
    }

    /** Replacement for TreeBasedTable.create() */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <R extends Comparable, C extends Comparable, V> FSnapshotTable<R, C, V> createTree() {
        return new FSnapshotTable<>(true, TreeBasedTable.<R, C, V>create());
    }

    /** Replacement for TreeBasedTable.create(table) */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <R extends Comparable, C extends Comparable, V> FSnapshotTable<R, C, V> createTree(Table<R, C, V> src) {
        FSnapshotTable<R, C, V> t = createTree();
        t.putAll(src);
        return t;
    }

    /** Replacement for HashBasedTable.create() */
    public static <R, C, V> FSnapshotTable<R, C, V> createHash() {
        return new FSnapshotTable<>(false, HashBasedTable.<R, C, V>create());
    }

    /** Replacement for HashBasedTable.create(table) */
    public static <R, C, V> FSnapshotTable<R, C, V> createHash(Table<? extends R, ? extends C, ? extends V> src) {
        return new FSnapshotTable<>(false, HashBasedTable.<R, C, V>create(src));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Table<R, C, V> copy() {
        Table<R, C, V> cur = snap.table;
        if (sorted) {
            return TreeBasedTable.create((TreeBasedTable) cur);
        }
        return HashBasedTable.create(cur);
    }

    // ---------------- writes (synchronized, copy-on-write) ----------------

    @Override
    public synchronized V put(R rowKey, C columnKey, V value) {
        if (rowKey == null || columnKey == null || value == null) {
            throw new NullPointerException();
        }
        V old = snap.table.get(rowKey, columnKey);
        if (old == value) {
            return old; // same instance already stored, skip the copy
        }
        Table<R, C, V> next = copy();
        next.put(rowKey, columnKey, value);
        snap = new Snap<>(next);
        return old;
    }

    @Override
    public synchronized V remove(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) return null;
        if (!snap.table.contains(rowKey, columnKey)) return null;
        Table<R, C, V> next = copy();
        V old = next.remove(rowKey, columnKey);
        snap = new Snap<>(next);
        return old;
    }

    @Override
    public synchronized void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        if (table.isEmpty()) return;
        Table<R, C, V> next = copy();
        next.putAll(table);
        snap = new Snap<>(next);
    }

    @Override
    public synchronized void clear() {
        if (snap.table.isEmpty()) return;
        Table<R, C, V> next = copy();
        next.clear();
        snap = new Snap<>(next);
    }

    /** Copies once, lets the action mutate the private copy, publishes only if it returns true. */
    private synchronized boolean update(Predicate<Table<R, C, V>> action) {
        Table<R, C, V> next = copy();
        if (!action.test(next)) return false;
        snap = new Snap<>(next);
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

    @Override public boolean contains(Object r, Object c) { return snap.table.contains(r, c); }
    @Override public boolean containsRow(Object r) { return snap.table.containsRow(r); }
    @Override public boolean containsColumn(Object c) { return snap.table.containsColumn(c); }
    @Override public boolean containsValue(Object v) { return snap.table.containsValue(v); }
    @Override public V get(Object r, Object c) { return snap.table.get(r, c); }
    @Override public boolean isEmpty() { return snap.table.isEmpty(); }
    @Override public int size() { return snap.table.size(); }

    @Override public Map<C, V> row(R r) { return Collections.unmodifiableMap(snap.table.row(r)); }
    @Override public Map<R, V> column(C c) { return Collections.unmodifiableMap(snap.table.column(c)); }
    @Override public Map<R, Map<C, V>> rowMap() { return Collections.unmodifiableMap(snap.table.rowMap()); }
    @Override public Map<C, Map<R, V>> columnMap() { return Collections.unmodifiableMap(snap.table.columnMap()); }
    @Override public Set<R> rowKeySet() { return new KeyView<R>() { Set<R> keys(Table<R, C, V> t) { return t.rowKeySet(); } }; }
    @Override public Set<C> columnKeySet() { return new KeyView<C>() { Set<C> keys(Table<R, C, V> t) { return t.columnKeySet(); } }; }
    @Override public Collection<V> values() { return snap.values(); }

    @Override
    public Set<Cell<R, C, V>> cellSet() {
        Table<R, C, V> t = snap.table;
        return t.isEmpty() ? Collections.<Cell<R, C, V>>emptySet() : Collections.unmodifiableSet(t.cellSet());
    }

    @Override public boolean equals(Object o) { return o == this || snap.table.equals(o); }
    @Override public int hashCode() { return snap.table.hashCode(); }
    @Override public String toString() { return snap.table.toString(); }

    // ---------------- serialization ----------------

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(snap.table);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        snap = new Snap<>((Table<R, C, V>) in.readObject());
    }

    /**
     * Key set view: reads come from the current snapshot, removals (remove, removeAll,
     * retainAll, removeIf, clear) are applied copy-on-write. Iterator.remove is not supported.
     */
    private abstract class KeyView<K> extends AbstractSet<K> {
        abstract Set<K> keys(Table<R, C, V> t);

        @Override public Iterator<K> iterator() { return Collections.unmodifiableSet(keys(snap.table)).iterator(); }
        @Override public int size() { return keys(snap.table).size(); }
        @Override public boolean contains(Object o) { return keys(snap.table).contains(o); }
        @Override public boolean remove(Object o) { return update(t -> keys(t).remove(o)); }
        @Override public boolean removeAll(Collection<?> c) { return update(t -> keys(t).removeAll(c)); }
        @Override public boolean retainAll(Collection<?> c) { return update(t -> keys(t).retainAll(c)); }
        @Override public boolean removeIf(Predicate<? super K> f) { return update(t -> keys(t).removeIf(f)); }
        @Override public void clear() { update(t -> { boolean changed = !t.isEmpty(); t.clear(); return changed; }); }
    }
}