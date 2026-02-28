package forge.net;

import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for delta sync components.
 *
 * Tests individual classes used by the delta sync system:
 * - DeltaPacket - size calculation formula
 * - DeltaSyncManager - client tracking and sequence management
 * - NetworkByteTracker - enable/disable behavior
 *
 * These are fast unit tests that don't involve actual network I/O.
 * For integration tests with real network traffic, see NetworkPlayIntegrationTest.
 */
public class DeltaSyncUnitTest {

    // ==================== Delta Packet Size Tests ====================

    @Test
    public void testDeltaSizeCalculationAccuracy() {
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);
        deltas.put(3, new byte[50]);

        Set<Integer> removed = new HashSet<>();
        removed.add(10);
        removed.add(20);

        DeltaPacket packet = new DeltaPacket(1L, deltas, removed);

        // Header: 8 (seq) + 4 (checksum) = 12 bytes
        // Deltas: (4+100) + (4+200) + (4+50) = 362 bytes
        // Removed: 2 * 4 = 8 bytes
        // Total: 382 bytes
        int expectedSize = 12 + (4 + 100) + (4 + 200) + (4 + 50) + (2 * 4);

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta size calculation should match expected value");
    }

    @Test
    public void testDeltaSizeWithNewObjects() {
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);

        Map<Integer, DeltaPacket.NewObjectData> newObjects = new HashMap<>();
        newObjects.put(100, new DeltaPacket.NewObjectData(100, 0, new byte[150]));
        newObjects.put(101, new DeltaPacket.NewObjectData(101, 1, new byte[200]));

        DeltaPacket packet = new DeltaPacket(1L, deltas, newObjects, new HashSet<>(), 0, false);

        // Header: 12, Delta: (4+50)=54, New 100: (4+4+150)=158, New 101: (4+4+200)=208
        int expectedSize = 12 + 54 + 158 + 208;

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta packet with new objects should match expected size");
    }

    @Test
    public void testEmptyDeltaPacketSize() {
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashSet<>());
        int size = packet.getApproximateSize();

        // Empty packet should just have header: 8 + 4 = 12 bytes
        Assert.assertEquals(size, 12, "Empty delta packet should be exactly 12 bytes (header only)");
    }

    // ==================== Delta Sync Manager Tests ====================

    @Test
    public void testDeltaSyncManagerAcknowledgment() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.processAcknowledgment(0, 0L);
        manager.processAcknowledgment(1, 0L);

        // Client 0 acknowledges sequence 5
        manager.processAcknowledgment(0, 5L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 0L); // Client 1 still at 0

        // Client 1 acknowledges sequence 3
        manager.processAcknowledgment(1, 3L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 3L); // Min of 5 and 3

        // Client 1 catches up
        manager.processAcknowledgment(1, 10L);
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 5L); // Min of 5 and 10
    }

    @Test
    public void testNeedsFullResync() {
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.processAcknowledgment(0, 0L);

        // New client should not need resync initially
        Assert.assertFalse(manager.needsFullResync(0));

        // Unregistered client should need resync
        Assert.assertTrue(manager.needsFullResync(99));
    }

    // ==================== Network Byte Tracker Tests ====================

    @Test
    public void testNetworkByteTrackerEnableDisable() {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        // Initially enabled
        Assert.assertTrue(tracker.isEnabled());

        tracker.recordBytesSent(100, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 100L);

        // Disable tracking
        tracker.setEnabled(false);
        Assert.assertFalse(tracker.isEnabled());

        // Should not record when disabled
        tracker.recordBytesSent(200, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 100L); // Still 100, not 300

        // Re-enable
        tracker.setEnabled(true);
        tracker.recordBytesSent(300, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 400L); // 100 + 300
    }
}
