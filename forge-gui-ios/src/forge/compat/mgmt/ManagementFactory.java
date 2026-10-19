package forge.compat.mgmt;

import java.util.Collections;
import java.util.List;

/**
 * Whole-type remap target for java.lang.management.ManagementFactory (absent
 * on MobiVM). Backed by java.lang.Runtime; satisfies the boot-time probes of
 * apfloat's ApfloatContext, tinylog's runtime provider, and RestartUtil.
 */
public final class ManagementFactory {
    private static final long START = System.currentTimeMillis();

    private static final RuntimeMXBean RUNTIME = new RuntimeMXBean() {
        @Override
        public String getName() {
            return "1@forge-ios";
        }

        @Override
        public long getStartTime() {
            return START;
        }

        @Override
        public long getUptime() {
            return System.currentTimeMillis() - START;
        }

        @Override
        public List<String> getInputArguments() {
            return Collections.emptyList();
        }
    };

    private static final MemoryMXBean MEMORY = new MemoryMXBean() {
        @Override
        public MemoryUsage getHeapMemoryUsage() {
            Runtime rt = Runtime.getRuntime();
            long used = rt.totalMemory() - rt.freeMemory();
            long max = rt.maxMemory();
            return new MemoryUsage(rt.totalMemory(), used, rt.totalMemory(), max);
        }

        @Override
        public MemoryUsage getNonHeapMemoryUsage() {
            return new MemoryUsage(0, 0, 0, -1);
        }
    };

    private ManagementFactory() { }

    public static RuntimeMXBean getRuntimeMXBean() {
        return RUNTIME;
    }

    public static MemoryMXBean getMemoryMXBean() {
        return MEMORY;
    }
}
