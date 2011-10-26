package net.slightlymagic.braids.util.lambda;

/**
 * The Class Lambda3.
 * 
 * @param <R>
 *            the generic type
 * @param <A1>
 *            the generic type
 * @param <A2>
 *            the generic type
 * @param <A3>
 *            the generic type
 */
public abstract class Lambda3<R, A1, A2, A3> implements Lambda<R> {

    /**
     * Apply.
     * 
     * @param arg1
     *            the arg1
     * @param arg2
     *            the arg2
     * @param arg3
     *            the arg3
     * @return the r
     */
    public abstract R apply(A1 arg1, A2 arg2, A3 arg3);

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
        return apply((A1) args[0], (A2) args[1], (A3) args[2]);
    }

}
