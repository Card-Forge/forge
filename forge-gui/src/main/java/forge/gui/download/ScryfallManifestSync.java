package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Client-side alternative (or supplement) to the forge-extras hosted cdn_uuid data:
 * builds or incrementally refreshes the local CDN UUID cache directly from Scryfall's
 * {@code /cards/manifest} endpoint, entirely on the user's machine.
 *
 * <p>Compared to a full bulk-data export (~100 MB for English only, ~2.5 GB for every
 * language), the manifest returns only the fields needed here (id, set code, collector
 * number, lang) in pages of up to 15,000 entries — full English coverage is currently
 * ~8 pages, roughly 25-30 MB total.
 *
 * <p>Supports incremental resync: entries are requested with {@code order=imageupdated}
 * (newest image update first), and a per-language watermark of the most recent
 * {@code image_updated_at} seen is persisted alongside the cache. A later call stops
 * as soon as it reaches an entry at or before that watermark, so a repeat sync after
 * the first full one is typically a single page.
 *
 * <p>The manifest doesn't expose the actual CDN image URL, only the card's own
 * {@code id} — so a double-faced card's back face is assumed to share the front
 * face's id. That's true for the overwhelming majority of double-faced cards (the
 * CDN URL only differs by a {@code front}/{@code back} path segment); a small number
 * of known exceptions fall back to the existing rate-limited-API path on a CDN miss,
 * same as any set/card this sync hasn't (yet) covered.
 */
public final class ScryfallManifestSync {

    private static final String MANIFEST_PATH_AND_QUERY = "?order=imageupdated&lang=%s&page=%d";
    private static final String DEFAULT_MANIFEST_URL = "https://api.scryfall.com/cards/manifest";
    private static final String WATERMARK_FILE = ".manifest-watermark.json";

    /** Scryfall documents this endpoint's rate limit as 10 requests per minute. */
    private static final long REQUEST_INTERVAL_MS = 10_000;
    private static final int  CONNECT_TIMEOUT_MS  = 15_000;

    /** Override for tests; must be a full base URL with no trailing query string. */
    static volatile String manifestBaseUrlOverride = null;

    private ScryfallManifestSync() {}

    /** Reports sync progress; {@code done} is true on the final call. */
    public interface ProgressListener {
        void onProgress(int page, int cardsMerged, boolean done);
    }

    /**
     * Builds or incrementally refreshes the local CDN UUID cache for {@code lang}
     * straight from Scryfall.
     *
     * @param lang     2-3 letter Scryfall language code, e.g. {@code "en"}
     * @param listener optional progress callback; may be {@code null}
     * @return number of (set, collector number, language) entries newly merged
     */
    public static int sync(String lang, ProgressListener listener) throws IOException, InterruptedException {
        String watermark = readWatermark(lang);
        String newestSeen = null;
        Map<String, Map<String, Map<String, String>>> bySet = new HashMap<>();
        int page = 1;
        int totalMerged = 0;

        while (true) {
            if (page > 1) Thread.sleep(REQUEST_INTERVAL_MS);
            JsonObject response = fetchPage(lang, page);
            JsonArray data = response.has("data") ? response.getAsJsonArray("data") : new JsonArray();

            boolean reachedWatermark = false;
            for (JsonElement el : data) {
                JsonObject card = el.getAsJsonObject();
                String imageUpdatedAt = str(card, "image_updated_at");
                if (imageUpdatedAt == null) continue; // no image yet; nothing useful to record

                if (newestSeen == null) newestSeen = imageUpdatedAt; // first entry overall is the newest (descending order)

                if (watermark != null && imageUpdatedAt.compareTo(watermark) <= 0) {
                    reachedWatermark = true;
                    break;
                }

                String setCode  = str(card, "set_code");
                String cn       = str(card, "collector_number");
                String id       = str(card, "id");
                String cardLang = str(card, "lang");
                if (setCode == null || cn == null || id == null || cardLang == null) continue;

                bySet.computeIfAbsent(setCode.toLowerCase(), k -> new HashMap<>())
                     .computeIfAbsent(cn, k -> new HashMap<>())
                     .put(cardLang, id);
                totalMerged++;
            }

            boolean hasMore = !reachedWatermark && response.has("has_more") && response.get("has_more").getAsBoolean();
            if (listener != null) listener.onProgress(page, totalMerged, !hasMore);
            if (!hasMore) break;
            page++;
        }

        for (Map.Entry<String, Map<String, Map<String, String>>> e : bySet.entrySet()) {
            CdnUuidCache.mergeSetEntries(e.getKey(), e.getValue());
        }
        if (newestSeen != null) writeWatermark(lang, newestSeen);
        return totalMerged;
    }

    // -------------------------------------------------------------------------

    private static JsonObject fetchPage(String lang, int page) throws IOException {
        String base = manifestBaseUrlOverride != null ? manifestBaseUrlOverride : DEFAULT_MANIFEST_URL;
        String url = base + String.format(MANIFEST_PATH_AND_QUERY, lang, page);
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(CONNECT_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.connect();
        if (conn instanceof HttpURLConnection) {
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status + " for " + url);
        }
        boolean gzipped = "gzip".equalsIgnoreCase(conn.getContentEncoding());
        try (InputStream raw = conn.getInputStream();
             InputStream is = gzipped ? new GZIPInputStream(raw) : raw;
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static String readWatermark(String lang) {
        File file = new File(CdnUuidCache.cacheDir(), WATERMARK_FILE);
        if (!file.exists()) return null;
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return str(obj, lang);
        } catch (Exception e) {
            Logger.warn("ScryfallManifestSync: corrupt watermark file {}: {}", file, e.getMessage());
            return null;
        }
    }

    private static void writeWatermark(String lang, String value) {
        File file = new File(CdnUuidCache.cacheDir(), WATERMARK_FILE);
        JsonObject obj = new JsonObject();
        if (file.exists()) {
            try {
                obj = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception ignored) {
                obj = new JsonObject();
            }
        }
        obj.addProperty(lang, value);
        try {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            Files.writeString(file.toPath(), obj.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.warn("ScryfallManifestSync: could not write watermark file {}: {}", file, e.getMessage());
        }
    }
}
