package forge.gamesimulationtests.util;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import forge.util.collect.FCollection;
import forge.util.collect.FSnapshotTable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Micro benchmarks for the collections of the ANDROID-OPTIMIZE patch. Not a test: run main() from the IDE
 * (desktop JVM numbers; compare the variants with each other, absolute numbers on Android differ).
 *
 * <ul>
 * <li>FCollection: building the result of Game.getCardsIn (addAll vs addAllUnique) and the iteration patterns
 *     (finished loop, find-first with break, then modify).</li>
 * <li>Tables: "many lords". One state-effects pass removes the effects of the last pass and applies them again,
 *     reading all values after every write (that is what updating the view of a card does). Compared:
 *     a plain TreeBasedTable (what the code used before), a copy-on-write TreeBasedTable (the first version of
 *     FSnapshotTable) and the sorted-array FSnapshotTable.</li>
 * </ul>
 */
public final class PerfPatchBenchmark {
    private PerfPatchBenchmark() { }

    private static final com.sun.management.ThreadMXBean MX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static long sink;

    /** Best of 7 timed rounds after a warm-up; prints time and allocated bytes per operation. */
    private static void measure(String name, int warmup, int ops, Runnable r) {
        for (int i = 0; i < warmup; i++) r.run();
        long best = Long.MAX_VALUE, alloc = 0;
        for (int round = 0; round < 7; round++) {
            long a0 = MX.getCurrentThreadAllocatedBytes(), t0 = System.nanoTime();
            for (int i = 0; i < ops; i++) r.run();
            long dt = System.nanoTime() - t0;
            if (dt < best) { best = dt; alloc = MX.getCurrentThreadAllocatedBytes() - a0; }
        }
        System.out.printf("  %-46s %10.1f ns/op %10.1f B/op%n", name, best / (double) ops, alloc / (double) ops);
    }

    private static final class Card {
        final int id;
        Card(int id) { this.id = id; }
        @Override public int hashCode() { return id; }
        @Override public boolean equals(Object o) { return o instanceof Card && ((Card) o).id == id; }
    }

    private static FCollection<Card> cards(int from, int n) {
        FCollection<Card> c = new FCollection<>();
        for (int i = 0; i < n; i++) c.add(new Card(from + i));
        return c;
    }

    // ------------------------------------------------------------------ FCollection

    static void collections() {
        System.out.println("FCollection");
        final List<FCollection<Card>> zones = new ArrayList<>();
        for (int z = 0; z < 4; z++) zones.add(cards(z * 100, 50));

        measure("merge 4 zones x 50 cards with addAll (dedup)", 20000, 20000, () -> {
            FCollection<Card> r = new FCollection<>();
            for (FCollection<Card> z : zones) r.addAll((Collection<Card>) z);
            sink += r.size();
        });
        measure("merge 4 zones x 50 cards with addAllUnique", 20000, 20000, () -> {
            FCollection<Card> r = new FCollection<>();
            for (FCollection<Card> z : zones) r.addAllUnique(z);
            sink += r.size();
        });

        for (int n : new int[]{6, 20, 200}) {
            final FCollection<Card> fc = cards(1000, n);
            measure("for-each over " + n + " cards, runs to the end", 30000, 20000, () -> {
                long x = 0;
                for (Card c : fc) x += c.id;
                sink += x;
            });
        }
        for (int n : new int[]{20, 200}) {
            final FCollection<Card> fc = cards(1000, n);
            final Card extra = new Card(999999);
            measure("find-first (break), then add+remove, n=" + n, 30000, 20000, () -> {
                for (Card c : fc) { if (c.id == 1003) break; }
                fc.add(extra);
                fc.remove(extra);
            });
            measure("full loop, then add+remove, n=" + n, 30000, 20000, () -> {
                long x = 0;
                for (Card c : fc) x += c.id;
                sink += x;
                fc.add(extra);
                fc.remove(extra);
            });
        }
        final FCollection<Card> removal = cards(0, 50);
        measure("iterate a copy and it.remove() 5 of 50", 30000, 20000, () -> {
            FCollection<Card> fc = removal.clone();
            int k = 0;
            for (Iterator<Card> it = fc.iterator(); it.hasNext(); ) { it.next(); if (k++ % 10 == 0) it.remove(); }
            sink += fc.size();
        });
    }

    // ------------------------------------------------------------------ tables

    /** The operations the "many lords" workload needs, so that the three implementations can be driven alike. */
    private interface Tab {
        void put(long row, long column, Integer value);
        void remove(long row, long column);
        Iterable<Integer> values();
    }

    private static Tab plain() {
        final Table<Long, Long, Integer> t = TreeBasedTable.create();
        return new Tab() {
            @Override public void put(long r, long c, Integer v) { t.put(r, c, v); }
            @Override public void remove(long r, long c) { t.remove(r, c); }
            @Override public Iterable<Integer> values() { return t.values(); }
        };
    }

    /** What the first version of FSnapshotTable did: copy the whole Guava table on every write. */
    private static Tab copyOnWrite() {
        return new Tab() {
            private volatile Table<Long, Long, Integer> cur = TreeBasedTable.create();
            @Override public void put(long r, long c, Integer v) {
                if (cur.get(r, c) == v) return;
                Table<Long, Long, Integer> next = TreeBasedTable.create((TreeBasedTable<Long, Long, Integer>) cur);
                next.put(r, c, v);
                cur = next;
            }
            @Override public void remove(long r, long c) {
                if (!cur.contains(r, c)) return;
                Table<Long, Long, Integer> next = TreeBasedTable.create((TreeBasedTable<Long, Long, Integer>) cur);
                next.remove(r, c);
                cur = next;
            }
            @Override public Iterable<Integer> values() { return cur.values(); }
        };
    }

    private static Tab sortedArray() {
        final FSnapshotTable<Long, Long, Integer> t = FSnapshotTable.createTree();
        return new Tab() {
            @Override public void put(long r, long c, Integer v) { t.put(r, c, v); }
            @Override public void remove(long r, long c) { t.remove(r, c); }
            @Override public Iterable<Integer> values() { return t.values(); }
        };
    }

    private static void pass(List<Tab> cards, int lords, long[] previous, long[] clock) {
        for (Tab t : cards) {
            for (int l = 0; l < lords; l++) if (previous[l] >= 0) t.remove(previous[l], l);
        }
        for (int l = 0; l < lords; l++) {
            final long ts = ++clock[0];
            previous[l] = ts;
            final Integer v = (int) ts;
            for (Tab t : cards) {
                t.put(ts, l, v);
                int sum = 0;                                  // updating the view of the card reads every value
                for (Integer x : t.values()) sum += x;
                sink += sum;
            }
        }
    }

    private static void lords(String name, Supplier<Tab> factory, final int lords, final int creatures) {
        final List<Tab> cards = new ArrayList<>();
        for (int i = 0; i < creatures; i++) cards.add(factory.get());
        final long[] previous = new long[lords];
        Arrays.fill(previous, -1);
        final long[] clock = {0};
        final int ops = Math.max(30, 20000 / (lords * creatures));
        long best = Long.MAX_VALUE, alloc = 0;
        for (int i = 0; i < 300; i++) pass(cards, lords, previous, clock);          // warm-up
        for (int round = 0; round < 7; round++) {
            long a0 = MX.getCurrentThreadAllocatedBytes(), t0 = System.nanoTime();
            for (int i = 0; i < ops; i++) pass(cards, lords, previous, clock);
            long dt = System.nanoTime() - t0;
            if (dt < best) { best = dt; alloc = MX.getCurrentThreadAllocatedBytes() - a0; }
        }
        System.out.printf("  %-46s %10.1f us/pass %10.1f KB/pass%n", name, best / 1000.0 / ops, alloc / 1024.0 / ops);
    }

    static void tables() {
        System.out.println("Tables: one state-effects pass = remove all effects, re-apply all, read after every write");
        for (int[] c : new int[][]{{5, 20}, {1, 10}, {2, 20}, {5, 20}, {10, 30}, {20, 30}, {30, 40}}) {   // the first case only warms up the JIT
            System.out.println(" lords=" + c[0] + " creatures=" + c[1]);
            lords("plain TreeBasedTable (before the PR)", PerfPatchBenchmark::plain, c[0], c[1]);
            lords("copy-on-write TreeBasedTable (PR version)", PerfPatchBenchmark::copyOnWrite, c[0], c[1]);
            lords("FSnapshotTable (sorted array)", PerfPatchBenchmark::sortedArray, c[0], c[1]);
        }
    }

    public static void main(String[] args) {
        collections();
        tables();
        System.out.println("(sink " + sink + ")");
    }
}
