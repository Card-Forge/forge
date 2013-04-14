package forge.net.protocol.incoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class IncorrectPacket implements IPacket {

    private final PacketOpcode intendedCode;
    private final int index;
    private final String sParam;
    
    public IncorrectPacket(PacketOpcode code, int iParameter, String value) {
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

    @Override
    public PacketOpcode getOpCode() {
        return PacketOpcode.Incorrect;
    }

}
