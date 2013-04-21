package forge.net.protocol.toclient;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChatPacketClt implements IPacketClt {

    private final String actor;
    private final String message;

    public ChatPacketClt(String playerName, String message) {
        // TODO Auto-generated constructor stub
        this.message = message;
        this.actor = playerName;
    }

    public String getActor() {
        return actor;
    }

    public String getMessage() {
        return message;
    }
}
