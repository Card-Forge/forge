package java.nio.file;

import java.io.File;

/**
 * Minimal java.nio.file supply for MobiVM (whose Java-7-era runtime lacks the
 * package entirely, despite java.nio.file being a Java 7 API). Backed by
 * java.io.File. Only the surface demanded by the MobiVmLinkAudit is provided;
 * extend when the audit reports new members after an upstream merge.
 */
public interface Path extends Comparable<Path>, Iterable<Path> {
    File toFile();
    Path getParent();
    Path getFileName();
    Path resolve(Path other);
    Path resolve(String other);
    Path relativize(Path other);
    Path normalize();
    Path toAbsolutePath();
    boolean isAbsolute();
    boolean startsWith(Path other);
    boolean endsWith(Path other);
}
