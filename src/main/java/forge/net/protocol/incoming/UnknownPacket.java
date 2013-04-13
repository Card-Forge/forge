package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnknownPacket extends Packet {

    private final String message;
    public UnknownPacket(String data) {
        super(PacketOpcode.Unknown);
        message = data;
    }
    public String getMessage() {
        return message;
    }

}
