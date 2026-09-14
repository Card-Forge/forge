package java.nio.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.ForgeFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal Files implementation over java.io.File for the MobiVM supply.
 * Charset-taking JDK members default to UTF-8, matching real nio behavior.
 * Stream-returning members (walk, lines, list) are NOT here — the bridge
 * pass redirects those to forge.compat.JNioBridge (they must return
 * java8.util.stream types, which java.base-patched code cannot reference).
 */
public final class Files {
    private Files() { }

    public static boolean exists(Path path, LinkOption... options) {
        return path.toFile().exists();
    }

    public static boolean notExists(Path path, LinkOption... options) {
        return !path.toFile().exists();
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        return path.toFile().isDirectory();
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        return path.toFile().isFile();
    }

    public static long size(Path path) throws IOException {
        return path.toFile().length();
    }

    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = dir.toFile();
        if (!f.exists() && !f.mkdirs()) {
            throw new IOException("could not create directories " + f);
        }
        return dir;
    }

    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = dir.toFile();
        if (f.exists()) {
            throw new FileAlreadyExistsException(f.getPath());
        }
        if (!f.mkdir()) {
            throw new IOException("could not create directory " + f);
        }
        return dir;
    }

    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        File f = path.toFile();
        if (!f.createNewFile()) {
            throw new FileAlreadyExistsException(f.getPath());
        }
        return path;
    }

    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
        File tmp = File.createTempFile(prefix == null ? "tmp" : prefix, null);
        if (!tmp.delete() || !tmp.mkdir()) {
            throw new IOException("could not create temp directory");
        }
        return new ForgeFilePath(tmp);
    }

    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs) throws IOException {
        File tmp = File.createTempFile(prefix == null ? "tmp" : prefix, null, dir.toFile());
        if (!tmp.delete() || !tmp.mkdir()) {
            throw new IOException("could not create temp directory");
        }
        return new ForgeFilePath(tmp);
    }

    public static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        return new ForgeFilePath(File.createTempFile(prefix == null ? "tmp" : prefix, suffix, dir.toFile()));
    }

    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        return new ForgeFilePath(File.createTempFile(prefix == null ? "tmp" : prefix, suffix));
    }

    public static void delete(Path path) throws IOException {
        File f = path.toFile();
        if (!f.exists()) {
            throw new NoSuchFileException(f.getPath());
        }
        if (!f.delete()) {
            throw new IOException("could not delete " + f);
        }
    }

    public static boolean deleteIfExists(Path path) throws IOException {
        return path.toFile().delete();
    }

    public static Path createLink(Path link, Path existing) throws IOException {
        // Hard links are unavailable through java.io; a copy preserves the
        // observable contract tinylog's rolling writer relies on (a second
        // name with the same content).
        return copy(existing, link, StandardCopyOption.REPLACE_EXISTING);
    }

    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        File dst = target.toFile();
        boolean replace = false;
        for (CopyOption o : options) {
            if (o == StandardCopyOption.REPLACE_EXISTING) {
                replace = true;
            }
        }
        if (dst.exists() && !replace) {
            throw new FileAlreadyExistsException(dst.getPath());
        }
        if (source.toFile().isDirectory()) {
            if (!dst.exists() && !dst.mkdirs()) {
                throw new IOException("could not create " + dst);
            }
            return target;
        }
        InputStream in = new BufferedInputStream(new FileInputStream(source.toFile()));
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));
            try {
                byte[] buf = new byte[65536];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        return target;
    }

    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        File src = source.toFile();
        File dst = target.toFile();
        for (CopyOption o : options) {
            if (o == StandardCopyOption.REPLACE_EXISTING && dst.exists() && !dst.delete()) {
                throw new IOException("could not replace " + dst);
            }
        }
        if (!src.renameTo(dst)) {
            copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            if (!src.delete()) {
                throw new IOException("could not remove " + src);
            }
        }
        return target;
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        InputStream in = new FileInputStream(path.toFile());
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int r;
            while ((r = in.read(buf)) > 0) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        } finally {
            in.close();
        }
    }

    public static List<String> readAllLines(Path path) throws IOException {
        BufferedReader r = newBufferedReader(path);
        try {
            List<String> lines = new ArrayList<String>();
            String line;
            while ((line = r.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } finally {
            r.close();
        }
    }

    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        OutputStream out = newOutputStream(path, options);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
        return path;
    }

    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return new FileInputStream(path.toFile());
    }

    public static OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        boolean append = false;
        for (OpenOption o : options) {
            if (o == StandardOpenOption.APPEND) {
                append = true;
            }
        }
        return new FileOutputStream(path.toFile(), append);
    }

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), "UTF-8"));
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        boolean append = false;
        for (OpenOption o : options) {
            if (o == StandardOpenOption.APPEND) {
                append = true;
            }
        }
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile(), append), "UTF-8"));
    }

    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) throws IOException {
        walkRecursive(start.toFile(), visitor);
        return start;
    }

    private static FileVisitResult walkRecursive(File f, FileVisitor<? super Path> visitor) throws IOException {
        Path p = new ForgeFilePath(f);
        BasicFileAttributes attrs = new ForgeFileAttributes(f);
        if (f.isDirectory()) {
            FileVisitResult pre = visitor.preVisitDirectory(p, attrs);
            if (pre == FileVisitResult.SKIP_SUBTREE || pre == FileVisitResult.TERMINATE) {
                return pre == FileVisitResult.TERMINATE ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
            }
            File[] children = f.listFiles();
            if (children != null) {
                Arrays.sort(children);
                for (File c : children) {
                    FileVisitResult r = walkRecursive(c, visitor);
                    if (r == FileVisitResult.TERMINATE) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (r == FileVisitResult.SKIP_SIBLINGS) {
                        break;
                    }
                }
            }
            return visitor.postVisitDirectory(p, null);
        }
        return visitor.visitFile(p, attrs);
    }
}
