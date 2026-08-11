package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.localinstance.properties.ForgeConstants;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Lazy-loading, thread-safe cache for Scryfall CDN UUIDs.
 *
 * <p>All UUID data is generated on the user's machine, straight from Scryfall — there
 * is no hosted/bundled data source. On the first lookup for a set, the cache checks
 * for a local copy under {@code {cacheDir}/cdn_uuid/{setCode}.json.gz}. If absent, it
 * asks {@link ScryfallSetSync} to build that set's mapping from the Scryfall card
 * search API and writes the result locally (gzip-compressed) so subsequent lookups
 * are instant. {@link ScryfallManifestSync} additionally lets a set's entries be kept
 * fresh in bulk (newest image updates first) instead of one set at a time. Returns
 * {@code null} on any failure so callers fall back to the rate-limited Scryfall API
 * or the cardforge server.
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

    /** Sentinel: set was looked up and no data exists (locally or on Scryfall). */
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

    private CdnUuidCache() {}

    /** Clears the in-memory cache. Package-private for unit tests only. */
    static void clearCacheForTesting() { setCache.clear(); }

    /** The directory local set caches live in, honoring the test override. Package-private for {@link ScryfallManifestSync}. */
    static String cacheDir() {
        return localCacheDirOverride != null ? localCacheDirOverride : ForgeConstants.CACHE_CDN_UUID_DIR;
    }

    /**
     * Deletes every locally cached CDN UUID set file and clears the in-memory cache.
     * Exposed as a user-facing "clear CDN image cache" action: a set whose local
     * copy predates a Scryfall image update (or was cached before it had any image
     * at all) stays stale until re-fetched, and this is the simplest way to force
     * that without per-file expiry bookkeeping.
     */
    public static void clearCache() {
        setCache.clear();
        File dir = new File(cacheDir());
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    /**
     * Merges externally-sourced (setCode → collectorNumber → lang → uuid) entries into
     * the on-disk cache. Used by {@link ScryfallManifestSync}, whose data source (the
     * manifest endpoint) never distinguishes a double-faced card's back-face UUID from
     * its front — every entry is treated as front-only. Delegates to
     * {@link #mergeSetEntriesWithFaces} so the never-overwrite guarantee lives in one place.
     */
    static void mergeSetEntries(String setCode, Map<String, Map<String, String>> newEntries) {
        Map<String, Map<String, String[]>> withFaces = new HashMap<>(newEntries.size() * 2);
        for (Map.Entry<String, Map<String, String>> cnEntry : newEntries.entrySet()) {
            Map<String, String[]> langMap = new HashMap<>(cnEntry.getValue().size() * 2);
            for (Map.Entry<String, String> langEntry : cnEntry.getValue().entrySet()) {
                langMap.put(langEntry.getKey(), new String[]{langEntry.getValue(), null});
            }
            withFaces.put(cnEntry.getKey(), langMap);
        }
        mergeSetEntriesWithFaces(setCode, withFaces);
    }

    /**
     * Merges externally-sourced (setCode → collectorNumber → lang → [frontUuid, backUuidOrNull])
     * entries into the on-disk cache, creating or updating {@code {cacheDir}/{setCode}.json.gz}.
     * Used by {@link ScryfallSetSync} when a set has no local data at all yet and is built
     * fresh from Scryfall's card search API, which — unlike the manifest endpoint — exposes
     * each face's own image URL, so a double-faced card's genuinely distinct back-face UUID
     * (a real but rare case) can be recorded precisely instead of assumed.
     *
     * <p>Existing entries are never overwritten, so a precise front/back pair already on
     * disk can't be degraded by a later, less precise source filling the same slot.
     */
    static synchronized void mergeSetEntriesWithFaces(String setCode, Map<String, Map<String, String[]>> newEntries) {
        File file = localCacheFile(setCode);
        JsonObject merged = new JsonObject();
        if (file.exists()) {
            try (InputStream is = new GZIPInputStream(new FileInputStream(file));
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                merged = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                Logger.warn("CdnUuidCache: corrupt local cache {}, rebuilding: {}", file, e.getMessage());
            }
        }

        for (Map.Entry<String, Map<String, String[]>> cnEntry : newEntries.entrySet()) {
            String cn = cnEntry.getKey();
            JsonObject langObj = merged.has(cn) && merged.get(cn).isJsonObject()
                    ? merged.getAsJsonObject(cn) : new JsonObject();
            for (Map.Entry<String, String[]> langEntry : cnEntry.getValue().entrySet()) {
                String lang  = langEntry.getKey();
                if (langObj.has(lang)) continue;
                String front = langEntry.getValue()[0];
                String back  = langEntry.getValue()[1];
                if (back != null && !back.equals(front)) {
                    JsonArray arr = new JsonArray();
                    arr.add(front);
                    arr.add(back);
                    langObj.add(lang, arr);
                } else {
                    langObj.addProperty(lang, front);
                }
            }
            if (langObj.size() > 0) merged.add(cn, langObj);
        }

        writeLocalCache(file, merged.toString());
        setCache.remove(setCode); // force a re-read of the freshly-written file on next lookup
    }

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
        return cdnUrl(uuid, side, size);
    }

    /**
     * Builds a Scryfall CDN image URL from a card UUID. The CDN ({@code cards.scryfall.io})
     * is not rate-limited; given a UUID and image size, the URL is fully deterministic:
     * {@code https://cards.scryfall.io/{size}/{front|back}/{uuid[0]}/{uuid[1]}/{uuid}.jpg}.
     *
     * @param uuid  the Scryfall card UUID (e.g. {@code "4e7a547f-..."})
     * @param side  {@code "front"} or {@code "back"}
     * @param size  {@code "normal"} or {@code "art_crop"}
     */
    public static String cdnUrl(String uuid, String side, String size) {
        return "https://cards.scryfall.io/" + size + "/" + side
                + "/" + uuid.charAt(0) + "/" + uuid.charAt(1) + "/" + uuid + ".jpg";
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

        // 2. Nothing local yet — build this set's mapping straight from Scryfall.
        // ScryfallSetSync merges its own results into the local cache file (via
        // mergeSetEntriesWithFaces) and evicts setCache, so re-reading that file
        // afterward picks up whatever it found.
        if (ScryfallSetSync.sync(setCode) && localFile.exists()) {
            try {
                return parseSetFile(localFile);
            } catch (Exception e) {
                Logger.warn("CdnUuidCache: corrupt local cache {} after Scryfall sync: {}", localFile, e.getMessage());
            }
        }

        return MISSING_SET;
    }

    private static File localCacheFile(String setCode) {
        return new File(cacheDir(), setCode + ".json.gz");
    }

    private static void writeLocalCache(File file, String json) {
        try {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            Path tmp = Files.createTempFile(file.getParentFile().toPath(), "cdn-", ".tmp");
            try {
                try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(tmp))) {
                    gz.write(json.getBytes(StandardCharsets.UTF_8));
                }
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
        try (InputStream is = new GZIPInputStream(new FileInputStream(file));
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return parseSetObject(JsonParser.parseReader(reader).getAsJsonObject());
        }
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
