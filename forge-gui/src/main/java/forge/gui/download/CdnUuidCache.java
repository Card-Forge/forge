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
 * Thread-safe cache of Scryfall CDN image URLs, built on demand via {@link ScryfallSetSync}
 * and stored at {@code {cacheDir}/cdn_uuid/{setCode}.json.gz}. Never blocks on network I/O:
 * a miss queues the set for {@link #syncPendingSets} and returns {@code null}. An unresolved
 * (cn, lang) is recorded as a timestamped miss so repeat lookups skip until it's stale
 * (see {@link #trackMiss}).
 *
 * <p>Set JSON: {@code {"cn": {"lang": "uuid" | ["front","back"] | {"miss": timestamp}}}}
 */
public final class CdnUuidCache {

    private static final String FALLBACK_LANG    = "en";
    private static final String BULK_SYNC_PROMPT_MARKER = ".bulk_sync_prompt";
    private static final Duration MISS_RETRY_AFTER = Duration.ofDays(1);

    /**
     * Every Scryfall language code observed
     */
    public static final String[] LANGUAGE_CODES = {
            "ar",  // Arabic
            "de",  // German
            "dw",  // Dwarvish (joke/funny-set language)
            "en",  // English
            "es",  // Spanish
            "fr",  // French
            "grc", // Ancient Greek
            "he",  // Hebrew
            "it",  // Italian
            "ja",  // Japanese
            "ko",  // Korean
            "la",  // Latin
            "ph",  // Phyrexian
            "pt",  // Portuguese
            "qya", // Quenya
            "ru",  // Russian
            "sa",  // Sanskrit
            "zhs", // Chinese Simplified
            "zht"  // Chinese Traditional
    };

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

    /** Submits {@link #syncPendingSets} to the shared pool; tests disable this. */
    static volatile boolean autoSyncEnabled = true;

    /** Test override for the local cache directory. */
    static volatile String localCacheDirOverride = null;

    private CdnUuidCache() {}

    /** Test helper */
    static void clearCacheForTesting() { setCache.clear(); }

    /** Local set-cache directory, honoring the test override. */
    public static String cacheDir() {
        return localCacheDirOverride != null ? localCacheDirOverride : ForgeConstants.CACHE_CDN_UUID_DIR;
    }

    /** Whether {@code scryfallCode} already has a local cache file (per-set or bulk-data sync). */
    public static boolean isSetCached(String scryfallCode) {
        return scryfallCode != null && localCacheFile(scryfallCode.toLowerCase()).exists();
    }

    /** Whether any set has ever been synced locally. Used to offer a one-time bulk warm-up on first run. */
    public static boolean hasAnyCachedSets() {
        File[] files = new File(cacheDir()).listFiles((dir, name) -> name.endsWith(".json.gz"));
        return files != null && files.length > 0;
    }

    public static boolean shouldPromptForBulkSync() {
        return !hasAnyCachedSets() && !bulkSyncPromptMarker().exists();
    }

    public static void markBulkSyncPromptAnswered() {
        File marker = bulkSyncPromptMarker();
        try {
            File dir = marker.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return;
            if (!marker.exists()) Files.createFile(marker.toPath());
        } catch (Exception e) {
            Logger.debug(e, "Could not write bulk sync prompt marker at {}", marker);
        }
    }

    private static File bulkSyncPromptMarker() {
        return new File(cacheDir(), BULK_SYNC_PROMPT_MARKER);
    }

    /** Deletes every local cache file and clears the in-memory cache. */
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
     * Merges (cn → lang → [front, back]) entries for {@code setCode} into its cache file.
     * Never overwrites a real entry, but a miss record doesn't count as "already there" --
     * real data always upgrades a miss.
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

    /** Records {@code (cn, lang)} as missing as of now, so lookups skip retrying until {@link #MISS_RETRY_AFTER} passes. */
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

    public static boolean isAvailableInLanguage(String scryfallCode, String collectorNum, String lang) {
        if (scryfallCode == null || collectorNum == null || lang == null) return false;
        String setCode = scryfallCode.toLowerCase();

        Map<String, Map<String, LangUuids>> cardMap = ensureSetLoadedReadOnly(setCode);
        if (cardMap == MISSING_SET) return false;

        Map<String, LangUuids> langMap = cardMap.get(collectorNum);
        if (langMap == null) return false;

        LangUuids uuids = langMap.get(lang.toLowerCase());
        return uuids != null && !uuids.isMiss();
    }

    public static String resolvePreferredLangCode(String preferredLang, String scryfallCode,
                                                   String collectorNum, String defaultLangCode) {
        if (preferredLang == null || preferredLang.isEmpty()
                || FALLBACK_LANG.equalsIgnoreCase(preferredLang)
                || preferredLang.equalsIgnoreCase(defaultLangCode)) {
            return defaultLangCode;
        }
        return isAvailableInLanguage(scryfallCode, collectorNum, preferredLang) ? preferredLang : defaultLangCode;
    }

    /**
     * Read-only counterpart to {@link #getCdnUrl}: never queues a sync or records a miss. Use
     * from the gameplay image-fetch path, which must not trigger background Scryfall traffic.
     */
    public static String getCdnUrlIfCached(String scryfallCode, String collectorNum,
                                           String lang, String face, String size) {
        if (scryfallCode == null || collectorNum == null) return null;
        String setCode = scryfallCode.toLowerCase();

        Map<String, Map<String, LangUuids>> cardMap = ensureSetLoadedReadOnly(setCode);
        if (cardMap == MISSING_SET) return null;

        LangUuids uuids = resolveWithFallback(cardMap, collectorNum, lang);
        if (uuids == null) return null;

        boolean wantBack = "back".equals(face);
        String uuid = (wantBack && uuids.back != null) ? uuids.back : uuids.front;
        String side = wantBack ? "back" : "front";
        return cdnUrl(uuid, side, size);
    }

    /** Like {@link #ensureSetLoaded}, but never queues a sync -- an unsynced set stays retryable rather than triggering work or being cached as permanently absent. */
    private static Map<String, Map<String, LangUuids>> ensureSetLoadedReadOnly(String setCode) {
        Map<String, Map<String, LangUuids>> cached = setCache.get(setCode);
        if (cached != null) return cached;

        Map<String, Map<String, LangUuids>> onDisk = readSetFromDisk(setCode);
        if (onDisk == MISSING_SET) return MISSING_SET;

        Map<String, Map<String, LangUuids>> existing = setCache.putIfAbsent(setCode, onDisk);
        return existing != null ? existing : onDisk;
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
     * Returns the Scryfall CDN URL for a card face, or {@code null} if unavailable.
     *
     * @param scryfallCode lowercase Scryfall set code
     * @param collectorNum collector number
     * @param lang preferred language code
     * @param face {@code ""}/{@code "front"} or {@code "back"}
     * @param size {@code "normal"} or {@code "art_crop"}
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
     * Records a first-time miss immediately. A miss younger than {@link #MISS_RETRY_AFTER}
     * is left alone; an older one queues the set for {@link #syncPendingSets}.
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

    /** Syncs every queued set from Scryfall. Meant to run off the EDT/render thread. */
    public static void syncPendingSets() {
        for (String setCode : pendingSyncs) {
            if (!pendingSyncs.remove(setCode)) continue; // another thread already claimed it
            ScryfallSetSync.sync(setCode);
            refreshStaleMisses(setCode);
        }
    }

    /** Refreshes the timestamp of any miss still unresolved after a resync. */
    private static void refreshStaleMisses(String setCode) {
        for (Map.Entry<String, Map<String, LangUuids>> cnEntry : readSetFromDisk(setCode).entrySet()) {
            for (Map.Entry<String, LangUuids> langEntry : cnEntry.getValue().entrySet()) {
                LangUuids v = langEntry.getValue();
                if (v.isMiss() && isStale(v.missedAt)) recordMiss(setCode, cnEntry.getKey(), langEntry.getKey());
            }
        }
    }

    /**
     * Builds the deterministic Scryfall CDN URL for a card UUID.
     *
     * @param uuid Scryfall card UUID
     * @param side {@code "front"} or {@code "back"}
     * @param size {@code "normal"} or {@code "art_crop"}
     */
    public static String cdnUrl(String uuid, String side, String size) {
        return ForgeConstants.URL_SCRYFALL_CDN + size + "/" + side
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

        // Nothing local yet -- queue instead of blocking; the next lookup picks up whatever
        // syncPendingSets finds.
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
        return new File(cacheDir(), safeFileStem(setCode) + ".json.gz");
    }

    /**
     * Windows reserves CON, PRN, AUX, NUL, COM1-9 and LPT1-9 as device names -- a file called
     * {@code con.json.gz} can never be created there, extension or not. This bites us for real:
     * Conflux's Scryfall set code is "con". Prefix the handful of set codes that collide so their
     * cache files can actually be written; every other set code is untouched.
     */
    private static final java.util.Set<String> WINDOWS_RESERVED_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"));

    private static String safeFileStem(String setCode) {
        return WINDOWS_RESERVED_NAMES.contains(setCode.toLowerCase()) ? "set-" + setCode : setCode;
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

    /** Format: {@code {"cn": {"lang": "uuid"|["front","back"]|{"miss": timestamp}}}} */
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
