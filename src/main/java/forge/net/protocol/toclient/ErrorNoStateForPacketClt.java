package forge.net.protocol.toclient;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ErrorNoStateForPacketClt implements IPacketClt {

    private final String message;
        
    public ErrorNoStateForPacketClt(String simpleName) {
        message = simpleName;
    }

    public String getMessage() {
        return message;
    }

}
