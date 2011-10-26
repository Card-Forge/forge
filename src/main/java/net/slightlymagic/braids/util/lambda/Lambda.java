package net.slightlymagic.braids.util.lambda;

/**
 * The Interface Lambda.
 * 
 * @param <R>
 *            the generic type
 */
public interface Lambda<R> {

    /**
     * Apply.
     * 
     * @param args
     *            the args
     * @return the r
     */
    R apply(Object[] args);
}
