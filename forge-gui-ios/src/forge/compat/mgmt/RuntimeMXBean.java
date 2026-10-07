package forge.compat.mgmt;

import java.util.List;

/**
 * Whole-type remap target for java.lang.management.RuntimeMXBean (absent on
 * MobiVM). Interface because JDK callers use invokeinterface.
 */
public interface RuntimeMXBean {
    String getName();

    long getStartTime();

    long getUptime();

    List<String> getInputArguments();
}
