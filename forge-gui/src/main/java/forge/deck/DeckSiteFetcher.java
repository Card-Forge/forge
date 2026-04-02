package forge.deck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Base class for site-specific deck fetchers.
 * Provides shared HTTP and JSON parsing utilities.
 */
public abstract class DeckSiteFetcher {

    protected static final String USER_AGENT = "Forge-MTG/2.0 (Deck Importer)";

    /** Returns the URL pattern for this site. Group 1 must capture the deck identifier. */
    public abstract Pattern getUrlPattern();

    /** Returns the display name of this site. */
    public abstract String getSiteName();

    /** Fetches and parses a deck given the identifier extracted from the URL pattern. */
    public abstract FetchResult fetchDeck(String deckId) throws IOException;

    // ========== HTTP ==========

    protected static String httpGet(String urlStr) throws IOException {
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

    // ========== Deck Name ==========

    protected static void appendDeckName(StringBuilder sb, String deckName) {
        if (deckName != null && !deckName.trim().isEmpty()) {
            sb.append("Name: ").append(deckName.trim()).append("\n");
        }
    }

    // ========== JSON Parsing ==========

    /** Extracts a JSON object string starting at the given brace position, handling nesting. */
    protected static String extractJsonObject(String json, int braceStart) {
        if (braceStart < 0 || braceStart >= json.length() || json.charAt(braceStart) != '{') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(braceStart, i + 1);
            }
        }
        return null;
    }

    /** Extracts a JSON array string starting at the given bracket position, handling nesting. */
    protected static String extractJsonArray(String json, int bracketStart) {
        if (bracketStart < 0 || bracketStart >= json.length() || json.charAt(bracketStart) != '[') {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;

            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return json.substring(bracketStart, i + 1);
            }
        }
        return null;
    }

    /** Extracts an integer value after a "key": pattern. */
    protected static int extractIntValue(String json, int keyStart) {
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

    /** Finds targetKey starting from searchStart and extracts its string value. */
    protected static String extractStringValue(String json, int searchStart, String targetKey) {
        int keyIdx = json.indexOf(targetKey, searchStart);
        if (keyIdx < 0) return null;
        if (keyIdx - searchStart > 2000) return null;

        int colonIdx = json.indexOf(':', keyIdx + targetKey.length());
        if (colonIdx < 0) return null;

        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart < 0) return null;

        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            char c = json.charAt(quoteEnd);
            if (c == '\\') { quoteEnd += 2; continue; }
            if (c == '"') break;
            quoteEnd++;
        }

        if (quoteEnd >= json.length()) return null;
        String value = json.substring(quoteStart + 1, quoteEnd);
        value = value.replace("\\\"", "\"").replace("\\\\", "\\")
                     .replace("\\/", "/").replace("\\n", "").replace("\\t", "");
        value = unescapeUnicode(value);
        return value;
    }

    /** Replaces \\uXXXX sequences with the corresponding character. */
    protected static String unescapeUnicode(String text) {
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
                    // not a valid unicode escape
                }
            }
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    /** Extracts the first quoted string from a text. */
    protected static String extractFirstQuotedString(String text) {
        int firstQuote = text.indexOf('"');
        if (firstQuote < 0) return null;
        int secondQuote = text.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return text.substring(firstQuote + 1, secondQuote);
    }

    /** Extracts a card name from a "N CardName" line. */
    protected static String extractCardName(String line) {
        int spaceIdx = line.indexOf(' ');
        return spaceIdx > 0 ? line.substring(spaceIdx + 1).trim() : line;
    }
}
