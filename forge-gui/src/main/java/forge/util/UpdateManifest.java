package forge.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/** A small, dependency-free manifest for the China update mirror. */
public final class UpdateManifest {
    public static final int SCHEMA_VERSION = 1;

    public static final class Artifact {
        private final String version;
        private final String url;
        private final long size;
        private final String sha256;

        private Artifact(final String version0, final String url0, final long size0, final String sha2560) {
            version = version0;
            url = url0;
            size = size0;
            sha256 = sha2560;
        }

        public String version() {
            return version;
        }

        public String url() {
            return url;
        }

        public long size() {
            return size;
        }

        public String sha256() {
            return sha256;
        }

        public boolean isPresent() {
            return url != null && !url.isBlank();
        }
    }

    private final String sourceUrl;
    private final String version;
    private final String publishedAt;
    private final Artifact android;
    private final Artifact desktop;
    private final Artifact assets;

    private UpdateManifest(final String sourceUrl0, final Properties properties) throws IOException {
        sourceUrl = sourceUrl0;
        final int schema = parseInt(properties.getProperty("schema"), -1);
        if (schema != SCHEMA_VERSION) {
            throw new IOException("Unsupported update manifest schema: " + schema);
        }
        version = required(properties, "version");
        publishedAt = properties.getProperty("publishedAt", "").trim();
        android = readArtifact(properties, "android", version);
        desktop = readArtifact(properties, "desktop", version);
        assets = readArtifact(properties, "assets", version);
    }

    public static UpdateManifest load(final String manifestUrl) throws IOException {
        final String content = FileUtil.readFileToString(new URL(manifestUrl));
        if (content == null || content.isBlank()) {
            throw new IOException("Empty update manifest: " + manifestUrl);
        }
        final Properties properties = new Properties();
        properties.load(new StringReader(content));
        return new UpdateManifest(manifestUrl, properties);
    }

    public String version() {
        return version;
    }

    public String publishedAt() {
        return publishedAt;
    }

    public Artifact android() {
        return android;
    }

    public Artifact desktop() {
        return desktop;
    }

    public Artifact assets() {
        return assets;
    }

    public String resolveUrl(final Artifact artifact) throws MalformedURLException {
        if (artifact == null || !artifact.isPresent()) {
            return "";
        }
        return new URL(new URL(sourceUrl), artifact.url()).toString();
    }

    public static boolean verify(final File file, final long expectedSize, final String expectedSha256) {
        if (file == null || !file.isFile()) {
            return false;
        }
        if (expectedSize > 0 && file.length() != expectedSize) {
            return false;
        }
        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return true;
        }
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var stream = Files.newInputStream(file.toPath())) {
                final byte[] buffer = new byte[64 * 1024];
                int length;
                while ((length = stream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, length);
                }
            }
            final StringBuilder hash = new StringBuilder(64);
            for (byte value : digest.digest()) {
                hash.append(String.format("%02x", value & 0xff));
            }
            return hash.toString().equalsIgnoreCase(expectedSha256.trim());
        } catch (IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static Artifact readArtifact(final Properties properties, final String prefix,
                                         final String defaultVersion) throws IOException {
        final String url = properties.getProperty(prefix + ".url", "").trim();
        if (url.isEmpty()) {
            return new Artifact("", "", 0, "");
        }
        final String artifactVersion = properties.getProperty(prefix + ".version", defaultVersion).trim();
        final long size = parseLong(properties.getProperty(prefix + ".size"), 0);
        final String sha256 = required(properties, prefix + ".sha256");
        if (!sha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IOException("Invalid SHA-256 for " + prefix);
        }
        return new Artifact(artifactVersion, url, size, sha256);
    }

    private static String required(final Properties properties, final String key) throws IOException {
        final String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) {
            throw new IOException("Missing update manifest property: " + key);
        }
        return value;
    }

    private static int parseInt(final String value, final int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(final String value, final long fallback) {
        try {
            return Long.parseLong(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
