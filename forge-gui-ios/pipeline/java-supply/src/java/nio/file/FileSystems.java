package java.nio.file;

public final class FileSystems {
    private static final FileSystem DEFAULT = new FileSystem() { };

    private FileSystems() {
    }

    public static FileSystem getDefault() {
        return DEFAULT;
    }
}
