package crucible.oracle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Emits a golden trace of java.util.Random behaviour for pkg/javarand to diff against.
 *
 * <p>This is the M1 exit gate. Forge routes every draw through MyRandom, whose default is a
 * SecureRandom; deterministic simulation swaps in a java.util.Random via setRandom. Only the
 * latter is reproducible, so only the latter is what Go has to match.
 *
 * <p>Output is one record per line, tab-separated, so a diff points at the exact draw that
 * diverged rather than at the whole file.
 */
public final class RandomDumper {
    private static final int DRAWS = 1_000_000;

    public static void main(String[] args) {
        StringBuilder out = new StringBuilder(1 << 22);

        // Fixed seeds, including the edge cases: zero, negative, and the boundaries.
        long[] seeds = {0L, 1L, 42L, -1L, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE};

        for (long seed : seeds) {
            Random r = new Random(seed);
            for (int i = 0; i < 16; i++) {
                out.append("nextInt\t").append(seed).append('\t').append(i)
                   .append('\t').append(r.nextInt()).append('\n');
            }
            r = new Random(seed);
            for (int i = 0; i < 16; i++) {
                out.append("nextLong\t").append(seed).append('\t').append(i)
                   .append('\t').append(r.nextLong()).append('\n');
            }
            r = new Random(seed);
            for (int i = 0; i < 16; i++) {
                out.append("nextBoolean\t").append(seed).append('\t').append(i)
                   .append('\t').append(r.nextBoolean()).append('\n');
            }
            r = new Random(seed);
            for (int i = 0; i < 16; i++) {
                out.append("nextDouble\t").append(seed).append('\t').append(i)
                   .append('\t').append(Double.doubleToRawLongBits(r.nextDouble())).append('\n');
            }
            r = new Random(seed);
            for (int i = 0; i < 16; i++) {
                out.append("nextFloat\t").append(seed).append('\t').append(i)
                   .append('\t').append(Float.floatToRawIntBits(r.nextFloat())).append('\n');
            }
            // Bounds covering the power-of-two fast path and the rejection loop.
            int[] bounds = {1, 2, 3, 7, 8, 60, 100, 1024, 1000000, Integer.MAX_VALUE};
            for (int bound : bounds) {
                r = new Random(seed);
                for (int i = 0; i < 8; i++) {
                    out.append("nextIntBound\t").append(seed).append('\t').append(bound)
                       .append('\t').append(i).append('\t').append(r.nextInt(bound)).append('\n');
                }
            }
            // percentTrue, as Forge calls it.
            r = new Random(seed);
            for (int p = 0; p <= 100; p += 25) {
                out.append("percentTrue\t").append(seed).append('\t').append(p)
                   .append('\t').append(p > r.nextInt(100)).append('\n');
            }
            // Collections.shuffle over a 60-card deck, the real use.
            for (int size : new int[] {0, 1, 2, 7, 60, 100}) {
                List<Integer> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(i);
                Collections.shuffle(list, new Random(seed));
                out.append("shuffle\t").append(seed).append('\t').append(size).append('\t');
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) out.append(',');
                    out.append(list.get(i));
                }
                out.append('\n');
            }
        }

        // Bulk stream: one seed, a million consecutive draws, checksummed.
        // Catches drift that a 16-draw prefix would miss.
        Random bulk = new Random(20260907L);
        long sum = 0;
        for (int i = 0; i < DRAWS; i++) sum = sum * 31 + bulk.nextInt();
        out.append("bulkInt\t20260907\t").append(DRAWS).append('\t').append(sum).append('\n');

        Random bulkB = new Random(20260907L);
        long sumB = 0;
        for (int i = 0; i < DRAWS; i++) sumB = sumB * 31 + bulkB.nextInt(60);
        out.append("bulkIntBound60\t20260907\t").append(DRAWS).append('\t').append(sumB).append('\n');

        System.out.print(out);
    }
}
