package net.slightlymagic.braids.util.generator;

import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;

/**
 * Creates a Generator from an array; generators are a handy substitute for
 * passing around and creating temporary lists, collections, and arrays.
 * 
 * @param <T>
 *            the generic type {@link com.google.code.jyield.Generator}
 */
public class GeneratorFromArray<T> implements Generator<T> {
    private T[] array;

    /**
     * Create a Generator from an array.
     * 
     * @param array
     *            from which to generate items
     */
    public GeneratorFromArray(final T[] array) {
        this.array = array;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.google.code.jyield.Generator#generate(com.google.code.jyield.Yieldable
     * )
     */

    /**
     * Submits all of the array's elements to the yieldable.
     * 
     * @param yy
     *            the yieldable which receives the elements
     */
    @Override
    public final void generate(final Yieldable<T> yy) {
        for (T item : array) {
            yy.yield(item);
        }
    }
}
