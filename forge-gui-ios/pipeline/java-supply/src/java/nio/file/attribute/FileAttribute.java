package java.nio.file.attribute;

public interface FileAttribute<T> {
    String name();

    T value();
}
