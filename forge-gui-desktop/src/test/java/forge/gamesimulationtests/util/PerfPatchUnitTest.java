package forge.gamesimulationtests.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.util.Visitor;
import forge.util.collect.FCollection;
import forge.util.collect.FSnapshotTable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Regression tests for the ANDROID-OPTIMIZE patch.
 * Strategy: differential testing. Every optimized structure is driven with long random operation
 * sequences next to a trivially-correct reference (Guava tables / ArrayList) and compared after EVERY step.
 */
public class PerfPatchUnitTest {

    // helpers
    private static void check(boolean ok, String msg) {
        Assert.assertTrue(ok, msg);
    }

    private static void eq(Object actual, Object expected, String ctx) {
        check(Objects.equals(actual, expected), ctx + "\n  expected=" + expected + "\n  actual  =" + actual);
    }

    private static void expectUnsupported(Runnable r, String ctx) {
        try {
            r.run();
        } catch (UnsupportedOperationException ok) {
            return;
        }
        Assert.fail("expected UnsupportedOperationException: " + ctx);
    }

    private static <R, C, V> void assertSameTable(Table<R, C, V> actual, Table<R, C, V> expected, boolean ordered, String ctx) {
        eq(actual.size(), expected.size(), ctx + " size");
        eq(actual.isEmpty(), expected.isEmpty(), ctx + " isEmpty");
        eq(actual.rowMap(), expected.rowMap(), ctx + " rowMap");
        eq(actual.columnMap(), expected.columnMap(), ctx + " columnMap");
        eq(new HashSet<>(actual.cellSet()), new HashSet<>(expected.cellSet()), ctx + " cellSet(as set)");
        eq(new HashSet<>(actual.values()), new HashSet<>(expected.values()), ctx + " values(as set)");
        eq(actual.rowKeySet(), expected.rowKeySet(), ctx + " rowKeySet(as set)");
        eq(actual.columnKeySet(), expected.columnKeySet(), ctx + " columnKeySet(as set)");
        if (ordered) {
            eq(new ArrayList<>(actual.values()), new ArrayList<>(expected.values()), ctx + " values order");
            eq(new ArrayList<>(actual.cellSet()), new ArrayList<>(expected.cellSet()), ctx + " cellSet order");
            eq(new ArrayList<>(actual.rowKeySet()), new ArrayList<>(expected.rowKeySet()), ctx + " rowKeySet order");
            eq(new ArrayList<>(actual.columnKeySet()), new ArrayList<>(expected.columnKeySet()), ctx + " columnKeySet order");
        }
        for (Table.Cell<R, C, V> cell : expected.cellSet()) {
            eq(actual.get(cell.getRowKey(), cell.getColumnKey()), cell.getValue(), ctx + " get");
            check(actual.contains(cell.getRowKey(), cell.getColumnKey()), ctx + " contains");
            check(actual.containsRow(cell.getRowKey()), ctx + " containsRow");
            check(actual.containsColumn(cell.getColumnKey()), ctx + " containsColumn");
            check(actual.containsValue(cell.getValue()), ctx + " containsValue");
            eq(actual.row(cell.getRowKey()), expected.row(cell.getRowKey()), ctx + " row()");
            eq(actual.column(cell.getColumnKey()), expected.column(cell.getColumnKey()), ctx + " column()");
        }
        check(actual.equals(expected), ctx + " actual.equals(expected)");
        check(expected.equals(actual), ctx + " expected.equals(actual)");
        eq(actual.hashCode(), expected.hashCode(), ctx + " hashCode");
    }

    // FSnapshotTable
    @Test
    public void snapshotTree_matchesGuavaTreeBasedTable_randomized() {
        for (long seed = 1; seed <= 25; seed++) {
            Random rnd = new Random(seed);
            FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
            Table<Long, Long, String> ref = TreeBasedTable.create();
            for (int i = 0; i < 400; i++) {
                String ctx = "seed=" + seed + " step=" + i;
                long r = rnd.nextInt(8), c = rnd.nextInt(8);
                switch (rnd.nextInt(9)) {
                    case 0: case 1: case 2: {
                        String v = "v" + rnd.nextInt(1000);
                        eq(t.put(r, c, v), ref.put(r, c, v), ctx + " put");
                        break;
                    }
                    case 3:
                        eq(t.remove(r, c), ref.remove(r, c), ctx + " remove");
                        break;
                    case 4:
                        if (rnd.nextInt(8) == 0) { t.clear(); ref.clear(); }
                        break;
                    case 5: {
                        Table<Long, Long, String> other = TreeBasedTable.create();
                        other.put(r, c, "o" + i);
                        other.put(c, r, "p" + i);
                        t.putAll(other);
                        ref.putAll(other);
                        break;
                    }
                    case 6:
                        eq(t.rowKeySet().remove(r), ref.rowKeySet().remove(r), ctx + " rowKeySet.remove");
                        break;
                    case 7: {
                        // Guava's TreeBasedTable.columnKeySet() has an unmodifiable iterator, so removeIf() throws UOE there
                        // (same for the snapshot table = parity). Use rowKeySet().removeIf and columnKeySet().removeAll instead.
                        final long mod = 1 + rnd.nextInt(3);
                        eq(t.rowKeySet().removeIf(k -> k % mod == 0), ref.rowKeySet().removeIf(k -> k % mod == 0), ctx + " rowKeySet.removeIf");
                        eq(t.columnKeySet().removeAll(Collections.singleton(c)), ref.columnKeySet().removeAll(Collections.singleton(c)), ctx + " columnKeySet.removeAll");
                        break;
                    }
                    default: {
                        Set<Long> keep = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));
                        eq(t.rowKeySet().retainAll(keep), ref.rowKeySet().retainAll(keep), ctx + " rowKeySet.retainAll");
                        break;
                    }
                }
                assertSameTable(t, ref, true, ctx);
            }
        }
    }

    @Test
    public void snapshotHash_matchesGuavaHashBasedTable_randomized() {
        for (long seed = 1; seed <= 25; seed++) {
            Random rnd = new Random(seed);
            FSnapshotTable<String, String, Integer> t = FSnapshotTable.createHash();
            Table<String, String, Integer> ref = HashBasedTable.create();
            for (int i = 0; i < 300; i++) {
                String r = "r" + rnd.nextInt(6), c = "c" + rnd.nextInt(6);
                switch (rnd.nextInt(5)) {
                    case 0: case 1: case 2:
                        eq(t.put(r, c, i), ref.put(r, c, i), "put seed=" + seed);
                        break;
                    case 3:
                        eq(t.remove(r, c), ref.remove(r, c), "remove seed=" + seed);
                        break;
                    default:
                        eq(t.rowKeySet().remove(r), ref.rowKeySet().remove(r), "rowKeySet.remove seed=" + seed);
                }
                assertSameTable(t, ref, false, "hash seed=" + seed + " step=" + i);
            }
        }
    }

    @Test
    public void snapshot_factoryCopiesAreIndependentAndOrdered() {
        TreeBasedTable<Long, Long, String> src = TreeBasedTable.create();
        src.put(5L, 1L, "a");
        src.put(2L, 9L, "b");
        src.put(2L, 3L, "c");
        FSnapshotTable<Long, Long, String> tree = FSnapshotTable.createTree(src);
        assertSameTable(tree, src, true, "createTree(src)");
        src.put(7L, 7L, "later");
        check(!tree.contains(7L, 7L), "createTree(src) must be a copy, not a view");
        tree.put(0L, 0L, "z");
        check(!src.contains(0L, 0L), "writes must not leak into the source");
        eq(new ArrayList<>(tree.rowKeySet()), Arrays.asList(0L, 2L, 5L), "tree order is sorted");

        FSnapshotTable<Long, Long, String> hash = FSnapshotTable.createHash(src);
        assertSameTable(hash, src, false, "createHash(src)");
    }

    @Test
    public void snapshot_nullsAreRejectedLikeGuava() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        for (Object[] args : new Object[][]{{null, 1L, "x"}, {1L, null, "x"}, {1L, 1L, null}}) {
            try {
                t.put((Long) args[0], (Long) args[1], (String) args[2]);
                Assert.fail("put with a null must throw NPE");
            } catch (NullPointerException expected) {
                // ok
            }
        }
        eq(t.remove(null, 1L), null, "remove(null,..) is a no-op");
        eq(t.size(), 0, "nothing was stored");
    }

    @Test
    public void snapshot_readersNeverSeeConcurrentModification() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        for (long i = 0; i < 5; i++) t.put(i, i, "v" + i);

        int seen = 0;
        for (String ignored : t.values()) {                // the exact pattern "for (X x : table.values())"
            t.put(100L + seen, 0L, "added during iteration");
            t.remove((long) seen, (long) seen);
            seen++;
        }
        eq(seen, 5, "iteration runs over the snapshot taken when it started");

        Iterator<Table.Cell<Long, Long, String>> it = t.cellSet().iterator();
        t.clear();
        int cells = 0;
        while (it.hasNext()) { it.next(); cells++; }
        check(cells > 0, "an iterator created before clear() still walks its snapshot");
        eq(t.size(), 0, "table is empty after clear");
    }

    @Test
    public void snapshot_versionOnlyChangesOnRealChange() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        Object v0 = t.version();
        t.clear();
        check(t.version() == v0, "clear() on empty table must not change version");
        t.remove(1L, 1L);
        check(t.version() == v0, "remove of absent key must not change version");
        t.putAll(TreeBasedTable.<Long, Long, String>create());
        check(t.version() == v0, "putAll(empty) must not change version");

        String value = "x";
        t.put(1L, 1L, value);
        Object v1 = t.version();
        check(v1 != v0, "put must change version");
        t.put(1L, 1L, value);
        check(t.version() == v1, "putting the identical instance again must not change version");
        t.put(1L, 1L, new String("x"));
        check(t.version() != v1, "putting an equal-but-different instance MUST change version (value identity matters to caches)");

        Object v2 = t.version();
        t.rowKeySet().remove(42L);
        check(t.version() == v2, "no-op key-set removal must not change version");
        t.rowKeySet().remove(1L);
        check(t.version() != v2, "effective key-set removal must change version");

        check(FSnapshotTable.versionOf(t) == t.version(), "versionOf(FSnapshotTable) == version()");
        Assert.assertNull(FSnapshotTable.versionOf(TreeBasedTable.<Long, Long, String>create()),
                "foreign Table implementations must yield null so callers never cache");
    }

    @Test
    public void snapshot_valuesViewIsCachedPerSnapshot() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        check(t.values() == FSnapshotTable.<Long, Long, String>createHash().values(), "empty values() is one shared instance");
        t.put(1L, 1L, "a");
        Collection<String> a = t.values();
        check(a == t.values(), "values() is cached while the table does not change");
        t.put(2L, 2L, "b");
        Collection<String> b = t.values();
        check(a != b, "a write produces a new values() snapshot");
        eq(new ArrayList<>(a), Collections.singletonList("a"), "old snapshot is not mutated");
        eq(new ArrayList<>(b), Arrays.asList("a", "b"), "new snapshot content");
        expectUnsupported(() -> b.add("nope"), "values() must be read-only");
    }

    @Test
    public void snapshot_readViewsAreReadOnly_keySetsRemoveThroughCopyOnWrite() {
        // DOCUMENTS A BEHAVIOUR CHANGE: with Guava, row()/column()/rowMap()/cellSet() were live and writable.
        // If any caller in the code base does table.row(x).put/clear/remove, it now throws.
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        t.put(1L, 1L, "a");
        t.put(1L, 2L, "b");
        t.put(2L, 1L, "c");
        expectUnsupported(() -> t.row(1L).clear(), "row() view");
        expectUnsupported(() -> t.column(1L).clear(), "column() view");
        expectUnsupported(() -> t.rowMap().clear(), "rowMap() view");
        expectUnsupported(() -> t.columnMap().clear(), "columnMap() view");
        expectUnsupported(() -> t.cellSet().clear(), "cellSet() view");
        eq(t.size(), 3, "failed writes through views must not modify the table");

        eq(t.rowKeySet().removeAll(Collections.singleton(2L)), true, "rowKeySet.removeAll");
        eq(t.size(), 2, "row 2 is gone");
        eq(t.columnKeySet().retainAll(Collections.singleton(2L)), true, "columnKeySet.retainAll");
        eq(t.size(), 1, "only column 2 left");
        t.columnKeySet().clear();
        check(t.isEmpty(), "columnKeySet.clear empties the table");
    }

    @Test
    public void snapshot_serializationRoundTrip() throws Exception {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        for (long i = 0; i < 20; i++) t.put(i % 5, i, "v" + i);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) { out.writeObject(t); }
        @SuppressWarnings("unchecked")
        FSnapshotTable<Long, Long, String> back;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            back = (FSnapshotTable<Long, Long, String>) in.readObject();
        }
        assertSameTable(back, t, true, "deserialized");
        check(back.version() != null, "snapshot is restored");
        back.put(99L, 99L, "after");
        check(back.contains(99L, 99L) && !t.contains(99L, 99L), "deserialized table is writable and independent");
    }

    @Test
    public void snapshot_concurrentReadersAndWriters() throws Exception {
        final FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        final int writers = 3, perWriter = 400;
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final CountDownLatch start = new CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);
        ExecutorService pool = Executors.newFixedThreadPool(writers + 4);
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int w = 0; w < writers; w++) {
            final long wid = w;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    for (long i = 0; i < perWriter; i++) t.put(wid, i, "w" + wid + "-" + i);
                } catch (Throwable e) { errors.add(e); }
            }));
        }
        for (int r = 0; r < 4; r++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    while (!done.get()) {
                        int n = 0;
                        for (String s : t.values()) { n += s.length() > 0 ? 1 : 0; }
                        for (Table.Cell<Long, Long, String> c : t.cellSet()) { Objects.requireNonNull(c.getValue()); }
                        t.get(0L, 0L);
                        t.contains(1L, 1L);
                        t.rowKeySet().size();
                    }
                } catch (Throwable e) { errors.add(e); }
            }));
        }
        start.countDown();
        for (int i = 0; i < writers; i++) futures.get(i).get(30, TimeUnit.SECONDS);
        done.set(true);
        for (int i = writers; i < futures.size(); i++) futures.get(i).get(30, TimeUnit.SECONDS);
        pool.shutdownNow();
        check(errors.isEmpty(), "no thread may fail: " + errors);
        eq(t.size(), writers * perWriter, "no write may be lost (writes are synchronized)");
    }

    // FCollection - differential test against a plain ArrayList model

    /** Hash-colliding element (id % 5) to stress linear probing and backward-shift deletion. */
    private static final class Coll {
        final int id;
        Coll(int id) { this.id = id; }
        @Override public int hashCode() { return id % 5; }
        @Override public boolean equals(Object o) { return o instanceof Coll && ((Coll) o).id == id; }
        @Override public String toString() { return "C" + id; }
    }

    private static <E> List<E> randomList(Random rnd, int domain, int max, IntFunction<E> mk) {
        List<E> l = new ArrayList<>();
        int n = rnd.nextInt(max + 1);
        for (int i = 0; i < n; i++) l.add(mk.apply(rnd.nextInt(domain)));
        return l;
    }

    @SuppressWarnings("unchecked")
    private static <E> void differential(long seed, int domain, int ops, IntFunction<E> mk) {
        Random rnd = new Random(seed);
        FCollection<E> fc = new FCollection<>();
        List<E> ref = new ArrayList<>();

        for (int i = 0; i < ops; i++) {
            int op = (i < ops / 4 && rnd.nextInt(10) < 7) ? 0 : rnd.nextInt(14);
            String ctx = "seed=" + seed + " domain=" + domain + " op#" + i + " kind=" + op;
            E e = mk.apply(rnd.nextInt(domain));
            switch (op) {
                case 0: case 1: {
                    boolean expect = !ref.contains(e);
                    if (expect) ref.add(e);
                    eq(fc.add(e), expect, ctx + " add");
                    break;
                }
                case 2:
                    eq(fc.remove((Object) e), ref.remove(e), ctx + " remove(Object)");
                    break;
                case 3:
                    if (!ref.isEmpty()) {
                        int idx = rnd.nextInt(ref.size());
                        eq(fc.remove(idx), ref.remove(idx), ctx + " remove(int)");
                    }
                    break;
                case 4: {
                    List<E> in = randomList(rnd, domain, 40, mk);
                    boolean changed = false;
                    for (E x : in) if (!ref.contains(x)) { ref.add(x); changed = true; }
                    eq(fc.addAll((Collection<E>) in), changed, ctx + " addAll(Collection)");
                    break;
                }
                case 5: {
                    List<E> in = randomList(rnd, domain, 20, mk);
                    eq(fc.removeAll((Collection<?>) in), ref.removeAll(in), ctx + " removeAll");
                    break;
                }
                case 6:
                    if (rnd.nextInt(8) == 0) {
                        List<E> in = randomList(rnd, domain, domain, mk);
                        eq(fc.retainAll((Collection<?>) in), ref.retainAll(in), ctx + " retainAll");
                    }
                    break;
                case 7:
                    if (rnd.nextInt(5) == 0) {
                        final int k = rnd.nextInt(7);
                        eq(fc.removeIf(x -> Math.abs(x.hashCode()) % 7 == k), ref.removeIf(x -> Math.abs(x.hashCode()) % 7 == k), ctx + " removeIf");
                    }
                    break;
                case 8: {
                    int idx = rnd.nextInt(ref.size() + 1);
                    int old = ref.indexOf(e);
                    if (old < 0) {
                        ref.add(idx, e);
                    } else if (idx != old) {            // FCollection re-positions an existing element
                        int target = idx > old ? idx - 1 : idx;
                        ref.remove(old);
                        ref.add(target, e);
                    }
                    fc.add(idx, e);
                    break;
                }
                case 9:
                    if (!ref.isEmpty() && !ref.contains(e)) {
                        int idx = rnd.nextInt(ref.size());
                        eq(fc.replace(idx, e), ref.set(idx, e), ctx + " replace");
                    }
                    break;
                case 10:
                    if (rnd.nextInt(40) == 0) { fc.clear(); ref.clear(); }
                    break;
                case 11: {
                    List<E> in = randomList(rnd, domain, 30, mk);
                    boolean changed = false;
                    for (E x : in) if (!ref.contains(x)) { ref.add(x); changed = true; }
                    E[] arr = (E[]) in.toArray();
                    eq(fc.addAll(arr), changed, ctx + " addAll(T[])");
                    break;
                }
                case 12: {
                    FCollection<E> other = new FCollection<>();
                    other.addAll((Collection<E>) randomList(rnd, domain, 60, mk));
                    boolean changed = false;
                    for (E x : other) if (!ref.contains(x)) { ref.add(x); changed = true; }
                    eq(fc.addAll((Collection<E>) other), changed, ctx + " addAll(FCollection)");
                    break;
                }
                default:
                    break;
            }

            // full invariant after every operation
            eq(new ArrayList<>(fc), ref, ctx + " content/order");
            eq(fc.size(), ref.size(), ctx + " size");
            Set<E> refSet = new HashSet<>(ref);
            for (int k = 0; k < domain; k++) {
                E probe = mk.apply(k);
                boolean in = refSet.contains(probe);
                eq(fc.contains(probe), in, ctx + " contains(" + probe + ")");
                if (in) {
                    check(fc.get(probe) == ref.get(ref.indexOf(probe)), ctx + " get(T) must return the STORED instance");
                } else {
                    check(fc.get(probe) == probe, ctx + " get(T) of absent element returns the argument");
                }
            }
            eq(fc.contains(null), false, ctx + " contains(null)");
            eq(fc.add(null), false, ctx + " add(null)");
        }
    }

    @Test
    public void fcollection_differential_strings() {
        for (long seed = 1; seed <= 4; seed++) {
            for (int domain : new int[]{12, 100, 300, 700}) {   // crosses the 64 / 128 hashing thresholds
                differential(seed, domain, 2500, k -> new String("e" + k));
            }
        }
    }

    @Test
    public void fcollection_differential_collidingHashes() {
        for (long seed = 1; seed <= 4; seed++) {
            for (int domain : new int[]{30, 150, 400}) {
                differential(seed, domain, 2500, Coll::new);
            }
        }
    }

    @Test
    public void fcollection_constructorsAndCopies() {
        FCollection<String> fromDups = new FCollection<>(Arrays.asList("a", "b", "a", "c", "b"));
        eq(new ArrayList<>(fromDups), Arrays.asList("a", "b", "c"), "Iterable ctor dedupes keeping first occurrence");
        eq(new ArrayList<>(new FCollection<>(new String[]{"x", "y", "x"})), Arrays.asList("x", "y"), "array ctor dedupes");
        eq(new ArrayList<>(new FCollection<>("solo")), Collections.singletonList("solo"), "single element ctor");

        FCollection<String> big = new FCollection<>();
        for (int i = 0; i < 300; i++) big.add("s" + i);
        FCollection<String> copy = big.clone();
        copy.remove("s5");
        copy.add("extra");
        check(big.contains("s5") && !big.contains("extra"), "clone is independent");
        check(!copy.contains("s5") && copy.contains("extra"), "clone has its own membership");
        FCollection<String> copy2 = new FCollection<>(big);
        check(copy2.contains("s299") && copy2.size() == 300, "copy ctor from a hashed FCollection");

        List<String> sub = big.subList(1, 4);
        eq(sub, Arrays.asList("s1", "s2", "s3"), "subList content");
        big.remove("s2");
        eq(sub, Arrays.asList("s1", "s2", "s3"), "subList is a static copy (documented contract)");

        Iterable<String> safe = big.threadSafeIterable();
        big.add("late");
        int n = 0;
        for (String ignored : safe) n++;
        eq(n, 299, "threadSafeIterable is a snapshot");

        FCollection<String> empty = FCollection.getEmpty();
        empty.ensureCapacity(10);
        eq(empty.add("x"), false, "shared EMPTY refuses adds");
        eq(empty.size(), 0, "shared EMPTY stays empty");
        check(!empty.iterator().hasNext() && empty.asSet().isEmpty() && !empty.contains("x"), "EMPTY reads");
        eq(empty.get("zzz"), "zzz", "EMPTY.get(T) returns the argument");
    }

    @Test
    public void fcollection_serializationKeepsMembershipWorking() throws Exception {
        FCollection<String> big = new FCollection<>();
        for (int i = 0; i < 400; i++) big.add("s" + i);
        big.contains("s1");                                     // force the lazy table
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) { out.writeObject(big); }
        @SuppressWarnings("unchecked")
        FCollection<String> back;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            back = (FCollection<String>) in.readObject();
        }
        eq(new ArrayList<>(back), new ArrayList<>(big), "content survives");
        check(back.contains("s399") && !back.contains("nope"), "contains after deserialization");
        eq(back.add("s7"), false, "uniqueness still enforced after deserialization");
        eq(back.add("fresh"), true, "adds still work after deserialization");
        check(back.contains("fresh"), "membership includes later adds");
    }

    @Test
    public void fcollection_asSetIsALiveView() {
        FCollection<String> fc = new FCollection<>();
        for (int i = 0; i < 200; i++) fc.add("s" + i);
        Set<String> view = fc.asSet();
        check(view.add("new") && fc.contains("new"), "add through view");
        check(view.remove("s0") && !fc.contains("s0"), "remove through view");
        Iterator<String> it = view.iterator();
        String first = it.next();
        it.remove();
        check(!fc.contains(first) && !view.contains(first), "Iterator.remove through the SET view keeps the hash table in sync");
        eq(fc.size(), 199, "size after view mutations (200 +1 add -1 remove -1 iterator.remove)");
        eq(fc.add(first), true, "removed element can be added again");
    }

    @Test
    public void fcollection_iteratorRemoveKeepsMembershipInSync() {
        // was a hazard (also before the patch); fixed by HashSyncIterator / listIterator dropping the table
        FCollection<String> fc = new FCollection<>();
        for (int i = 0; i < 300; i++) fc.add("s" + i);
        fc.contains("s1");
        Iterator<String> it = fc.iterator();
        it.next();
        it.remove();
        check(!fc.contains("s0"), "contains must be false after Iterator.remove");
        check(fc.add("s0"), "removed element can be added again");
        java.util.ListIterator<String> li = fc.listIterator();
        li.next();
        li.remove();
        check(!fc.contains("s1") && fc.add("s1"), "same for listIterator().remove()");
    }

    @Test
    public void fcollection_shareCopyIsCopyOnWrite() {
        for (int size : new int[]{0, 5, 100, 300}) {
            for (long seed = 1; seed <= 25; seed++) {
                Random rnd = new Random(seed);
                FCollection<String> master = new FCollection<>();
                List<String> masterRef = new ArrayList<>();
                for (int i = 0; i < size; i++) { master.add("e" + i); masterRef.add("e" + i); }
                FCollection<String> a = master.shareCopy();
                FCollection<String> b = master.shareCopy();
                List<String> ra = new ArrayList<>(masterRef), rb = new ArrayList<>(masterRef);
                for (int i = 0; i < 300; i++) {
                    boolean useA = rnd.nextBoolean();
                    FCollection<String> f = useA ? a : b;
                    List<String> ref = useA ? ra : rb;
                    String e = "e" + rnd.nextInt(size + 40);
                    String ctx = "size=" + size + " seed=" + seed + " op#" + i;
                    switch (rnd.nextInt(8)) {
                        case 0: { boolean ex = !ref.contains(e); if (ex) ref.add(e); eq(f.add(e), ex, ctx + " add"); break; }
                        case 1: eq(f.remove((Object) e), ref.remove(e), ctx + " remove"); break;
                        case 2: {
                            Iterator<String> it = f.iterator();
                            int step = 2 + rnd.nextInt(3), idx = 0;
                            while (it.hasNext()) { String v = it.next(); if (idx++ % step == 0) { it.remove(); ref.remove(v); } }
                            break;
                        }
                        case 3: if (rnd.nextInt(10) == 0) { f.clear(); ref.clear(); } break;
                        case 4: f.removeIf(v -> v.hashCode() % 5 == 0); ref.removeIf(v -> v.hashCode() % 5 == 0); break;
                        case 5: if (!ref.isEmpty()) { int k = rnd.nextInt(ref.size()); eq(f.remove(k), ref.remove(k), ctx + " remove(int)"); } break;
                        case 6: f.sort(); ref.sort(java.util.Comparator.comparing(Object::toString)); break;
                        default: {
                            List<String> in = new ArrayList<>();
                            for (int j = 0; j < 10; j++) in.add("n" + rnd.nextInt(30));
                            boolean ch = false;
                            for (String v : in) if (!ref.contains(v)) { ref.add(v); ch = true; }
                            eq(f.addAll((Collection<String>) in), ch, ctx + " addAll");
                        }
                    }
                    eq(new ArrayList<>(a), ra, ctx + " copy A");
                    eq(new ArrayList<>(b), rb, ctx + " copy B");
                    eq(new ArrayList<>(master), masterRef, ctx + " master must never change");
                    for (String v : ra) check(a.contains(v), ctx + " A.contains");
                    check(!a.contains("absent"), ctx + " A.contains(absent)");
                }
                FCollection<String> late = master.shareCopy();
                master.add("late");
                check(!late.contains("late"), "a share is not affected by later writes to the master");
            }
        }
    }

    @Test(enabled = false, description = "KNOWN HAZARD: new FCollection(int) overload steals 'new FCollection<Integer>(5)'")
    public void fcollection_intLiteralConstructorHazard() {
        FCollection<Integer> c = new FCollection<Integer>(5);
        eq(c.size(), 1, "before the patch this created a collection CONTAINING 5");
    }

    // Visitor.visitAll (FCollection index-loop fast path)

    @Test
    public void visitor_fastPathBehavesLikeIteratorPath() {
        FCollection<String> fc = new FCollection<>(Arrays.asList("a", "b", "c", "d"));
        List<String> plain = new ArrayList<>(fc);

        List<String> seenFast = new ArrayList<>();
        List<String> seenSlow = new ArrayList<>();
        Visitor<String> fast = o -> { seenFast.add(o); return true; };
        Visitor<String> slow = o -> { seenSlow.add(o); return true; };
        check(fast.visitAll(fc), "fast path returns true when nothing stops it");
        check(slow.visitAll(plain), "slow path returns true");
        eq(seenFast, seenSlow, "same visiting order");

        List<String> stopFast = new ArrayList<>();
        List<String> stopSlow = new ArrayList<>();
        check(!((Visitor<String>) o -> { stopFast.add(o); return !o.equals("b"); }).visitAll(fc), "fast path stops on false");
        check(!((Visitor<String>) o -> { stopSlow.add(o); return !o.equals("b"); }).visitAll(plain), "slow path stops on false");
        eq(stopFast, stopSlow, "same early-stop point");

        check(((Visitor<String>) o -> { throw new AssertionError("must not be called"); }).visitAll(new FCollection<String>()), "empty FCollection");
    }

    // ColorSet.getSymbolsString (replaces stream().map(getSymbol).collect(joining()))
    @Test
    public void colorSet_symbolsStringEqualsOriginalStreamExpression() {
        for (int mask = 0; mask < 32; mask++) {
            ColorSet cs = ColorSet.fromMask(mask);
            String expected = cs.stream().map(MagicColor.Color::getSymbol).collect(Collectors.joining());
            eq(cs.getSymbolsString(), expected, "mask=" + mask);
            check(cs.getSymbolsString() == cs.getSymbolsString(), "mask=" + mask + " is cached (same instance)");
        }
        eq(ColorSet.fromMask(0).getSymbolsString(),
                ColorSet.fromMask(0).stream().map(MagicColor.Color::getSymbol).collect(Collectors.joining()), "colorless (is \"{C}\" before AND after the patch)");
    }
}