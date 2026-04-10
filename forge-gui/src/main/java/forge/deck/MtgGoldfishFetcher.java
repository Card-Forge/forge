package forge.deck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Fetches decks from MTGGoldfish (mtggoldfish.com) via their download endpoint.
 */
public class MtgGoldfishFetcher extends DeckSiteFetcher {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?mtggoldfish\\.com/deck/(\\d+)", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getUrlPattern() { return URL_PATTERN; }

    @Override
    public String getSiteName() { return "MTGGoldfish"; }

    @Override
    public FetchResult fetchDeck(String deckId) throws IOException {
        String downloadUrl = "https://www.mtggoldfish.com/deck/download/" + deckId;
        String text = httpGet(downloadUrl);
        if (text == null || text.trim().isEmpty()) {
            return FetchResult.error("Could not fetch deck from MTGGoldfish. Make sure the deck exists.");
        }

        // MTGGoldfish's download format is a flat list: main deck, then an optional blank line
        // followed by the sideboard. Commander decks are imported without a commander designation.
        List<String> mainCards = new ArrayList<>();
        List<String> sideCards = new ArrayList<>();
        boolean hitBlankLine = false;
        boolean wroteMain = false;
        boolean isSideboard = false;
        int totalCards = 0;

        Pattern cardPattern = Pattern.compile("^(\\d+)\\s+(.+)$");
        for (String line : text.split("\n")) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                hitBlankLine = true;
                continue;
            }

            Matcher cm = cardPattern.matcher(trimmed);
            if (cm.matches()) {
                if (!isSideboard && hitBlankLine && wroteMain) {
                    isSideboard = true;
                }
                if (isSideboard) {
                    sideCards.add(trimmed);
                } else {
                    mainCards.add(trimmed);
                    wroteMain = true;
                }
                totalCards++;
            }
            hitBlankLine = false;
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from MTGGoldfish.");
        }

        StringBuilder sb = new StringBuilder();
        if (!mainCards.isEmpty()) {
            sb.append("Main\n");
            for (String card : mainCards) {
                sb.append(card).append("\n");
            }
            sb.append("\n");
        }
        if (!sideCards.isEmpty()) {
            sb.append("Sideboard\n");
            for (String card : sideCards) {
                sb.append(card).append("\n");
            }
        }

        return FetchResult.ok(sb.toString().trim(), getSiteName(), totalCards);
    }
}
