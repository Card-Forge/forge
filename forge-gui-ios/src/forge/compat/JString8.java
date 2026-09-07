package forge.compat;

import java.util.Iterator;

/**
 * Bytecode-rewrite targets for java.lang.String / CharSequence Java 8
 * members missing from MobiVM's Java-7-era runtime. Referenced only by the
 * iOS bridge pass (tmp/jvmdg) — never from source. Bodies are
 * self-implemented: they must NOT call the APIs they replace (the bridge
 * pass also processes this class and would self-loop).
 */
public final class JString8 {
    private JString8() { }

    public static String join(CharSequence delimiter, CharSequence... elements) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(elements[i]);
        }
        return sb.toString();
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        StringBuilder sb = new StringBuilder();
        Iterator<? extends CharSequence> it = elements.iterator();
        boolean firstDone = false;
        while (it.hasNext()) {
            if (firstDone) {
                sb.append(delimiter);
            }
            sb.append(it.next());
            firstDone = true;
        }
        return sb.toString();
    }

    public static java8.util.stream.IntStream chars(final CharSequence cs) {
        return java8.util.stream.IntStreams.range(0, cs.length())
                .map(new java8.util.function.IntUnaryOperator() {
                    @Override public int applyAsInt(int i) { return cs.charAt(i); }
                });
    }

    public static java8.util.stream.IntStream codePoints(final CharSequence cs) {
        // Simple correct implementation: collect code points then stream them.
        int[] buf = new int[cs.length()];
        int n = 0;
        for (int i = 0; i < cs.length(); ) {
            int cp = Character.codePointAt(cs, i);
            buf[n++] = cp;
            i += Character.charCount(cp);
        }
        int[] exact = new int[n];
        System.arraycopy(buf, 0, exact, 0, n);
        return java8.util.J8Arrays.stream(exact);
    }
}
