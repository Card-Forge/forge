package net.slightlymagic.braids.util.lambda;

/**
 * The Interface Thunk.
 * 
 * @param <T>
 *            the generic type
 */
public interface Thunk<T> {

    /**
     * Apply.
     * 
     * @return the t
     */
    T apply();
}
