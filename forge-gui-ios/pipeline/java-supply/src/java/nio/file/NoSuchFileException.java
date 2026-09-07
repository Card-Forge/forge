package java.nio.file;

public class NoSuchFileException extends FileSystemException {
    public NoSuchFileException(String file) {
        super(file);
    }

    public NoSuchFileException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
