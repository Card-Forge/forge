package java.util.function;

/**
 * Empty stub: MobiVM's bundled java.util.Comparator references this type in
 * method signatures but MobiVM ships no java.util.function package, causing
 * NoClassDefFoundError when serialization reflects over Comparator (e.g.
 * EnumMap.readObject during multiplayer networking). This satisfies the
 * class loader; real functional types are remapped to java8.util.function.
 */
@FunctionalInterface
public interface ToLongFunction<T> {
    long applyAsLong(T value);
}
