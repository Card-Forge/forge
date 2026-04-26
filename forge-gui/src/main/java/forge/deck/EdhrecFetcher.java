package forge.deck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Fetches decks from EDHREC (edhrec.com) deck preview pages.
 */
public class EdhrecFetcher extends DeckSiteFetcher {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?edhrec\\.com/deckpreview/([\\w-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getUrlPattern() { return URL_PATTERN; }

    @Override
    public String getSiteName() { return "EDHREC"; }

    @Override
    public FetchResult fetchDeck(String deckId) throws IOException {
        String pageUrl = "https://edhrec.com/deckpreview/" + deckId;
        String html = httpGet(pageUrl);
        if (html == null) {
            return FetchResult.error("Could not fetch deck from EDHREC. Make sure the link is valid.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        String deckName = extractDeckName(html);
        appendDeckName(sb, deckName);

        List<String> commanders = extractSimpleArray(html, "commanders");
        if (!commanders.isEmpty()) {
            sb.append("Commander\n");
            for (String card : commanders) {
                sb.append("1 ").append(card).append("\n");
            }
            totalCards += commanders.size();
            sb.append("\n");
        }

        Set<String> cmdNames = new HashSet<>(commanders);
        List<String> cards = extractCardlists(html);
        if (!cards.isEmpty()) {
            sb.append("Main\n");
            for (String card : cards) {
                if (cmdNames.contains(card)) continue;
                sb.append("1 ").append(card).append("\n");
                totalCards++;
            }
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from EDHREC. The link may be invalid or the deck may be empty.");
        }

        return FetchResult.ok(sb.toString().trim(), getSiteName(), totalCards);
    }

    private static String extractDeckName(String html) {
        String header = extractStringValue(html, 0, "\"header\"");
        if (header != null && !header.isEmpty() && !header.contains("<")) {
            return header;
        }
        int titleStart = html.indexOf("<title>");
        if (titleStart >= 0) {
            int titleEnd = html.indexOf("</title>", titleStart);
            if (titleEnd > titleStart) {
                String title = html.substring(titleStart + 7, titleEnd).trim();
                int dashIdx = title.lastIndexOf(" - ");
                if (dashIdx > 0) title = title.substring(0, dashIdx).trim();
                if (!title.isEmpty()) return title;
            }
        }
        return null;
    }

    private static List<String> extractSimpleArray(String html, String key) {
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

    private static List<String> extractCardlists(String html) {
        List<String> cards = new ArrayList<>();
        int searchFrom = 0;
        Set<String> seen = new LinkedHashSet<>();

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

            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]{2,50})\"");
            Matcher matcher = namePattern.matcher(cardviewsArray);
            while (matcher.find()) {
                String name = matcher.group(1);
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
}
