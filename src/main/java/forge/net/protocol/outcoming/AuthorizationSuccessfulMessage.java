package forge.net.protocol.outcoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AuthorizationSuccessfulMessage implements IMessage {
    
    private final String username;
    
    public AuthorizationSuccessfulMessage(String user) {
        username = user;
    }

    @Override
    public String toNetString() {
        // TODO Auto-generated method stub
        return "Authorization Successful. Welcome, " + username; 
    }

}
