package forge.net.protocol.toclient;

import forge.model.BuildInfo;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class WelcomePacketClt implements IPacketClt {
    public final String message;
    public final String version;
    
    public WelcomePacketClt(String welcome) {
        message = welcome;
        version = BuildInfo.getVersionString();
    }
}
