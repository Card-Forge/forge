package forge.gamemodes.net;

import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

/**
 * Marker interface for classes that perform network logging.
 * Provides a shared NETWORK-tagged logger so the tag string
 * lives in one place and implementing classes are self-documenting.
 */
public interface IHasNetLog {
    TaggedLogger netLog = Logger.tag("NETWORK");
}
