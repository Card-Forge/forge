package forge.gamemodes.net;

import forge.util.IHasForgeLog;

import java.io.ObjectInputStream;
import java.lang.reflect.Method;

/**
 * Resource bounds for a single frame arriving on the multiplayer wire.
 * {@link WireClassFilter} says which classes may be named; these bound what a
 * frame may ask the JVM to do while building them — an array is allocated from
 * its declared length before any element is read, handles accumulate in the
 * reference table, and nesting is walked recursively.
 *
 * <p>Installed reflectively because JEP 290 is the only hook for this and
 * {@code java.io.ObjectInputFilter} reached Android at API 33, above the
 * {@code minSdkVersion=26} that {@code forge-gui-android} declares. Where the
 * API is absent this is a no-op rather than a class-load failure. The pattern
 * ends in {@code *}, admitting every class, so class filtering stays with
 * {@link WireClassFilter}, where it is portable.
 *
 * <p>The limits are measured; the commit message carries the derivation. They
 * sit orders of magnitude clear of real traffic because a false rejection drops
 * a frame mid-game, while the attacks need values far larger still.
 */
final class WireStreamLimits implements IHasForgeLog {

    /** Measured max 3,635. */
    private static final int MAX_ARRAY_LENGTH =
            Integer.getInteger("forge.net.maxWireArrayLength", 1 << 20);
    /** Measured max 31,378, and it scales with board size and seat count. */
    private static final int MAX_REFERENCES =
            Integer.getInteger("forge.net.maxWireReferences", 1_000_000);
    /** Measured max 15, flat across the run; a stack overflow needs thousands. */
    private static final int MAX_DEPTH =
            Integer.getInteger("forge.net.maxWireDepth", 100);

    private static final Method SET_FILTER = findSetter();
    private static final Object FILTER = createFilter();

    private WireStreamLimits() {
    }

    private static Method findSetter() {
        try {
            return ObjectInputStream.class.getMethod("setObjectInputFilter",
                    Class.forName("java.io.ObjectInputFilter"));
        } catch (final ReflectiveOperationException absent) {
            return null; // Android below API 33, or the iOS downgrader.
        }
    }

    private static Object createFilter() {
        if (SET_FILTER == null) {
            return null;
        }
        try {
            final Class<?> filter = Class.forName("java.io.ObjectInputFilter");
            final Method config = Class.forName("java.io.ObjectInputFilter$Config")
                    .getMethod("createFilter", String.class);
            return filter.cast(config.invoke(null, "maxarray=" + MAX_ARRAY_LENGTH
                    + ";maxrefs=" + MAX_REFERENCES
                    + ";maxdepth=" + MAX_DEPTH
                    + ";*"));
        } catch (final ReflectiveOperationException e) {
            netLog.warn("Wire stream limits unavailable: {}", e.toString());
            return null;
        }
    }

    /** Installs the bounds if this platform has the API; otherwise a no-op. */
    static void applyTo(final ObjectInputStream in) {
        if (FILTER == null) {
            return;
        }
        try {
            SET_FILTER.invoke(in, FILTER);
        } catch (final ReflectiveOperationException e) {
            netLog.warn("Could not install wire stream limits: {}", e.toString());
        }
    }
}
