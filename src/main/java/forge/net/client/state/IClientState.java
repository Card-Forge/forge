package forge.net.client.state;

import forge.net.protocol.incoming.IPacket;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IClientState {
    boolean processPacket(IPacket data);
}
