package forge.gui.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import forge.localinstance.properties.ForgeConstants;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazy-loading, thread-safe cache for Scryfall CDN UUIDs stored in
 * {@code forge-gui/res/cdn_uuid/{setCode}/{collectorNumber}.json}.
 *
 * <p>Each JSON file maps language codes to the CDN UUID for that card print:
 * <ul>
 *   <li>{@code {"en":"uuid"}} — single language</li>
 *   <li>{@code {"en":"uuid","ja":"ja-uuid"}} — multiple languages</li>
 *   <li>{@code {"en":["frontUuid","backUuid"]}} — DFC with distinct face UUIDs (rare)</li>
 * </ul>
 *
 * <p>On the first lookup for a given set, all {@code {cn}.json} files in that set's
 * directory are loaded at once and held in memory for the lifetime of the process.
 * Returns {@code null} when no UUID data is available (caller falls back to the
 * rate-limited Scryfall API or the cardforge server).
 */
public final class CdnUuidCache {

    private static final String FALLBACK_LANG = "en";
    /** Sentinel: set directory was scanned and found empty / absent. */
    private static final Map<String, Map<String, LangUuids>> MISSING_SET = Collections.emptyMap();

    /**
     * Holds the front and (optionally different) back UUID for one language of one card.
     * {@code back} is null when both faces share the same UUID.
     */
    private static final class LangUuids {
        final String front;
        final String back; // null → same as front
        LangUuids(String front, String back) { this.front = front; this.back = back; }
    }

    /** Cache: setCode -> (collectorNumber -> (lang -> LangUuids)) */
    private static final ConcurrentHashMap<String, Map<String, Map<String, LangUuids>>> setCache =
            new ConcurrentHashMap<>();

    private CdnUuidCache() {}

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

        File setDir = new File(ForgeConstants.CDN_UUID_DIR + setCode);
        if (!setDir.isDirectory()) {
            setCache.put(setCode, MISSING_SET);
            return MISSING_SET;
        }

        File[] files = setDir.listFiles(f -> f.getName().endsWith(".json"));
        if (files == null || files.length == 0) {
            setCache.put(setCode, MISSING_SET);
            return MISSING_SET;
        }

        Map<String, Map<String, LangUuids>> cardMap = new HashMap<>(files.length * 2);
        for (File f : files) {
            String cn = f.getName().substring(0, f.getName().length() - 5); // strip .json
            try {
                Map<String, LangUuids> langMap = parseCardFile(f);
                if (!langMap.isEmpty()) cardMap.put(cn, langMap);
            } catch (Exception e) {
                Logger.warn("CdnUuidCache: failed to parse {}: {}", f, e.getMessage());
            }
        }

        Map<String, Map<String, LangUuids>> result = Collections.unmodifiableMap(cardMap);
        setCache.put(setCode, result);
        return result;
    }

    /**
     * Parses a single {@code {cn}.json} file.
     * Format: {@code {"en":"uuid","ja":["fuuid","buuid"],...}}
     */
    private static Map<String, LangUuids> parseCardFile(File file) throws Exception {
        JsonObject obj;
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            obj = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Map<String, LangUuids> result = new HashMap<>(obj.size() * 2);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                result.put(entry.getKey(), new LangUuids(val.getAsString(), null));
            } else if (val.isJsonArray()) {
                JsonArray arr = val.getAsJsonArray();
                if (arr.size() >= 2) {
                    String front = arr.get(0).getAsString();
                    String back  = arr.get(1).getAsString();
                    result.put(entry.getKey(), new LangUuids(front, back.equals(front) ? null : back));
                } else if (arr.size() == 1) {
                    result.put(entry.getKey(), new LangUuids(arr.get(0).getAsString(), null));
                }
            }
        }
        return result;
    }
}
