package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class Packet {
    private final PacketOpcode opCode; 
    
    public Packet(PacketOpcode code) {
        opCode = code; 
    }

    public final PacketOpcode getOpCode() {
        return opCode;
    }
}
