package java.nio.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** File-backed Path implementation for the MobiVM java.nio.file supply. */
final class ForgeFilePath implements Path {
    final File file;

    ForgeFilePath(File file) {
        this.file = file;
    }

    @Override
    public File toFile() {
        return file;
    }

    @Override
    public Path getParent() {
        File p = file.getParentFile();
        return p == null ? null : new ForgeFilePath(p);
    }

    @Override
    public Path getFileName() {
        return new ForgeFilePath(new File(file.getName()));
    }

    @Override
    public Path resolve(Path other) {
        return resolve(other.toString());
    }

    @Override
    public Path resolve(String other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        File o = new File(other);
        if (o.isAbsolute()) {
            return new ForgeFilePath(o);
        }
        return new ForgeFilePath(new File(file, other));
    }

    @Override
    public Path relativize(Path other) {
        String base = normalizedAbsolute(file);
        String target = normalizedAbsolute(other.toFile());
        if (target.equals(base)) {
            return new ForgeFilePath(new File(""));
        }
        String prefix = base.endsWith(File.separator) ? base : base + File.separator;
        if (target.startsWith(prefix)) {
            return new ForgeFilePath(new File(target.substring(prefix.length())));
        }
        throw new IllegalArgumentException("cannot relativize " + target + " against " + base);
    }

    private static String normalizedAbsolute(File f) {
        try {
            return f.getCanonicalPath();
        } catch (Exception e) {
            return f.getAbsolutePath();
        }
    }

    @Override
    public Path normalize() {
        return new ForgeFilePath(new File(normalizedAbsolute(file)));
    }

    @Override
    public Path toAbsolutePath() {
        return new ForgeFilePath(file.getAbsoluteFile());
    }

    @Override
    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    @Override
    public boolean startsWith(Path other) {
        return normalizedAbsolute(file).startsWith(normalizedAbsolute(other.toFile()));
    }

    @Override
    public boolean endsWith(Path other) {
        return file.getPath().endsWith(other.toFile().getPath());
    }

    @Override
    public int compareTo(Path other) {
        return file.getPath().compareTo(other.toFile().getPath());
    }

    @Override
    public Iterator<Path> iterator() {
        List<Path> parts = new ArrayList<Path>();
        for (String part : file.getPath().split(File.separator.equals("\\") ? "\\\\" : File.separator)) {
            if (!part.isEmpty()) {
                parts.add(new ForgeFilePath(new File(part)));
            }
        }
        return parts.iterator();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForgeFilePath && file.getPath().equals(((ForgeFilePath) o).file.getPath());
    }

    @Override
    public int hashCode() {
        return file.getPath().hashCode();
    }

    @Override
    public String toString() {
        return file.getPath();
    }
}
