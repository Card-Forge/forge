package forge.compat;

/**
 * Bytecode-rewrite targets for java.util.function interface statics that
 * streamsupport's companion classes don't provide (Predicates has
 * and/or/negate but no isEqual).
 */
public final class JFunction8 {
    private JFunction8() { }

    public static <T> java8.util.function.Predicate<T> isEqual(final Object target) {
        return new java8.util.function.Predicate<T>() {
            @Override
            public boolean test(T t) {
                return target == null ? t == null : target.equals(t);
            }
        };
    }
}
