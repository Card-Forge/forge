package forge.ios;

import org.robovm.rt.bro.Bro;
import org.robovm.rt.bro.Struct;
import org.robovm.rt.bro.annotation.Bridge;
import org.robovm.rt.bro.annotation.Library;
import org.robovm.rt.bro.annotation.StructMember;

/**
 * Raises the process file-descriptor soft limit (RLIMIT_NOFILE).
 *
 * iOS processes start with a 256-fd soft limit. Forge legitimately holds ~80
 * descriptors (card database, logs, textures in flight) and image-download
 * bursts open dozens of sockets on top; anything that pushes past the limit
 * fails with EMFILE, which surfaces confusingly: texture loads throw
 * "Couldn't load file" (blank card frames / broken match board) and even DNS
 * lookups fail ("Unable to resolve host") because getaddrinfo needs a
 * descriptor. Raising the soft limit to min(hard limit, OPEN_MAX) at launch
 * removes the cliff entirely.
 */
final class FileDescriptorLimit {
    private static final int RLIMIT_NOFILE = 8;      // sys/resource.h (Darwin)
    private static final long OPEN_MAX = 10240;      // sys/syslimits.h (Darwin)
    private static final long RLIM_INFINITY = 0x7fffffffffffffffL;

    /** struct rlimit { rlim_t rlim_cur; rlim_t rlim_max; } — two 64-bit fields on Darwin. */
    public static final class RLimit extends Struct<RLimit> {
        @StructMember(0) public native long cur();
        @StructMember(0) public native RLimit cur(long v);
        @StructMember(1) public native long max();
        @StructMember(1) public native RLimit max(long v);
    }

    @Library("c")
    private static final class LibC {
        static { Bro.bind(LibC.class); }
        @Bridge static native int getrlimit(int resource, RLimit rlp);
        @Bridge static native int setrlimit(int resource, RLimit rlp);
    }

    private FileDescriptorLimit() {
    }

    /** Raise RLIMIT_NOFILE's soft limit to min(hard, OPEN_MAX). Never throws. */
    static void raise() {
        try {
            RLimit r = new RLimit();
            if (LibC.getrlimit(RLIMIT_NOFILE, r) != 0) {
                System.err.println("FileDescriptorLimit: getrlimit failed");
                return;
            }
            long oldSoft = r.cur();
            long hard = r.max();
            long target = (hard == RLIM_INFINITY || hard > OPEN_MAX) ? OPEN_MAX : hard;
            if (target <= oldSoft) {
                System.out.println("FileDescriptorLimit: soft limit already " + oldSoft + ", leaving as-is");
                return;
            }
            r.cur(target);
            if (LibC.setrlimit(RLIMIT_NOFILE, r) == 0) {
                System.out.println("FileDescriptorLimit: raised RLIMIT_NOFILE " + oldSoft + " -> " + target
                        + " (hard=" + (hard == RLIM_INFINITY ? "infinity" : String.valueOf(hard)) + ")");
            } else {
                System.err.println("FileDescriptorLimit: setrlimit(" + target + ") failed, staying at " + oldSoft);
            }
        } catch (Throwable t) {
            // Never let fd-limit tuning break launch.
            System.err.println("FileDescriptorLimit: " + t);
        }
    }
}
