package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum PacketOpcode {
    Echo("/echo"),
    Chat("/s"),
    Unknown("");
    
    
    
    private final String opcode;
    
    private PacketOpcode(String code) {
        opcode = code;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param data
     * @return
     */
    public static Packet decode(String data) {
        for(PacketOpcode s : PacketOpcode.values()) {
            if ( data.startsWith(s.opcode) )
                return decodePacket(s, data.substring(s.opcode.length()).trim());
        }
        if( data.startsWith("/") )
            return new UnknownPacket(data.substring(1));
        else
            return new ChatPacket(data);
    }


    private static Packet decodePacket(PacketOpcode code, String data) {
        switch(code) {
            case Echo:
                return new EchoPacket(data);
            default: 
                return new UnknownPacket(data);
        }
    }
}
