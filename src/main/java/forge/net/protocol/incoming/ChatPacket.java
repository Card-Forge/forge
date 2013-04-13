package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChatPacket extends Packet {
    private final String message;
    public ChatPacket(String data) {
        super(PacketOpcode.Chat);
        message = data;
    }
    public String getMessage() {
        return message;
    }

}
