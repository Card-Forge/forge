package forge.net.protocol.toclient;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AuthResultPacketClt implements IPacketClt {
    private final boolean successful;
    private final String username;

    public AuthResultPacketClt(String user, boolean success) {
        username = user;
        successful = success; 
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getUsername() {
        return username;
    }
}
