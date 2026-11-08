package forge.game.undo;

import forge.game.mementotest.Model;
import forge.game.mementotest.Model.Item;
import forge.game.mementotest.Model.KeyedItem;
import forge.game.mementotest.Model.Root;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Validates the delta-based memento history: exact restores (single steps, jumps, branching), memory proportional to
 * what changed, byte accounting that never drifts, and refusal instead of corruption when a memento cannot be used.
 */
public class GameMementoTest {

    // ---- helpers

    private static Item item(Root r, int id) {
        final Item i = new Item(id);
        i.name = "item" + id;
        r.items.add(i);
        return i;
    }

    /** A graph with every kind of content. */
    private static Root sampleRoot() {
        final Root r = new Root();
        for (int i = 0; i < 6; i++) { item(r, i); }
        r.items.get(0).partner = r.items.get(1);
        r.items.get(2).kids.add(r.items.get(3));
        r.byName.put("a", r.items.get(0));
        r.marked.add(r.items.get(4));
        r.multi.put("m", r.items.get(1));
        r.multi.put("m", r.items.get(2));
        r.grid.put("r", "c", r.items.get(5));
        r.keyed.put(new KeyedItem(1), "one");
        r.fragile.add(r.items.get(0));
        r.current = r.items.get(2);
        r.ring[1] = r.items.get(3);
        return r;
    }

    private static String fp(Root r) {
        r.fragile.armed = false;
        return Model.fingerprint(r);
    }

    private static void assertAccounting(GameMemento.Recorder rec) {
        AssertJUnit.assertEquals("incremental byte count drifted", rec.recomputeLiveBytes(), rec.liveBytes());
        AssertJUnit.assertTrue("dense node arrays out of step with the registry", rec.checkDense());
    }

    /** The recorder must track exactly what is reachable: the same objects a brand new recorder would find. */
    private static void assertTracksExactlyWhatIsReachable(GameMemento.Recorder rec, Root r) {
        final GameMemento.Recorder fresh = new GameMemento.Recorder();
        fresh.captureStandalone(r);
        AssertJUnit.assertEquals("stale or missing tracked objects", fresh.registrySize(), rec.registrySize());
    }

    @Test
    public void manyObjectsDyingAtOnceAreAllForgotten() {
        final Root r = new Root();
        for (int i = 0; i < 40; i++) { item(r, i); }
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento m0 = rec.capture(r);
        final String p0 = fp(r);
        r.items.clear();                                       // forty items (and their arrays) unreachable at once
        final GameMemento m1 = rec.capture(r);
        assertTracksExactlyWhatIsReachable(rec, r);
        assertAccounting(rec);
        AssertJUnit.assertTrue(m0.restore());
        AssertJUnit.assertEquals(p0, fp(r));
        assertTracksExactlyWhatIsReachable(rec, r);
        AssertJUnit.assertNotNull(m1);
    }

    // ---- exact restore

    @Test
    public void restoreBringsBackEveryKindOfContent() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento m0 = rec.capture(r);
        final String before = fp(r);

        final Item x = r.items.get(0);
        x.value = 99; x.big = 1L << 40; x.flag = true; x.ratio = 2.5; x.letter = 'z'; x.name = "changed";
        x.slots[1] = 7;
        x.partner = null;
        r.items.remove(3);
        r.items.add(new Item(100));
        r.byName.clear();
        r.marked.add(r.items.get(0));
        r.multi.clear();
        r.grid.clear();
        r.keyed.clear();
        r.current = null;
        r.ring[1] = null;
        r.counter = 5;
        AssertJUnit.assertFalse(before.equals(fp(r)));

        final GameMemento m1 = rec.capture(r);
        AssertJUnit.assertTrue(m1.deltaSize() == 0);
        AssertJUnit.assertTrue(m0.deltaSize() > 0);
        AssertJUnit.assertTrue(m0.restore());
        AssertJUnit.assertEquals(before, fp(r));
        AssertJUnit.assertTrue(m0.problems.isEmpty());
        assertAccounting(rec);
    }

    @Test
    public void fieldsAreRestoredBeforeContainersSoHashCodesAreRight() {
        final Root r = new Root();
        final KeyedItem k = new KeyedItem(1);
        r.keyed.put(k, "v");
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento m0 = rec.capture(r);
        k.key = 2;                       // the map is now "broken": its entry sits in the wrong bucket
        rec.capture(r);
        AssertJUnit.assertTrue(m0.restore());
        AssertJUnit.assertEquals(1, k.key);
        AssertJUnit.assertEquals("v", r.keyed.get(k));
    }

    @Test
    public void multipleStepsBackAtOnce() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final List<GameMemento> ms = new ArrayList<>();
        final List<String> prints = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            r.items.get(i % 6).value = 10 + i;
            if (i % 3 == 0) { r.items.add(new Item(50 + i)); }
            if (i % 4 == 0) { r.byName.put("k" + i, r.items.get(i % 6)); }
            ms.add(rec.capture(r));
            prints.add(fp(r));
        }
        AssertJUnit.assertTrue(ms.get(2).restore());          // jump over five newer steps
        AssertJUnit.assertEquals(prints.get(2), fp(r));
        AssertJUnit.assertTrue(rec.isLatest(ms.get(2)));
        AssertJUnit.assertFalse(ms.get(5).restore());         // those steps are gone now ...
        AssertJUnit.assertEquals(prints.get(2), fp(r));       // ... and the refusal changed nothing
        AssertJUnit.assertTrue(ms.get(1).restore());
        AssertJUnit.assertEquals(prints.get(1), fp(r));
        assertAccounting(rec);
    }

    @Test
    public void objectsThatDisappearAndReappearComeBackRight() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final Item gone = r.items.get(5);
        gone.value = 11;
        final GameMemento m0 = rec.capture(r);
        final String p0 = fp(r);

        r.items.remove(gone);
        r.grid.clear();                                       // item 5 is now unreachable
        final GameMemento m1 = rec.capture(r);
        final String p1 = fp(r);

        gone.value = 22;                                      // it comes back with a different value
        r.items.add(gone);
        final GameMemento m2 = rec.capture(r);
        final String p2 = fp(r);

        AssertJUnit.assertTrue(m1.restore());
        AssertJUnit.assertEquals(p1, fp(r));
        AssertJUnit.assertTrue(m0.restore());
        AssertJUnit.assertEquals(p0, fp(r));
        AssertJUnit.assertEquals(11, gone.value);
        AssertJUnit.assertFalse(m2.restore());
        assertAccounting(rec);
        rec.capture(r);                                       // the chain continues normally after going back
        assertAccounting(rec);
        AssertJUnit.assertEquals(p0, fp(r));
        AssertJUnit.assertNotNull(p2);
    }

    @Test
    public void branchingAfterUndoKeepsOlderStatesRestorable() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento a = rec.capture(r);
        final String pa = fp(r);
        r.counter = 1;
        final GameMemento b = rec.capture(r);
        AssertJUnit.assertTrue(a.restore());                  // undo ...
        r.counter = 2;                                        // ... and take a different action
        r.items.get(1).value = 8;
        final GameMemento c = rec.capture(r);
        final String pc = fp(r);
        AssertJUnit.assertFalse(b.restore());                 // the abandoned branch is gone
        AssertJUnit.assertEquals(pc, fp(r));
        AssertJUnit.assertTrue(a.restore());
        AssertJUnit.assertEquals(pa, fp(r));
        AssertJUnit.assertNotNull(c);
        assertAccounting(rec);
    }

    @Test
    public void trimmingTheOldestStepsLeavesTheNewerOnesWorking() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        GameMemento[] ms = new GameMemento[6];
        final String[] prints = new String[6];
        for (int i = 0; i < 6; i++) {
            r.counter = i;
            ms[i] = rec.capture(r);
            prints[i] = fp(r);
        }
        ms[0] = null;                                         // what UndoManager does when it drops the oldest
        ms[1] = null;
        for (int i = 4; i >= 2; i--) {
            AssertJUnit.assertTrue(ms[i].restore());
            AssertJUnit.assertEquals(prints[i], fp(r));
        }
        assertAccounting(rec);
    }

    @Test
    public void randomisedOperationsAlwaysRestoreExactly() {
        for (int seed = 1; seed <= 40; seed++) { fuzz(seed); }
    }

    private void fuzz(final int seed) {
        final Random rnd = new Random(seed);
        final Root r = sampleRoot();
        final int[] nextId = { 1000 };
        final List<Item> pool = new ArrayList<>(r.items);     // every item ever made, reachable or not
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final List<GameMemento> stack = new ArrayList<>();
        final List<String> prints = new ArrayList<>();

        for (int step = 0; step < 300; step++) {
            final int what = rnd.nextInt(12);
            if (what < 6) {
                final int n = 1 + rnd.nextInt(4);
                for (int i = 0; i < n; i++) { mutate(r, rnd, pool, nextId); }
            } else if (what < 9) {
                stack.add(rec.capture(r));
                prints.add(fp(r));
                assertTracksExactlyWhatIsReachable(rec, r);
            } else if (what == 9 && stack.size() >= 2) {      // undo one step
                stack.remove(stack.size() - 1);
                prints.remove(prints.size() - 1);
                final GameMemento target = stack.get(stack.size() - 1);
                AssertJUnit.assertTrue("seed " + seed, target.restore());
                AssertJUnit.assertEquals("seed " + seed + " step " + step, prints.get(prints.size() - 1), fp(r));
                AssertJUnit.assertTrue(target.problems.isEmpty());
                assertTracksExactlyWhatIsReachable(rec, r);
            } else if (what == 10 && stack.size() >= 3) {     // jump back several steps
                final int k = 2 + rnd.nextInt(stack.size() - 2);
                for (int i = 0; i < k; i++) {
                    stack.remove(stack.size() - 1);
                    prints.remove(prints.size() - 1);
                }
                final GameMemento target = stack.get(stack.size() - 1);
                AssertJUnit.assertTrue("seed " + seed, target.restore());
                AssertJUnit.assertEquals("seed " + seed + " step " + step, prints.get(prints.size() - 1), fp(r));
            } else if (what == 11) {
                final int dice = rnd.nextInt(10);
                if (dice < 5 && stack.size() > 1) {            // trim the oldest
                    stack.remove(0);
                    prints.remove(0);
                } else if (dice < 8) {                         // barrier: history dropped, registry kept
                    stack.clear();
                    prints.clear();
                    rec.detach();
                } else if (dice == 8) {                        // memory pressure: everything dropped
                    stack.clear();
                    prints.clear();
                    rec.reset();
                }
            }
            assertAccounting(rec);
        }
        // whatever is left must still unwind completely, newest to oldest
        while (stack.size() >= 2) {
            stack.remove(stack.size() - 1);
            prints.remove(prints.size() - 1);
            AssertJUnit.assertTrue(stack.get(stack.size() - 1).restore());
            AssertJUnit.assertEquals(prints.get(prints.size() - 1), fp(r));
        }
    }

    private static void mutate(Root r, Random rnd, List<Item> pool, int[] nextId) {
        final List<Item> live = new ArrayList<>(r.items);
        final Item any = live.isEmpty() ? null : live.get(rnd.nextInt(live.size()));
        final Item old = pool.get(rnd.nextInt(pool.size()));   // may be unreachable right now
        switch (rnd.nextInt(16)) {
            case 0: if (any != null) { any.value = rnd.nextInt(); } break;
            case 1: if (any != null) { any.big = rnd.nextLong(); any.flag = rnd.nextBoolean(); any.ratio = rnd.nextDouble(); } break;
            case 2: if (any != null) { any.name = rnd.nextBoolean() ? null : "n" + rnd.nextInt(50); any.letter = (char) ('a' + rnd.nextInt(26)); } break;
            case 3: { final Item i = new Item(nextId[0]++); pool.add(i); r.items.add(i); } break;
            case 4: if (any != null) { r.items.remove(any); } break;
            case 5: r.byName.put("k" + rnd.nextInt(5), old); break;
            case 6: r.byName.remove("k" + rnd.nextInt(5)); break;
            case 7: if (rnd.nextBoolean()) { r.marked.add(old); } else { r.marked.remove(old); } break;
            case 8: if (rnd.nextBoolean()) { r.multi.put("m" + rnd.nextInt(3), old); } else { r.multi.remove("m" + rnd.nextInt(3), old); } break;
            case 9: if (rnd.nextBoolean()) { r.grid.put("r" + rnd.nextInt(3), "c" + rnd.nextInt(3), old); } else { r.grid.remove("r" + rnd.nextInt(3), "c" + rnd.nextInt(3)); } break;
            case 10: if (any != null) { any.partner = rnd.nextInt(4) == 0 ? null : old; } break;
            case 11: if (any != null) { any.slots[rnd.nextInt(3)] = rnd.nextInt(100); } break;
            case 12: r.ring[rnd.nextInt(4)] = rnd.nextInt(3) == 0 ? null : old; break;
            case 13: r.current = rnd.nextInt(3) == 0 ? null : old; r.counter++; break;
            case 14: if (any != null) { if (rnd.nextBoolean()) { any.kids.add(old); } else if (!any.kids.isEmpty()) { any.kids.remove(0); } } break;
            default: if (rnd.nextBoolean()) { r.fragile.add(old); } else if (!r.fragile.isEmpty()) { r.fragile.remove(0); } break;
        }
    }

    // ---- memory

    @Test
    public void reverseDeltasStoreOnlyChangedFieldsAndSequenceSplices() {
        final Root r = new Root();
        for (int i = 0; i < 200; i++) { item(r, i); }
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento before = rec.capture(r);
        final String fingerprint = fp(r);

        r.items.get(0).value++;
        final Item a = r.items.get(100);
        r.items.set(100, r.items.get(101));
        r.items.set(101, a);
        rec.capture(r);

        AssertJUnit.assertEquals(2, before.deltaSize());
        AssertJUnit.assertTrue("single-field and short-splice deltas should be compact: "
                + before.retainedBytes(), before.retainedBytes() < 512);
        AssertJUnit.assertTrue(before.restore());
        AssertJUnit.assertEquals(fingerprint, fp(r));
        assertAccounting(rec);
    }

    @Test
    public void unchangedGraphCostsNothing() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento m0 = rec.capture(r);
        final long live = rec.liveBytes();
        rec.capture(r);
        AssertJUnit.assertEquals(0, rec.lastAllocated);
        AssertJUnit.assertEquals(0, m0.deltaSize());
        AssertJUnit.assertEquals(live, rec.liveBytes());
    }

    @Test
    public void historyCostFollowsWhatChangedNotTheSizeOfTheGame() {
        final Root r = new Root();
        for (int i = 0; i < 20000; i++) { item(r, i); }
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final List<GameMemento> ms = new ArrayList<>();
        ms.add(rec.capture(r));
        final int objects = ms.get(0).objectCount();
        AssertJUnit.assertTrue(objects > 20000);

        final int steps = 50;
        for (int i = 1; i <= steps; i++) {
            r.items.get(i * 7).value = i;                      // exactly one object changes per step
            ms.add(rec.capture(r));
        }
        long deltaTotal = 0;
        for (int i = 0; i < steps; i++) {
            AssertJUnit.assertEquals("one changed object, one delta entry", 1, ms.get(i).deltaSize());
            deltaTotal += ms.get(i).retainedBytes();
        }
        final long fullCopies = (long) steps * objects * 16;   // what 50 mementos holding a pair of arrays would cost
        AssertJUnit.assertTrue("history " + deltaTotal + " bytes vs " + fullCopies, deltaTotal * 100 < fullCopies);
        AssertJUnit.assertTrue(rec.liveBytes() > deltaTotal * 100);
        assertAccounting(rec);

        for (int i = steps - 1; i >= 0; i--) {                 // and it still unwinds exactly
            AssertJUnit.assertTrue(ms.get(i).restore());
            AssertJUnit.assertEquals("not yet changed at step " + i, 0, r.items.get((i + 1) * 7).value);
            if (i >= 1) { AssertJUnit.assertEquals("changed at step " + i, i, r.items.get(i * 7).value); }
        }
        AssertJUnit.assertEquals(0, r.items.get(7).value);
    }

    @Test
    public void bytesAreGivenBackAfterReset() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        rec.capture(r);
        AssertJUnit.assertTrue(rec.liveBytes() > 0);
        rec.reset();
        AssertJUnit.assertEquals(0, rec.liveBytes());
        AssertJUnit.assertEquals(0, rec.registrySize());
    }

    // ---- safety

    @Test
    public void mementosThatCannotBeUsedAreRefusedAndTouchNothing() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento.Recorder other = new GameMemento.Recorder();
        final GameMemento a = rec.capture(r);
        final GameMemento foreign = other.capture(r);
        r.counter = 3;
        final GameMemento b = rec.capture(r);
        final String now = fp(r);

        AssertJUnit.assertFalse(rec.restoreTo(foreign));       // another recorder's memento
        AssertJUnit.assertFalse(rec.restoreTo(null));
        AssertJUnit.assertEquals(now, fp(r));

        rec.detach();                                          // a barrier: older mementos leave the chain
        final GameMemento c = rec.capture(r);
        AssertJUnit.assertFalse(a.restore());
        AssertJUnit.assertFalse(b.restore());
        AssertJUnit.assertEquals(now, fp(r));
        AssertJUnit.assertTrue(c.restore());

        rec.reset();                                           // memory pressure: nothing is restorable any more
        AssertJUnit.assertFalse(c.restore());
        AssertJUnit.assertFalse(a.problems.isEmpty());
        AssertJUnit.assertEquals(now, fp(r));
    }

    @Test
    public void aCaptureThatFailsHalfWayResetsTheRecorderInsteadOfCorruptingIt() {
        final Root r = sampleRoot();
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        final GameMemento a = rec.capture(r);
        r.counter = 1;
        final GameMemento b = rec.capture(r);

        r.counter = 2;
        r.fragile.armed = true;
        boolean failed = false;
        try { rec.capture(r); } catch (IllegalStateException expected) { failed = true; }
        r.fragile.armed = false;
        AssertJUnit.assertTrue(failed);
        AssertJUnit.assertEquals(0, rec.registrySize());
        AssertJUnit.assertEquals(0, rec.liveBytes());
        final String now = fp(r);
        AssertJUnit.assertFalse(a.restore());                  // refused, not half applied
        AssertJUnit.assertFalse(b.restore());
        AssertJUnit.assertEquals(now, fp(r));

        final GameMemento c = rec.capture(r);                  // and the recorder is fully usable again
        r.counter = 9;
        final GameMemento d = rec.capture(r);
        AssertJUnit.assertTrue(c.restore());
        AssertJUnit.assertEquals(now, fp(r));
        AssertJUnit.assertNotNull(d);
        assertAccounting(rec);
    }

    @Test
    public void standaloneMementoIsIndependentOfLaterCaptures() {
        final Root r = sampleRoot();
        final String before = fp(r);
        final GameMemento mark = GameMemento.capture(r);       // what UndoManager.markForRollback takes
        final GameMemento.Recorder rec = new GameMemento.Recorder();
        rec.capture(r);
        r.items.get(0).value = 5;
        r.items.clear();
        rec.capture(r);
        AssertJUnit.assertTrue(mark.restore());
        AssertJUnit.assertEquals(before, fp(r));
        AssertJUnit.assertTrue(mark.problems.isEmpty());
        AssertJUnit.assertTrue(mark.restore());                // and it can be used again
        AssertJUnit.assertEquals(before, fp(r));
        AssertJUnit.assertEquals(0, mark.deltaSize());
    }
}
