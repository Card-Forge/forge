package forge.util;

import forge.localinstance.properties.ForgeConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CardCdnUuidBridge
 *
 * TEMPORARY. Stopgap so image downloads can use the Scryfall CDN
 * (cards.scryfall.io, no rate limit) instead of api.scryfall.com (rate
 * limited) ahead of PR #10928, which introduces a proper CdnUuidCache:
 * remote-fetched per-set data hosted on forge-extras, gzip-compressed,
 * DFC-aware, with 27 unit tests and a user-triggered Scryfall manifest sync
 * as a no-forge-extras-dependency alternative.
 *
 * DELETE THIS CLASS, both ForgeConstants entries it uses,
 * card-art-cdn-uuid.txt, and forge-gui/tools/updateCardArtCdnUuids.py once
 * #10928 merges, and call CdnUuidCache instead - it supersedes all of this.
 *
 * Two tiers, to avoid bundling one huge file with every install for a
 * feature most players never touch:
 *
 * 1. English (bundled, ~2MB): forge-gui/res/languages/card-art-cdn-uuid.txt,
 *    one UUID per printing, generated ahead of time and loaded eagerly.
 *
 * 2. Other languages (never bundled, built at runtime): the first time a
 *    card needs a UUID in a non-English language, this class pages through
 *    Scryfall's own /cards/search?q=lang:xx&unique=prints - a normal,
 *    rate-limited, publicly documented endpoint, same one used by browsers
 *    and every third-party Scryfall tool - in a background thread, paced at
 *    one request per SEARCH_PACE_MS. The result is written to
 *    {cacheDir}/cdn_uuid_bridge/<lang>.txt and read from there on every
 *    later lookup, this run and future ones. Lookups return null (falling
 *    back to the API, exactly as before) while the sync is still running or
 *    if it fails; a failed sync is not retried automatically within the
 *    same run.
 *
 * A language search typically returns a few thousand to a few tens of
 * thousands of prints (175 per page), so a first-time sync can take from a
 * few seconds up to a couple of minutes depending on the language and
 * connection - all in the background, never blocking a card image load.
 */
public final class CardCdnUuidBridge {

    private static final long SEARCH_PACE_MS = 150;

    // Anchors each card object at its guaranteed-unique "object":"card" marker,
    // then reads the FIRST id/set/collector_number found before the next such
    // marker (or end of page). None of Scryfall's nested sub-objects (card_faces,
    // image_uris, prices, related_uris, purchase_uris) use exactly these three key
    // names, so the first match within that window is reliably the top-level one.
    private static final Pattern CARD_BLOCK = Pattern.compile(
            "\"object\"\\s*:\\s*\"card\"(.*?)(?=\"object\"\\s*:\\s*\"card\"|\\z)", Pattern.DOTALL);
    private static final Pattern ID_FIELD = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F-]{36})\"");
    private static final Pattern SET_FIELD = Pattern.compile("\"set\"\\s*:\\s*\"([a-z0-9]+)\"");
    private static final Pattern CN_FIELD = Pattern.compile("\"collector_number\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HAS_MORE = Pattern.compile("\"has_more\"\\s*:\\s*(true|false)");
    private static final Pattern NEXT_PAGE = Pattern.compile("\"next_page\"\\s*:\\s*\"([^\"]+)\"");

    private static volatile CardCdnUuidBridge instance;

    private final Map<String, Map<String, String>> englishIndex;
    private final Map<String, Map<String, Map<String, String>>> foreignCache = new HashMap<>();
    private final Set<String> foreignSyncInFlightOrDone = new HashSet<>();

    public static CardCdnUuidBridge instance() {
        CardCdnUuidBridge result = instance;
        if (result == null) {
            synchronized (CardCdnUuidBridge.class) {
                result = instance;
                if (result == null) {
                    instance = result = new CardCdnUuidBridge(ForgeConstants.CARD_ART_CDN_UUID_FILE);
                }
            }
        }
        return result;
    }

    CardCdnUuidBridge(String englishFilePath) {
        this.englishIndex = loadFlatFile(new File(englishFilePath));
    }

    /** Returns the Scryfall id for this printing/language, or null if not known locally yet. */
    public String getUuid(String setCode, String collectorNumber, String langCode) {
        if (setCode == null || collectorNumber == null) {
            return null;
        }
        final String lang = (langCode == null || langCode.isEmpty()) ? "en" : langCode.toLowerCase();
        final String set = setCode.toLowerCase();

        if ("en".equals(lang)) {
            final Map<String, String> collectors = englishIndex.get(set);
            return collectors == null ? null : collectors.get(collectorNumber);
        }

        final Map<String, String> collectors = getOrTriggerForeignIndex(lang).get(set);
        return collectors == null ? null : collectors.get(collectorNumber);
    }

    /**
     * Call this proactively (e.g. when the player changes their preferred card
     * image language in Preferences) to start building that language's index
     * right away, instead of waiting for the first card lookup to trigger it
     * mid-gameplay. Safe to call repeatedly; a sync only ever starts once.
     */
    public void ensureLanguageAvailable(String langCode) {
        if (langCode != null && !langCode.isEmpty() && !"en".equalsIgnoreCase(langCode)) {
            getOrTriggerForeignIndex(langCode.toLowerCase());
        }
    }

    private synchronized Map<String, Map<String, String>> getOrTriggerForeignIndex(String lang) {
        Map<String, Map<String, String>> cached = foreignCache.get(lang);
        if (cached != null) {
            return cached;
        }

        final File localFile = new File(ForgeConstants.CACHE_CDN_UUID_BRIDGE_DIR, lang + ".txt");
        if (localFile.isFile()) {
            // Already built in a previous run (or the background sync just finished
            // writing it): load once and keep it in memory for the rest of this run.
            Map<String, Map<String, String>> loaded = loadFlatFile(localFile);
            foreignCache.put(lang, loaded);
            return loaded;
        }

        if (!foreignSyncInFlightOrDone.contains(lang)) {
            foreignSyncInFlightOrDone.add(lang);
            startBackgroundSync(lang, localFile);
        }
        // Sync not finished yet (or failed): return an empty map for now. Callers
        // get null lookups and fall back to the API, exactly as before. The next
        // call to this method - the next card that needs this language - will
        // pick up the finished file once it's there.
        return new HashMap<>();
    }

    private void startBackgroundSync(String lang, File destination) {
        Thread syncThread = new Thread(() -> runForeignSync(lang, destination), "CardCdnUuidBridge-sync-" + lang);
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void runForeignSync(String lang, File destination) {
        final Map<String, Map<String, String>> collected = new HashMap<>();
        String pageUrl = ForgeConstants.URL_SCRYFALL_SEARCH_BRIDGE + "?q=lang%3A" + lang + "&unique=prints";
        int pages = 0;
        try {
            while (pageUrl != null) {
                String body = fetchPage(pageUrl);
                if (body == null) {
                    System.err.println("CardCdnUuidBridge: sync for '" + lang + "' failed on page " + (pages + 1)
                            + " - falling back to api.scryfall.com for this language this run.");
                    return;
                }
                extractCards(body, collected);
                pages++;

                Matcher hasMore = HAS_MORE.matcher(body);
                boolean more = hasMore.find() && "true".equals(hasMore.group(1));
                if (!more) {
                    pageUrl = null;
                } else {
                    Matcher next = NEXT_PAGE.matcher(body);
                    pageUrl = next.find() ? unescapeJsonString(next.group(1)) : null;
                }

                if (pageUrl != null) {
                    Thread.sleep(SEARCH_PACE_MS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        writeFlatFile(collected, destination);
        System.out.println("CardCdnUuidBridge: built '" + lang + "' index (" + pages + " page(s), "
                + collected.values().stream().mapToInt(Map::size).sum() + " prints) -> " + destination);
    }

    private String fetchPage(String pageUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(pageUrl).toURL().openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            connection.setRequestProperty("User-Agent", "Forge/1.0 (temporary CDN bridge, see PR #10928)");
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != 200) {
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("CardCdnUuidBridge: request failed for " + pageUrl + ": " + e.getMessage());
            return null;
        }
    }

    /** Undoes JSON string escaping (forward slashes, unicode escapes, etc.) - next_page needs this, or an escaped '&' breaks pagination past page 2. */
    private static String unescapeJsonString(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                if (next == 'u' && i + 5 < s.length()) {
                    out.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                    i += 5;
                } else {
                    out.append(next);
                    i += 1;
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static void extractCards(String pageBody, Map<String, Map<String, String>> out) {
        Matcher blocks = CARD_BLOCK.matcher(pageBody);
        while (blocks.find()) {
            String block = blocks.group(1);
            Matcher idM = ID_FIELD.matcher(block);
            Matcher setM = SET_FIELD.matcher(block);
            Matcher cnM = CN_FIELD.matcher(block);
            if (!idM.find() || !setM.find() || !cnM.find()) {
                continue;
            }
            String id = idM.group(1).toLowerCase();
            String set = setM.group(1).toLowerCase();
            String cn = cnM.group(1);
            out.computeIfAbsent(set, k -> new HashMap<>()).put(cn, id);
        }
    }

    private static Map<String, Map<String, String>> loadFlatFile(File file) {
        final Map<String, Map<String, String>> result = new HashMap<>();
        if (!file.isFile()) {
            return result;
        }
        for (String line : FileUtil.readFile(file)) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            final int slash = line.indexOf('/');
            final int eq = line.indexOf('=');
            if (slash <= 0 || eq <= slash || eq == line.length() - 1) {
                continue;
            }
            final String setCode = line.substring(0, slash).toLowerCase();
            final String collector = line.substring(slash + 1, eq);
            final String uuid = line.substring(eq + 1).trim().toLowerCase();
            if (uuid.length() <= 2) {
                continue;
            }
            result.computeIfAbsent(setCode, k -> new HashMap<>()).put(collector, uuid);
        }
        return result;
    }

    private static void writeFlatFile(Map<String, Map<String, String>> index, File destination) {
        try {
            File dir = destination.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                return;
            }
            File tempFile = File.createTempFile("cdn_uuid_bridge", ".tmp", dir);
            try (java.io.Writer w = Files.newBufferedWriter(tempFile.toPath(), StandardCharsets.UTF_8)) {
                w.write("# TEMPORARY BRIDGE - built at runtime from Scryfall's /cards/search. See PR #10928.\n");
                for (Map.Entry<String, Map<String, String>> setEntry : index.entrySet()) {
                    for (Map.Entry<String, String> cnEntry : setEntry.getValue().entrySet()) {
                        w.write(setEntry.getKey().toUpperCase() + "/" + cnEntry.getKey() + "=" + cnEntry.getValue() + "\n");
                    }
                }
            }
            Files.move(tempFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("CardCdnUuidBridge: could not write " + destination + ": " + e.getMessage());
        }
    }

    /**
     * Builds a cards.scryfall.io CDN URL from a Scryfall id, following the
     * documented formula: https://cards.scryfall.io/{size}/{face}/{id[0]}/{id[1]}/{id}.jpg
     */
    public static String buildCdnUrl(String uuid, String size, String face) {
        if (uuid == null || uuid.length() < 2) {
            return null;
        }
        final String faceSegment = "back".equals(face) ? "back" : "front";
        return ForgeConstants.URL_SCRYFALL_CDN_BRIDGE + size + "/" + faceSegment + "/"
                + uuid.charAt(0) + "/" + uuid.charAt(1) + "/" + uuid + ".jpg";
    }
}
