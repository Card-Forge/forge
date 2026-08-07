package forge.scryfall.uuidmap;

import com.google.gson.stream.JsonReader;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Fetches the Scryfall bulk-data index and downloads the {@code all_cards} dataset.
 *
 * <p>The bulk-data index at {@code api.scryfall.com/bulk-data} is a small JSON file
 * (~3 KB) listing available datasets and their CDN download URIs. Once we have the
 * download URI, the actual data file is served from {@code data.scryfall.io} (CDN,
 * no rate limit).
 */
public final class BulkDataFetcher {

    private static final String BULK_INDEX_URL  = "https://api.scryfall.com/bulk-data";
    private static final int    CONNECT_TIMEOUT = 10_000;
    private static final int    READ_TIMEOUT    = 300_000;  // 5 min for large downloads

    private BulkDataFetcher() {}

    /**
     * Fetches the bulk-data index and returns the download URI for the
     * {@code default_cards} dataset (one English entry per print, ~100 MB).
     * Sufficient for patching edition files; prefer this over {@link #fetchAllCardsUri}
     * for faster downloads.
     */
    public static String fetchDefaultCardsUri() throws IOException {
        System.err.println("Fetching Scryfall bulk-data index...");
        String json = fetchText(BULK_INDEX_URL);
        String uri = parseDownloadUri(json, "default_cards");
        if (uri == null) {
            throw new IOException("'default_cards' entry not found in Scryfall bulk-data index");
        }
        System.err.println("  Found: " + uri);
        return uri;
    }

    /**
     * Fetches the bulk-data index and returns the download URI for the
     * {@code all_cards} dataset (every language, every art variant, ~2.5 GB).
     */
    public static String fetchAllCardsUri() throws IOException {
        System.err.println("Fetching Scryfall bulk-data index...");
        String json = fetchText(BULK_INDEX_URL);
        String uri = parseDownloadUri(json, "all_cards");
        if (uri == null) {
            throw new IOException("'all_cards' entry not found in Scryfall bulk-data index");
        }
        System.err.println("  Found: " + uri);
        return uri;
    }

    /**
     * Downloads {@code sourceUrl} to {@code dest}, printing progress every 50 MB.
     */
    public static void downloadToFile(String sourceUrl, Path dest) throws IOException {
        System.err.println("Downloading: " + sourceUrl);
        System.err.println("         to: " + dest.toAbsolutePath());

        URL url = new URL(sourceUrl);
        HttpURLConnection conn = openConnection(url);
        long total = conn.getContentLengthLong();
        long bytesRead = 0L;
        long lastReport = 0L;
        byte[] buf = new byte[65_536];

        try (InputStream in  = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dest.toFile()))) {
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                bytesRead += n;
                if (bytesRead - lastReport >= 50L << 20) {
                    lastReport = bytesRead;
                    String progress = total > 0
                            ? String.format("%.0f%%", 100.0 * bytesRead / total)
                            : String.format("%.0f MB received", bytesRead / 1e6);
                    System.err.printf("  %.1f MB  [%s]%n", bytesRead / 1e6, progress);
                }
            }
        } finally {
            conn.disconnect();
        }
        System.err.printf("  Download complete: %.1f MB%n", bytesRead / 1e6);
    }

    /**
     * Parses the bulk-data index JSON and returns the {@code download_uri} for
     * the entry whose {@code type} matches {@code targetType}.
     */
    static String parseDownloadUri(String json, String targetType) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if ("data".equals(reader.nextName())) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        String uri = readEntry(reader, targetType);
                        if (uri != null) {
                            return uri;
                        }
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static String readEntry(JsonReader reader, String targetType) throws IOException {
        String type = null;
        String downloadUri = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if ("type".equals(name)) {
                type = reader.nextString();
            } else if ("download_uri".equals(name)) {
                downloadUri = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return targetType.equals(type) ? downloadUri : null;
    }

    private static String fetchText(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = openConnection(url);
        try {
            byte[] data = conn.getInputStream().readAllBytes();
            return new String(data, StandardCharsets.UTF_8);
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "forge-scryfall-uuid-map/1.0");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("HTTP " + code + " fetching " + url);
        }
        return conn;
    }
}
