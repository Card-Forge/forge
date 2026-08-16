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
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Lazy-loading, thread-safe cache for Scryfall CDN UUIDs.
 *
 * All UUID data is generated on the user's machine, straight from Scryfall.
 *
 * The cache resolves data locally first from
 * {@code {cacheDir}/cdn_uuid/{setCode}.json.gz} files.
 *
 * When there is a cache miss (file or entry), it's repopulated from
 * the scryfall search api.
 *
 * The cache returns null, so the user may resolve via a rate-limited, non-cdn scryfall
 * image hosting. The client rate limits these requests.
 *
 * <p>A (collector number, language) that a set's data genuinely doesn't have -- e.g. a
 * card released after the set was last synced -- is recorded as a negative-cache "miss"
 * with a timestamp, right alongside the real entries. A miss younger than
 * {@link #MISS_RETRY_AFTER} short-circuits with no network call; once it's older, the
 * next lookup re-syncs the set from Scryfall to see if it's showed up since.
 *
 * <p>Set JSON format:
 * <pre>
 *   {
 *     "1":   {"en": "uuid"},
 *     "2":   {"en": "uuid", "ja": "ja-uuid"},
 *     "A-40":{"en": ["frontUuid", "backUuid"]},
 *     "9":   {"en": {"miss": "2026-08-11T18:23:45.123Z"}}
 *   }
 * </pre>
 */
public final class CdnUuidCache {

    private static final String FALLBACK_LANG    = "en";
    private static final Duration MISS_RETRY_AFTER = Duration.ofDays(1);

    /** Used when set was looked up and no data exists. */
    private static final Map<String, Map<String, LangUuids>> MISSING_SET = Collections.emptyMap();

    private static final class LangUuids {
        final String front;      // null when this is a miss record
        final String back;       // null → same UUID for both faces (only meaningful for a real entry)
        final Instant missedAt;  // non-null → negative-cache record; front/back are unused

        private LangUuids(String front, String back, Instant missedAt) {
            this.front = front;
            this.back = back;
            this.missedAt = missedAt;
        }

        static LangUuids found(String front, String back) { return new LangUuids(front, back, null); }
        static LangUuids miss(Instant at) { return new LangUuids(null, null, at); }
        boolean isMiss() { return missedAt != null; }
    }

    /** Cache: setCode → (collectorNumber → (lang → LangUuids)) */
    private static final ConcurrentHashMap<String, Map<String, Map<String, LangUuids>>> setCache =
            new ConcurrentHashMap<>();

    /**
     * Override for the local cache directory for tests.
     */
    static volatile String localCacheDirOverride = null;

    private CdnUuidCache() {}

    /** Test helper */
    static void clearCacheForTesting() { setCache.clear(); }

    /** The directory local set caches live in, honoring the test override. Package-private for {@link ScryfallSetSync}. */
    static String cacheDir() {
        return localCacheDirOverride != null ? localCacheDirOverride : ForgeConstants.CACHE_CDN_UUID_DIR;
    }

    /**
     * Deletes every locally cached CDN UUID set file and clears the in-memory cache.
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
     * Merges externally-sourced (setCode → collectorNumber → lang → [frontUuid, backUuidOrNull])
     * entries into the on-disk cache, creating or updating {@code {cacheDir}/{setCode}.json.gz}.
     *
     * Used by {@link ScryfallSetSync} when a set has no local data at all yet (or a
     * previously-recorded miss is stale) and is (re)built from Scryfall's card search API.
     *
     * <p>A real entry already on disk is never overwritten, so a precise front/back pair
     * can't be degraded by a later, less precise source filling the same slot. A miss
     * record occupying the slot doesn't count as "already there" -- real data always
     * upgrades a miss.
     */
    static synchronized void mergeSetEntriesWithFaces(String setCode, Map<String, Map<String, String[]>> newEntries) {
        File file = localCacheFile(setCode);
        JsonObject merged = readLocalJson(file);

        for (Map.Entry<String, Map<String, String[]>> cnEntry : newEntries.entrySet()) {
            String cn = cnEntry.getKey();
            JsonObject langObj = merged.has(cn) && merged.get(cn).isJsonObject()
                    ? merged.getAsJsonObject(cn) : new JsonObject();
            for (Map.Entry<String, String[]> langEntry : cnEntry.getValue().entrySet()) {
                String lang  = langEntry.getKey();
                if (isRealEntry(langObj.get(lang))) continue; // never overwrite real data
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
     * Records that {@code (cn, lang)} has no real data in {@code setCode} as of now, so
     * repeat lookups can short-circuit without hitting Scryfall again until
     * {@link #MISS_RETRY_AFTER} has passed. Never overwrites a real entry already there.
     */
    private static synchronized void recordMiss(String setCode, String cn, String lang) {
        File file = localCacheFile(setCode);
        JsonObject setObj = readLocalJson(file);
        JsonObject langObj = setObj.has(cn) && setObj.get(cn).isJsonObject()
                ? setObj.getAsJsonObject(cn) : new JsonObject();
        if (isRealEntry(langObj.get(lang))) return; // real data already recorded; never clobber

        JsonObject missObj = new JsonObject();
        missObj.addProperty("miss", Instant.now().toString());
        langObj.add(lang, missObj);
        setObj.add(cn, langObj);

        writeLocalCache(file, setObj.toString());
        setCache.remove(setCode);
    }

    /** A JSON string or array value is real data; an object (miss record) or absent value is not. */
    private static boolean isRealEntry(JsonElement value) {
        return value != null && !value.isJsonObject();
    }

    private static JsonObject readLocalJson(File file) {
        if (!file.exists()) return new JsonObject();
        try (InputStream is = new GZIPInputStream(new FileInputStream(file));
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            Logger.warn("CdnUuidCache: corrupt local cache {}, rebuilding: {}", file, e.getMessage());
            return new JsonObject();
        }
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

        LangUuids uuids = resolveWithFallback(cardMap, collectorNum, lang);
        if (uuids == null) uuids = trackMiss(setCode, cardMap, collectorNum, lang);
        if (uuids == null) return null;

        String uuid = (wantBack && uuids.back != null) ? uuids.back : uuids.front;
        String side = wantBack ? "back" : "front";
        return cdnUrl(uuid, side, size);
    }

    /** The real (non-miss) entry for {@code lang}, or its English fallback, if either has real data. */
    private static LangUuids resolveWithFallback(Map<String, Map<String, LangUuids>> cardMap, String cn, String lang) {
        Map<String, LangUuids> langMap = cardMap.get(cn);
        if (langMap == null) return null;

        LangUuids direct = langMap.get(lang);
        if (direct != null && !direct.isMiss()) return direct;

        if (!FALLBACK_LANG.equals(lang)) {
            LangUuids fallback = langMap.get(FALLBACK_LANG);
            if (fallback != null && !fallback.isMiss()) return fallback;
        }
        return null;
    }

    /**
     * Neither {@code lang} nor its English fallback resolved to real data. Tracks that as a
     * miss against the exact requested {@code lang}: a miss seen for the first time, or one
     * younger than {@link #MISS_RETRY_AFTER}, is recorded/left alone and this returns
     * {@code null} with no network call. A miss older than that is worth re-checking --
     * the set is re-synced from Scryfall, in case the card has shown up since.
     */
    private static LangUuids trackMiss(String setCode, Map<String, Map<String, LangUuids>> cardMap,
                                        String cn, String lang) {
        Map<String, LangUuids> langMap = cardMap.get(cn);
        LangUuids existing = langMap != null ? langMap.get(lang) : null;

        if (existing != null && existing.isMiss() && isStale(existing.missedAt)) {
            ScryfallSetSync.sync(setCode);
            Map<String, Map<String, LangUuids>> refreshed = loadSet(setCode);
            LangUuids retried = resolveWithFallback(refreshed, cn, lang);
            if (retried != null) return retried;
        } else if (existing != null && existing.isMiss()) {
            return null; // recorded recently; don't hit the network again yet
        }

        recordMiss(setCode, cn, lang);
        return null;
    }

    private static boolean isStale(Instant missedAt) {
        return Duration.between(missedAt, Instant.now()).compareTo(MISS_RETRY_AFTER) >= 0;
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
     * Format: {@code {"cn": {"lang": "uuid"|["frontUuid","backUuid"]|{"miss": timestamp}}, ...}}
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
                    langMap.put(langEntry.getKey(), LangUuids.found(val.getAsString(), null));
                } else if (val.isJsonArray()) {
                    JsonArray arr = val.getAsJsonArray();
                    if (arr.size() >= 2) {
                        String front = arr.get(0).getAsString();
                        String back  = arr.get(1).getAsString();
                        langMap.put(langEntry.getKey(),
                                LangUuids.found(front, back.equals(front) ? null : back));
                    } else if (arr.size() == 1) {
                        langMap.put(langEntry.getKey(),
                                LangUuids.found(arr.get(0).getAsString(), null));
                    }
                } else if (val.isJsonObject() && val.getAsJsonObject().has("miss")) {
                    try {
                        Instant missedAt = Instant.parse(val.getAsJsonObject().get("miss").getAsString());
                        langMap.put(langEntry.getKey(), LangUuids.miss(missedAt));
                    } catch (Exception ignored) {
                        // corrupt timestamp -- treat as though this (cn, lang) was never checked
                    }
                }
            }
            if (!langMap.isEmpty())
                cardMap.put(cnEntry.getKey(), Collections.unmodifiableMap(langMap));
        }
        return Collections.unmodifiableMap(cardMap);
    }
}
