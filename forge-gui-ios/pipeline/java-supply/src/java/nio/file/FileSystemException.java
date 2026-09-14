package java.nio.file;

import java.io.IOException;

public class FileSystemException extends IOException {
    private final String file;
    private final String other;

    public FileSystemException(String file) {
        this.file = file;
        this.other = null;
    }

    public FileSystemException(String file, String other, String reason) {
        super(reason);
        this.file = file;
        this.other = other;
    }

    public String getFile() {
        return file;
    }

    public String getOtherFile() {
        return other;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (file != null) {
            sb.append(file);
        }
        if (other != null) {
            sb.append(" -> ").append(other);
        }
        if (super.getMessage() != null) {
            sb.append(": ").append(super.getMessage());
        }
        return sb.toString();
    }
}
