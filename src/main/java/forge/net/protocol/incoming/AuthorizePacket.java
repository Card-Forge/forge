package forge.net.protocol.incoming;

import org.apache.commons.lang3.StringUtils;

import forge.util.TextUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AuthorizePacket implements IPacket {
    private final String username;
    private final String password;
    
    private AuthorizePacket(String name, String pass) {
        username = name;
        password = pass;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


    public static IPacket parse(String data) {
        String[] parts = TextUtil.splitWithParenthesis(data, ' ', '\"', '\"');
        if(parts.length == 1 || parts.length == 2) {
            if(!StringUtils.isAlphanumericSpace(parts[0]))
                return new IncorrectPacket(PacketOpcode.Authorize, 0, parts[0]);
            if( parts.length == 1)
                return new AuthorizePacket(parts[0], null);
            
            if(!StringUtils.isAsciiPrintable(parts[1]))
                return new IncorrectPacket(PacketOpcode.Authorize, 1, parts[1]);
            else
                return new AuthorizePacket(parts[0], parts[1]); 
        }
        return UnknownPacket.parse(data);
    }

    @Override
    public PacketOpcode getOpCode() {
        return PacketOpcode.Authorize;
    }

}
