package forge.compat.mgmt;

/** Whole-type remap target for java.lang.management.MemoryMXBean. */
public interface MemoryMXBean {
    MemoryUsage getHeapMemoryUsage();

    MemoryUsage getNonHeapMemoryUsage();
}
