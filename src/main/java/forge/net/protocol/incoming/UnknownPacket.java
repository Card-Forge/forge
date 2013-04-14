package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnknownPacket implements IPacket {

    private final String message;
    private UnknownPacket(String data) {
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
    public static IPacket parse(String substring) {
        return new UnknownPacket(substring);
    }
    
    @Override
    public PacketOpcode getOpCode() {
        return PacketOpcode.Unknown;
    }


}
