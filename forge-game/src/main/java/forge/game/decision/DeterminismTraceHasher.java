package forge.game.decision;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** SHA-256 reduction and first-divergence support for ordered canonical trace records. */
public final class DeterminismTraceHasher {
    private DeterminismTraceHasher() {
    }

    public static String sha256(final List<String> records) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Java runtime does not provide SHA-256", ex);
        }
        for (final String record : records) {
            digest.update(record.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static int firstDivergence(final List<String> left, final List<String> right) {
        final int commonLength = Math.min(left.size(), right.size());
        for (int index = 0; index < commonLength; index++) {
            if (!left.get(index).equals(right.get(index))) {
                return index;
            }
        }
        return left.size() == right.size() ? -1 : commonLength;
    }
}
