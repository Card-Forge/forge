package forge.compat;

/**
 * Bytecode-rewrite target for ThreadLocal.withInitial (Java 8), missing from
 * MobiVM's Java-7-era runtime. The Supplier parameter type is written as
 * java8.util.function.Supplier directly because the bridge pass remaps all
 * java.util.function references to java8.util.function — call-site
 * descriptors will match after remapping.
 */
public final class JThreadLocal8 {
    private JThreadLocal8() { }

    public static <S> ThreadLocal<S> withInitial(final java8.util.function.Supplier<? extends S> supplier) {
        return new ThreadLocal<S>() {
            @Override
            protected S initialValue() {
                return supplier.get();
            }
        };
    }
}
