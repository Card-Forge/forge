package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChatPacket implements IPacket {
    private final String message;
    public ChatPacket(String data) {
        message = data;
    }
    public String getMessage() {
        return message;
    }
    
    @Override
    public PacketOpcode getOpCode() {
        return PacketOpcode.Chat;
    }


}
