package forge.adventure.util;

import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.util.BuildInfo;
import forge.util.ThreadUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Downloads the battle backgrounds declared by the selected Adventure plane. A
 * plane can list images directly in {@code <plane>/skin/battle-backgrounds.txt}:
 * <pre>
 * relative/folder/image.jpg https://example.com/direct/image.jpg
 * </pre>
 * or refer to a remote index containing paths relative to the index URL:
 * <pre>
 * {@literal @manifest} https://example.com/battle-backgrounds/index.txt
 * </pre>
 * Remote indexes are cached so a temporary server failure does not remove
 * previously downloaded backgrounds. The combined list is authoritative.
 */
public final class AdventureBackgroundDownloader {
    private static final String LIST_PATH = "skin/battle-backgrounds.txt";
    private static final String CACHE_PATH = "skin/battle_backgrounds";
    private static final String MANIFEST_PREFIX = "@manifest";
    private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

    private AdventureBackgroundDownloader() {
    }

    public static void start() {
        Config config = Config.instance();
        File listFile = new File(config.getFilePath(LIST_PATH));
        if (!listFile.isFile() || !RUNNING.add(listFile.getAbsolutePath())) {
            return;
        }

        String cachePrefix = config.getCachePrefix();
        ThreadUtil.invokeInGameThread(() -> {
            boolean changed = false;
            try {
                changed = sync(listFile, new File(cachePrefix, CACHE_PATH).toPath());
            } catch (Exception e) {
                System.err.println("Failed to sync Adventure battle backgrounds: " + e.getMessage());
            } finally {
                RUNNING.remove(listFile.getAbsolutePath());
            }
            if (changed) {
                GuiBase.getInterface().invokeInEdtLater(FSkinTexture::refreshAdventureBackgroundFiles);
            }
        });
    }

    private static boolean sync(File listFile, Path cacheRoot) throws IOException {
        Path normalizedRoot = cacheRoot.toAbsolutePath().normalize();
        Set<Path> expected = new HashSet<>();
        boolean changed = false;

        for (Pair<String, String> entry : readEntries(listFile, normalizedRoot)) {
            Path target = normalizedRoot.resolve(entry.getLeft().replace('\\', '/')).normalize();
            if (!target.startsWith(normalizedRoot) || !isImage(target)) {
                System.err.println("Ignoring invalid Adventure background path: " + entry.getLeft());
                continue;
            }
            expected.add(target);
            if (!Files.isRegularFile(target) || Files.size(target) == 0) {
                changed |= download(entry.getRight(), target);
            }
        }

        if (Files.isDirectory(normalizedRoot)) {
            try (Stream<Path> files = Files.walk(normalizedRoot)) {
                for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                        .filter(AdventureBackgroundDownloader::isImage)::iterator) {
                    if (!expected.contains(file.toAbsolutePath().normalize())) {
                        Files.deleteIfExists(file);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    private static List<Pair<String, String>> readEntries(File listFile, Path cacheRoot) throws IOException {
        List<Pair<String, String>> entries = new ArrayList<>();
        for (String line : Files.readAllLines(listFile.toPath(), StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) {
                continue;
            }
            String[] parts = value.split("\\s+", 2);
            if (MANIFEST_PREFIX.equals(parts[0])) {
                if (parts.length == 2) {
                    entries.addAll(readManifest(parts[1], cacheRoot));
                }
            } else if (parts.length == 2) {
                entries.add(Pair.of(parts[0].replace("%20", " "), parts[1]));
            } else {
                int fileName = value.lastIndexOf('/') + 1;
                entries.add(Pair.of(value.substring(fileName).replace("%20", " "), value));
            }
        }
        return entries;
    }

    private static List<Pair<String, String>> readManifest(String source, Path cacheRoot) {
        Path cached = cacheRoot.resolve(".remote-index-"
                + Integer.toUnsignedString(source.hashCode(), 16) + ".txt");
        try {
            URL manifestUrl = requireHttps(source);
            List<String> lines = readLines(manifestUrl);
            List<Pair<String, String>> entries = parseManifest(lines, manifestUrl);
            writeAtomically(cached, lines);
            return entries;
        } catch (Exception e) {
            System.err.println("Failed to download Adventure background index " + source + ": " + e.getMessage());
            try {
                if (Files.isRegularFile(cached)) {
                    return parseManifest(Files.readAllLines(cached, StandardCharsets.UTF_8), new URL(source));
                }
            } catch (Exception cachedError) {
                System.err.println("Failed to read cached Adventure background index: " + cachedError.getMessage());
            }
            return new ArrayList<>();
        }
    }

    private static List<Pair<String, String>> parseManifest(List<String> lines, URL manifestUrl) throws IOException {
        List<Pair<String, String>> entries = new ArrayList<>();
        for (String line : lines) {
            String value = line.trim();
            if (value.isEmpty() || value.startsWith("#")) {
                continue;
            }
            String[] parts = value.split("\\s+", 2);
            String imagePath = parts[0].replace("%20", " ");
            String imageUrl = parts.length == 1 ? new URL(manifestUrl, parts[0]).toString() : parts[1];
            Path relativePath = new File(imagePath).toPath().normalize();
            if (relativePath.isAbsolute() || relativePath.startsWith("..") || !isImage(relativePath)) {
                throw new IOException("Invalid background path " + imagePath);
            }
            requireHttps(imageUrl);
            entries.add(Pair.of(imagePath, imageUrl));
        }
        return entries;
    }

    private static List<String> readLines(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestProperty("Accept", "text/plain");
            connection.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + connection.getResponseCode());
            }
            String contentType = connection.getContentType();
            if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("text/html")) {
                throw new IOException("Unexpected content type " + contentType);
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines;
        } finally {
            connection.disconnect();
        }
    }

    private static void writeAtomically(Path target, List<String> lines) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.createDirectories(target.getParent());
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        moveAtomically(temporary, target);
    }

    private static boolean download(String source, Path target) {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        HttpURLConnection connection = null;
        try {
            URL url = requireHttps(source);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "image/*");
            connection.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + connection.getResponseCode());
            }
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new IOException("Unexpected content type " + contentType);
            }

            Files.createDirectories(target.getParent());
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (Files.size(temporary) == 0) {
                throw new IOException("Downloaded file is empty");
            }
            moveAtomically(temporary, target);
            System.out.println("Downloaded Adventure battle background: " + target);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to download Adventure battle background " + source + ": " + e.getMessage());
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    private static URL requireHttps(String source) throws IOException {
        URL url = new URL(source);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Only HTTPS background URLs are supported");
        }
        return url;
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
