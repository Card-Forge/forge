package forge.net.client;

/** 
 * Indicates incorrect field in an incoming packet
 */
public class InvalidFieldInPacketException extends RuntimeException {

    private static final long serialVersionUID = 4505312413627923468L;


    /**
     * TODO: Write javadoc for Constructor.
     * @param message
     */
    public InvalidFieldInPacketException(String message) {
        super(message);
    }


    /**
     * TODO: Write javadoc for Constructor.
     * @param message
     * @param cause
     */
    public InvalidFieldInPacketException(String message, Throwable cause) {
        super(message, cause);
    }

}
