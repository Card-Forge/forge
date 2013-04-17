package forge.net.protocol.outcoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ChatMessage implements IMessage {

    private final String player;
    private final String message;

    public ChatMessage(String playerName, String message) {
        // TODO Auto-generated constructor stub
        this.message = message;
        this.player = playerName;
    }

    @Override
    public String toNetString() {
        // TODO Auto-generated method stub
        return String.format("%s: %s", player, message);
    }

}
