package net.slightlymagic.braids.util.lambda;

/**
 * The Class Lambda2.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 * @param <A2>
 *            the generic type
 */
public abstract class Lambda2<R, A1, A2> implements Lambda<R> {

    /**
     * Apply.
     * 
     * @param arg1
     *            the arg1
     * @param arg2
     *            the arg2
     * @return the r
     */
    public abstract R apply(A1 arg1, A2 arg2);

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.slightlymagic.braids.util.lambda.Lambda#apply(java.lang.Object[])
     */

    // TODO @Override
    /**
     * @return R
     * @param args Object[]
     */
    @SuppressWarnings("unchecked")
    public final R apply(final Object[] args) {
        return apply((A1) args[0], (A2) args[1]);
    }

}
