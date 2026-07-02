package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.localinstance.properties.ForgeConstants;
import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy-loading, thread-safe cache for Scryfall CDN UUIDs.
 *
 * <p>UUID data lives in per-set JSON files hosted in the forge-extras repository.
 * On the first lookup for a set, the cache checks for a local copy under
 * {@code {cacheDir}/cdn_uuid/{setCode}.json}. If absent it fetches the file from
 * forge-extras and writes it locally so subsequent lookups are instant.
 * Returns {@code null} on any failure so callers fall back to the rate-limited
 * Scryfall API or the cardforge server.
 *
 * <p>Set JSON format:
 * <pre>
 *   {
 *     "1":   {"en": "uuid"},
 *     "2":   {"en": "uuid", "ja": "ja-uuid"},
 *     "A-40":{"en": ["frontUuid", "backUuid"]}
 *   }
 * </pre>
 */
public final class CdnUuidCache {

    private static final String FALLBACK_LANG    = "en";
    private static final int    FETCH_TIMEOUT_MS = 10_000;

    /** Sentinel: set was looked up and no data exists (locally or remotely). */
    private static final Map<String, Map<String, LangUuids>> MISSING_SET = Collections.emptyMap();

    private static final class LangUuids {
        final String front;
        final String back; // null → same UUID for both faces
        LangUuids(String front, String back) { this.front = front; this.back = back; }
    }

    /** Cache: setCode → (collectorNumber → (lang → LangUuids)) */
    private static final ConcurrentHashMap<String, Map<String, Map<String, LangUuids>>> setCache =
            new ConcurrentHashMap<>();

    /**
     * Override the local cache directory. Package-private for unit tests.
     * Must end with the platform file separator when set.
     */
    static volatile String localCacheDirOverride = null;

    /**
     * Override the remote base URL. Package-private for unit tests.
     * Must end with '/'. Supports {@code file://} URLs for offline testing.
     */
    static volatile String remoteBaseUrlOverride = null;

    private CdnUuidCache() {}

    /** Clears the in-memory cache. Package-private for unit tests only. */
    static void clearCacheForTesting() { setCache.clear(); }

    /**
     * Returns the Scryfall CDN image URL for a given card face, or {@code null}
     * if no UUID data is available.
     *
     * @param scryfallCode  lowercase Scryfall set code (e.g. {@code "ltr"})
     * @param collectorNum  collector number as in Scryfall data (e.g. {@code "51"}, {@code "T1"})
     * @param lang          preferred language code (e.g. {@code "en"}, {@code "ja"})
     * @param face          {@code ""} or {@code "front"} for the front face; {@code "back"} for the back
     * @param size          {@code "normal"} or {@code "art_crop"}
     */
    public static String getCdnUrl(String scryfallCode, String collectorNum,
                                   String lang, String face, String size) {
        if (scryfallCode == null || collectorNum == null) return null;
        String setCode = scryfallCode.toLowerCase();
        boolean wantBack = "back".equals(face);

        Map<String, Map<String, LangUuids>> cardMap = ensureSetLoaded(setCode);
        if (cardMap == MISSING_SET) return null;

        Map<String, LangUuids> langMap = cardMap.get(collectorNum);
        if (langMap == null) return null;

        LangUuids uuids = langMap.get(lang);
        if (uuids == null && !FALLBACK_LANG.equals(lang)) uuids = langMap.get(FALLBACK_LANG);
        if (uuids == null) return null;

        String uuid = (wantBack && uuids.back != null) ? uuids.back : uuids.front;
        String side = wantBack ? "back" : "front";
        return ScryfallBulkData.cdnUrl(uuid, side, size);
    }

    // -------------------------------------------------------------------------

    private static Map<String, Map<String, LangUuids>> ensureSetLoaded(String setCode) {
        Map<String, Map<String, LangUuids>> cached = setCache.get(setCode);
        if (cached != null) return cached;

        Map<String, Map<String, LangUuids>> loaded = loadSet(setCode);
        // putIfAbsent: if another thread raced and loaded first, use its result
        Map<String, Map<String, LangUuids>> existing = setCache.putIfAbsent(setCode, loaded);
        return existing != null ? existing : loaded;
    }

    private static Map<String, Map<String, LangUuids>> loadSet(String setCode) {
        File localFile = localCacheFile(setCode);

        // 1. Try local disk cache
        if (localFile.exists()) {
            try {
                return parseSetFile(localFile);
            } catch (Exception e) {
                Logger.warn("CdnUuidCache: corrupt local cache {}: {}", localFile, e.getMessage());
                //noinspection ResultOfMethodCallIgnored
                localFile.delete();
            }
        }

        // 2. Fetch from remote, cache locally
        String remoteUrl = remoteUrl(setCode);
        try {
            String json = fetchString(remoteUrl);
            if (json != null) {
                writeLocalCache(localFile, json);
                return parseSetJson(json);
            }
        } catch (Exception e) {
            Logger.debug("CdnUuidCache: no UUID data for set '{}': {}", setCode, e.getMessage());
        }

        return MISSING_SET;
    }

    private static File localCacheFile(String setCode) {
        String dir = localCacheDirOverride != null
                ? localCacheDirOverride
                : ForgeConstants.CACHE_CDN_UUID_DIR;
        return new File(dir, setCode + ".json");
    }

    private static String remoteUrl(String setCode) {
        String base = remoteBaseUrlOverride != null
                ? remoteBaseUrlOverride
                : ForgeConstants.FORGE_EXTRAS_CDN_UUID_URL;
        return base + setCode + ".json";
    }

    /** Fetches {@code urlStr} and returns the body as a string, or {@code null} for HTTP 404. */
    private static String fetchString(String urlStr) throws Exception {
        URLConnection conn = new URL(urlStr).openConnection();
        conn.setConnectTimeout(FETCH_TIMEOUT_MS);
        conn.setReadTimeout(FETCH_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.connect();
        if (conn instanceof HttpURLConnection) {
            int status = ((HttpURLConnection) conn).getResponseCode();
            if (status == 404) return null;
            if (status != 200) throw new Exception("HTTP " + status + " for " + urlStr);
        }
        try (InputStream is = conn.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static void writeLocalCache(File file, String json) {
        try {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            Path tmp = Files.createTempFile(file.getParentFile().toPath(), "cdn-", ".tmp");
            try {
                Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
                Files.move(tmp, file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                Files.deleteIfExists(tmp);
                throw e;
            }
        } catch (Exception e) {
            Logger.warn("CdnUuidCache: could not write local cache {}: {}", file, e.getMessage());
        }
    }

    private static Map<String, Map<String, LangUuids>> parseSetFile(File file) throws Exception {
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            return parseSetObject(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }

    private static Map<String, Map<String, LangUuids>> parseSetJson(String json) {
        return parseSetObject(JsonParser.parseString(json).getAsJsonObject());
    }

    /**
     * Parses a set JSON object.
     * Format: {@code {"cn": {"lang": "uuid"|["frontUuid","backUuid"]}, ...}}
     */
    private static Map<String, Map<String, LangUuids>> parseSetObject(JsonObject setObj) {
        Map<String, Map<String, LangUuids>> cardMap = new HashMap<>(setObj.size() * 2);
        for (Map.Entry<String, JsonElement> cnEntry : setObj.entrySet()) {
            if (!cnEntry.getValue().isJsonObject()) continue;
            JsonObject langObj = cnEntry.getValue().getAsJsonObject();
            Map<String, LangUuids> langMap = new HashMap<>(langObj.size() * 2);
            for (Map.Entry<String, JsonElement> langEntry : langObj.entrySet()) {
                JsonElement val = langEntry.getValue();
                if (val.isJsonPrimitive()) {
                    langMap.put(langEntry.getKey(), new LangUuids(val.getAsString(), null));
                } else if (val.isJsonArray()) {
                    JsonArray arr = val.getAsJsonArray();
                    if (arr.size() >= 2) {
                        String front = arr.get(0).getAsString();
                        String back  = arr.get(1).getAsString();
                        langMap.put(langEntry.getKey(),
                                new LangUuids(front, back.equals(front) ? null : back));
                    } else if (arr.size() == 1) {
                        langMap.put(langEntry.getKey(),
                                new LangUuids(arr.get(0).getAsString(), null));
                    }
                }
            }
            if (!langMap.isEmpty())
                cardMap.put(cnEntry.getKey(), Collections.unmodifiableMap(langMap));
        }
        return Collections.unmodifiableMap(cardMap);
    }
}
