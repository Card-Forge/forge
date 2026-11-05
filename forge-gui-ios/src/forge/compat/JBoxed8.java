package forge.compat;

/**
 * Bytecode-rewrite targets for boxed-primitive Java 8 statics missing from
 * MobiVM's Java-7-era runtime (Integer.sum, Long.hashCode(long), ...).
 * Bodies self-implemented — must not call the APIs they replace.
 */
public final class JBoxed8 {
    private JBoxed8() { }

    public static int sum(int a, int b) { return a + b; }
    public static int max(int a, int b) { return Math.max(a, b); }
    public static int min(int a, int b) { return Math.min(a, b); }
    public static int hashCode(int v) { return v; }

    public static long sum(long a, long b) { return a + b; }
    public static long max(long a, long b) { return Math.max(a, b); }
    public static long min(long a, long b) { return Math.min(a, b); }
    public static int hashCode(long v) { return (int) (v ^ (v >>> 32)); }

    public static double sum(double a, double b) { return a + b; }
    public static double max(double a, double b) { return Math.max(a, b); }
    public static double min(double a, double b) { return Math.min(a, b); }
    public static int hashCode(double v) {
        long bits = Double.doubleToLongBits(v);
        return (int) (bits ^ (bits >>> 32));
    }

    public static int hashCode(float v) { return Float.floatToIntBits(v); }

    public static int hashCode(boolean v) { return v ? 1231 : 1237; }
    public static boolean logicalAnd(boolean a, boolean b) { return a && b; }
    public static boolean logicalOr(boolean a, boolean b) { return a || b; }

    public static float sum(float a, float b) { return a + b; }
    public static float max(float a, float b) { return Math.max(a, b); }
    public static float min(float a, float b) { return Math.min(a, b); }
    public static boolean isFinite(float v) { return Math.abs(v) <= Float.MAX_VALUE; }
    public static boolean isFinite(double v) { return Math.abs(v) <= Double.MAX_VALUE; }

    public static int compareUnsigned(int a, int b) {
        return compare(a + Integer.MIN_VALUE, b + Integer.MIN_VALUE);
    }

    public static int compareUnsigned(long a, long b) {
        long x = a + Long.MIN_VALUE;
        long y = b + Long.MIN_VALUE;
        return x < y ? -1 : (x == y ? 0 : 1);
    }

    private static int compare(int x, int y) { return x < y ? -1 : (x == y ? 0 : 1); }

    public static long toUnsignedLong(int v) { return v & 0xffffffffL; }

    public static String toUnsignedString(int v) { return Long.toString(v & 0xffffffffL); }

    public static String toUnsignedString(int v, int radix) { return Long.toString(v & 0xffffffffL, radix); }

    public static String toUnsignedString(long v) {
        if (v >= 0) {
            return Long.toString(v);
        }
        // divide-and-conquer for negative (high-bit-set) values
        long q = (v >>> 1) / 5;
        long r = v - q * 10;
        return Long.toString(q) + r;
    }

    public static int toUnsignedInt(byte v) { return v & 0xff; }

    public static int toUnsignedInt(short v) { return v & 0xffff; }
}
