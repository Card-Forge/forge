package forge.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @author Gili Tzabari
 */
public final class ComparatorUtil
{
    /**
     * Sorts a list with {@link List#sort}, but never lets a comparator that breaks its contract
     * (not transitive / not antisymmetric) escape as "Comparison method violates its general contract!".
     * TimSort, which Android's runtime is strict about, throws that as soon as it notices; on failure the list is
     * restored to its original order and sorted again with a stable merge sort that tolerates inconsistent
     * comparators, so the result is always a usable (if not perfectly ordered) permutation.
     *
     * @return true if the fallback sort had to be used, so callers can report a comparator that needs fixing
     */
    public static <T> boolean safeSort(final List<T> list, final Comparator<? super T> comparator) {
        if (list.size() < 2) {
            return false;
        }
        final Object[] original = list.toArray();
        try {
            list.sort(comparator);
            return false;
        } catch (IllegalArgumentException ex) {
            final Object[] sorted = original.clone();
            mergeSort(sorted, original.clone(), 0, sorted.length, comparator);
            for (int i = 0; i < sorted.length; i++) {
                @SuppressWarnings("unchecked") final T item = (T) sorted[i];
                list.set(i, item);
            }
            return true;
        }
    }

    /** Stable top-down merge sort. Unlike TimSort it never checks the comparator's consistency. */
    @SuppressWarnings("unchecked")
    private static <T> void mergeSort(final Object[] dest, final Object[] src, final int low, final int high,
                                      final Comparator<? super T> comparator) {
        if (high - low < 2) {
            return;
        }
        final int mid = (low + high) >>> 1;
        mergeSort(src, dest, low, mid, comparator);
        mergeSort(src, dest, mid, high, comparator);
        int left = low, right = mid;
        for (int i = low; i < high; i++) {
            // take from the right run only when strictly smaller, which keeps equal elements in their original order
            if (right >= high || (left < mid && comparator.compare((T) src[left], (T) src[right]) <= 0)) {
                dest[i] = src[left++];
            } else {
                dest[i] = src[right++];
            }
        }
    }

    /**
     * Verify that a comparator is transitive.
     *
     * @param <T>        the type being compared
     * @param comparator the comparator to test
     * @param elements   the elements to test against
     * @throws AssertionError if the comparator is not transitive
     */
    public static <T> String verifyTransitivity(Comparator<T> comparator, Collection<T> elements)
    {
        String exception = "";
        for (T first: elements)
        {
            for (T second: elements)
            {
                int result1 = comparator.compare(first, second);
                int result2 = comparator.compare(second, first);
                if (result1 != -result2)
                {
                    // Uncomment the following line to step through the failed case
                    //comparator.compare(first, second);
                    /*throw new AssertionError("compare(" + first + ", " + second + ") == " + result1 +
                        " but swapping the parameters returns " + result2);*/
                    exception = "compare(" + first + ", " + second + ") == " + result1 +
                            " but swapping the parameters returns " + result2;
                    System.err.println(exception);
                    return exception;
                }
            }
        }
        for (T first: elements)
        {
            for (T second: elements)
            {
                int firstGreaterThanSecond = comparator.compare(first, second);
                if (firstGreaterThanSecond <= 0)
                    continue;
                for (T third: elements)
                {
                    int secondGreaterThanThird = comparator.compare(second, third);
                    if (secondGreaterThanThird <= 0)
                        continue;
                    int firstGreaterThanThird = comparator.compare(first, third);
                    if (firstGreaterThanThird <= 0)
                    {
                        // Uncomment the following line to step through the failed case
                        //comparator.compare(first, third);
                        /*throw new AssertionError("compare(" + first + ", " + second + ") > 0, " +
                            "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
                            firstGreaterThanThird);*/
                        exception = "compare(" + first + ", " + second + ") > 0, " +
                                "compare(" + second + ", " + third + ") > 0, but compare(" + first + ", " + third + ") == " +
                                firstGreaterThanThird;
                        System.err.println(exception);
                        return exception;
                    }
                }
            }
        }
        return exception;
    }

    /**
     * Prevent construction.
     */
    private ComparatorUtil()
    {
    }
}