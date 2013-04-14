package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoPacket implements IPacket {

    private final String message;
    private EchoPacket(String data) {
        message = data;
    }
    
    public static EchoPacket parse(String data) {
        return new EchoPacket(data);
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public PacketOpcode getOpCode() {
        return PacketOpcode.Echo;
    }

}
