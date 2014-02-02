package forge.net.protocol.toclient;

/**
 * A response packet to an Incorrect packet client had sent to server
 */
public class ErrorIncorrectPacketClt implements IPacketClt {

    private final String message;
    
    public ErrorIncorrectPacketClt(String msg) {
        message = msg;
    }

    public String getMessage() {
        return message;
    }
}
