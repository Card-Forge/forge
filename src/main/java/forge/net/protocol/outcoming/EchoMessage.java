package forge.net.protocol.outcoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoMessage implements IMessage {

    private final String message;
    

    /**
     * TODO: Write javadoc for Constructor.
     * @param message2
     */
    public EchoMessage(String message) {
        this.message = message;
    }


    @Override
    public String toNetString() {
        return String.format("System: %s", message);
    }

}
