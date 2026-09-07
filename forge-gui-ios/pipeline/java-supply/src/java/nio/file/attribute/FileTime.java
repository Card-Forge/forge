package java.nio.file.attribute;

public final class FileTime implements Comparable<FileTime> {
    private final long millis;

    private FileTime(long millis) {
        this.millis = millis;
    }

    public static FileTime fromMillis(long value) {
        return new FileTime(value);
    }

    public long toMillis() {
        return millis;
    }

    @Override
    public int compareTo(FileTime other) {
        return millis < other.millis ? -1 : (millis == other.millis ? 0 : 1);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FileTime && millis == ((FileTime) o).millis;
    }

    @Override
    public int hashCode() {
        return (int) (millis ^ (millis >>> 32));
    }

    @Override
    public String toString() {
        return new java.util.Date(millis).toString();
    }
}
