package forge.net.protocol.incoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class IncorrectPacket extends Packet {

    private final PacketOpcode intendedCode;
    private final int index;
    private final String sParam;
    
    public IncorrectPacket(PacketOpcode code, int iParameter, String value) {
        super(PacketOpcode.Incorrect);
        intendedCode = code;
        index = iParameter;
        sParam = value;
    }

    public String getString() {
        return sParam;
    }

    public int getIndex() {
        return index;
    }

    public PacketOpcode getIntendedCode() {
        return intendedCode;
    }

}
