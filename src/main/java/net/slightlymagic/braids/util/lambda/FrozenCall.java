package net.slightlymagic.braids.util.lambda;

import static net.slightlymagic.braids.util.UtilFunctions.checkNotNull;
import net.slightlymagic.braids.util.UtilFunctions;

/**
 * This embodies a promise to invoke a certain method at a later time; the
 * FrozenCall remembers the arguments to use and the return type.
 * 
 * @param <T>
 *            the return type of apply
 * 
 * @see Thunk
 */
public class FrozenCall<T> implements Thunk<T> {
    private Lambda<T> proc;
    private Object[] args;

    /**
     * Instantiates a new frozen call.
     * 
     * @param proc
     *            the proc
     * @param args
     *            the args
     */
    public FrozenCall(final Lambda<T> proc, final Object[] args) {
        checkNotNull("proc", proc);
        checkNotNull("args", args);

        this.proc = proc;
        this.args = args;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.slightlymagic.braids.util.lambda.Thunk#apply()
     */
    /**
     * @return <T>
     */
    public final T apply() {
        return proc.apply(args);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /**
     * @return boolean
     * @param obj Object
     */
    @Override
    public final boolean equals(final Object obj) {
        FrozenCall<T> that = UtilFunctions.checkNullOrNotInstance(this, obj);
        if (that == null) {
            return false;
        } else if (!this.proc.equals(that.proc)) {
            return false;
        } else if (this.args.length != that.args.length) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (this.args[i] == null && that.args[i] != null) {
                return false;
            } else if (!this.args[i].equals(that.args[i])) {
                return false;
            }
        }

        return true;
    }
}
