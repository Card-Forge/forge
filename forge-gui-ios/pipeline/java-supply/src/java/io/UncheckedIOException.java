package java.io;

/**
 * Java 8 class missing from MobiVM's Java-7-era runtime; referenced by
 * commons-lang3, jvmdg stubs, and stream-close paths. Faithful minimal
 * implementation.
 */
public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(String message, IOException cause) {
        super(message, cause);
    }

    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
