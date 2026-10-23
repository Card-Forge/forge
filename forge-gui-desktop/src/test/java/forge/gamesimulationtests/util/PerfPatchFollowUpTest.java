package forge.gamesimulationtests.util;

import forge.util.collect.FCollection;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Follow-up to PerfPatchUnitTest: pins the FCollection fixes made on top of PR #12297
 * (set() membership sync, atomic removeIf, int-constructor hijack, addAllUnique, version(), single-object fail-fast
 * iterator, ensureCapacity on shared copies / the EMPTY singleton). Pure unit tests: no game needed.
 */
public class PerfPatchFollowUpTest {

    /** Elements whose hashCode collides a lot (id % 5), to stress linear probing and backward-shift deletion. */
    private static final class Coll {
        final int id;
        Coll(int id) { this.id = id; }
        @Override public int hashCode() { return id % 5; }
        @Override public boolean equals(Object o) { return o instanceof Coll && ((Coll) o).id == id; }
        @Override public String toString() { return "C" + id; }
    }

    private static final List<IntFunction<Object>> MAKERS = Arrays.<IntFunction<Object>>asList(i -> "e" + i, Coll::new);

    private static void assertSameMembership(FCollection<?> actual, List<?> expected, int domain, IntFunction<Object> mk, String ctx) {
        Assert.assertEquals(new ArrayList<Object>(actual), new ArrayList<Object>(expected), ctx + ": content/order");
        Set<Object> set = new HashSet<Object>(expected);
        for (int k = 0; k < domain; k++) {
            Object probe = mk.apply(k);
            Assert.assertEquals(actual.contains(probe), set.contains(probe), ctx + ": contains(" + probe + ")");
        }
    }

    // ------------------------------------------------------------------ set()

    @Test
    public void set_keepsTheMembershipTableInSync() {
        FCollection<String> fc = new FCollection<>();
        for (int i = 0; i < 100; i++) fc.add("s" + i);
        Assert.assertTrue(fc.contains("s1"), "forces the table to exist");
        Assert.assertEquals(fc.set(0, "NEW"), "s0");
        Assert.assertTrue(fc.contains("NEW"), "new element must be found");
        Assert.assertFalse(fc.contains("s0"), "replaced element must be gone");
        Assert.assertTrue(fc.add("s0"), "replaced element can be added again");
        Assert.assertFalse(fc.add("NEW"), "new element is a member, so adding it again is a duplicate");
    }

    @Test
    public void set_onAShareDoesNotTouchTheMaster() {
        FCollection<String> master = new FCollection<>(Arrays.asList("a", "b", "c"));
        FCollection<String> copy = master.shareCopy();
        copy.set(0, "z");
        Assert.assertEquals(new ArrayList<String>(master), Arrays.asList("a", "b", "c"));
        Assert.assertEquals(new ArrayList<String>(copy), Arrays.asList("z", "b", "c"));
    }

    // ------------------------------------------------------------------ removeIf

    @Test
    public void removeIf_leavesTheCollectionUntouchedWhenThePredicateThrows() {
        FCollection<Integer> nums = new FCollection<>();
        for (int i = 0; i < 20; i++) nums.add(i);
        List<Integer> before = new ArrayList<Integer>(nums);
        int versionBefore = nums.version();
        try {
            nums.removeIf(x -> {
                if (x == 15) throw new IllegalStateException("boom");
                return x % 2 == 0;
            });
            Assert.fail("the predicate's exception must propagate");
        } catch (IllegalStateException expected) {
            // fine
        }
        Assert.assertEquals(new ArrayList<Integer>(nums), before, "no element may be lost or duplicated");
        Assert.assertEquals(nums.version(), versionBefore, "nothing changed, so the version must not change");
        Assert.assertTrue(nums.contains(2) && nums.contains(15));
    }

    @Test
    public void removeIf_matchesAnArrayListModel() {
        for (long seed = 1; seed <= 150; seed++) {
            Random r = new Random(seed);
            int n = r.nextInt(150);
            FCollection<Integer> fc = new FCollection<>();
            List<Integer> ref = new ArrayList<>();
            for (int i = 0; i < n; i++) { fc.add(i); ref.add(i); }
            if (r.nextBoolean()) fc.contains(0);                      // sometimes with a table, sometimes without
            final int mod = 1 + r.nextInt(7);
            Assert.assertEquals(fc.removeIf(x -> x % mod == 0), ref.removeIf(x -> x % mod == 0), "result, seed=" + seed);
            assertSameMembership(fc, ref, n + 3, i -> i, "removeIf seed=" + seed);
            Assert.assertEquals(fc.add(0), !ref.contains(0), "add after removeIf, seed=" + seed);
        }
        Assert.assertFalse(new FCollection<>(Arrays.asList(1, 2, 3)).removeIf(x -> false));
        Assert.assertFalse(new FCollection<Integer>().removeIf(x -> true));
    }

    @Test
    public void everyMutator_onAShareLeavesTheMasterAlone() {
        // The copy-on-write contract: whatever is done to a share, the collection it came from must not change.
        // (PerfPatchUnitTest checks this with random operations, this one names each mutator so that a missing
        // ownList() points straight at the culprit.)
        List<String> names = new ArrayList<>();
        List<java.util.function.Consumer<FCollection<String>>> mutators = new ArrayList<>();
        mutators.add(c -> c.add("new")); names.add("add");
        mutators.add(c -> c.remove("b")); names.add("remove(Object)");
        mutators.add(c -> c.remove(1)); names.add("remove(int)");
        mutators.add(c -> c.set(1, "z")); names.add("set");
        mutators.add(c -> c.replace(1, "z")); names.add("replace");
        mutators.add(c -> c.add(1, "ins")); names.add("add(int, T)");
        mutators.add(c -> c.addAll(Arrays.asList("p", "q"))); names.add("addAll(Collection)");
        mutators.add(c -> c.addAll(new String[]{"p"})); names.add("addAll(T[])");
        mutators.add(c -> c.addAllUnique(Arrays.asList("p", "q"))); names.add("addAllUnique");
        mutators.add(c -> c.removeIf(x -> x.equals("b"))); names.add("removeIf (match)");
        mutators.add(c -> c.removeAll(Arrays.asList("b"))); names.add("removeAll");
        mutators.add(c -> c.retainAll(Arrays.asList("a"))); names.add("retainAll");
        mutators.add(c -> c.sort(Comparator.<String>reverseOrder())); names.add("sort");
        mutators.add(c -> c.clear()); names.add("clear");
        mutators.add(c -> c.ensureCapacity(500)); names.add("ensureCapacity");
        mutators.add(c -> { Iterator<String> it = c.iterator(); it.next(); it.remove(); }); names.add("iterator.remove");
        mutators.add(c -> { Iterator<String> it = c.listIterator(); it.next(); it.remove(); }); names.add("listIterator.remove");
        mutators.add(c -> { Iterator<String> it = c.asSet().iterator(); it.next(); it.remove(); }); names.add("asSet().iterator.remove");
        mutators.add(c -> c.asSet().remove("b")); names.add("asSet().remove");
        for (int i = 0; i < mutators.size(); i++) {
            for (int size : new int[]{3, 100}) {                               // below and above the hash table threshold
                List<String> content = new ArrayList<>();
                content.add("a"); content.add("b"); content.add("c");
                for (int k = 3; k < size; k++) content.add("e" + k);
                FCollection<String> master = new FCollection<>(content);
                FCollection<String> share = master.shareCopy();
                mutators.get(i).accept(share);
                Assert.assertEquals(new ArrayList<String>(master), content, names.get(i) + " on a share changed the master (size " + size + ")");
                Assert.assertTrue(master.contains("b") && master.contains("a"), names.get(i) + ": master membership (size " + size + ")");
            }
        }
    }

    // ------------------------------------------------------------------ constructors

    @Test
    public void intLiteralConstructor_noLongerHijacksTheElementConstructor() {
        FCollection<Integer> five = new FCollection<Integer>(5);          // must be "a collection containing 5"
        Assert.assertEquals(five.size(), 1);
        Assert.assertTrue(five.contains(5));

        for (Constructor<?> c : FCollection.class.getConstructors()) {   // getConstructors() = public ones only
            Class<?>[] p = c.getParameterTypes();
            Assert.assertFalse(p.length == 1 && p[0] == int.class, "a public FCollection(int) is a trap for FCollection<Integer>");
        }
    }

    @Test
    public void withCapacity_createsAnEmptyUsableCollection() {
        FCollection<Integer> cap = FCollection.withCapacity(5);
        Assert.assertEquals(cap.size(), 0);
        Assert.assertTrue(cap.add(5));
        Assert.assertTrue(cap.contains(5));
        Assert.assertEquals(cap.size(), 1);
    }

    // ------------------------------------------------------------------ ensureCapacity

    @Test
    public void ensureCapacity_doesNotDisturbSharedCopiesOrTheEmptySingleton() {
        FCollection<String> master = new FCollection<>(Arrays.asList("a", "b", "c"));
        FCollection<String> share = master.shareCopy();
        share.ensureCapacity(1000);
        share.add("z");
        Assert.assertEquals(new ArrayList<String>(master), Arrays.asList("a", "b", "c"), "master untouched");
        Assert.assertEquals(new ArrayList<String>(share), Arrays.asList("a", "b", "c", "z"));

        FCollection<String> empty = FCollection.getEmpty();
        empty.ensureCapacity(100000);
        Assert.assertEquals(empty.size(), 0);
        Assert.assertFalse(empty.add("x"));
        Assert.assertFalse(empty.addAllUnique(Arrays.asList("x")), "the shared EMPTY refuses appends");
        Assert.assertEquals(empty.size(), 0);
    }

    // ------------------------------------------------------------------ addAllUnique

    @Test
    public void addAllUnique_appendsInOrderAndKeepsMembershipCorrect() {
        for (int total : new int[]{0, 1, 5, 9, 40, 64, 65, 200}) {
            for (boolean preTable : new boolean[]{false, true}) {
                for (IntFunction<Object> mk : MAKERS) {
                    String ctx = "total=" + total + " preTable=" + preTable;
                    FCollection<Object> fc = new FCollection<>();
                    List<Object> ref = new ArrayList<>();
                    int pre = preTable ? 20 : 3;
                    for (int i = 0; i < pre; i++) { fc.add(mk.apply(i)); ref.add(mk.apply(i)); }
                    if (preTable) fc.contains(mk.apply(0));                  // make sure the table exists

                    List<Object> chunk = new ArrayList<>();
                    for (int i = 0; i < total; i++) chunk.add(mk.apply(1000 + i));
                    chunk.add(null);                                         // nulls are skipped like everywhere else
                    Assert.assertEquals(fc.addAllUnique(chunk), total > 0, ctx);
                    ref.addAll(chunk.subList(0, total));
                    assertSameMembership(fc, ref, 1000 + total + 5, mk, ctx);
                    for (Object o : chunk) {
                        if (o != null) Assert.assertFalse(fc.add(o), ctx + ": appended element must be a known member");
                    }

                    FCollection<Object> source = new FCollection<>();         // FCollection source takes the indexed path
                    for (int i = 0; i < total; i++) source.add(mk.apply(5000 + i));
                    fc.addAllUnique(source);
                    for (int i = 0; i < total; i++) ref.add(mk.apply(5000 + i));
                    assertSameMembership(fc, ref, 5000 + total + 5, mk, ctx + " (FCollection source)");

                    Object victim = ref.get(ref.size() / 2);
                    Assert.assertTrue(fc.remove(victim), ctx + ": remove after addAllUnique");
                    ref.remove(victim);
                    assertSameMembership(fc, ref, 5000 + total + 5, mk, ctx + " after remove");

                    Assert.assertFalse(fc.addAllUnique(fc), "appending a collection to itself is refused");
                    Assert.assertFalse(fc.addAllUnique(Collections.emptyList()));
                    Assert.assertFalse(fc.addAllUnique(null));
                }
            }
        }
    }

    @Test
    public void addAllUnique_onAShareLeavesTheMasterAlone() {
        FCollection<String> master = new FCollection<>(Arrays.asList("a", "b"));
        FCollection<String> copy = master.shareCopy();
        Assert.assertTrue(copy.addAllUnique(Arrays.asList("c", "d")));
        Assert.assertEquals(new ArrayList<String>(master), Arrays.asList("a", "b"));
        Assert.assertEquals(new ArrayList<String>(copy), Arrays.asList("a", "b", "c", "d"));
        Assert.assertTrue(copy.contains("d") && !master.contains("d"));
    }

    // ------------------------------------------------------------------ version()

    @Test
    public void version_changesOnEveryChangeAndOnlyThen() {
        final FCollection<String> t = new FCollection<>();
        for (int i = 0; i < 12; i++) t.add("x" + i);

        List<String> names = new ArrayList<>();
        List<Runnable> changers = new ArrayList<>();
        changers.add(() -> t.add("new1")); names.add("add");
        changers.add(() -> t.remove("new1")); names.add("remove(Object)");
        changers.add(() -> t.remove(0)); names.add("remove(int)");
        changers.add(() -> t.set(0, "setval")); names.add("set");
        changers.add(() -> t.replace(1, "repl")); names.add("replace");
        changers.add(() -> t.addAll(Arrays.asList("p1", "p2"))); names.add("addAll(Collection)");
        changers.add(() -> t.addAll(new String[]{"q1"})); names.add("addAll(T[])");
        changers.add(() -> t.addAllUnique(Arrays.asList("u1"))); names.add("addAllUnique");
        changers.add(() -> t.add(2, "ins")); names.add("add(int, T)");
        changers.add(() -> t.removeIf(x -> x.equals("ins"))); names.add("removeIf");
        changers.add(() -> t.removeAll(Arrays.asList("p1"))); names.add("removeAll");
        changers.add(() -> t.retainAll(new ArrayList<String>(t).subList(0, t.size() - 1))); names.add("retainAll");
        changers.add(() -> t.sort(Comparator.<String>reverseOrder())); names.add("sort");
        changers.add(() -> { Iterator<String> it = t.iterator(); it.next(); it.remove(); }); names.add("iterator.remove");
        changers.add(() -> t.listIterator()); names.add("listIterator");
        changers.add(() -> t.clear()); names.add("clear");
        for (int i = 0; i < changers.size(); i++) {
            int before = t.version();
            changers.get(i).run();
            Assert.assertTrue(t.version() != before, "version must change on " + names.get(i));
        }

        FCollection<String> n = new FCollection<>(Arrays.asList("a", "b", "c"));
        int v = n.version();
        n.add("a");                                       // duplicate
        n.remove("zzz");                                  // absent
        n.removeIf(x -> false);
        n.addAll(Arrays.asList("a", "b"));                // all duplicates
        n.retainAll(Arrays.asList("a", "b", "c"));        // keeps everything
        Assert.assertEquals(n.version(), v, "no-ops must not change the version");
        n.clear();
        n.clear();
        Assert.assertEquals(n.version(), v + 1, "only the first clear() changed anything");

        FCollection<String> share = n.shareCopy();
        int sv = share.version();
        share.add("w");
        Assert.assertTrue(share.version() != sv, "a share has its own version");
    }

    // ------------------------------------------------------------------ iterator

    private static List<Integer> drain(Iterator<Integer> it) {
        List<Integer> out = new ArrayList<>();
        while (it.hasNext()) out.add(it.next());
        return out;
    }

    private static Object backingList(FCollection<?> fc) throws Exception {
        java.lang.reflect.Field f = FCollection.class.getDeclaredField("list");
        f.setAccessible(true);
        return f.get(fc);
    }

    @Test
    public void iterator_neverThrowsConcurrentModification_andWalksASnapshot() {
        // A ConcurrentModificationException in the middle of a game is worse than a slightly stale loop.
        FCollection<Integer> fc = new FCollection<>();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 100; i++) { fc.add(i); expected.add(i); }
        fc.contains(1);                                    // table exists

        List<Integer> seen = new ArrayList<>();
        for (Integer x : fc) {
            seen.add(x);
            if (x == 10) {
                fc.add(1000);                              // structural changes while iterating ...
                fc.remove(Integer.valueOf(50));
                fc.removeIf(v -> v == 60);
                fc.set(3, 3000);
                fc.addAllUnique(Arrays.asList(2000, 2001));
                fc.sort(Comparator.<Integer>naturalOrder());
                expected.add(1000);
                expected.remove(Integer.valueOf(50));
                expected.remove(Integer.valueOf(60));
                expected.set(3, 3000);
                expected.addAll(Arrays.asList(2000, 2001));
                expected.sort(Comparator.<Integer>naturalOrder());
            }
        }
        List<Integer> original = new ArrayList<>();
        for (int i = 0; i < 100; i++) original.add(i);
        Assert.assertEquals(seen, original, "the loop sees the collection as it was when the loop started");
        Assert.assertEquals(new ArrayList<Integer>(fc), expected, "and every change really happened");
        assertSameMembership(fc, expected, 3100, i -> i, "after changes during iteration");

        // nested iteration over the same collection with changes in the inner loop
        FCollection<Integer> small = new FCollection<>(Arrays.asList(1, 2, 3, 4));
        int pairs = 0;
        for (Integer a : small) {
            for (Integer b : small) {
                pairs++;
                if (a == 1 && b == 1) small.add(99);
            }
        }
        // the running loops keep their 4-element snapshots; inner loops STARTED after the change see 5 elements
        Assert.assertEquals(pairs, 4 + 3 * 5, "each iterator walks the collection as it was when it was created");
        Assert.assertEquals(new ArrayList<Integer>(small), Arrays.asList(1, 2, 3, 4, 99));
    }

    @Test
    public void iterator_remove_removesFromTheLiveCollectionAndKeepsMembership() {
        FCollection<Integer> fc = new FCollection<>();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 100; i++) { fc.add(i); expected.add(i); }
        fc.contains(1);

        Iterator<Integer> it = fc.iterator();
        int index = 0;
        while (it.hasNext()) {
            Integer x = it.next();
            if (index++ % 3 == 0) { it.remove(); expected.remove(x); }
        }
        Assert.assertEquals(new ArrayList<Integer>(fc), expected);
        assertSameMembership(fc, expected, 110, i -> i, "after iterator.remove");
        Assert.assertTrue(fc.add(0), "a removed element can be added again");

        try {
            fc.iterator().remove();
            Assert.fail("remove() before next() must throw IllegalStateException");
        } catch (IllegalStateException expectedException) {
            // fine
        }
        Iterator<Integer> twice = fc.iterator();
        twice.next();
        twice.remove();
        try {
            twice.remove();
            Assert.fail("a second remove() for the same element must throw IllegalStateException");
        } catch (IllegalStateException expectedException) {
            // fine
        }
        Assert.assertFalse(new FCollection<Integer>().iterator().hasNext());
    }

    @Test
    public void iterator_abandonedLoopCostsAtMostOneCopy_finishedLoopCostsNone() throws Exception {
        FCollection<Integer> fc = new FCollection<>();
        for (int i = 0; i < 50; i++) fc.add(i);

        Object before = backingList(fc);
        for (Integer ignored : fc) { /* runs to its end */ }
        fc.add(500);
        Assert.assertSame(backingList(fc), before, "a finished loop must not make the next change copy the list");

        for (Integer x : fc) { if (x == 3) break; }            // abandoned (find-first style)
        Iterator<Integer> kept = fc.iterator();
        kept.next();
        fc.add(501);                                           // first change after the unfinished iterators: one copy
        Object afterCopy = backingList(fc);
        Assert.assertNotSame(afterCopy, before, "an unfinished iterator is protected by detaching the list");
        fc.add(502);
        Assert.assertSame(backingList(fc), afterCopy, "and the copy is made once, not on every change");

        List<Integer> rest = drain(kept);                      // the kept iterator still sees the old content
        Assert.assertEquals(rest.size(), 50, "51 elements existed when it started, one was already consumed");
        Assert.assertFalse(rest.contains(501) || rest.contains(502));
        Assert.assertEquals(fc.size(), 53);
    }

    @Test
    public void iterator_onAShareWalksTheSnapshotAndDetachesOnRemove() {
        FCollection<String> master = new FCollection<>(Arrays.asList("a", "b", "c", "d"));
        FCollection<String> copy = master.shareCopy();
        Iterator<String> it = copy.iterator();
        while (it.hasNext()) { if (it.next().equals("b")) it.remove(); }
        Assert.assertEquals(new ArrayList<String>(copy), Arrays.asList("a", "c", "d"));
        Assert.assertEquals(new ArrayList<String>(master), Arrays.asList("a", "b", "c", "d"));
    }

    // ------------------------------------------------------------------ randomized differential

    @Test
    @SuppressWarnings("unchecked")
    public void randomizedDifferential_includingAddAllUniqueAndSet() {
        for (long seed = 1; seed <= 60; seed++) {
            Random r = new Random(seed);
            IntFunction<Object> mk = MAKERS.get((int) (seed % 2));
            FCollection<Object> fc = new FCollection<>();
            List<Object> ref = new ArrayList<>();
            int fresh = 100000;
            for (int step = 0; step < 500; step++) {
                String ctx = "seed=" + seed + " step=" + step;
                Object e = mk.apply(r.nextInt(250));
                int version = fc.version();
                boolean mutated = false;
                switch (r.nextInt(9)) {
                    case 0: case 1: {
                        boolean expect = !ref.contains(e);
                        if (expect) ref.add(e);
                        Assert.assertEquals(fc.add(e), expect, ctx + " add");
                        mutated = expect;
                        break;
                    }
                    case 2: {
                        boolean expect = ref.remove(e);
                        Assert.assertEquals(fc.remove((Object) e), expect, ctx + " remove");
                        mutated = expect;
                        break;
                    }
                    case 3: {
                        List<Object> chunk = new ArrayList<>();
                        int k = r.nextInt(30);
                        for (int i = 0; i < k; i++) chunk.add(mk.apply(fresh++));
                        fc.addAllUnique(chunk);
                        ref.addAll(chunk);
                        mutated = k > 0;
                        break;
                    }
                    case 4:
                        if (!ref.isEmpty()) {
                            int idx = r.nextInt(ref.size());
                            Object replacement = mk.apply(fresh++);
                            ref.set(idx, replacement);
                            fc.set(idx, replacement);
                            mutated = true;
                        }
                        break;
                    case 5: {
                        final int mod = 2 + r.nextInt(6);
                        boolean expect = ref.removeIf(x -> Math.abs(x.hashCode()) % mod == 0);
                        Assert.assertEquals(fc.removeIf(x -> Math.abs(x.hashCode()) % mod == 0), expect, ctx + " removeIf");
                        mutated = expect;
                        break;
                    }
                    case 6: {
                        Iterator<Object> it = fc.iterator();
                        int skip = r.nextInt(5), j = 0;
                        while (it.hasNext()) {
                            Object x = it.next();
                            if (j++ == skip) { it.remove(); ref.remove(x); mutated = true; break; }
                        }
                        break;
                    }
                    case 7: {
                        List<Object> in = new ArrayList<>();
                        int k = r.nextInt(20);
                        for (int i = 0; i < k; i++) in.add(mk.apply(r.nextInt(250)));
                        boolean expect = false;
                        for (Object x : in) if (!ref.contains(x)) { ref.add(x); expect = true; }
                        Assert.assertEquals(fc.addAll((Collection<Object>) in), expect, ctx + " addAll");
                        mutated = expect;
                        break;
                    }
                    default:
                        if (r.nextInt(25) == 0) { mutated = !ref.isEmpty(); fc.clear(); ref.clear(); }
                }
                Assert.assertEquals(fc.version() != version, mutated, ctx + " version stamp vs. real change");
                if (step % 25 == 0) assertSameMembership(fc, ref, 250, mk, ctx);
            }
            assertSameMembership(fc, ref, 250, mk, "seed=" + seed + " final");
        }
    }
}
