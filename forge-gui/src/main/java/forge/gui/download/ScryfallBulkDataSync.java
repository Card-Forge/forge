package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.gui.GuiBase;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPInputStream;

/**
 * Warms {@link CdnUuidCache} for every set in one pass via Scryfall's Bulk Data API
 * (https://scryfall.com/docs/api/bulk-data) 
 */
public final class ScryfallBulkDataSync {
    private static final String BULK_DATA_LISTING_URL = "https://api.scryfall.com/bulk-data";
    public static final String BULK_TYPE_DEFAULT_CARDS = "default_cards";
    public static final String BULK_TYPE_ALL_CARDS = "all_cards";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    /** Override for tests; must be a full URL to a bulk-data listing JSON document. */
    static volatile String listingUrlOverride = null;

    private ScryfallBulkDataSync() {}

    /** Approximate compressed download size for the given bulk type, measured against the live API. */
    public static String approxSizeLabel(String bulkDataType) {
        return BULK_TYPE_ALL_CARDS.equals(bulkDataType) ? "375 MB" : "75 MB";
    }

    public interface ProgressListener {
        /** @param fractionDone 0.0-1.0, or -1 if not yet known (e.g. before a Content-Length is available) */
        void onProgress(String message, double fractionDone);
    }

    /**
     * Downloads and parses the given Scryfall bulk file type ({@link #BULK_TYPE_DEFAULT_CARDS} or
     * {@link #BULK_TYPE_ALL_CARDS}), merging every print into {@link CdnUuidCache}. If
     * {@code allowedLangs} is non-null, only prints in one of those languages are kept -- lets a
     * single-language sync of {@code all_cards} skip writing the other 16+ languages to disk.
     * Returns the number of sets written, or -1 on failure/cancellation before any data was merged.
     */
    public static int sync(String bulkDataType, Set<String> allowedLangs, ProgressListener progress, BooleanSupplier cancelled) {
        String downloadUrl = findBulkDataUrl(bulkDataType, progress);
        if (downloadUrl == null || cancelled.getAsBoolean()) {
            return -1;
        }

        final Map<String, Map<String, Map<String, String[]>>> bySet = new HashMap<>();
        // With every language in the file (allowedLangs == null) nothing is left out; otherwise prints that
        // exist in no English version must not vanish with the languages that are not wanted.
        final NoEnglishPrints noEnglish = allowedLangs == null ? null : new NoEnglishPrints();
        try {
            streamAndAccumulate(downloadUrl, bySet, allowedLangs, noEnglish, progress, cancelled);
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
        if (noEnglish != null) {
            noEnglish.addTo(bySet);
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
            progress.onProgress("Synced " + setCount + " sets from the online card index.", 1.0);
        }
        return setCount;
    }

    // -------------------------------------------------------------------------

    private static String findBulkDataUrl(String bulkDataType, ProgressListener progress) {
        String url = listingUrlOverride != null ? listingUrlOverride : BULK_DATA_LISTING_URL;
        if (progress != null) {
            progress.onProgress("Looking up available card index files...", -1);
        }
        try {
            ScryfallRateLimiter.acquire(url);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(CONNECT_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", GuiBase.getInterface().getUserAgent());
            conn.connect();

            int status = conn.getResponseCode();
            if (status == 429) {
                ScryfallRateLimiter.noteIfRateLimited(429, url, conn.getHeaderField("Retry-After"));
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
                if (!obj.has("type") || !bulkDataType.equals(obj.get("type").getAsString())) {
                    continue;
                }
                if (obj.has("jsonl_download_uri")) {
                    return obj.get("jsonl_download_uri").getAsString();
                }
                if (obj.has("download_uri")) {
                    return obj.get("download_uri").getAsString();
                }
            }
            Logger.error("ScryfallBulkDataSync: no '{}' entry found in bulk-data listing", bulkDataType);
            return null;
        } catch (IOException e) {
            Logger.error(e, "ScryfallBulkDataSync: failed to fetch bulk-data listing");
            return null;
        }
    }

    private static void streamAndAccumulate(String downloadUrl, Map<String, Map<String, Map<String, String[]>>> bySet,
                                             Set<String> allowedLangs, NoEnglishPrints noEnglish,
                                             ProgressListener progress, BooleanSupplier cancelled) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", GuiBase.getInterface().getUserAgent());
        // downloadUrl is a *.scryfall.io file origin, not api.scryfall.com -- unthrottled, no acquire() needed.
        conn.connect();

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + status + " for " + downloadUrl);
        }
        final long totalBytes = conn.getContentLength();
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
                addCard(bySet, card, allowedLangs, noEnglish);
                cardsSeen[0]++;

                long now = System.currentTimeMillis();
                if (progress != null && now - lastReportAt >= 500) {
                    lastReportAt = now;
                    double fraction = totalBytes > 0 ? Math.min(1.0, bytesRead[0] / (double) totalBytes) : -1;
                    String pct = totalBytes > 0 ? Math.round(fraction * 100) + "%" : (bytesRead[0] / 1_000_000) + " MB";
                    progress.onProgress("Downloading card index: " + pct
                            + " (" + cardsSeen[0] + " cards processed)...", fraction);
                }
            }
        }
    }

    private static void addCard(Map<String, Map<String, Map<String, String[]>>> bySet, JsonObject card, Set<String> allowedLangs,
                                NoEnglishPrints noEnglish) {
        String setCode = ScryfallSetSync.str(card, "set");
        String cn = ScryfallSetSync.str(card, "collector_number");
        String lang = ScryfallSetSync.str(card, "lang");
        if (setCode == null || cn == null || lang == null) {
            return;
        }
        if (noEnglish != null && "en".equals(lang)) {
            noEnglish.sawEnglish(setCode, cn); // whatever its image status: an English version exists
        }
        if (allowedLangs != null && !allowedLangs.contains(lang)) {
            if (noEnglish != null) {
                noEnglish.offer(setCode, cn, lang, card);
            }
            return;
        }

        String[] frontBack = ScryfallSetSync.frontBackUuids(card);
        if (frontBack == null) {
            return;
        }

        bySet.computeIfAbsent(setCode, k -> new HashMap<>())
                .computeIfAbsent(cn, k -> new HashMap<>())
                .put(lang, frontBack);
    }

    /**
     * Some prints exist in no English version: Neon Dynasty 293 is Japanese, 428 Phyrexian. Scryfall's default
     * listing has them in their own language, but a sync that only wants English and one other language
     * would drop them. This keeps, for each such print, the first of its languages in Scryfall's own order,
     * and adds it at the end if the print has no English version and nothing else was kept for it.
     */
    static final class NoEnglishPrints {
        private final Set<String> englishPrints = new HashSet<>();
        /** set -> collector number -> {language, uuids} of the best candidate so far. */
        private final Map<String, Map<String, Object[]>> candidates = new HashMap<>();

        void sawEnglish(String setCode, String cn) {
            englishPrints.add(setCode + '|' + cn);
            Map<String, Object[]> forSet = candidates.get(setCode);
            if (forSet != null) {
                forSet.remove(cn);
            }
        }

        void offer(String setCode, String cn, String lang, JsonObject card) {
            if (englishPrints.contains(setCode + '|' + cn)) {
                return;
            }
            String[] frontBack = ScryfallSetSync.frontBackUuids(card);
            if (frontBack == null) {
                return;
            }
            Map<String, Object[]> forSet = candidates.computeIfAbsent(setCode, k -> new HashMap<>());
            Object[] best = forSet.get(cn);
            if (best == null || CdnUuidCache.languageRank(lang) < CdnUuidCache.languageRank((String) best[0])) {
                forSet.put(cn, new Object[] {lang, frontBack});
            }
        }

        void addTo(Map<String, Map<String, Map<String, String[]>>> bySet) {
            for (Map.Entry<String, Map<String, Object[]>> forSet : candidates.entrySet()) {
                for (Map.Entry<String, Object[]> candidate : forSet.getValue().entrySet()) {
                    if (englishPrints.contains(forSet.getKey() + '|' + candidate.getKey())) {
                        continue;
                    }
                    Map<String, String[]> langs = bySet.computeIfAbsent(forSet.getKey(), k -> new HashMap<>())
                            .computeIfAbsent(candidate.getKey(), k -> new HashMap<>());
                    if (langs.isEmpty()) {
                        langs.put((String) candidate.getValue()[0], (String[]) candidate.getValue()[1]);
                    }
                }
            }
        }
    }
}
