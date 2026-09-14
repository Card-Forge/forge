package java.nio.file.attribute;

import java.io.File;

/** File-backed BasicFileAttributes for the MobiVM java.nio.file supply. */
public final class ForgeFileAttributes implements BasicFileAttributes {
    private final File file;

    public ForgeFileAttributes(File file) {
        this.file = file;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(file.lastModified());
    }

    @Override
    public boolean isRegularFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return file.length();
    }
}
