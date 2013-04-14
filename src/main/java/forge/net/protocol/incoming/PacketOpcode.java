package forge.net.protocol.incoming;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum PacketOpcode {
    Echo("/echo"),
    Chat("/s"),
    Authorize("/auth"),
    
    Incorrect(null),
    Unknown(null);
    
    
    
    private final String opcode;
    
    private PacketOpcode(String code) {
        opcode = code;
    }

    public final String getOpcode() {
        return opcode;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param data
     * @return
     */
    public static IPacket decode(String data) {
        for(PacketOpcode s : PacketOpcode.values()) {
            if ( s.opcode != null && data.startsWith(s.opcode) )
                return decodePacket(s, data.substring(s.opcode.length()).trim());
        }
        if( data.startsWith("/") )
            return UnknownPacket.parse(data.substring(1));
        else
            return new ChatPacket(data);
    }


    private static IPacket decodePacket(PacketOpcode code, String data) {
        switch(code) {
            case Echo: return EchoPacket.parse(data);
            case Authorize: return AuthorizePacket.parse(data);
            
            default: return UnknownPacket.parse(data);
        }
    }
}
