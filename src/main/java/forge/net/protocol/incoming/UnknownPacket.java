package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnknownPacket extends Packet {

    private final String message;
    private UnknownPacket(String data) {
        super(PacketOpcode.Unknown);
        message = data;
    }
    public String getMessage() {
        return message;
    }
    /**
     * TODO: Write javadoc for this method.
     * @param substring
     * @return
     */
    public static Packet parse(String substring) {
        return new UnknownPacket(substring);
    }

}
