package java.nio.file;

public class FileAlreadyExistsException extends FileSystemException {
    public FileAlreadyExistsException(String file) {
        super(file);
    }

    public FileAlreadyExistsException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
