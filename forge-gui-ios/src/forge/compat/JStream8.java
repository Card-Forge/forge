package forge.compat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java8.util.stream.Stream;

/**
 * Bytecode-rewrite target for the Java-16 {@code java.util.stream.Stream.toList()}
 * member, which MobiVM's Java-7-era runtime lacks. jvmdg's downgrade rewrites
 * {@code stream.toList()} into a call on its {@code J_U_S_Stream} stub, whose
 * static initializer reflectively loads {@code java.util.stream.ReferencePipeline}
 * — a class that does not exist on MobiVM — and rethrows, so the first
 * {@code toList()} actually executed on device dies with
 * {@code ExceptionInInitializerError}
 * bridge.cfg redirects that stub call to this method instead.
 *
 * <p>Referenced only by the iOS bridge pass — never from source. The body must
 * NOT call {@code Stream.toList()}: the bridge processes this class too and would
 * rewrite such a call straight back into this method (self-loop). We drain the
 * stream through its iterator, which streamsupport provides on Java 8.
 */
public final class JStream8 {
    private JStream8() { }

    public static <T> List<T> toList(final Stream<T> stream) {
        final List<T> list = new ArrayList<>();
        final Iterator<T> it = stream.iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }
}
