package forge.net.protocol.toserver;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AuthorizePacketSrv implements IPacketSrv {
    private final String username;
    private final String password;
    
    private AuthorizePacketSrv(String name, String pass) {
        username = name;
        password = pass;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
