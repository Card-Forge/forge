package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoPacket extends Packet {

    private final String message;
    private EchoPacket(String data) {
        super(PacketOpcode.Echo);
        message = data;
    }
    
    public static EchoPacket parse(String data) {
        return new EchoPacket(data);
    }
    
    public String getMessage() {
        return message;
    }

}
