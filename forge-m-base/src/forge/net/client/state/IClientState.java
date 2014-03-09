package forge.net.client.state;

import forge.net.protocol.toserver.IPacketSrv;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IClientState {
    boolean processPacket(IPacketSrv data);
}
