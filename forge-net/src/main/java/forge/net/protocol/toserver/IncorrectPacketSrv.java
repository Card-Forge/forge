package forge.net.protocol.toserver;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class IncorrectPacketSrv implements IPacketSrv {

    private final String message;
    
    public IncorrectPacketSrv(String errorMessage) {
        message = errorMessage;
    }

    public String getMessage() {
        return message;
    }
}
