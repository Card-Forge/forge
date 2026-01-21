package forge.gamemodes.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for compressing and decompressing network packets.
 * Provides GZIP compression with metrics tracking.
 */
public class NetworkCompressionUtil {

    // Compression settings
    public static final int COMPRESSION_THRESHOLD_BYTES = 512;
    public static final boolean ENABLE_COMPRESSION = true;

    // Debug flag to disable compression (useful for troubleshooting)
    private static boolean debugDisableCompression = false;

    // Compression type constants (for future expansion)
    public static final byte COMPRESSION_NONE = 0x00;
    public static final byte COMPRESSION_GZIP = 0x01;

    // Metrics tracking
    private static long totalUncompressedBytes = 0;
    private static long totalCompressedBytes = 0;
    private static long compressionCount = 0;
    private static long poorCompressionCount = 0;

    /**
     * Set debug flag to disable compression for troubleshooting.
     */
    public static void setDebugDisableCompression(boolean disable) {
        debugDisableCompression = disable;
        if (disable) {
            NetworkDebugLogger.warn("[Compression] Debug mode: Compression DISABLED");
        } else {
            NetworkDebugLogger.info("[Compression] Debug mode: Compression ENABLED");
        }
    }

    /**
     * Compress data using GZIP if it meets the threshold.
     * Returns a PacketData object containing compression metadata.
     */
    public static PacketData compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return new PacketData(COMPRESSION_NONE, data, data);
        }

        // Check if compression should be applied
        if (debugDisableCompression || !ENABLE_COMPRESSION || data.length < COMPRESSION_THRESHOLD_BYTES) {
            return new PacketData(COMPRESSION_NONE, data, data);
        }

        // Compress using GZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length / 2); // Estimate 50% compression
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
        }

        byte[] compressed = baos.toByteArray();

        // Update metrics
        totalUncompressedBytes += data.length;
        totalCompressedBytes += compressed.length;
        compressionCount++;

        // Calculate compression ratio
        double ratio = 1.0 - ((double) compressed.length / data.length);

        // Log if compression is poor (<50% reduction)
        if (ratio < 0.5) {
            poorCompressionCount++;
            NetworkDebugLogger.warn("[Compression] Poor compression ratio: %d bytes → %d bytes (%.1f%% reduction)",
                data.length, compressed.length, ratio * 100);
        }

        return new PacketData(COMPRESSION_GZIP, data, compressed);
    }

    /**
     * Decompress GZIP-compressed data.
     * @param compressed The compressed data
     * @param expectedSize The expected uncompressed size (for validation)
     * @return The uncompressed data
     * @throws IOException If decompression fails or size mismatch occurs
     */
    public static byte[] decompress(byte[] compressed, int expectedSize) throws IOException {
        if (compressed == null || compressed.length == 0) {
            throw new IOException("Compressed data is null or empty");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(expectedSize);
        try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }

        byte[] uncompressed = baos.toByteArray();

        // Validate size matches expected
        if (uncompressed.length != expectedSize) {
            throw new IOException(String.format(
                "Decompressed size mismatch: expected %d bytes, got %d bytes",
                expectedSize, uncompressed.length));
        }

        return uncompressed;
    }

    /**
     * Get overall compression ratio across all packets.
     */
    public static double getOverallCompressionRatio() {
        if (totalUncompressedBytes == 0) {
            return 0.0;
        }
        return 1.0 - ((double) totalCompressedBytes / totalUncompressedBytes);
    }

    /**
     * Get compression statistics.
     */
    public static String getCompressionStats() {
        if (compressionCount == 0) {
            return "No packets compressed yet";
        }

        double ratio = getOverallCompressionRatio();
        return String.format(
            "Compressed %d packets: %,d bytes → %,d bytes (%.1f%% reduction, %d poor compressions)",
            compressionCount,
            totalUncompressedBytes,
            totalCompressedBytes,
            ratio * 100,
            poorCompressionCount
        );
    }

    /**
     * Reset compression statistics.
     */
    public static void resetStats() {
        totalUncompressedBytes = 0;
        totalCompressedBytes = 0;
        compressionCount = 0;
        poorCompressionCount = 0;
    }

    /**
     * Container class for packet data with compression metadata.
     */
    public static class PacketData {
        public final byte compressionType;
        public final byte[] uncompressed;
        public final byte[] compressed;

        public PacketData(byte compressionType, byte[] uncompressed, byte[] compressed) {
            this.compressionType = compressionType;
            this.uncompressed = uncompressed;
            this.compressed = compressed;
        }

        public boolean isCompressed() {
            return compressionType != COMPRESSION_NONE;
        }

        public int getUncompressedSize() {
            return uncompressed.length;
        }

        public int getCompressedSize() {
            return compressed.length;
        }

        public double getCompressionRatio() {
            if (uncompressed.length == 0) {
                return 0.0;
            }
            return 1.0 - ((double) compressed.length / uncompressed.length);
        }
    }
}
