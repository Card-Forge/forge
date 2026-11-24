package forge.gamesimulationtests.util;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import forge.util.collect.FSnapshotTable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Follow-up to PerfPatchUnitTest for the sorted-array FSnapshotTable: the same table is driven next to Guava's
 * TreeBasedTable with big random operation sequences and with the "many lords" access pattern of continuous
 * effects (remove all effects, re-apply them one by one and read after every write).
 */
public class PerfPatchFollowUpTableTest {

    private static void eq(Object actual, Object expected, String ctx) {
        Assert.assertTrue(Objects.equals(actual, expected), ctx + "\n  expected=" + expected + "\n  actual  =" + actual);
    }

    /** Everything a caller can observe, compared with the reference table. */
    private static <R, C, V> void assertSame(Table<R, C, V> actual, Table<R, C, V> expected, String ctx) {
        eq(actual.size(), expected.size(), ctx + " size");
        eq(actual.isEmpty(), expected.isEmpty(), ctx + " isEmpty");
        eq(new ArrayList<>(actual.values()), new ArrayList<>(expected.values()), ctx + " values (order)");
        eq(new ArrayList<>(actual.cellSet()), new ArrayList<>(expected.cellSet()), ctx + " cellSet (order)");
        eq(new ArrayList<>(actual.rowKeySet()), new ArrayList<>(expected.rowKeySet()), ctx + " rowKeySet (order)");
        eq(new ArrayList<>(actual.columnKeySet()), new ArrayList<>(expected.columnKeySet()), ctx + " columnKeySet (order)");
        eq(actual.rowMap(), expected.rowMap(), ctx + " rowMap");
        eq(actual.columnMap(), expected.columnMap(), ctx + " columnMap");
        for (R r : expected.rowKeySet()) {
            eq(new ArrayList<>(actual.row(r).entrySet()), new ArrayList<>(expected.row(r).entrySet()), ctx + " row(" + r + ") (order)");
            eq(actual.row(r).size(), expected.row(r).size(), ctx + " row size");
        }
        for (C c : expected.columnKeySet()) {
            eq(new ArrayList<>(actual.column(c).entrySet()), new ArrayList<>(expected.column(c).entrySet()), ctx + " column(" + c + ") (order)");
        }
        for (Table.Cell<R, C, V> cell : expected.cellSet()) {
            eq(actual.get(cell.getRowKey(), cell.getColumnKey()), cell.getValue(), ctx + " get");
            Assert.assertTrue(actual.contains(cell.getRowKey(), cell.getColumnKey()), ctx + " contains");
            Assert.assertTrue(actual.containsRow(cell.getRowKey()) && actual.containsColumn(cell.getColumnKey()) && actual.containsValue(cell.getValue()), ctx + " contains*");
            Assert.assertTrue(actual.cellSet().contains(cell), ctx + " cellSet.contains");
        }
        Assert.assertTrue(actual.equals(expected) && expected.equals(actual), ctx + " equals");
        eq(actual.hashCode(), expected.hashCode(), ctx + " hashCode");
    }

    // ------------------------------------------------------------------ the "many lords" pattern

    @Test
    public void manyLords_removeAndReapplyWithReadAfterEveryWrite_matchesTreeBasedTable() {
        final int lords = 14, cards = 12, checks = 25;
        for (long seed = 1; seed <= 5; seed++) {
            Random rnd = new Random(seed);
            List<FSnapshotTable<Long, Long, String>> tables = new ArrayList<>();
            List<Table<Long, Long, String>> refs = new ArrayList<>();
            for (int i = 0; i < cards; i++) {
                tables.add(FSnapshotTable.<Long, Long, String>createTree());
                refs.add(TreeBasedTable.<Long, Long, String>create());
            }
            long[] previousTimestamp = new long[lords];
            Arrays.fill(previousTimestamp, -1);
            long clock = 0;

            for (int check = 0; check < checks; check++) {
                // like GameAction.checkStaticAbilities: forget what every lord did in the last pass ...
                for (int i = 0; i < cards; i++) {
                    for (int lord = 0; lord < lords; lord++) {
                        if (previousTimestamp[lord] >= 0) {
                            eq(tables.get(i).remove(previousTimestamp[lord], (long) lord), refs.get(i).remove(previousTimestamp[lord], (long) lord), "remove");
                        }
                    }
                }
                // ... and apply them again; some effects share a timestamp (same row, several columns)
                List<Integer> order = new ArrayList<>();
                for (int lord = 0; lord < lords; lord++) order.add(lord);
                Collections.shuffle(order, rnd);
                for (int lord : order) {
                    long ts = rnd.nextInt(4) == 0 ? clock : ++clock;
                    previousTimestamp[lord] = ts;
                    for (int i = 0; i < cards; i++) {
                        if (rnd.nextInt(4) == 0) continue;                         // not every lord affects every card
                        String v = "p" + check + "/" + lord + "/" + i;
                        eq(tables.get(i).put(ts, (long) lord, v), refs.get(i).put(ts, (long) lord, v), "put");
                        // the read after every write (updating the view of the card)
                        eq(new ArrayList<>(tables.get(i).values()), new ArrayList<>(refs.get(i).values()), "values after put, check " + check);
                    }
                }
                for (int i = 0; i < cards; i++) assertSame(tables.get(i), refs.get(i), "seed=" + seed + " check=" + check + " card=" + i);
            }
        }
    }

    // ------------------------------------------------------------------ big randomized differential

    @Test
    public void differential_bigTablesWithManySharedRowsAndColumns() {
        for (long seed = 1; seed <= 12; seed++) {
            Random rnd = new Random(seed);
            boolean stringKeys = seed % 2 == 0;
            FSnapshotTable<Comparable, Comparable, String> t = FSnapshotTable.createTree();
            Table<Comparable, Comparable, String> ref = TreeBasedTable.create();
            for (int step = 0; step < 350; step++) {
                String ctx = "seed=" + seed + " step=" + step;
                Comparable r = key(stringKeys, rnd.nextInt(30)), c = key(stringKeys, rnd.nextInt(30));
                switch (rnd.nextInt(12)) {
                    case 0: case 1: case 2: case 3: case 4: {
                        String v = "v" + rnd.nextInt(100000);
                        eq(t.put(r, c, v), ref.put(r, c, v), ctx + " put");
                        break;
                    }
                    case 5: case 6:
                        eq(t.remove(r, c), ref.remove(r, c), ctx + " remove");
                        break;
                    case 7: {
                        Table<Comparable, Comparable, String> other = TreeBasedTable.create();
                        for (int i = 0; i < 1 + rnd.nextInt(25); i++) other.put(key(stringKeys, rnd.nextInt(30)), key(stringKeys, rnd.nextInt(30)), "o" + rnd.nextInt(1000));
                        t.putAll(other);
                        ref.putAll(other);
                        break;
                    }
                    case 8:
                        eq(t.rowKeySet().remove(r), ref.rowKeySet().remove(r), ctx + " rowKeySet.remove");
                        break;
                    case 9:
                        eq(t.columnKeySet().removeAll(Collections.singleton(c)), ref.columnKeySet().removeAll(Collections.singleton(c)), ctx + " columnKeySet.removeAll");
                        break;
                    case 10: {
                        final int mod = 2 + rnd.nextInt(5);
                        eq(t.rowKeySet().removeIf(k -> hashOf(k) % mod == 0), ref.rowKeySet().removeIf(k -> hashOf(k) % mod == 0), ctx + " rowKeySet.removeIf");
                        break;
                    }
                    default:
                        if (rnd.nextInt(20) == 0) { t.clear(); ref.clear(); }
                }
                if (step % 5 == 0) assertSame(t, ref, ctx);
            }
            assertSame(t, ref, "seed=" + seed + " final");
        }
    }

    private static Comparable key(boolean string, int i) {
        return string ? (Comparable) String.format("k%03d", i) : (Comparable) Long.valueOf(i);
    }

    private static int hashOf(Object o) {
        return Math.abs(o.hashCode());
    }

    @Test
    public void outOfOrderInserts_endUpSorted() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        Table<Long, Long, String> ref = TreeBasedTable.create();
        for (long r = 9; r >= 0; r--) {
            for (long c = 0; c < 6; c++) {
                long cc = (c * 5) % 6;
                t.put(r, cc, "v" + r + "." + cc);
                ref.put(r, cc, "v" + r + "." + cc);
            }
        }
        assertSame(t, ref, "descending rows, shuffled columns");
    }

    @Test
    public void putAll_mergesAndOverwritesInOneStep() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        Table<Long, Long, String> ref = TreeBasedTable.create();
        for (long i = 0; i < 10; i++) { t.put(i, i, "a" + i); ref.put(i, i, "a" + i); }
        Table<Long, Long, String> incoming = TreeBasedTable.create();
        incoming.put(3L, 3L, "overwritten");
        incoming.put(-1L, 7L, "new first");
        incoming.put(50L, 1L, "new last");
        incoming.put(5L, 4L, "new middle");
        Object before = t.version();
        t.putAll(incoming);
        ref.putAll(incoming);
        assertSame(t, ref, "after putAll");
        Assert.assertTrue(t.version() != before, "putAll changed the table, so the version changes ...");
        Object after = t.version();
        t.putAll(incoming);                                      // the very same value instances again: nothing changes
        Assert.assertTrue(t.version() == after, "... exactly once");
    }

    // ------------------------------------------------------------------ snapshot semantics

    @Test
    public void iteratorsAndViewsTakenBeforeAWriteKeepWalkingTheirSnapshot() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        for (long i = 0; i < 6; i++) { t.put(i % 2, i, "v" + i); }
        List<String> valuesBefore = new ArrayList<>(t.values());
        List<Table.Cell<Long, Long, String>> cellsBefore = new ArrayList<>(t.cellSet());
        List<Long> rowsBefore = new ArrayList<>(t.rowKeySet());
        List<Long> columnsBefore = new ArrayList<>(t.columnKeySet());
        Map<Long, String> rowBefore = new java.util.LinkedHashMap<>(t.row(0L));

        Iterator<String> values = t.values().iterator();
        Iterator<Table.Cell<Long, Long, String>> cells = t.cellSet().iterator();
        Iterator<Long> rows = t.rowKeySet().iterator();
        Iterator<Long> columns = t.columnKeySet().iterator();
        Map<Long, String> row = t.row(0L);

        t.clear();                                               // writes of every kind while the iterators are open
        for (long i = 100; i < 120; i++) t.put(i, i, "later" + i);
        t.rowKeySet().removeIf(k -> k % 2 == 0);

        eq(drain(values), valuesBefore, "values iterator");
        eq(drain(cells), cellsBefore, "cellSet iterator");
        eq(drain(rows), rowsBefore, "rowKeySet iterator");
        eq(drain(columns), columnsBefore, "columnKeySet iterator");
        eq(new java.util.LinkedHashMap<>(row), rowBefore, "row view");
    }

    private static <T> List<T> drain(Iterator<T> it) {
        List<T> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    @Test
    public void versionTokensAreNeverReusedForADifferentState() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        seen.put(t.version(), true);
        int changes = 0;
        String[] values = {"a", "b"};
        for (int round = 0; round < 6; round++) {                // empty -> filled -> empty -> ... : equal content, new token each time
            t.put(1L, 1L, values[round % 2]);
            seen.put(t.version(), true);
            changes++;
            t.remove(1L, 1L);
            seen.put(t.version(), true);
            changes++;
        }
        eq(seen.size(), changes + 1, "every effective write must produce a token never seen before");
    }

    @Test
    public void keySetBulkRemoval_isAtomicWhenThePredicateThrows() {
        for (boolean tree : new boolean[]{true, false}) {
            FSnapshotTable<Long, Long, String> t = tree ? FSnapshotTable.<Long, Long, String>createTree() : FSnapshotTable.<Long, Long, String>createHash();
            for (long i = 0; i < 6; i++) t.put(i, i + 10, "v" + i);
            List<Table.Cell<Long, Long, String>> before = new ArrayList<>(t.cellSet());
            Object version = t.version();
            try {
                t.rowKeySet().removeIf(k -> { if (k == 4) throw new IllegalStateException("boom"); return k < 2; });
                Assert.fail("the predicate's exception must propagate");
            } catch (IllegalStateException expected) {
                // fine
            }
            eq(new HashSet<>(t.cellSet()), new HashSet<>(before), (tree ? "tree" : "hash") + ": nothing may be half removed");
            Assert.assertTrue(t.version() == version, (tree ? "tree" : "hash") + ": and the version must not change");
        }
    }

    @Test
    public void foreignKeyTypesAreSimplyNotFound() {
        FSnapshotTable<Long, Long, String> typed = FSnapshotTable.createTree();
        typed.put(1L, 2L, "a");
        @SuppressWarnings("rawtypes")
        Table raw = typed;
        Assert.assertNull(raw.get("x", 2L));
        Assert.assertNull(raw.get(1L, "x"));
        Assert.assertFalse(raw.contains("x", "y"));
        Assert.assertFalse(raw.containsRow("x"));
        Assert.assertFalse(raw.containsColumn("x"));
        Assert.assertNull(raw.remove("x", 2L));
        Assert.assertTrue(raw.row("x").isEmpty());
        Assert.assertNull(raw.get(null, 2L));
        eq(typed.size(), 1, "nothing changed");
    }

    @Test
    public void emptyTable_sharesTheEmptyValuesListAndStaysUsable() {
        FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        Assert.assertTrue(t.values() == FSnapshotTable.<Long, Long, String>createHash().values(), "one shared empty list");
        t.put(1L, 1L, "a");
        Assert.assertEquals(t.values().size(), 1);
        t.remove(1L, 1L);
        Assert.assertTrue(t.values() == Collections.<String>emptyList(), "back to the shared empty list");
        Assert.assertTrue(t.cellSet().isEmpty() && t.rowKeySet().isEmpty() && t.columnKeySet().isEmpty() && t.row(1L).isEmpty() && t.column(1L).isEmpty());
        Assert.assertEquals(t.toString(), "{}");
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    public void concurrentReaders_alwaysSeeASortedConsistentSnapshot() throws Exception {
        final FSnapshotTable<Long, Long, String> t = FSnapshotTable.createTree();
        final ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);
        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<?>> readers = new ArrayList<>();
        Future<?> writer = pool.submit(() -> {
            try {
                start.await();
                Random rnd = new Random(7);
                for (int i = 0; i < 20000; i++) {
                    long r = rnd.nextInt(40), c = rnd.nextInt(40);
                    if (rnd.nextInt(3) == 0) t.remove(r, c); else t.put(r, c, "v" + i);
                    if (i % 1500 == 0) t.rowKeySet().removeIf(k -> k % 7 == 0);
                }
            } catch (Throwable e) { errors.add(e); }
        });
        for (int r = 0; r < 4; r++) {
            readers.add(pool.submit(() -> {
                try {
                    start.await();
                    while (!done.get()) {
                        long lastRow = Long.MIN_VALUE, lastCol = Long.MIN_VALUE;
                        int n = 0;
                        for (Table.Cell<Long, Long, String> cell : t.cellSet()) {             // one snapshot: must be strictly ascending
                            boolean ascending = cell.getRowKey() > lastRow || (cell.getRowKey() == lastRow && cell.getColumnKey() > lastCol);
                            if (!ascending) throw new AssertionError("cellSet not sorted: " + cell + " after (" + lastRow + "," + lastCol + ")");
                            lastRow = cell.getRowKey();
                            lastCol = cell.getColumnKey();
                            n++;
                        }
                        for (String s : t.values()) Objects.requireNonNull(s);
                        long previous = Long.MIN_VALUE;
                        for (Long row : t.rowKeySet()) {
                            if (row <= previous) throw new AssertionError("rowKeySet not strictly ascending");
                            previous = row;
                        }
                    }
                } catch (Throwable e) { errors.add(e); }
            }));
        }
        start.countDown();
        writer.get(60, TimeUnit.SECONDS);
        done.set(true);
        for (Future<?> f : readers) f.get(60, TimeUnit.SECONDS);
        pool.shutdownNow();
        Assert.assertTrue(errors.isEmpty(), "no reader may see a torn or unsorted snapshot: " + errors);
    }
}
