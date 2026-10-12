package java.nio.file.attribute;

public interface BasicFileAttributes {
    FileTime lastModifiedTime();

    boolean isRegularFile();

    boolean isDirectory();

    boolean isSymbolicLink();

    boolean isOther();

    long size();
}
