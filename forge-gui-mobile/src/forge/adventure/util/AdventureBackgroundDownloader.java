package forge.adventure.util;

import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Downloads the battle backgrounds declared by the selected Adventure plane.
 * Plane authors list one image per line in {@code <plane>/skin/battle-backgrounds.txt}:
 * <pre>
 * relative/folder/image.jpg https://example.com/direct/image.jpg
 * </pre>
 * The first value is relative to {@code skin/battle_backgrounds/} and must end in
 * {@code .jpg}, {@code .jpeg}, or {@code .png}. The second value must be a direct
 * HTTPS image URL whose response has an image content type. Lines beginning with
 * {@code #} are comments. The list is authoritative, so removing an entry also
 * removes its cached file.
 */
public final class AdventureBackgroundDownloader {
    private static final String LIST_PATH = "skin/battle-backgrounds.txt";
    private static final String CACHE_PATH = "skin/battle_backgrounds";
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

        for (Pair<String, String> entry : FileUtil.readNameUrlFile(listFile.getAbsolutePath())) {
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

    private static boolean download(String source, Path target) {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(source);
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                throw new IOException("Only HTTPS background URLs are supported");
            }
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
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
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
}
