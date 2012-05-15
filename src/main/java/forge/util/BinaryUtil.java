package forge.util;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class BinaryUtil {

    public static int bitCount(final int num) {
        int v = num;
        int c = 0;
        for (; v != 0; c++) {
            v &= v - 1;
        }
        return c;
    } // bit count
}
