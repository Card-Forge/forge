package forge.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Java-compatible seeded random that records audit-only draw metadata without drawing again. */
public final class DeterminismAuditRandom extends Random {
    private static final long serialVersionUID = 1L;
    private static final String TRACE_VERSION = "RNG_TRACE_V1";

    private final List<Draw> draws = new ArrayList<>();

    public DeterminismAuditRandom(final long seed) {
        super(seed);
    }

    @Override
    protected synchronized int next(final int bits) {
        final int value = super.next(bits);
        draws.add(new Draw(draws.size(), bits, value, firstForgeCallSite()));
        return value;
    }

    public synchronized long getDrawCount() {
        return draws.size();
    }

    public synchronized List<String> getCanonicalRecords(final long fromInclusive, final long toExclusive) {
        return selectedDraws(fromInclusive, toExclusive).stream().map(Draw::canonical).toList();
    }

    public synchronized List<String> getDiagnosticRecords(final long fromInclusive, final long toExclusive) {
        return selectedDraws(fromInclusive, toExclusive).stream().map(Draw::diagnostic).toList();
    }

    private List<Draw> selectedDraws(final long fromInclusive, final long toExclusive) {
        if (fromInclusive < 0L || toExclusive < fromInclusive || toExclusive > draws.size()) {
            throw new IndexOutOfBoundsException("Invalid RNG trace range " + fromInclusive + ".." + toExclusive);
        }
        return List.copyOf(draws.subList(Math.toIntExact(fromInclusive), Math.toIntExact(toExclusive)));
    }

    private static String firstForgeCallSite() {
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if (frame.getClassName().startsWith("forge.")
                    && !frame.getClassName().equals(DeterminismAuditRandom.class.getName())) {
                return frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return "NO_FORGE_FRAME";
    }

    private record Draw(long index, int bits, int value, String callSite) {
        private String canonical() {
            return String.join("|", TRACE_VERSION, Long.toString(index), Integer.toString(bits),
                    Integer.toUnsignedString(value));
        }

        private String diagnostic() {
            return canonical() + "|" + callSite;
        }
    }
}
