package forge.net.client;

import forge.net.protocol.outcoming.Message;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface INetClient {

    /**
     * TODO: Write javadoc for this method.
     * @param echoMessage
     */
    void send(Message echoMessage);

}
