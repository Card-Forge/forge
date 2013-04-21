package forge.net.protocol.toserver;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChatPacketSrv implements IPacketSrv {
    private final String message;
    public ChatPacketSrv(String data) {
        message = data;
    }
    public String getMessage() {
        return message;
    }
}
