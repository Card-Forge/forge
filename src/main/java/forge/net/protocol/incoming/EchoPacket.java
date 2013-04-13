package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoPacket extends Packet {

    private final String message;
    public EchoPacket(String data) {
        super(PacketOpcode.Echo);
        message = data;
    }
    public String getMessage() {
        return message;
    }

}
