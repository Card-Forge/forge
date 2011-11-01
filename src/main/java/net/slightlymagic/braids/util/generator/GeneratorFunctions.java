/** Licensed under both the GPL and the Apache 2.0 License. */
package net.slightlymagic.braids.util.generator;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import net.slightlymagic.braids.util.lambda.Lambda1;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;
import com.google.code.jyield.Yieldable;

/**
 * For documentation on Java-Yield and its generators, see.
 * 
 * {@link com.google.code.jyield.Generator}.
 */
public final class GeneratorFunctions {

    /**
     * Do not instantiate.
     */
    private GeneratorFunctions() {
    }

    /**
     * Estimate the number of items in this generator by traversing all of its
     * elements.
     * 
     * Note this only works on a generator that can be reinstantiated once it
     * has been traversed. This is only an estimate, because a generator's size
     * may vary been traversals. This is especially true if the generator relies
     * on external resources, such as a file system.
     * 
     * If you call this on an infinite generator, this method will never return.
     * 
     * @param <T>
     *            the generic type
     * @param gen
     *            the gen
     * @return the estimated number of items provided by this generator
     */
    public static <T> long estimateSize(final Generator<T> gen) {
        long result = 0;
        for (@SuppressWarnings("unused")
        T ignored : YieldUtils.toIterable(gen)) {
            result++;
        }

        return result;
    }

    /**
     * Highly efficient means of filtering a long or infinite sequence.
     * 
     * @param <T>
     *            any type
     * 
     * @param predicate
     *            a Lambda (function) whose apply method takes an object of type
     *            <T> and returns a Boolean. If it returns false or null, the
     *            item from the inputGenerator is not yielded by this Generator;
     *            if predicate.apply returns true, then this Generator
     *            <i>does</i> yield the value.
     * 
     * @param inputGenerator
     *            the sequence upon which we operate
     * 
     * @return a generator which produces a subset <= the inputGenerator
     */
    public static <T> Generator<T> filterGenerator(final Lambda1<Boolean, T> predicate,
            final Generator<T> inputGenerator) {
        Generator<T> result = new Generator<T>() {

            @Override
            public void generate(final Yieldable<T> outputYield) {

                Yieldable<T> inputYield = new Yieldable<T>() {
                    private Boolean pResult;

                    @Override
                    public void yield(final T input) {
                        pResult = predicate.apply(input);
                        if (pResult != null && pResult) {
                            outputYield.yield(input);
                        }
                    }
                };

                inputGenerator.generate(inputYield);
            }

        };

        return result;
    }

    /**
     * Highly efficient means of applying a transform to a long or infinite
     * sequence.
     * 
     * @param <T>
     *            any type
     * 
     * @param transform
     *            a Lambda (function) whose apply method takes an object of type
     *            <T> and returns an object of the same type. This transforms
     *            the values from the inputGenerator into this Generator.
     * 
     * @param inputGenerator
     *            the sequence upon which we operate
     * 
     * @return a generator that yields transform.apply's return value for each
     *         item in the inputGenerator
     */
    public static <T> Generator<T> transformGenerator(final Lambda1<T, T> transform, final Generator<T> inputGenerator)
    {
        Generator<T> result = new Generator<T>() {

            @Override
            public void generate(final Yieldable<T> outputYield) {

                Yieldable<T> inputYield = new Yieldable<T>() {
                    @Override
                    public void yield(final T input) {
                        outputYield.yield(transform.apply(input));
                    }
                };

                inputGenerator.generate(inputYield);
            }

        };

        return result;
    }

    /**
     * Forces a generator to be completely evaluated into a temporary data
     * structure, then returns the generator over that same structure.
     * 
     * This effectively returns the same Generator, but it is a faster one. This
     * trades away heap space for reduced CPU intensity. This is particuarly
     * helpful if you know that a Generator is going to be totally evaluated
     * more than once in the near future.
     * 
     * @param <T>
     *            inferred automatically
     * 
     * @param unevaluated
     *            a Generator of T instances
     * 
     * @return the equivalent Generator, except that the result's generate
     *         method can be invoked multiple times for fast results.
     */
    public static <T> Generator<T> solidify(final Generator<T> unevaluated) {
        ArrayList<T> solidTmp = YieldUtils.toArrayList(unevaluated);
        solidTmp.trimToSize();
        return YieldUtils.toGenerator(solidTmp);
    }

    /**
     * Select an item at random from a Generator; this causes the entire
     * Generator to be evaluated once, but only once.
     * 
     * @param <T>
     *            the generic type
     * @param generator
     *            the generator from which to select a random item
     * @return an item chosen at random from the generator; this may be null, if
     *         the generator contains null items.
     *             if the generator has no contents
     */
    public static <T> T selectRandom(final Generator<T> generator) {
        /*
         * This algorithm requires some explanation. Each time we encounter a
         * new item from the generator, we determine via random chance if the
         * item is the one we select. At the end of each iteration, we have a
         * candidate, and we have a count of the number of items encountered so
         * far. Each iteration has a 1/n chance of replacing the candidate with
         * the current item, where n is the number of items encountered so far.
         * This allows us to randomly select an item from the generated contents
         * with an equal distribution; and we don't have to count the number of
         * items first!
         */

        int n = 0;
        T candidate = null;

        for (T item : YieldUtils.toIterable(generator)) {
            n++;
            int rand = (int) (Math.random() * n);
            // At this point, 0 <= rand < n.
            rand++; // Now, 1 <= rand <= n.

            if (rand == 1) {
                // We rolled a 1 on an n-sided die. We have a new candidate!
                // Note that on the first iteration, this always happens,
                // because n = 1.
                candidate = item;
            }
        }

        if (n == 0) {
            // There were no items in the generator!
            throw new NoSuchElementException("generator is empty");
        }

        return candidate;
    }
}
