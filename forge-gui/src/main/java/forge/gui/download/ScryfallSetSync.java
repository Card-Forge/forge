package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Builds the local CDN UUID cache for a single set, on demand, straight from
 * Scryfall's card search API — the client-side equivalent of what the old
 * forge-extras/CLI generator did against a full bulk-data export, but scoped
 * to exactly the set a lookup just missed on, so nothing needs to be
 * pre-generated or hosted anywhere.
 *
 * <p>The search API returns each card's own {@code image_uris} (single-faced) or
 * {@code card_faces[].image_uris} (double-faced), so the UUID is read out of the
 * actual CDN image URL for each face rather than assumed from the card's {@code id}.
 * That matters for a small number of double-faced cards (e.g. a couple of Secret
 * Lair prints) whose back face uses a genuinely different artwork UUID than the
 * card's {@code id}.
 *
 * <p>A card this doesn't find (e.g. released after the set was last synced) is left
 * for {@link CdnUuidCache} to record as a timestamped miss and retry later, rather
 * than retried here.
 */
final class ScryfallSetSync {

    private static final String DEFAULT_SEARCH_URL = "https://api.scryfall.com/cards/search";

    /** Scryfall's general API guidance: 50-100ms between requests. */
    private static final long REQUEST_INTERVAL_MS = 100;
    private static final int  CONNECT_TIMEOUT_MS  = 15_000;

    /** Override for tests; must be a full base URL with no trailing query string. */
    static volatile String searchBaseUrlOverride = null;

    private ScryfallSetSync() {}

    /**
     * Fetches every print of {@code setCode} (all languages) from Scryfall and merges
     * the result into {@link CdnUuidCache}'s local cache for that set.
     *
     * @return {@code true} if any card data was found and cached for this set
     */
    static boolean sync(String setCode) {
        Map<String, Map<String, String[]>> byCn = new HashMap<>();
        String url = searchUrl(setCode);
        try {
            while (url != null) {
                JsonObject page = fetchJson(url);
                if (page == null) break; // 404: no such set / no matching cards

                JsonArray data = page.has("data") ? page.getAsJsonArray("data") : new JsonArray();
                for (JsonElement el : data) {
                    addCard(byCn, el.getAsJsonObject());
                }

                boolean hasMore = page.has("has_more") && page.get("has_more").getAsBoolean();
                url = hasMore && page.has("next_page") ? page.get("next_page").getAsString() : null;
                if (url != null) Thread.sleep(REQUEST_INTERVAL_MS);
            }
        } catch (Exception e) {
            Logger.debug("ScryfallSetSync: could not build set '{}' from Scryfall: {}", setCode, e.getMessage());
        }

        if (byCn.isEmpty()) return false;
        CdnUuidCache.mergeSetEntriesWithFaces(setCode, byCn);
        return true;
    }

    // -------------------------------------------------------------------------

    private static void addCard(Map<String, Map<String, String[]>> byCn, JsonObject card) {
        String cn   = str(card, "collector_number");
        String lang = str(card, "lang");
        String id   = str(card, "id");
        if (cn == null || lang == null || id == null) return;

        String front;
        String back = null;
        if (card.has("image_uris") && card.get("image_uris").isJsonObject()) {
            front = uuidFromUrl(normalUrl(card.getAsJsonObject("image_uris")), id);
        } else if (card.has("card_faces") && card.get("card_faces").isJsonArray()) {
            JsonArray faces = card.getAsJsonArray("card_faces");
            if (faces.size() == 0) return;
            JsonObject face0 = faces.get(0).getAsJsonObject();
            if (!face0.has("image_uris") || !face0.get("image_uris").isJsonObject()) return;
            front = uuidFromUrl(normalUrl(face0.getAsJsonObject("image_uris")), id);
            if (faces.size() > 1) {
                JsonObject face1 = faces.get(1).getAsJsonObject();
                if (face1.has("image_uris") && face1.get("image_uris").isJsonObject()) {
                    back = uuidFromUrl(normalUrl(face1.getAsJsonObject("image_uris")), id);
                }
            }
        } else {
            return; // no image data for this card
        }
        if (front == null) return;

        byCn.computeIfAbsent(cn, k -> new HashMap<>()).put(lang, new String[]{front, back});
    }

    /** Reads an {@code image_uris} object and returns the value of the {@code normal} key. */
    private static String normalUrl(JsonObject imageUris) {
        return imageUris.has("normal") ? imageUris.get("normal").getAsString() : null;
    }

    /**
     * Extracts the UUID segment from a Scryfall CDN image URL.
     *
     * <p>URL format: {@code https://cards.scryfall.io/normal/front/4/e/{uuid}.jpg?timestamp}
     *
     * <p>Parsing the UUID from the URL (rather than using the card's {@code id} field directly)
     * correctly handles the rare double-faced cards where both faces share an artwork UUID
     * that differs from the card's own {@code id}. Falls back to {@code cardId} for non-CDN
     * URLs such as {@code errors.scryfall.com/soon.jpg} (placeholder for missing images).
     */
    private static String uuidFromUrl(String url, String cardId) {
        if (url == null) return null;
        if (!url.contains("cards.scryfall.io")) return cardId;
        int qmark = url.indexOf('?');
        String path = qmark >= 0 ? url.substring(0, qmark) : url;
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private static String searchUrl(String setCode) {
        String base = searchBaseUrlOverride != null ? searchBaseUrlOverride : DEFAULT_SEARCH_URL;
        String query = "set:" + setCode + " lang:any";
        return base + "?unique=prints&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    /** Fetches {@code urlStr} and parses it as JSON, or returns {@code null} for HTTP 404. */
    private static JsonObject fetchJson(String urlStr) throws IOException {
        URLConnection conn = new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(CONNECT_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        conn.connect();
        if (conn instanceof HttpURLConnection) {
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status == 404) return null;
            if (status != 200) throw new IOException("HTTP " + status + " for " + urlStr);
        }
        boolean gzipped = "gzip".equalsIgnoreCase(conn.getContentEncoding());
        try (InputStream raw = conn.getInputStream();
             InputStream is = gzipped ? new GZIPInputStream(raw) : raw;
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
