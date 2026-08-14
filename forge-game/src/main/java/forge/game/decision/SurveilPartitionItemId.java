package forge.game.decision;

final class SurveilPartitionItemId {
    private SurveilPartitionItemId() {
    }

    static long opaqueItemId(final int canonicalRank) {
        if (canonicalRank < 1) {
            throw new IllegalArgumentException("canonicalRank must be positive");
        }
        long z = 0x9E3779B97F4A7C15L ^ (long) canonicalRank;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
