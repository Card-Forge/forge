package forge.util;

import java.util.Collection;
import java.util.Comparator;

/**
 * @author Gili Tzabari
 */
public final class ComparatorUtil
{
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