package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import forge.assets.Assets;
import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.ThreadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Downloads the images listed by an Adventure plane's remote battle-background index. */
public final class AdventureBackgroundDownloader {
    private static final String LIST_PATH = "skin/battle-backgrounds.txt";
    private static final String CACHE_PATH = "skin/battle_backgrounds";
    private static final String INDEX_CACHE_PREFIX = ".remote-index-";
    private static final Set<String> RUNNING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AdventureBackgroundDownloader() {
    }

    public static void start() {
        Config config = Config.instance();
        FileHandle listFile = Assets.getFileHandle(config.getFilePath(LIST_PATH));
        String listPath = listFile.path();
        if (!listFile.exists() || listFile.isDirectory() || !RUNNING.add(listPath)) {
            return;
        }

        File cacheRoot = new File(config.getCachePrefix(), CACHE_PATH);
        ThreadUtil.invokeInGameThread(() -> {
            boolean changed = false;
            try {
                changed = sync(listFile, cacheRoot);
            } catch (Exception e) {
                System.err.println("Failed to sync Adventure battle backgrounds: " + e.getMessage());
            } finally {
                RUNNING.remove(listPath);
            }
            if (changed) {
                GuiBase.getInterface().invokeInEdtLater(FSkinTexture::invalidateAdventureTextures);
            }
        });
    }

    private static boolean sync(FileHandle listFile, File cacheRoot) throws IOException {
        File normalizedRoot = cacheRoot.getCanonicalFile();
        URL indexUrl = requireHttps(readIndexUrl(listFile));
        Set<File> expected = new HashSet<>();
        boolean changed = false;

        for (String path : readIndex(indexUrl, normalizedRoot)) {
            File target = new File(normalizedRoot, path).getCanonicalFile();
            if (!target.getPath().startsWith(normalizedRoot.getPath() + File.separator)) {
                throw new IOException("Invalid Adventure background path " + path);
            }
            expected.add(target);
            if (!target.isFile() || target.length() == 0) {
                changed |= download(new URL(indexUrl, path.replace(" ", "%20")), target);
            }
        }
        return cleanCache(normalizedRoot, expected) || changed;
    }

    private static boolean cleanCache(File directory, Set<File> expected) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        boolean changed = false;
        for (File file : files) {
            if (file.isDirectory()) {
                changed |= cleanCache(file, expected);
            } else if (isImage(file.getName()) && !expected.contains(file.getCanonicalFile())) {
                if (!file.delete()) {
                    throw new IOException("Could not delete cached background " + file);
                }
                changed = true;
            }
        }
        return changed;
    }

    private static String readIndexUrl(FileHandle listFile) throws IOException {
        for (String line : listFile.readString(StandardCharsets.UTF_8.name()).split("\\r?\\n")) {
            String value = line.trim();
            if (!value.isEmpty() && !value.startsWith("#")) {
                return value;
            }
        }
        throw new IOException("No battle background index URL in " + listFile);
    }

    private static List<String> readIndex(URL source, File cacheRoot) throws IOException {
        File cached = new File(cacheRoot, INDEX_CACHE_PREFIX
                + Integer.toUnsignedString(source.toString().hashCode(), 16) + ".txt");
        File temporary = new File(cached.getPath() + ".tmp");
        try {
            copy(source, temporary);
            List<String> entries = parseIndex(FileUtil.readFile(temporary));
            new FileHandle(temporary).moveTo(new FileHandle(cached));
            return entries;
        } catch (Exception e) {
            temporary.delete();
            if (cached.isFile()) {
                System.err.println("Failed to update Adventure background index; using cached copy: " + e.getMessage());
                return parseIndex(FileUtil.readFile(cached));
            }
            throw new IOException("No usable Adventure background index", e);
        }
    }

    private static List<String> parseIndex(List<String> lines) throws IOException {
        List<String> entries = new ArrayList<>();
        for (String line : lines) {
            String value = line.trim().replace('\\', '/');
            if (value.isEmpty() || value.startsWith("#")) {
                continue;
            }
            if (!isRelativePath(value) || !isImage(value)) {
                throw new IOException("Invalid Adventure background path " + value);
            }
            entries.add(value);
        }
        return entries;
    }

    private static boolean isRelativePath(String path) {
        if (path.startsWith("/") || path.indexOf(':') >= 0) {
            return false;
        }
        for (String part : path.split("/")) {
            if ("..".equals(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean download(URL source, File target) {
        File temporary = new File(target.getPath() + ".tmp");
        try {
            copy(source, temporary);
            new FileHandle(temporary).moveTo(new FileHandle(target));
            System.out.println("Downloaded Adventure battle background: " + target);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to download Adventure battle background " + source + ": " + e.getMessage());
            temporary.delete();
            return false;
        }
    }

    private static void copy(URL source, File target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) source.openConnection();
        try {
            connection.setRequestProperty("Accept", "*/*");
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

            if (!FileUtil.ensureDirectoryExists(target.getParentFile())) {
                throw new IOException("Could not create cache directory " + target.getParent());
            }
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
            if (target.length() == 0) {
                throw new IOException("Downloaded file is empty");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isImage(String name) {
        String lowerCaseName = name.toLowerCase(Locale.ROOT);
        return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".png");
    }

    private static URL requireHttps(String source) throws IOException {
        URL url = new URL(source);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Only HTTPS background URLs are supported");
        }
        return url;
    }
}
