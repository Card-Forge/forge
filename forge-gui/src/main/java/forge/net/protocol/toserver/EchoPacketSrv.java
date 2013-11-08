package forge.net.protocol.toserver;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoPacketSrv implements IPacketSrv {

    private final String message;
    private EchoPacketSrv(String data) {
        message = data;
    }
    
    public static EchoPacketSrv parse(String data) {
        return new EchoPacketSrv(data);
    }
    
    public String getMessage() {
        return message;
    }
}
