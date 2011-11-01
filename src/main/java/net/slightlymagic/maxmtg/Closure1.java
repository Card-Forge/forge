package net.slightlymagic.maxmtg;

import net.slightlymagic.braids.util.lambda.Lambda1;

/**
 * This class represents an action (lambda) and some arguments to make a call at
 * a later time.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 */
public class Closure1<R, A1> {
    private final Lambda1<R, A1> method;
    private final A1 argument;

    /**
     * Instantiates a new closure1.
     * 
     * @param lambda
     *            the lambda
     * @param object
     *            the object
     */
    public Closure1(final Lambda1<R, A1> lambda, final A1 object) {
        this.method = lambda;
        this.argument = object;
    }

    /**
     * Apply.
     * 
     * @return the r
     */
    public R apply() {
        return this.method.apply(this.argument);
    }
}
