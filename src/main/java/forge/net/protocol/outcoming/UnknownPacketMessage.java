package forge.net.protocol.outcoming;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnknownPacketMessage extends Message {

    /* (non-Javadoc)
     * @see forge.net.protocol.outcoming.Message#toNetString()
     */
    @Override
    public String toNetString() {
        return "Unkown packet received";
    }

}
