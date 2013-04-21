package forge.net.protocol.toclient;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class EchoPacketClt implements IPacketClt {

    private final String message;
    

    /**
     * TODO: Write javadoc for Constructor.
     * @param message2
     */
    public EchoPacketClt(String message) {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }
}
