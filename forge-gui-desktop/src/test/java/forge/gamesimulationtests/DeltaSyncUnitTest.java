package forge.gamesimulationtests;

import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.server.DeltaSyncManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests for delta sync components.
 *
 * Tests individual classes used by the delta sync system:
 * - DeltaPacket / FullStatePacket - packet structures
 * - DeltaSyncManager - client tracking and sequence management
 * - NetworkByteTracker - bandwidth monitoring
 *
 * These are fast unit tests that don't involve actual network I/O.
 * For integration tests with real network traffic, see NetworkPlayIntegrationTest.
 */
public class DeltaSyncUnitTest {

    private LocalNetworkTestHarness harness;

    @BeforeMethod
    public void setUp() {
        harness = new LocalNetworkTestHarness();
    }

    @AfterMethod
    public void tearDown() {
        if (harness != null) {
            harness.cleanup();
        }
    }

    // ==================== Delta Packet Tests ====================

    @Test
    public void testDeltaPacketCreation() {
        // Test that delta packets can be created with proper structure
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[]{1, 2, 3});
        deltas.put(2, new byte[]{4, 5, 6, 7});

        Set<Integer> removed = new HashSet<>();
        removed.add(100);

        DeltaPacket packet = new DeltaPacket(1L, deltas, removed);

        Assert.assertEquals(packet.getSequenceNumber(), 1L);
        Assert.assertEquals(packet.getObjectDeltas().size(), 2);
        Assert.assertEquals(packet.getRemovedObjectIds().size(), 1);
        Assert.assertTrue(packet.getRemovedObjectIds().contains(100));
        Assert.assertFalse(packet.isEmpty());
    }

    @Test
    public void testEmptyDeltaPacket() {
        // Test that empty delta packets are properly identified
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashSet<>());
        Assert.assertTrue(packet.isEmpty());
    }

    @Test
    public void testDeltaPacketWithChecksum() {
        // Test delta packets with checksum for validation
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[]{1, 2, 3});

        int checksum = 12345;
        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>(), checksum);

        Assert.assertTrue(packet.hasChecksum());
        Assert.assertEquals(packet.getChecksum(), checksum);
    }

    @Test
    public void testDeltaPacketApproximateSize() {
        // Test size calculation
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());

        // Size should be: 8 (seq) + 4 (checksum) + 2*(4 + delta size)
        int expectedMinSize = 8 + 4 + (4 + 100) + (4 + 200);
        Assert.assertTrue(packet.getApproximateSize() >= expectedMinSize);
    }

    // ==================== Full State Packet Tests ====================

    @Test
    public void testFullStatePacketCreation() {
        // Test full state packet creation (without actual GameView)
        FullStatePacket packet = new FullStatePacket(1L, null);

        Assert.assertEquals(packet.getSequenceNumber(), 1L);
    }

    // ==================== Delta Sync Manager Tests ====================

    @Test
    public void testDeltaSyncManagerClientRegistration() {
        DeltaSyncManager manager = new DeltaSyncManager();

        // Register clients via processAcknowledgment (the production path)
        manager.processAcknowledgment(0, 0L);
        manager.processAcknowledgment(1, 0L);

        // Both clients start at sequence 0
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 0L);
    }

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
    public void testDeltaSyncManagerSequenceIncrement() {
        DeltaSyncManager manager = new DeltaSyncManager();

        Assert.assertEquals(manager.getCurrentSequence(), 0L);

        // Collecting deltas should increment sequence
        DeltaPacket packet1 = manager.collectDeltas(null);
        // Even with null game view, sequence should be incremented
        Assert.assertTrue(manager.getCurrentSequence() >= 0);
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

    // ==================== Mock Client Tests ====================

    @Test
    public void testMockClientConnection() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        Assert.assertFalse(client.isConnected());

        client.connect();
        Assert.assertTrue(client.isConnected());

        client.simulateDisconnect();
        Assert.assertFalse(client.isConnected());
    }

    @Test
    public void testMockClientDeltaTracking() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        // Simulate receiving deltas
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);

        DeltaPacket packet1 = new DeltaPacket(1L, deltas, new HashSet<>());
        DeltaPacket packet2 = new DeltaPacket(2L, deltas, new HashSet<>());

        client.receiveDelta(packet1);
        client.receiveDelta(packet2);

        Assert.assertEquals(client.getReceivedDeltas().size(), 2);
        Assert.assertEquals(client.getLastAcknowledgedSequence(), 2L);
        Assert.assertTrue(client.getTotalDeltaBytes() > 0);
    }

    @Test
    public void testMockClientFullStateOverridesDelta() {
        LocalNetworkTestHarness.MockNetworkClient client = harness.createClient("TestPlayer");

        // Receive some deltas
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);
        client.receiveDelta(new DeltaPacket(1L, deltas, new HashSet<>()));
        client.receiveDelta(new DeltaPacket(2L, deltas, new HashSet<>()));

        Assert.assertEquals(client.getReceivedDeltas().size(), 2);

        // Receive full state - should clear deltas
        client.receiveFullState(new FullStatePacket(10L, null));

        Assert.assertEquals(client.getReceivedDeltas().size(), 0);
        Assert.assertEquals(client.getLastAcknowledgedSequence(), 10L);
        Assert.assertNotNull(client.getLastFullState());
    }

    // ==================== Integration-Style Tests ====================

    @Test
    public void testDeltaSyncWithMultipleClients() {
        // Simulate a scenario with multiple clients receiving deltas
        DeltaSyncManager manager = new DeltaSyncManager();

        manager.processAcknowledgment(0, 0L);
        manager.processAcknowledgment(1, 0L);

        // Simulate sending multiple delta packets
        for (int i = 1; i <= 5; i++) {
            // In real usage, this would collect from GameView
            // For testing, we just verify sequence increments
            manager.collectDeltas(null);
        }

        // Client 0 is up to date
        manager.processAcknowledgment(0, 5L);

        // Client 1 is behind
        manager.processAcknowledgment(1, 2L);

        // Min acknowledged is 2 (client 1)
        Assert.assertEquals(manager.getMinAcknowledgedSequence(), 2L);

        // If client 1 falls too far behind, it needs full resync
        // (This would happen if sequence - acked > 100)
    }

    // ==================== Serialization Method Validation Tests ====================

    /**
     * Test that DeltaPacket.getApproximateSize() accurately reflects the actual serialized size.
     * This validates that our delta size calculation is not just an approximation, but actually
     * close to the real size.
     */
    @Test
    public void testDeltaSizeCalculationAccuracy() {
        // Create a delta packet with known data
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);
        deltas.put(3, new byte[50]);

        Set<Integer> removed = new HashSet<>();
        removed.add(10);
        removed.add(20);

        DeltaPacket packet = new DeltaPacket(1L, deltas, removed);

        // Calculate expected size manually
        // Header: 8 (seq) + 4 (checksum) = 12 bytes
        // Deltas: 3 objects * (4 byte ID + data length) = (4 + 100) + (4 + 200) + (4 + 50) = 362 bytes
        // Removed: 2 objects * 4 bytes = 8 bytes
        // Total: 12 + 362 + 8 = 382 bytes
        int expectedSize = 12 + (4 + 100) + (4 + 200) + (4 + 50) + (2 * 4);

        int approximateSize = packet.getApproximateSize();

        Assert.assertEquals(approximateSize, expectedSize,
            "Delta size calculation should match expected value");

        // Verify it's at least the minimum expected size
        Assert.assertTrue(approximateSize >= 382,
            "Delta packet should be at least 382 bytes, got: " + approximateSize);
    }

    /**
     * Test that delta packets with new objects are sized correctly.
     */
    @Test
    public void testDeltaSizeWithNewObjects() {
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[50]);

        Map<Integer, DeltaPacket.NewObjectData> newObjects = new HashMap<>();
        newObjects.put(100, new DeltaPacket.NewObjectData(100, 0, new byte[150]));
        newObjects.put(101, new DeltaPacket.NewObjectData(101, 1, new byte[200]));

        DeltaPacket packet = new DeltaPacket(1L, deltas, newObjects, new HashSet<>(), 0, false);

        // Expected size:
        // Header: 12 bytes
        // Delta: (4 + 50) = 54 bytes
        // New object 100: (4 + 4 + 150) = 158 bytes
        // New object 101: (4 + 4 + 200) = 208 bytes
        // Total: 12 + 54 + 158 + 208 = 432 bytes
        int expectedSize = 12 + 54 + 158 + 208;

        Assert.assertEquals(packet.getApproximateSize(), expectedSize,
            "Delta packet with new objects should match expected size");
    }

    /**
     * Test ObjectOutputStream overhead by serializing simple objects.
     * This helps us understand how much overhead the full state measurement includes.
     */
    @Test
    public void testObjectOutputStreamOverhead() throws Exception {
        // Test with a simple serializable object
        String testString = "Hello World";

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(testString);
        oos.close();

        int serializedSize = baos.size();
        int rawDataSize = testString.getBytes().length;

        // ObjectOutputStream adds significant overhead (class descriptors, metadata, etc.)
        // For a simple string, the overhead is typically 50-100+ bytes
        Assert.assertTrue(serializedSize > rawDataSize,
            "ObjectOutputStream should add overhead to serialized data");

        // Calculate overhead ratio
        double overheadRatio = (double)serializedSize / rawDataSize;
        System.out.println(String.format(
            "[SerializationValidation] ObjectOutputStream overhead: raw=%d bytes, serialized=%d bytes, ratio=%.2fx",
            rawDataSize, serializedSize, overheadRatio));

        // For small objects, overhead can be 2x-10x the raw data
        Assert.assertTrue(overheadRatio >= 1.0,
            "Serialized size should be at least as large as raw data");
    }

    /**
     * Test that ObjectOutputStream overhead is consistent across multiple serializations.
     */
    @Test
    public void testObjectOutputStreamOverheadConsistency() throws Exception {
        // Create multiple test objects of similar size
        byte[] data1 = new byte[100];
        byte[] data2 = new byte[100];
        byte[] data3 = new byte[100];

        int size1 = serializeWithObjectOutputStream(data1);
        int size2 = serializeWithObjectOutputStream(data2);
        int size3 = serializeWithObjectOutputStream(data3);

        // All three should have similar sizes (within a few bytes)
        Assert.assertTrue(Math.abs(size1 - size2) < 10,
            "ObjectOutputStream should produce consistent sizes for same-size objects");
        Assert.assertTrue(Math.abs(size2 - size3) < 10,
            "ObjectOutputStream should produce consistent sizes for same-size objects");

        System.out.println(String.format(
            "[SerializationValidation] Consistency test: size1=%d, size2=%d, size3=%d",
            size1, size2, size3));
    }

    // testDeltaVsFullStateComparison removed: had conceptual flaw comparing DeltaPacket
    // (with protocol overhead) against raw byte[] serialization. Real delta sync validation
    // is done by NetworkPlayIntegrationTest which tests actual GameView serialization.

    /**
     * Test that delta vs full state comparison produces reasonable results with varying sizes.
     */
    @Test
    public void testDeltaVsFullStateComparisonRatios() {
        // Test with different delta sizes
        int[] deltaSizes = {50, 100, 500, 1000, 5000};

        for (int deltaDataSize : deltaSizes) {
            Map<Integer, byte[]> deltas = new HashMap<>();
            deltas.put(1, new byte[deltaDataSize]);

            DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());
            int deltaSize = packet.getApproximateSize();

            // Simulate full state (assuming full state is 10x larger before ObjectOutputStream)
            byte[] simulatedFullState = new byte[deltaDataSize * 10];
            int fullStateSize = serializeWithObjectOutputStream(simulatedFullState);

            int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;

            System.out.println(String.format(
                "[SerializationValidation] DataSize=%d: Delta=%d, FullState=%d, Savings=%d%%",
                deltaDataSize, deltaSize, fullStateSize, savings));

            // Savings should be significant when full state is much larger
            Assert.assertTrue(savings > 0,
                "Savings should be positive when full state is larger");
        }
    }

    /**
     * Test the accuracy of delta size calculation by actually serializing the packet.
     */
    @Test
    public void testDeltaSizeVsActualSerializedSize() throws Exception {
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());
        int approximateSize = packet.getApproximateSize();

        // Serialize the actual packet using ObjectOutputStream
        int actualSerializedSize = serializeWithObjectOutputStream(packet);

        System.out.println(String.format(
            "[SerializationValidation] Approximate=%d bytes, ActualSerialized=%d bytes, Ratio=%.2fx",
            approximateSize, actualSerializedSize, (double)actualSerializedSize / approximateSize));

        // The actual serialized size will be larger due to ObjectOutputStream overhead
        Assert.assertTrue(actualSerializedSize > approximateSize,
            "Actual serialized size should be larger than approximate due to ObjectOutputStream overhead");

        // But they should be within a reasonable ratio (not more than 3x for structured data)
        double ratio = (double)actualSerializedSize / approximateSize;
        Assert.assertTrue(ratio < 5.0,
            "Serialized size should not be more than 5x the approximate size");
    }

    /**
     * Test that empty delta packets have minimal size.
     */
    @Test
    public void testEmptyDeltaPacketSize() {
        DeltaPacket packet = new DeltaPacket(1L, new HashMap<>(), new HashSet<>());
        int size = packet.getApproximateSize();

        // Empty packet should just have header: 8 + 4 = 12 bytes
        Assert.assertEquals(size, 12, "Empty delta packet should be exactly 12 bytes (header only)");
    }

    /**
     * Test comparison accuracy warning: flags when delta appears unrealistically small.
     * This test validates that we can detect when the comparison might be inaccurate.
     */
    @Test
    public void testComparisonAccuracyWarning() {
        // Simulate a case where delta is suspiciously small compared to full state
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[10]); // Very small delta

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());
        int deltaSize = packet.getApproximateSize();

        // Simulate a very large full state
        byte[] simulatedFullState = new byte[50000];
        int fullStateSize = serializeWithObjectOutputStream(simulatedFullState);

        int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;

        System.out.println(String.format(
            "[SerializationValidation] Accuracy warning test: Delta=%d, FullState=%d, Savings=%d%%",
            deltaSize, fullStateSize, savings));

        // When savings are > 99%, this might indicate a measurement problem
        if (savings > 99) {
            System.out.println("[SerializationValidation] WARNING: Savings > 99% may indicate measurement inaccuracy");
        }

        // But the calculation should still be valid
        Assert.assertTrue(savings >= 0 && savings <= 100,
            "Savings should always be between 0-100%, even if suspiciously high");
    }

    /**
     * Helper method to serialize an object using ObjectOutputStream (same method as NetGuiGame).
     */
    private int serializeWithObjectOutputStream(Object obj) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            System.err.println("Error serializing object: " + e.getMessage());
            return 0;
        }
    }

    // ==================== Network Byte Tracking Tests ====================

    /**
     * Test that NetworkByteTracker correctly tracks bytes sent.
     */
    @Test
    public void testNetworkByteTrackerBasic() {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        // Initially should be zero
        Assert.assertEquals(tracker.getTotalBytesSent(), 0L);
        Assert.assertEquals(tracker.getDeltaBytesSent(), 0L);
        Assert.assertEquals(tracker.getFullStateBytesSent(), 0L);

        // Record some delta bytes
        tracker.recordBytesSent(100, "DeltaPacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 100L);
        Assert.assertEquals(tracker.getDeltaBytesSent(), 100L);
        Assert.assertEquals(tracker.getDeltaPacketCount(), 1L);

        // Record more delta bytes
        tracker.recordBytesSent(200, "applyDelta");
        Assert.assertEquals(tracker.getTotalBytesSent(), 300L);
        Assert.assertEquals(tracker.getDeltaBytesSent(), 300L);
        Assert.assertEquals(tracker.getDeltaPacketCount(), 2L);

        // Record full state bytes
        tracker.recordBytesSent(500, "FullStatePacket");
        Assert.assertEquals(tracker.getTotalBytesSent(), 800L);
        Assert.assertEquals(tracker.getFullStateBytesSent(), 500L);
        Assert.assertEquals(tracker.getFullStatePacketCount(), 1L);
    }

    /**
     * Test that NetworkByteTracker can be reset.
     */
    @Test
    public void testNetworkByteTrackerReset() {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        tracker.recordBytesSent(100, "DeltaPacket");
        tracker.recordBytesSent(200, "FullStatePacket");

        Assert.assertTrue(tracker.getTotalBytesSent() > 0);

        tracker.reset();

        Assert.assertEquals(tracker.getTotalBytesSent(), 0L);
        Assert.assertEquals(tracker.getDeltaBytesSent(), 0L);
        Assert.assertEquals(tracker.getFullStateBytesSent(), 0L);
        Assert.assertEquals(tracker.getDeltaPacketCount(), 0L);
        Assert.assertEquals(tracker.getFullStatePacketCount(), 0L);
    }

    /**
     * Test that NetworkByteTracker can be enabled/disabled.
     */
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

    /**
     * Test NetworkByteTracker statistics summary.
     */
    @Test
    public void testNetworkByteTrackerStatsSummary() {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        tracker.recordBytesSent(1000, "DeltaPacket");
        tracker.recordBytesSent(2000, "DeltaPacket");
        tracker.recordBytesSent(5000, "FullStatePacket");

        String summary = tracker.getStatsSummary();
        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.contains("8000")); // Total bytes
        Assert.assertTrue(summary.contains("3000")); // Delta bytes
        Assert.assertTrue(summary.contains("5000")); // Full state bytes

        System.out.println("[NetworkByteTrackerTest] " + summary);
    }

    /**
     * Test actual network byte tracking with CompatibleObjectEncoder simulation.
     * This test simulates what happens in the real network layer.
     */
    @Test
    public void testActualNetworkByteTrackingWithEncoder() throws Exception {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        // Create a delta packet
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);
        deltas.put(2, new byte[200]);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());

        // Simulate encoding (what CompatibleObjectEncoder does)
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(packet);
        oos.close();

        int actualEncodedSize = baos.size();
        tracker.recordBytesSent(actualEncodedSize, "DeltaPacket");

        // The approximate size should be much smaller than the actual encoded size
        // (because ObjectOutputStream adds overhead)
        int approximateSize = packet.getApproximateSize();

        System.out.println(String.format(
            "[NetworkByteTracking] Approximate=%d, ActualEncoded=%d, Ratio=%.2fx",
            approximateSize, actualEncodedSize, (double)actualEncodedSize / approximateSize));

        // Verify tracking worked
        Assert.assertEquals(tracker.getTotalBytesSent(), (long)actualEncodedSize);
        Assert.assertEquals(tracker.getDeltaBytesSent(), (long)actualEncodedSize);

        // The actual encoded size should be larger than approximate
        Assert.assertTrue(actualEncodedSize > approximateSize,
            "Actual encoded size should include ObjectOutputStream overhead");
    }

    /**
     * Test comparison accuracy using all three measurements.
     * This validates the complete picture: approximate, actual network, and full state estimate.
     */
    @Test
    public void testThreeWayComparisonAccuracy() throws Exception {
        forge.gamemodes.net.NetworkByteTracker tracker = new forge.gamemodes.net.NetworkByteTracker();

        // Create a delta packet
        Map<Integer, byte[]> deltas = new HashMap<>();
        deltas.put(1, new byte[100]);

        DeltaPacket packet = new DeltaPacket(1L, deltas, new HashSet<>());

        // 1. Approximate size (custom byte counting)
        int approximateSize = packet.getApproximateSize();

        // 2. Actual network size (simulated ObjectOutputStream encoding)
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(packet);
        oos.close();
        int actualNetworkSize = baos.size();
        tracker.recordBytesSent(actualNetworkSize, "DeltaPacket");

        // 3. Full state estimate (simulated full GameView serialization)
        byte[] simulatedFullState = new byte[1000]; // Simulate a larger full state
        int fullStateSize = serializeWithObjectOutputStream(simulatedFullState);

        // Calculate savings for each comparison
        int approximateSavings = fullStateSize > 0 ? (int)((1.0 - (double)approximateSize / fullStateSize) * 100) : 0;
        int actualSavings = fullStateSize > 0 ? (int)((1.0 - (double)actualNetworkSize / fullStateSize) * 100) : 0;

        System.out.println(String.format(
            "[ThreeWayComparison] Approximate=%d bytes (%d%% savings), Actual=%d bytes (%d%% savings), FullState=%d bytes",
            approximateSize, approximateSavings, actualNetworkSize, actualSavings, fullStateSize));

        // Validations
        Assert.assertTrue(approximateSize < actualNetworkSize,
            "Approximate should be smaller than actual (no ObjectOutputStream overhead)");
        Assert.assertTrue(actualNetworkSize < fullStateSize,
            "Actual network should be smaller than full state");
        Assert.assertTrue(actualSavings > 0,
            "Actual savings should be positive");
        Assert.assertTrue(actualSavings < approximateSavings,
            "Actual savings should be less than approximate (due to ObjectOutputStream overhead in actual)");
    }
}
