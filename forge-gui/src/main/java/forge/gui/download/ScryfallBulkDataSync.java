package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.util.BuildInfo;
import forge.util.ScryfallRateLimiter;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPInputStream;

/**
 * Warms {@link CdnUuidCache} for every set in one pass using Scryfall's Bulk Data API
 * (https://scryfall.com/docs/api/bulk-data), instead of one paginated {@code /cards/search}
 * request per set via {@link ScryfallSetSync}. A format like Standard touches dozens of sets
 * full of reprints; resolving each of those sets individually means dozens of paced (500ms)
 * search requests just to build CDN UUIDs Scryfall already publishes in one file. The
 * "default_cards" bulk file -- one row per print, in English or the card's sole language -- is a
 * single download from a direct *.scryfall.io file origin, which Scryfall documents as having no
 * rate limit at all, and covers the whole catalog at once.
 */
public final class ScryfallBulkDataSync {
    private static final String BULK_DATA_LISTING_URL = "https://api.scryfall.com/bulk-data";
    private static final String BULK_DATA_TYPE = "default_cards";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    /** Override for tests; must be a full URL to a bulk-data listing JSON document. */
    static volatile String listingUrlOverride = null;

    private ScryfallBulkDataSync() {}

    public interface ProgressListener {
        /** @param fractionDone 0.0-1.0, or -1 if not yet known (e.g. before a Content-Length is available) */
        void onProgress(String message, double fractionDone);
    }

    /**
     * Downloads and parses the "default_cards" bulk file, merging every print into
     * {@link CdnUuidCache}. Returns the number of sets written, or -1 on failure/cancellation
     * before any data was merged.
     */
    public static int sync(ProgressListener progress, BooleanSupplier cancelled) {
        String downloadUrl = findDefaultCardsUrl(progress);
        if (downloadUrl == null || cancelled.getAsBoolean()) {
            return -1;
        }

        final Map<String, Map<String, Map<String, String[]>>> bySet = new HashMap<>();
        try {
            streamAndAccumulate(downloadUrl, bySet, progress, cancelled);
        } catch (IOException e) {
            Logger.error(e, "ScryfallBulkDataSync: failed to download/parse bulk data");
            if (progress != null) {
                progress.onProgress("Bulk data download failed: " + e.getMessage(), -1);
            }
            return -1;
        }

        if (cancelled.getAsBoolean()) {
            return -1;
        }

        if (progress != null) {
            progress.onProgress("Writing " + bySet.size() + " sets to local cache...", -1);
        }
        int setCount = 0;
        for (Map.Entry<String, Map<String, Map<String, String[]>>> e : bySet.entrySet()) {
            if (cancelled.getAsBoolean()) {
                break;
            }
            CdnUuidCache.mergeSetEntriesWithFaces(e.getKey(), e.getValue());
            setCount++;
        }
        if (progress != null) {
            progress.onProgress("Synced " + setCount + " sets from Scryfall bulk data.", 1.0);
        }
        return setCount;
    }

    // -------------------------------------------------------------------------

    private static String findDefaultCardsUrl(ProgressListener progress) {
        String url = listingUrlOverride != null ? listingUrlOverride : BULK_DATA_LISTING_URL;
        if (progress != null) {
            progress.onProgress("Looking up Scryfall bulk data files...", -1);
        }
        try {
            ScryfallRateLimiter.acquire(url);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CONNECT_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
            conn.connect();

            int status = conn.getResponseCode();
            if (status == 429) {
                if (ScryfallRateLimiter.isApiUrl(url)) {
                    long retryAfter = ScryfallRateLimiter.parseRetryAfterSeconds(conn.getHeaderField("Retry-After"));
                    ScryfallRateLimiter.noteRateLimited(url, retryAfter);
                }
                return null;
            }
            if (status != 200) {
                Logger.error("ScryfallBulkDataSync: bulk-data listing returned HTTP {}", status);
                return null;
            }

            JsonObject root;
            try (InputStream is = conn.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonArray data = root.has("data") ? root.getAsJsonArray("data") : new JsonArray();
            for (var el : data) {
                JsonObject obj = el.getAsJsonObject();
                if (!obj.has("type") || !BULK_DATA_TYPE.equals(obj.get("type").getAsString())) {
                    continue;
                }
                if (obj.has("jsonl_download_uri")) {
                    return obj.get("jsonl_download_uri").getAsString();
                }
                if (obj.has("download_uri")) {
                    return obj.get("download_uri").getAsString();
                }
            }
            Logger.error("ScryfallBulkDataSync: no '{}' entry found in bulk-data listing", BULK_DATA_TYPE);
            return null;
        } catch (IOException e) {
            Logger.error(e, "ScryfallBulkDataSync: failed to fetch bulk-data listing");
            return null;
        }
    }

    private static void streamAndAccumulate(String downloadUrl, Map<String, Map<String, Map<String, String[]>>> bySet,
                                             ProgressListener progress, BooleanSupplier cancelled) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
        // downloadUrl is a *.scryfall.io direct file origin -- Scryfall documents these as having
        // no rate limit, and ScryfallRateLimiter.isApiUrl() correctly excludes this host, so no
        // acquire() call here (matches how CDN image URLs are already treated).
        conn.connect();

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + status + " for " + downloadUrl);
        }
        final long totalBytes = conn.getContentLengthLong();
        final long[] bytesRead = {0};
        final long[] cardsSeen = {0};

        InputStream counting = new FilterInputStream(conn.getInputStream()) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len);
                if (n > 0) {
                    bytesRead[0] += n;
                }
                return n;
            }

            @Override
            public int read() throws IOException {
                int n = super.read();
                if (n >= 0) {
                    bytesRead[0]++;
                }
                return n;
            }
        };

        try (InputStream gz = new GZIPInputStream(counting);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gz, StandardCharsets.UTF_8))) {
            String line;
            long lastReportAt = 0;
            while ((line = reader.readLine()) != null) {
                if (cancelled.getAsBoolean()) {
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }

                JsonObject card;
                try {
                    card = JsonParser.parseString(line).getAsJsonObject();
                } catch (Exception e) {
                    continue; // skip a malformed line rather than aborting the whole sync
                }
                addCard(bySet, card);
                cardsSeen[0]++;

                long now = System.currentTimeMillis();
                if (progress != null && now - lastReportAt >= 500) {
                    lastReportAt = now;
                    double fraction = totalBytes > 0 ? Math.min(1.0, bytesRead[0] / (double) totalBytes) : -1;
                    String pct = totalBytes > 0 ? Math.round(fraction * 100) + "%" : (bytesRead[0] / 1_000_000) + " MB";
                    progress.onProgress("Downloading Scryfall bulk card data: " + pct
                            + " (" + cardsSeen[0] + " cards processed)...", fraction);
                }
            }
        }
    }

    private static void addCard(Map<String, Map<String, Map<String, String[]>>> bySet, JsonObject card) {
        String setCode = str(card, "set");
        String cn = str(card, "collector_number");
        String lang = str(card, "lang");
        if (setCode == null || cn == null || lang == null) {
            return;
        }

        String[] frontBack = frontBackUuids(card);
        if (frontBack == null) {
            return;
        }

        bySet.computeIfAbsent(setCode, k -> new HashMap<>())
                .computeIfAbsent(cn, k -> new HashMap<>())
                .put(lang, frontBack);
    }

    /** Per-face CDN UUIDs for one card, or {@code null} if it has no usable image data yet. */
    private static String[] frontBackUuids(JsonObject card) {
        String front;
        String back = null;
        if (card.has("image_uris") && card.get("image_uris").isJsonObject()) {
            front = uuidFromUrl(normalUrl(card.getAsJsonObject("image_uris")));
        } else if (card.has("card_faces") && card.get("card_faces").isJsonArray()) {
            JsonArray faces = card.getAsJsonArray("card_faces");
            if (faces.isEmpty()) {
                return null;
            }
            JsonObject face0 = faces.get(0).getAsJsonObject();
            if (!face0.has("image_uris") || !face0.get("image_uris").isJsonObject()) {
                return null;
            }
            front = uuidFromUrl(normalUrl(face0.getAsJsonObject("image_uris")));
            if (faces.size() > 1) {
                JsonObject face1 = faces.get(1).getAsJsonObject();
                if (face1.has("image_uris") && face1.get("image_uris").isJsonObject()) {
                    back = uuidFromUrl(normalUrl(face1.getAsJsonObject("image_uris")));
                }
            }
        } else {
            return null;
        }
        return front == null ? null : new String[]{front, back};
    }

    private static String normalUrl(JsonObject imageUris) {
        return imageUris.has("normal") ? imageUris.get("normal").getAsString() : null;
    }

    private static String uuidFromUrl(String url) {
        if (url == null || !url.contains("cards.scryfall.io")) {
            return null;
        }
        int qmark = url.indexOf('?');
        String path = qmark >= 0 ? url.substring(0, qmark) : url;
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
