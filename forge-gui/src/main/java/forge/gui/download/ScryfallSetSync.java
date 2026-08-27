package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.BuildInfo;
import forge.util.ScryfallRateLimiter;
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
 * Builds one set's CDN UUID mapping from Scryfall's card search API, on demand.
 * Reads each face's own image URL rather than assuming from the card's {@code id},
 * so double-faced cards with distinct front/back art resolve correctly.
 * A card not found here is left for {@link CdnUuidCache} to record as a retryable miss.
 */
final class ScryfallSetSync {

    private static final String DEFAULT_SEARCH_URL = "https://api.scryfall.com/cards/search";

    private static final int CONNECT_TIMEOUT_MS = 15_000;

    /** Override for tests; must be a full base URL with no trailing query string. */
    static volatile String searchBaseUrlOverride = null;

    private ScryfallSetSync() {}

    /**
     * Fetches every print of {@code setCode} from Scryfall and merges it into the cache.
     * @return true if anything was found and cached
     */
    static boolean sync(String setCode) {
        Map<String, Map<String, String[]>> byCn = new HashMap<>();
        String url = searchUrl(setCode);
        try {
            while (url != null) {
                if (ScryfallRateLimiter.isCoolingDown()) break; // back off; a later sync retries

                JsonObject page = fetchJson(url);
                if (page == null) break; // 404: no such set / no matching cards

                JsonArray data = page.has("data") ? page.getAsJsonArray("data") : new JsonArray();
                for (JsonElement el : data) {
                    addCard(byCn, el.getAsJsonObject());
                }

                boolean hasMore = page.has("has_more") && page.get("has_more").getAsBoolean();
                url = hasMore && page.has("next_page") ? page.get("next_page").getAsString() : null;
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
        if (cn == null || lang == null) return;

        String[] frontBack = frontBackUuids(card);
        if (frontBack == null) return; // no image data yet, or a placeholder URL

        byCn.computeIfAbsent(cn, k -> new HashMap<>()).put(lang, frontBack);
    }

    /** Per-face CDN UUIDs for one card, or {@code null} if it has no usable image data yet. Shared with {@link ScryfallBulkDataSync}. */
    static String[] frontBackUuids(JsonObject card) {
        String imageStatus = str(card, "image_status");
        if ("missing".equals(imageStatus) || "placeholder".equals(imageStatus)) return null;

        String front;
        String back = null;
        if (card.has("image_uris") && card.get("image_uris").isJsonObject()) {
            front = uuidFromUrl(normalUrl(card.getAsJsonObject("image_uris")));
        } else if (card.has("card_faces") && card.get("card_faces").isJsonArray()) {
            JsonArray faces = card.getAsJsonArray("card_faces");
            if (faces.size() == 0) return null;
            JsonObject face0 = faces.get(0).getAsJsonObject();
            if (!face0.has("image_uris") || !face0.get("image_uris").isJsonObject()) return null;
            front = uuidFromUrl(normalUrl(face0.getAsJsonObject("image_uris")));
            if (faces.size() > 1) {
                JsonObject face1 = faces.get(1).getAsJsonObject();
                if (face1.has("image_uris") && face1.get("image_uris").isJsonObject()) {
                    back = uuidFromUrl(normalUrl(face1.getAsJsonObject("image_uris")));
                }
            }
        } else {
            return null; // no image data for this card
        }
        return front == null ? null : new String[]{front, back};
    }

    /** Reads an {@code image_uris} object and returns the value of the {@code normal} key. */
    private static String normalUrl(JsonObject imageUris) {
        return imageUris.has("normal") ? imageUris.get("normal").getAsString() : null;
    }

    /** UUID segment of a Scryfall CDN URL, or {@code null} for a non-CDN placeholder URL. */
    private static String uuidFromUrl(String url) {
        if (url == null || !url.contains("cards.scryfall.io")) return null;
        int qmark = url.indexOf('?');
        String path = qmark >= 0 ? url.substring(0, qmark) : url;
        int slash = path.lastIndexOf('/');
        String filename = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private static String searchUrl(String setCode) {
        String base = searchBaseUrlOverride != null ? searchBaseUrlOverride : DEFAULT_SEARCH_URL;
        // Always fetch English: CdnUuidCache.getCdnUrl() falls back to it, and virtually every
        // edition's cards are English. "lang:any" fetched ~10x more pages (every printed
        // language) than needed and was tripping Scryfall's rate limit before finishing a set --
        // so if the user has a non-English language preference, add just that one language too,
        // instead of every language.
        String preferredLang = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_CARD_DOWNLOAD_LANG);
        String query = (preferredLang != null && !preferredLang.isEmpty() && !"en".equalsIgnoreCase(preferredLang))
                ? "set:" + setCode + " (lang:en or lang:" + preferredLang + ")"
                : "set:" + setCode + " lang:en";
        return base + "?unique=prints&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    /** Shared with {@link ScryfallBulkDataSync}. */
    static String str(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    /** Fetches {@code urlStr} and parses it as JSON, or returns {@code null} for HTTP 404. */
    private static JsonObject fetchJson(String urlStr) throws IOException {
        ScryfallRateLimiter.acquire(urlStr);
        URLConnection conn = new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(CONNECT_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Encoding", "gzip");
        // Scryfall asks for a descriptive User-Agent and rate-limits harder without one.
        conn.setRequestProperty("User-Agent", BuildInfo.getUserAgent());
        conn.connect();
        if (conn instanceof HttpURLConnection) {
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status == 404) return null;
            if (status == 429) {
                ScryfallRateLimiter.noteIfRateLimited(429, urlStr, conn.getHeaderField("Retry-After"));
                throw new IOException("HTTP 429 (rate limited) for " + urlStr);
            }
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
