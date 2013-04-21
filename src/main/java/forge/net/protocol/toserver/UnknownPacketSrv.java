package forge.net.protocol.toserver;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnknownPacketSrv implements IPacketSrv {

    private final String message;
    public UnknownPacketSrv(String data) {
        message = data;
    }
    public String getMessage() {
        return message;
    }

}
