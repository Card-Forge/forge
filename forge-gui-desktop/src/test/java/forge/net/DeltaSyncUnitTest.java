package forge.net;

import forge.gamemodes.net.DeltaPacket;
import forge.trackable.TrackableProperty;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for delta sync components.
 *
 * Tests individual classes used by the delta sync system:
 * - DeltaPacket - size calculation formula
 * - NetworkByteTracker - enable/disable behavior
 *
 * These are fast unit tests that don't involve actual network I/O.
 * For integration tests with real network traffic, see NetworkPlayIntegrationTest.
 */
public class DeltaSyncUnitTest {

    @Test
    public void testDeltaSizeCalculationAccuracy() {
        Map<Integer, Map<TrackableProperty, Object>> deltas = new HashMap<>();
        Map<TrackableProperty, Object> props1 = new HashMap<>();
        props1.put(TrackableProperty.Name, "Test");
        props1.put(TrackableProperty.Power, 3);
        deltas.put(1, props1);

        Map<TrackableProperty, Object> props2 = new HashMap<>();
        props2.put(TrackableProperty.Life, 20);
        props2.put(TrackableProperty.Toughness, 5);
        props2.put(TrackableProperty.MaxHandSize, 7);
        deltas.put(2, props2);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashMap<>(), 0, null);

        // Header: 8 (seq) + 4 (checksum) = 12 bytes
        // Delta key=1: 4 + 2*50 = 104 bytes
        // Delta key=2: 4 + 3*50 = 154 bytes
        // Total: 270 bytes
        int expectedSize = 12 + (4 + 2 * 50) + (4 + 3 * 50);

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta size calculation should match expected value");
    }

    @Test
    public void testDeltaSizeWithNewObjects() {
        Map<Integer, Map<TrackableProperty, Object>> deltas = new HashMap<>();
        Map<TrackableProperty, Object> deltaProps = new HashMap<>();
        deltaProps.put(TrackableProperty.Name, "Test");
        deltas.put(1, deltaProps);

        Map<Integer, Map<TrackableProperty, Object>> newObjects = new HashMap<>();
        Map<TrackableProperty, Object> newProps1 = new HashMap<>();
        newProps1.put(TrackableProperty.Name, "Card1");
        newProps1.put(TrackableProperty.Power, 2);
        newProps1.put(TrackableProperty.Toughness, 3);
        newObjects.put(100, newProps1);

        Map<TrackableProperty, Object> newProps2 = new HashMap<>();
        newProps2.put(TrackableProperty.Life, 20);
        newProps2.put(TrackableProperty.Toughness, 0);
        newProps2.put(TrackableProperty.MaxHandSize, 7);
        newProps2.put(TrackableProperty.IsAI, false);
        newObjects.put(101, newProps2);

        DeltaPacket packet = new DeltaPacket(1L, deltas, newObjects, 0, null);

        // Header: 12, Delta: (4+1*50)=54, New 100: (4+3*50)=154, New 101: (4+4*50)=204
        int expectedSize = 12 + 54 + 154 + 204;

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta packet with new objects should match expected size");
    }

    @Test
    public void testEmptyDeltaPacketSize() {
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashMap<>(), 0, null);
        int size = packet.getApproximateSize();

        // Empty packet should just have header: 8 + 4 = 12 bytes
        Assert.assertEquals(size, 12, "Empty delta packet should be exactly 12 bytes (header only)");
    }

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
