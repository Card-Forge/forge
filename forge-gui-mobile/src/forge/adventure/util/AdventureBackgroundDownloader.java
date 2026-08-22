package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import forge.assets.Assets;
import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.util.BuildInfo;
import forge.util.FileUtil;
import forge.util.ThreadUtil;

import java.io.File;
import java.io.FileInputStream;
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
import java.util.concurrent.atomic.AtomicInteger;

/** Downloads relative image paths from the HTTPS URL in {@code skin/battle-backgrounds.txt}. */
public final class AdventureBackgroundDownloader {
    private static final String LIST_PATH = "skin/battle-backgrounds.txt";
    private static final String CACHE_PATH = "skin/battle_backgrounds";
    private static final String INDEX_CACHE_PREFIX = ".remote-index-";
    private static final int MAX_INDEX_ENTRIES = 1000;
    private static final long MAX_INDEX_BYTES = 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 32L * 1024 * 1024;
    private static final long MAX_CACHE_BYTES = 256L * 1024 * 1024;
    private static final ConcurrentHashMap<String, Integer> RUNNING = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final AtomicInteger GENERATION = new AtomicInteger();

    private AdventureBackgroundDownloader() {
    }

    public static void cancel() {
        GENERATION.incrementAndGet();
    }

    public static void start() {
        Config config = Config.instance();
        if (!config.getSettingData().enableExtraBattleBackgrounds) {
            return;
        }

        FileHandle listFile = Assets.getFileHandle(config.getFilePath(LIST_PATH));
        String customSource = config.getBattleBackgroundSource();
        if ((customSource != null && customSource.isEmpty())
                || (customSource == null && (!listFile.exists() || listFile.isDirectory()))) {
            return;
        }

        File cacheRoot = new File(config.getCachePrefix(), CACHE_PATH);
        String cacheKey = cacheRoot.getAbsolutePath();
        int generation = GENERATION.get();
        Integer runningGeneration = RUNNING.putIfAbsent(cacheKey, generation);
        if (runningGeneration != null) {
            if (runningGeneration != generation) {
                PENDING.add(cacheKey);
            }
            return;
        }
        ThreadUtil.invokeInGameThread(() -> {
            boolean changed = false;
            try {
                String source = customSource == null ? readIndexUrl(listFile) : customSource;
                changed = sync(source, cacheRoot, generation);
            } catch (SyncCancelledException ignored) {
            } catch (Exception e) {
                System.err.println("Failed to sync Adventure battle backgrounds: " + e.getMessage());
            } finally {
                RUNNING.remove(cacheKey, generation);
                if (PENDING.remove(cacheKey)) {
                    start();
                }
            }
            if (changed) {
                GuiBase.getInterface().invokeInEdtLater(FSkinTexture::invalidateAdventureBackgroundFiles);
            }
        });
    }

    private static boolean sync(String source, File cacheRoot, int generation) throws IOException {
        checkCancelled(generation);
        File normalizedRoot = cacheRoot.getCanonicalFile();
        URL indexUrl = requireHttps(source);
        Set<File> expected = new HashSet<>();
        boolean changed = false;
        long totalSize = 0;

        for (String path : readIndex(indexUrl, normalizedRoot, generation)) {
            checkCancelled(generation);
            File target = new File(normalizedRoot, path).getCanonicalFile();
            if (!target.getPath().startsWith(normalizedRoot.getPath() + File.separator)) {
                throw new IOException("Invalid Adventure background path " + path);
            }
            expected.add(target);
            if (!isValidImageFile(target)) {
                if (target.exists() && !target.delete()) {
                    throw new IOException("Could not replace invalid cached background " + target);
                }
                long remaining = MAX_CACHE_BYTES - totalSize;
                if (remaining <= 0) {
                    throw new IOException("Adventure background cache exceeds size limit");
                }
                changed |= download(new URL(indexUrl, path.replace(" ", "%20")), target,
                        Math.min(MAX_IMAGE_BYTES, remaining), generation);
            }
            if (target.isFile()) {
                totalSize += target.length();
                if (totalSize > MAX_CACHE_BYTES) {
                    throw new IOException("Adventure background cache exceeds size limit");
                }
            }
        }
        checkCancelled(generation);
        return cleanCache(normalizedRoot, expected, generation) || changed;
    }

    private static boolean cleanCache(File directory, Set<File> expected, int generation) throws IOException {
        checkCancelled(generation);
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        boolean changed = false;
        for (File file : files) {
            checkCancelled(generation);
            if (file.isDirectory()) {
                changed |= cleanCache(file, expected, generation);
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

    private static List<String> readIndex(URL source, File cacheRoot, int generation) throws IOException {
        File cached = new File(cacheRoot, INDEX_CACHE_PREFIX
                + Integer.toUnsignedString(source.toString().hashCode(), 16) + ".txt");
        File temporary = new File(cached.getPath() + ".tmp");
        try {
            copy(source, temporary, MAX_INDEX_BYTES, false, generation);
            List<String> entries = parseIndex(FileUtil.readFile(temporary));
            new FileHandle(temporary).moveTo(new FileHandle(cached));
            return entries;
        } catch (SyncCancelledException e) {
            temporary.delete();
            throw e;
        } catch (Exception e) {
            temporary.delete();
            if (cached.isFile() && cached.length() <= MAX_INDEX_BYTES) {
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
            if (entries.size() > MAX_INDEX_ENTRIES) {
                throw new IOException("Adventure background index exceeds entry limit");
            }
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

    private static boolean download(URL source, File target, long maxBytes, int generation)
            throws SyncCancelledException {
        File temporary = new File(target.getPath() + ".tmp");
        try {
            copy(source, temporary, maxBytes, true, generation);
            new FileHandle(temporary).moveTo(new FileHandle(target));
            System.out.println("Downloaded Adventure battle background: " + target);
            return true;
        } catch (SyncCancelledException e) {
            temporary.delete();
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to download Adventure battle background " + source + ": " + e.getMessage());
            temporary.delete();
            return false;
        }
    }

    private static void copy(URL source, File target, long maxBytes, boolean image, int generation)
            throws IOException {
        checkCancelled(generation);
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
            long contentLength = connection.getContentLengthLong();
            if (contentLength > maxBytes) {
                throw new IOException("Download exceeds size limit");
            }

            if (!FileUtil.ensureDirectoryExists(target.getParentFile())) {
                throw new IOException("Could not create cache directory " + target.getParent());
            }
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                byte[] header = new byte[8];
                int headerLength = 0;
                long downloaded = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled(generation);
                    downloaded += count;
                    if (downloaded > maxBytes) {
                        throw new IOException("Download exceeds size limit");
                    }
                    int headerBytes = Math.min(count, header.length - headerLength);
                    if (headerBytes > 0) {
                        System.arraycopy(buffer, 0, header, headerLength, headerBytes);
                        headerLength += headerBytes;
                    }
                    output.write(buffer, 0, count);
                }
                if (image && !hasImageSignature(header, headerLength)) {
                    throw new IOException("Downloaded file is not a PNG or JPEG image");
                }
            }
            if (target.length() == 0) {
                throw new IOException("Downloaded file is empty");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isValidImageFile(File file) {
        if (!file.isFile() || file.length() == 0 || file.length() > MAX_IMAGE_BYTES) {
            return false;
        }
        byte[] header = new byte[8];
        try (InputStream input = new FileInputStream(file)) {
            int length = input.read(header);
            return hasImageSignature(header, length);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean hasImageSignature(byte[] header, int length) {
        return length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff
                || length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50
                && header[2] == 0x4e && header[3] == 0x47 && header[4] == 0x0d
                && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
    }

    private static void checkCancelled(int generation) throws SyncCancelledException {
        if (generation != GENERATION.get()
                || !Config.instance().getSettingData().enableExtraBattleBackgrounds) {
            throw new SyncCancelledException();
        }
    }

    private static final class SyncCancelledException extends IOException {
        private static final long serialVersionUID = 1L;
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
