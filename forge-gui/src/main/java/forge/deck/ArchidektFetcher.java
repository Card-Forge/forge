package forge.deck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Fetches decks from Archidekt (archidekt.com) via their API.
 */
public class ArchidektFetcher extends DeckSiteFetcher {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?archidekt\\.com/decks/(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getUrlPattern() { return URL_PATTERN; }

    @Override
    public String getSiteName() { return "Archidekt"; }

    @Override
    public FetchResult fetchDeck(String deckId) throws IOException {
        String apiUrl = "https://archidekt.com/api/decks/" + deckId + "/";
        String json = httpGet(apiUrl);

        if (json == null || json.contains("\"error\"")) {
            return FetchResult.error("Could not fetch deck from Archidekt. The deck may be private or use an unsupported URL format.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        String deckName = extractStringValue(json, 0, "\"name\"");
        appendDeckName(sb, deckName);

        Map<String, List<String>> sections = new LinkedHashMap<>();

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

            String cardName = null;
            int oracleIdx = cardObj.indexOf("\"oracleCard\"");
            if (oracleIdx >= 0) {
                cardName = extractStringValue(cardObj, oracleIdx, "\"name\"");
            }
            if (cardName == null || cardName.isEmpty()) {
                cardName = extractStringValue(cardObj, 0, "\"displayName\"");
            }
            if (cardName == null || cardName.isEmpty()) {
                int cardIdx = cardObj.indexOf("\"card\"");
                if (cardIdx >= 0) {
                    cardName = extractStringValue(cardObj, cardIdx, "\"name\"");
                }
            }

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
                                category = mapCategory(firstCat);
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
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            if (entry.getKey().equals("Maybeboard")) continue;
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

        return FetchResult.ok(sb.toString().trim(), getSiteName(), totalCards);
    }

    private static String mapCategory(String category) {
        String lower = category.toLowerCase();
        if (lower.contains("commander")) return "Commander";
        if (lower.contains("sideboard")) return "Sideboard";
        if (lower.contains("maybeboard") || lower.contains("considering")) return "Maybeboard";
        return "Main";
    }
}
