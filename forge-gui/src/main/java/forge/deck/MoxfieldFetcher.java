package forge.deck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Fetches decks from Moxfield (moxfield.com) via their API.
 */
public class MoxfieldFetcher extends DeckSiteFetcher {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?moxfield\\.com/decks/([\\w-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getUrlPattern() { return URL_PATTERN; }

    @Override
    public String getSiteName() { return "Moxfield"; }

    @Override
    public FetchResult fetchDeck(String deckId) throws IOException {
        String apiUrl = "https://api2.moxfield.com/v2/decks/all/" + deckId;
        String json = httpGet(apiUrl);
        if (json == null) {
            return FetchResult.error("Could not fetch deck from Moxfield. Make sure the deck is public.");
        }

        StringBuilder sb = new StringBuilder();
        int totalCards = 0;

        String deckName = extractStringValue(json, 0, "\"name\"");
        appendDeckName(sb, deckName);

        List<String> commanders = parseSection(json, "commanders");
        if (!commanders.isEmpty()) {
            sb.append("Commander\n");
            for (String line : commanders) {
                sb.append(line).append("\n");
            }
            totalCards += commanders.size();
            sb.append("\n");
        }

        List<String> companions = parseSection(json, "companions");
        if (!companions.isEmpty()) {
            sb.append("Commander\n");
            for (String line : companions) {
                sb.append(line).append("\n");
            }
            totalCards += companions.size();
            sb.append("\n");
        }

        Set<String> commandZoneNames = new HashSet<>();
        for (String line : commanders) {
            commandZoneNames.add(extractCardName(line));
        }
        for (String line : companions) {
            commandZoneNames.add(extractCardName(line));
        }

        List<String> mainboard = parseSection(json, "mainboard");
        if (!mainboard.isEmpty()) {
            sb.append("Main\n");
            for (String line : mainboard) {
                if (commandZoneNames.contains(extractCardName(line))) continue;
                sb.append(line).append("\n");
                totalCards++;
            }
            sb.append("\n");
        }

        List<String> sideboard = parseSection(json, "sideboard");
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

        return FetchResult.ok(sb.toString().trim(), getSiteName(), totalCards);
    }

    private static List<String> parseSection(String json, String sectionName) {
        List<String> cards = new ArrayList<>();
        String sectionKey = "\"" + sectionName + "\"";
        int sectionStart = json.indexOf(sectionKey);
        if (sectionStart < 0) return cards;

        int braceStart = json.indexOf('{', sectionStart + sectionKey.length());
        if (braceStart < 0) return cards;

        String sectionJson = extractJsonObject(json, braceStart);
        if (sectionJson == null || sectionJson.equals("{}")) return cards;

        int pos = 0;
        while (pos < sectionJson.length()) {
            int qtyIdx = sectionJson.indexOf("\"quantity\"", pos);
            if (qtyIdx < 0) break;

            int quantity = extractIntValue(sectionJson, qtyIdx);
            if (quantity <= 0) quantity = 1;

            int cardObjIdx = sectionJson.indexOf("\"card\"", qtyIdx);
            int nextQtyIdx = sectionJson.indexOf("\"quantity\"", qtyIdx + 10);
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
}
