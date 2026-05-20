package forge.util;

import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;

public record HWInfo(Device device, OperatingSystem os, boolean getChipset) {

    public int getTotalRam() {
        return Math.round(device.getMemorySize() / 1024f / 1024f);
    }

}
