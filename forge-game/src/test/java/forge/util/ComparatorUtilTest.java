package forge.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ComparatorUtilTest {
    /** {value, flagged, original index} */
    private static List<int[]> randomItems(Random r, int n) {
        final List<int[]> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new int[]{r.nextInt(8), r.nextInt(3) == 0 ? 1 : 0, i});
        }
        return list;
    }

    @Test
    public void safeSortSurvivesComparatorThatBreaksItsContract() {
        // flagged items sort first in both directions, like the old FreeSpellAI rule did
        final Comparator<int[]> broken = (a, b) -> {
            if (a[1] == 1) return -1;
            if (b[1] == 1) return 1;
            return Integer.compare(b[0], a[0]);
        };
        final Random r = new Random(42);
        int fellBack = 0;
        for (int round = 0; round < 500; round++) {
            final List<int[]> list = randomItems(r, 2 + r.nextInt(60));
            final int size = list.size();
            if (ComparatorUtil.safeSort(list, broken)) {
                fellBack++;
            }
            final Set<Integer> indexes = new HashSet<>();
            for (int[] item : list) {
                indexes.add(item[2]);
            }
            Assert.assertEquals(list.size(), size);
            Assert.assertEquals(indexes.size(), size, "no element may be lost or duplicated");
        }
        Assert.assertTrue(fellBack > 0, "the broken comparator should have exercised the fallback");
    }

    @Test
    public void fallbackIsAStableSort() {
        final Random r = new Random(7);
        final List<int[]> list = randomItems(r, 50);
        final boolean[] first = {true};
        final Comparator<int[]> throwsOnce = (a, b) -> {
            if (first[0]) {
                first[0] = false;
                throw new IllegalArgumentException("Comparison method violates its general contract!");
            }
            return Integer.compare(b[0], a[0]);
        };
        Assert.assertTrue(ComparatorUtil.safeSort(list, throwsOnce));
        for (int i = 1; i < list.size(); i++) {
            final int[] prev = list.get(i - 1), cur = list.get(i);
            Assert.assertTrue(prev[0] >= cur[0], "sorted, highest value first");
            if (prev[0] == cur[0]) {
                Assert.assertTrue(prev[2] < cur[2], "equal values keep their original order");
            }
        }
    }

    @Test
    public void consistentComparatorDoesNotNeedTheFallback() {
        final List<int[]> list = randomItems(new Random(1), 50);
        Assert.assertFalse(ComparatorUtil.safeSort(list, (a, b) -> Integer.compare(b[0], a[0])));
    }
}
