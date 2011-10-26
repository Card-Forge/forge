package net.slightlymagic.braids.util;

/**
 * This exception indicates the particular method (or part of a method) being
 * called has not been implemented; getting this exception is generally
 * considered a programming error.
 * 
 * Throwing this exception does not necessarily mean the method will be
 * implemented at any point in the future.
 */
public class NotImplementedError extends RuntimeException {

    private static final long serialVersionUID = -6714022569997781370L;

    /**
     * No-arg constructor; this usually means the entire method or block from
     * which it is thrown has not been implemented.
     */
    public NotImplementedError() {
        super();
    }

    /**
     * Indicates what has not been implemented.
     * 
     * @param message
     *            indicates what exactly has not been implemented. May include
     *            information about future plans to implement the described
     *            section of code.
     */
    public NotImplementedError(final String message) {
        super(message);
    }

    /**
     * Like the no-arg constructor, but with a cause parameter.
     * 
     * @param cause
     *            the exception that caused this one to be thrown
     * 
     * @see #NotImplementedError()
     */
    public NotImplementedError(final Throwable cause) {
        super(cause);
    }

    /**
     * Like the String constructor, but with a cause parameter.
     * 
     * @param message
     *            indicates what exactly has not been implemented. May include
     *            information about future plans to implement the described
     *            section of code.
     * 
     * @param cause
     *            the exception that caused this one to be thrown
     * 
     * @see #NotImplementedError(String)
     */
    public NotImplementedError(final String message, final Throwable cause) {
        super(message, cause);
    }
}
