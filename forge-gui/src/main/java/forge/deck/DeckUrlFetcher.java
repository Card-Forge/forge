package forge.deck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches deck lists from popular deck-building websites and converts them
 * to a text format compatible with {@link DeckRecognizer}.
 *
 * Supported sites: Moxfield, Archidekt, EDHREC, TappedOut, MTGGoldfish
 */
public class DeckUrlFetcher {

    /** Result of a URL fetch operation. */
    public static class FetchResult {
        private final boolean success;
        private final String deckText;
        private final String message;
        private final String siteName;

        private FetchResult(boolean success, String deckText, String message, String siteName) {
            this.success = success;
            this.deckText = deckText;
            this.message = message;
            this.siteName = siteName;
        }

        static FetchResult ok(String deckText, String siteName, int cardCount) {
            return new FetchResult(true, deckText,
                    String.format("Deck loaded from %s (%d cards)", siteName, cardCount), siteName);
        }

        static FetchResult error(String message) {
            return new FetchResult(false, null, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getDeckText() { return deckText; }
        public String getMessage() { return message; }
        public String getSiteName() { return siteName; }
    }

    // URL patterns for each site
    private static final Pattern MOXFIELD_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?moxfield\\.com/decks/([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARCHIDEKT_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?archidekt\\.com/decks/(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EDHREC_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?edhrec\\.com/deckpreview/([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAPPEDOUT_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?tappedout\\.net/mtg-decks/([\\w-]+)/?", Pattern.CASE_INSENSITIVE);
    private static final Pattern MTGGOLDFISH_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?mtggoldfish\\.com/deck/(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final String USER_AGENT = "Forge-MTG/2.0 (Deck Importer)";

    /**
     * Returns true if the given string looks like a supported deck URL.
     */
    public static boolean isSupportedUrl(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String url = text.trim();
        return MOXFIELD_PATTERN.matcher(url).find()
                || ARCHIDEKT_PATTERN.matcher(url).find()
                || EDHREC_PATTERN.matcher(url).find()
                || TAPPEDOUT_PATTERN.matcher(url).find()
                || MTGGOLDFISH_PATTERN.matcher(url).find();
    }

    /**
     * Fetches a deck from a URL.
     * @param url the deck URL from a supported site
     * @return a FetchResult with the deck text or error message
     */
    public static FetchResult fetch(String url) {
        if (url == null || url.trim().isEmpty()) {
            return FetchResult.error("Please enter a URL.");
        }
        url = url.trim();

        try {
            Matcher m;

            m = MOXFIELD_PATTERN.matcher(url);
            if (m.find()) return fetchMoxfield(m.group(1));

            m = ARCHIDEKT_PATTERN.matcher(url);
            if (m.find()) return fetchArchidekt(m.group(1));

            m = EDHREC_PATTERN.matcher(url);
            if (m.find()) return fetchEdhrec(m.group(1));

            m = TAPPEDOUT_PATTERN.matcher(url);
            if (m.find()) return fetchTappedOut(m.group(1));

            m = MTGGOLDFISH_PATTERN.matcher(url);
            if (m.find()) return fetchMTGGoldfish(m.group(1));

            return FetchResult.error("Site not supported. Supported: Moxfield, Archidekt, EDHREC, TappedOut, MTGGoldfish");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("HTTP")) {
                return FetchResult.error(msg);
            }
            return FetchResult.error("Connection error. Please check your internet connection.");
        }
    }

    // ========== MOXFIELD ==========

    private static FetchResult fetchMoxfield(String deckId) throws IOException {
        String apiUrl = "https://api2.moxfield.com/v2/decks/all/" + deckId;
        String json = httpGet(apiUrl);
        if (json == null) {
            return FetchResult.error("Could not fetch deck from Moxfield. Make sure the deck is public.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        // Parse commanders
        List<String> commanders = parseMoxfieldSection(json, "commanders");
        if (!commanders.isEmpty()) {
            sb.append("Commander\n");
            for (String line : commanders) {
                sb.append(line).append("\n");
            }
            totalCards += commanders.size();
            sb.append("\n");
        }

        // Parse companions
        List<String> companions = parseMoxfieldSection(json, "companions");
        if (!companions.isEmpty()) {
            sb.append("Commander\n"); // companions go in commander zone
            for (String line : companions) {
                sb.append(line).append("\n");
            }
            totalCards += companions.size();
            sb.append("\n");
        }

        // Parse mainboard
        List<String> mainboard = parseMoxfieldSection(json, "mainboard");
        if (!mainboard.isEmpty()) {
            sb.append("Main\n");
            for (String line : mainboard) {
                sb.append(line).append("\n");
            }
            totalCards += mainboard.size();
            sb.append("\n");
        }

        // Parse sideboard
        List<String> sideboard = parseMoxfieldSection(json, "sideboard");
        if (!sideboard.isEmpty()) {
            sb.append("Sideboard\n");
            for (String line : sideboard) {
                sb.append(line).append("\n");
            }
            totalCards += sideboard.size();
        }

        if (totalCards == 0) {
            return FetchResult.error("Deck is empty or could not be parsed from Moxfield.");
        }

        return FetchResult.ok(sb.toString().trim(), "Moxfield", totalCards);
    }

    /**
     * Parses a Moxfield section from JSON.
     * Moxfield JSON has sections like: "mainboard":{"Card Name":{"quantity":4,"card":{"name":"Card Name",...}}, ...}
     * The keys of each section object are card identifiers, and each entry has a "quantity" field.
     * The card name can be found as the key itself or in the nested "card"."name" field.
     */
    private static List<String> parseMoxfieldSection(String json, String sectionName) {
        List<String> cards = new ArrayList<>();
        // Find the section in JSON: "sectionName":{...}
        String sectionKey = "\"" + sectionName + "\"";
        int sectionStart = json.indexOf(sectionKey);
        if (sectionStart < 0) return cards;

        int braceStart = json.indexOf('{', sectionStart + sectionKey.length());
        if (braceStart < 0) return cards;

        String sectionJson = extractJsonObject(json, braceStart);
        if (sectionJson == null || sectionJson.equals("{}")) return cards;

        // Parse each card entry in the section
        // Look for "quantity": N and "name": "Card Name" patterns within each card object
        int pos = 0;
        while (pos < sectionJson.length()) {
            // Find next card object - look for the pattern "quantity":
            int qtyIdx = sectionJson.indexOf("\"quantity\"", pos);
            if (qtyIdx < 0) break;

            int quantity = extractIntValue(sectionJson, qtyIdx);
            if (quantity <= 0) quantity = 1;

            // Find the card name - look for "name" within the "card" sub-object
            // First find the "card" object near this quantity
            int cardObjIdx = sectionJson.indexOf("\"card\"", qtyIdx);
            int nextQtyIdx = sectionJson.indexOf("\"quantity\"", qtyIdx + 10);
            // Make sure the "card" key belongs to this entry
            if (cardObjIdx > 0 && (nextQtyIdx < 0 || cardObjIdx < nextQtyIdx)) {
                String cardName = extractStringValue(sectionJson, cardObjIdx, "\"name\"");
                if (cardName != null && !cardName.isEmpty()) {
                    cards.add(quantity + " " + cardName);
                }
            }

            pos = (nextQtyIdx > 0) ? nextQtyIdx : sectionJson.length();
        }

        return cards;
    }

    // ========== ARCHIDEKT ==========

    private static FetchResult fetchArchidekt(String deckId) throws IOException {
        String apiUrl = "https://archidekt.com/api/decks/" + deckId + "/";
        String json = httpGet(apiUrl);

        // If the API returns an error or null, the deck may use new ID format
        if (json == null || json.contains("\"error\"")) {
            return FetchResult.error("Could not fetch deck from Archidekt. The deck may be private or use an unsupported URL format.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        Map<String, List<String>> sections = new LinkedHashMap<>();

        // Find the "cards" array
        int cardsIdx = json.indexOf("\"cards\"");
        if (cardsIdx < 0) {
            return FetchResult.error("Could not parse deck from Archidekt.");
        }

        int arrayStart = json.indexOf('[', cardsIdx);
        if (arrayStart < 0) {
            return FetchResult.error("Could not parse deck from Archidekt.");
        }

        String cardsArray = extractJsonArray(json, arrayStart);
        if (cardsArray == null) {
            return FetchResult.error("Could not parse deck from Archidekt.");
        }

        // Parse each card object in the array
        int pos = 0;
        while (pos < cardsArray.length()) {
            int objStart = cardsArray.indexOf('{', pos);
            if (objStart < 0) break;

            String cardObj = extractJsonObject(cardsArray, objStart);
            if (cardObj == null) {
                pos = objStart + 1;
                continue;
            }

            int quantity = 0;
            int qtyIdx = cardObj.indexOf("\"quantity\"");
            if (qtyIdx >= 0) {
                quantity = extractIntValue(cardObj, qtyIdx);
            }
            if (quantity <= 0) quantity = 1;

            // Get card name from oracleCard.name
            String cardName = null;
            int oracleIdx = cardObj.indexOf("\"oracleCard\"");
            if (oracleIdx >= 0) {
                cardName = extractStringValue(cardObj, oracleIdx, "\"name\"");
            }
            // Fallback: try displayName or card.name
            if (cardName == null || cardName.isEmpty()) {
                cardName = extractStringValue(cardObj, 0, "\"displayName\"");
            }
            if (cardName == null || cardName.isEmpty()) {
                int cardIdx = cardObj.indexOf("\"card\"");
                if (cardIdx >= 0) {
                    cardName = extractStringValue(cardObj, cardIdx, "\"name\"");
                }
            }

            // Get category
            String category = "Main";
            int catIdx = cardObj.indexOf("\"categories\"");
            if (catIdx >= 0) {
                int catArrayStart = cardObj.indexOf('[', catIdx);
                if (catArrayStart >= 0) {
                    int catArrayEnd = cardObj.indexOf(']', catArrayStart);
                    if (catArrayEnd >= 0) {
                        String catStr = cardObj.substring(catArrayStart + 1, catArrayEnd).trim();
                        if (!catStr.isEmpty()) {
                            String firstCat = extractFirstQuotedString(catStr);
                            if (firstCat != null) {
                                category = mapArchidektCategory(firstCat);
                            }
                        }
                    }
                }
            }

            if (cardName != null && !cardName.isEmpty()) {
                sections.computeIfAbsent(category, k -> new ArrayList<>()).add(quantity + " " + cardName);
                totalCards++;
            }

            pos = objStart + cardObj.length();
        }

        // Build output in order: Commander, Main, Sideboard
        String[] sectionOrder = {"Commander", "Main", "Sideboard"};
        for (String section : sectionOrder) {
            List<String> cardLines = sections.remove(section);
            if (cardLines != null && !cardLines.isEmpty()) {
                sb.append(section).append("\n");
                for (String line : cardLines) {
                    sb.append(line).append("\n");
                }
                sb.append("\n");
            }
        }
        // Remaining sections
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sb.append(entry.getKey()).append("\n");
                for (String line : entry.getValue()) {
                    sb.append(line).append("\n");
                }
                sb.append("\n");
            }
        }

        if (totalCards == 0) {
            return FetchResult.error("Deck is empty or could not be parsed from Archidekt.");
        }

        return FetchResult.ok(sb.toString().trim(), "Archidekt", totalCards);
    }

    private static String mapArchidektCategory(String category) {
        String lower = category.toLowerCase();
        if (lower.contains("commander")) return "Commander";
        if (lower.contains("sideboard")) return "Sideboard";
        if (lower.contains("maybeboard") || lower.contains("considering")) return "Maybeboard";
        return "Main";
    }

    // ========== EDHREC ==========

    private static FetchResult fetchEdhrec(String deckId) throws IOException {
        // EDHREC deckpreview pages embed deck data as JSON in the HTML
        String pageUrl = "https://edhrec.com/deckpreview/" + deckId;
        String html = httpGet(pageUrl);
        if (html == null) {
            return FetchResult.error("Could not fetch deck from EDHREC. Make sure the link is valid.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        // Extract commanders from "commanders":["Name1","Name2"] (may contain nulls)
        List<String> commanders = extractEdhrecSimpleArray(html, "commanders");
        if (!commanders.isEmpty()) {
            sb.append("Commander\n");
            for (String card : commanders) {
                sb.append("1 ").append(card).append("\n");
            }
            totalCards += commanders.size();
            sb.append("\n");
        }

        // Extract cards from "cardlists":[{"cardviews":[{"name":"Card Name",...},...]}]
        List<String> cards = extractEdhrecCardlists(html);
        if (!cards.isEmpty()) {
            sb.append("Main\n");
            for (String card : cards) {
                sb.append("1 ").append(card).append("\n");
            }
            totalCards += cards.size();
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from EDHREC. The link may be invalid or the deck may be empty.");
        }

        return FetchResult.ok(sb.toString().trim(), "EDHREC", totalCards);
    }

    /** Extracts card names from a simple JSON array like "key":["Name1","Name2",null] */
    private static List<String> extractEdhrecSimpleArray(String html, String key) {
        List<String> cards = new ArrayList<>();
        String searchKey = "\"" + key + "\"";
        int idx = html.indexOf(searchKey);
        if (idx < 0) return cards;

        int arrayStart = html.indexOf('[', idx);
        if (arrayStart < 0 || arrayStart > idx + searchKey.length() + 5) return cards;

        int arrayEnd = html.indexOf(']', arrayStart);
        if (arrayEnd < 0) return cards;

        String arrayContent = html.substring(arrayStart + 1, arrayEnd);
        Pattern namePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = namePattern.matcher(arrayContent);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!name.isEmpty() && !name.startsWith("http") && !name.equals("null")) {
                cards.add(name);
            }
        }
        return cards;
    }

    /** Extracts card names from EDHREC's "cardlists":[{"cardviews":[{"name":"X",...}]}] structure */
    private static List<String> extractEdhrecCardlists(String html) {
        List<String> cards = new ArrayList<>();
        // EDHREC embeds card data in cardviews objects with "name" fields
        // The structure is deeply nested, so we search for "cardviews" arrays
        // and extract "name" values from each card object within them

        int searchFrom = 0;
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();

        while (searchFrom < html.length()) {
            int cvIdx = html.indexOf("\"cardviews\"", searchFrom);
            if (cvIdx < 0) break;

            int arrayStart = html.indexOf('[', cvIdx);
            if (arrayStart < 0) break;

            String cardviewsArray = extractJsonArray(html, arrayStart);
            if (cardviewsArray == null) {
                searchFrom = cvIdx + 12;
                continue;
            }

            // Extract "name":"Card Name" from each cardview object
            // The "name" field appears right after other fields like "cmc", "id", etc.
            // We look for "name" that's followed by a card name (not a URL or set code)
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]{2,50})\"");
            Matcher matcher = namePattern.matcher(cardviewsArray);
            while (matcher.find()) {
                String name = matcher.group(1);
                // Filter: card names start with uppercase, don't contain URLs or slashes
                if (Character.isUpperCase(name.charAt(0))
                        && !name.startsWith("http")
                        && !name.contains("scryfall")
                        && !name.contains(".com")
                        && !name.contains(".io")
                        && !name.equals("Normal") && !name.equals("Foil")
                        && !name.equals("Creature") && !name.equals("Instant")
                        && !name.equals("Sorcery") && !name.equals("Artifact")
                        && !name.equals("Enchantment") && !name.equals("Land")
                        && !name.equals("Planeswalker")) {
                    seen.add(name);
                }
            }

            searchFrom = arrayStart + cardviewsArray.length();
        }

        cards.addAll(seen);
        return cards;
    }

    // ========== TAPPEDOUT ==========

    private static FetchResult fetchTappedOut(String deckSlug) throws IOException {
        // TappedOut supports ?fmt=txt for plain text export
        String textUrl = "https://tappedout.net/mtg-decks/" + deckSlug + "/?fmt=txt";
        String text = httpGet(textUrl);
        if (text == null || text.trim().isEmpty()) {
            return FetchResult.error("Could not fetch deck from TappedOut. Make sure the deck is public.");
        }

        // TappedOut text format: "1x Card Name" or "1 Card Name" with "Sideboard:" section
        // Clean up the format to match DeckRecognizer expectations
        StringBuilder sb = new StringBuilder();
        int totalCards = 0;
        boolean inSideboard = false;
        boolean wroteMain = false;

        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.toLowerCase().startsWith("sideboard")) {
                inSideboard = true;
                sb.append("\nSideboard\n");
                continue;
            }

            // Match lines like "1x Card Name" or "1 Card Name"
            Pattern cardPattern = Pattern.compile("^(\\d+)x?\\s+(.+)$");
            Matcher cm = cardPattern.matcher(line);
            if (cm.matches()) {
                if (!wroteMain && !inSideboard) {
                    sb.append("Main\n");
                    wroteMain = true;
                }
                int qty = Integer.parseInt(cm.group(1));
                String name = cm.group(2).trim();
                // Remove category tags that TappedOut sometimes appends
                int hashIdx = name.indexOf('#');
                if (hashIdx > 0) name = name.substring(0, hashIdx).trim();
                int asterIdx = name.indexOf('*');
                if (asterIdx > 0) name = name.substring(0, asterIdx).trim();
                sb.append(qty).append(" ").append(name).append("\n");
                totalCards++;
            }
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from TappedOut. The deck may be empty or private.");
        }

        return FetchResult.ok(sb.toString().trim(), "TappedOut", totalCards);
    }

    // ========== MTGGOLDFISH ==========

    private static FetchResult fetchMTGGoldfish(String deckId) throws IOException {
        // MTGGoldfish has a download link at /deck/download/{id}
        String downloadUrl = "https://www.mtggoldfish.com/deck/download/" + deckId;
        String text = httpGet(downloadUrl);
        if (text == null || text.trim().isEmpty()) {
            return FetchResult.error("Could not fetch deck from MTGGoldfish. Make sure the deck exists.");
        }

        // The download format is plain text:
        // Card Name\n... (mainboard), blank line, then sideboard
        StringBuilder sb = new StringBuilder();
        int totalCards = 0;
        boolean inSideboard = false;
        boolean wroteMain = false;
        boolean hitBlankLine = false;

        for (String line : text.split("\n")) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                hitBlankLine = true;
                continue;
            }

            // Match lines like "4 Card Name" or just "Card Name"
            Pattern cardPattern = Pattern.compile("^(\\d+)\\s+(.+)$");
            Matcher cm = cardPattern.matcher(trimmed);
            if (cm.matches()) {
                if (hitBlankLine && !inSideboard && wroteMain) {
                    inSideboard = true;
                    sb.append("\nSideboard\n");
                }
                if (!wroteMain && !inSideboard) {
                    sb.append("Main\n");
                    wroteMain = true;
                }
                sb.append(trimmed).append("\n");
                totalCards++;
            }
            hitBlankLine = false;
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from MTGGoldfish.");
        }

        return FetchResult.ok(sb.toString().trim(), "MTGGoldfish", totalCards);
    }

    // ========== HTTP Utility ==========

    private static String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "application/json, text/html, text/plain, */*");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status == 404) {
            throw new IOException("HTTP 404 - Deck not found. Make sure the URL is correct and the deck is public.");
        }
        if (status == 403) {
            throw new IOException("HTTP 403 - Access denied. The deck may be private.");
        }
        if (status != 200) {
            throw new IOException("HTTP " + status + " error from server.");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            conn.disconnect();
        }

        return sb.toString();
    }

    // ========== JSON Parsing Utilities ==========

    /** Extracts a JSON object string starting at the given brace position, handling nesting. */
    static String extractJsonObject(String json, int braceStart) {
        if (braceStart < 0 || braceStart >= json.length() || json.charAt(braceStart) != '{') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(braceStart, i + 1);
                }
            }
        }
        return null;
    }

    /** Extracts a JSON array string starting at the given bracket position, handling nesting. */
    static String extractJsonArray(String json, int bracketStart) {
        if (bracketStart < 0 || bracketStart >= json.length() || json.charAt(bracketStart) != '[') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;

            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(bracketStart, i + 1);
                }
            }
        }
        return null;
    }

    /** Extracts an integer value after a "key": pattern. */
    private static int extractIntValue(String json, int keyStart) {
        int colonIdx = json.indexOf(':', keyStart);
        if (colonIdx < 0) return 0;
        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (end <= start) return 0;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Starting from searchStart, finds the next occurrence of targetKey and extracts the string value.
     * For example, given searchStart pointing to "card" object and targetKey "name",
     * finds "name":"Some Card" and returns "Some Card".
     */
    private static String extractStringValue(String json, int searchStart, String targetKey) {
        int keyIdx = json.indexOf(targetKey, searchStart);
        if (keyIdx < 0) return null;

        // Don't search too far (max 2000 chars forward for the name within this context)
        if (keyIdx - searchStart > 2000) return null;

        int colonIdx = json.indexOf(':', keyIdx + targetKey.length());
        if (colonIdx < 0) return null;

        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;

        // Find closing quote, respecting escapes
        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            char c = json.charAt(quoteEnd);
            if (c == '\\') {
                quoteEnd += 2; // skip escaped char
                continue;
            }
            if (c == '"') break;
            quoteEnd++;
        }

        if (quoteEnd >= json.length()) return null;
        String value = json.substring(quoteStart + 1, quoteEnd);
        // Unescape common JSON escapes
        value = value.replace("\\\"", "\"").replace("\\\\", "\\")
                     .replace("\\/", "/").replace("\\n", "").replace("\\t", "");
        // Unescape unicode sequences like \u0027 (apostrophe)
        value = unescapeUnicode(value);
        return value;
    }

    /** Replaces \\uXXXX sequences with the corresponding character. */
    private static String unescapeUnicode(String text) {
        if (!text.contains("\\u")) return text;
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (i + 5 < text.length() && text.charAt(i) == '\\' && text.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(text.substring(i + 2, i + 6), 16);
                    result.append((char) code);
                    i += 5;
                    continue;
                } catch (NumberFormatException e) {
                    // not a valid unicode escape, keep as-is
                }
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    /** Extracts the first quoted string from a text. */
    private static String extractFirstQuotedString(String text) {
        int firstQuote = text.indexOf('"');
        if (firstQuote < 0) return null;
        int secondQuote = text.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return text.substring(firstQuote + 1, secondQuote);
    }
}
