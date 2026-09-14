package forge.compat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Bytecode-rewrite targets for assorted members missing from MobiVM's
 * runtime that don't fit the other compat holders:
 *  - StringBuilder.append(byte): JvmDowngrader's generated record toString
 *    emits this signature, which exists in NO Java version — widen to int.
 *  - Type.getTypeName / Class.getTypeName (Java 8).
 *  - Files.walk (returns a stream, so it can't live in the java.base-patched
 *    supply, which cannot reference java8.util types).
 *  - FileChannel.open(Path, OpenOption...) (Java 8 static).
 */
public final class JMisc8 {
    private JMisc8() { }

    public static StringBuilder append(StringBuilder sb, byte b) {
        return sb.append((int) b);
    }

    public static java.util.Date from(java.time.Instant instant) {
        return new java.util.Date(instant.toEpochMilli());
    }

    public static String getBaseBundleName(java.util.ResourceBundle bundle) {
        // Java 8 API used by Forge only in a success log line; a best-effort
        // name keeps the message useful without runtime support.
        try {
            return String.valueOf(java.util.ResourceBundle.class
                    .getMethod("getBaseBundleName").invoke(bundle));
        } catch (Throwable t) {
            return bundle.getLocale() != null ? bundle.getLocale().toString() : "unknown";
        }
    }

    public static String getTypeName(Type type) {
        if (type instanceof Class) {
            Class<?> c = (Class<?>) type;
            if (c.isArray()) {
                StringBuilder sb = new StringBuilder();
                Class<?> el = c;
                while (el.isArray()) {
                    sb.append("[]");
                    el = el.getComponentType();
                }
                return el.getName() + sb;
            }
            return c.getName();
        }
        return type.toString();
    }

    public static java8.util.stream.Stream<Path> walk(Path start, FileVisitOption... options) {
        List<Path> acc = new ArrayList<>();
        collect(start.toFile(), acc);
        return java8.util.stream.StreamSupport.stream(acc);
    }

    private static void collect(File f, List<Path> acc) {
        acc.add(java.nio.file.Paths.get(f.getPath()));
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                java.util.Arrays.sort(children);
                for (File c : children) {
                    collect(c, acc);
                }
            }
        }
    }

    public static Path toPath(File file) {
        return java.nio.file.Paths.get(file.getPath());
    }

    public static FileChannel open(Path path, java.nio.file.OpenOption... options) throws IOException {
        boolean write = false;
        boolean append = false;
        boolean truncate = false;
        for (java.nio.file.OpenOption o : options) {
            if (o == java.nio.file.StandardOpenOption.WRITE || o == java.nio.file.StandardOpenOption.CREATE
                    || o == java.nio.file.StandardOpenOption.CREATE_NEW) {
                write = true;
            } else if (o == java.nio.file.StandardOpenOption.APPEND) {
                write = true;
                append = true;
            } else if (o == java.nio.file.StandardOpenOption.TRUNCATE_EXISTING) {
                truncate = true;
            }
        }
        RandomAccessFile raf = new RandomAccessFile(path.toFile(), write ? "rw" : "r");
        if (truncate) {
            raf.setLength(0);
        }
        FileChannel ch = raf.getChannel();
        if (append) {
            ch.position(ch.size());
        }
        return ch;
    }
}
