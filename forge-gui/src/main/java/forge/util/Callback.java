package forge.util;

@FunctionalInterface
public interface Callback<T> {
    void run(T result);
}
