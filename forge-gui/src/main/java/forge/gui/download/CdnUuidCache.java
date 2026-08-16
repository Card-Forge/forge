package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.localinstance.properties.ForgeConstants;
import forge.util.ThreadUtil;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Lazy-loading, thread-safe cache for Scryfall CDN UUIDs, generated on demand from Scryfall's
 * search API (see {@link ScryfallSetSync}) and cached locally at
 * {@code {cacheDir}/cdn_uuid/{setCode}.json.gz}. A {@code null} result lets the caller fall
 * back to rate-limited, non-CDN Scryfall image hosting.
 *
 * <p>Callers include interactive gameplay code that runs on the EDT/render thread, so a lookup
 * this can't answer from disk never blocks on network I/O itself: it queues the set code and
 * returns as if the data weren't there yet. {@link #syncPendingSets} does the actual work and
 * is submitted to the existing shared {@link ThreadUtil#getServicePool}, warming the cache for
 * the next lookup instead of the current one.
 *
 * <p>A (collector number, language) that a set's cached data genuinely doesn't have -- e.g. a
 * card released after the set was last synced -- is recorded as a negative-cache "miss" with
 * a timestamp; see {@link #trackMiss} for the retry policy.
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

    /** Set codes a lookup couldn't answer locally, waiting for {@link #syncPendingSets}. */
    private static final Set<String> pendingSyncs = ConcurrentHashMap.newKeySet();

    /** Submits {@link #syncPendingSets} to the shared pool for tests to disable. */
    static volatile boolean autoSyncEnabled = true;

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
     * Merges externally-sourced (collectorNumber → lang → [frontUuid, backUuidOrNull]) entries
     * for {@code setCode} into its on-disk cache file. Called by {@link ScryfallSetSync} after
     * (re)building a set from Scryfall's card search API.
     *
     * <p>A real entry already on disk is never overwritten -- a precise front/back pair can't be
     * degraded by a later, less precise source -- but a miss record occupying the slot doesn't
     * count as "already there", so real data always upgrades a miss.
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
        try {
            return readGzipJson(file);
        } catch (Exception e) {
            Logger.warn("CdnUuidCache: corrupt local cache {}, rebuilding: {}", file, e.getMessage());
            return new JsonObject();
        }
    }

    private static JsonObject readGzipJson(File file) throws Exception {
        try (InputStream is = new GZIPInputStream(new FileInputStream(file));
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
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
        if (uuids == null) {
            trackMiss(setCode, cardMap, collectorNum, lang);
            return null;
        }

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
     * Neither {@code lang} nor its English fallback resolved to real data. A miss seen for the
     * first time is recorded immediately (no network call). One already recorded and younger
     * than {@link #MISS_RETRY_AFTER} is left alone -- also no network call. One older than that
     * queues the set for {@link #syncPendingSets} to re-check.
     */
    private static void trackMiss(String setCode, Map<String, Map<String, LangUuids>> cardMap,
                                   String cn, String lang) {
        Map<String, LangUuids> langMap = cardMap.get(cn);
        LangUuids existing = langMap != null ? langMap.get(lang) : null;

        if (existing != null && existing.isMiss()) {
            if (isStale(existing.missedAt)) queueSync(setCode);
            return;
        }

        recordMiss(setCode, cn, lang);
    }

    private static boolean isStale(Instant missedAt) {
        return Duration.between(missedAt, Instant.now()).compareTo(MISS_RETRY_AFTER) >= 0;
    }

    /** Queues {@code setCode} and, unless a test disabled it, submits {@link #syncPendingSets} to the shared pool. */
    private static void queueSync(String setCode) {
        if (pendingSyncs.add(setCode) && autoSyncEnabled) {
            ThreadUtil.getServicePool().submit(CdnUuidCache::syncPendingSets);
        }
    }

    /**
     * Synchronously syncs every set a lookup has queued (a never-seen set, or a stale miss due
     * a retry) from Scryfall. Meant to run off the EDT/render thread -- see the class javadoc.
     */
    public static void syncPendingSets() {
        for (String setCode : pendingSyncs) {
            if (!pendingSyncs.remove(setCode)) continue; // another thread already claimed it
            ScryfallSetSync.sync(setCode);
            refreshStaleMisses(setCode);
        }
    }

    /** A miss still unresolved after a resync attempt just got reconfirmed -- refresh its timestamp. */
    private static void refreshStaleMisses(String setCode) {
        for (Map.Entry<String, Map<String, LangUuids>> cnEntry : readSetFromDisk(setCode).entrySet()) {
            for (Map.Entry<String, LangUuids> langEntry : cnEntry.getValue().entrySet()) {
                LangUuids v = langEntry.getValue();
                if (v.isMiss() && isStale(v.missedAt)) recordMiss(setCode, cnEntry.getKey(), langEntry.getKey());
            }
        }
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
        Map<String, Map<String, LangUuids>> onDisk = readSetFromDisk(setCode);
        if (onDisk != MISSING_SET) return onDisk;

        // Nothing local yet -- queue this set for syncPendingSets instead of blocking on network
        // I/O here. ScryfallSetSync merges its results into the local cache file and evicts
        // setCache when it's done, so the next lookup picks up whatever it found.
        queueSync(setCode);
        return MISSING_SET;
    }

    /** Pure disk read: {@link #MISSING_SET} if there's no local file yet, without queuing a sync. */
    private static Map<String, Map<String, LangUuids>> readSetFromDisk(String setCode) {
        File localFile = localCacheFile(setCode);
        if (!localFile.exists()) return MISSING_SET;
        try {
            return parseSetFile(localFile);
        } catch (Exception e) {
            Logger.warn("CdnUuidCache: corrupt local cache {}: {}", localFile, e.getMessage());
            //noinspection ResultOfMethodCallIgnored
            localFile.delete();
            return MISSING_SET;
        }
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
        return parseSetObject(readGzipJson(file));
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
