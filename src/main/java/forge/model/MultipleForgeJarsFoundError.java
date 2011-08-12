package forge.model;

//import java.io.IOException;

/**
 * Exception thrown by model when it is trying to find a single forge jar, but
 * it finds more than one.
 */
public class MultipleForgeJarsFoundError extends RuntimeException {
    /** Automatically generated. */
    private static final long serialVersionUID = 8899307272033517172L;


    /**
     * Create an exception with a message.
     *
     * @param message  the message, which could be the System's class path.
     */
    public MultipleForgeJarsFoundError(final String message) {
        super(message);
    }

}
