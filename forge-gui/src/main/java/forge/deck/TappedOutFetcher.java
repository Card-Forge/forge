package forge.deck;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.deck.DeckUrlFetcher.FetchResult;

/**
 * Fetches decks from TappedOut (tappedout.net) via their text export.
 */
public class TappedOutFetcher extends DeckSiteFetcher {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?tappedout\\.net/mtg-decks/([\\w-]+)/?", Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getUrlPattern() { return URL_PATTERN; }

    @Override
    public String getSiteName() { return "TappedOut"; }

    @Override
    public FetchResult fetchDeck(String deckSlug) throws IOException {
        String textUrl = "https://tappedout.net/mtg-decks/" + deckSlug + "/?fmt=txt";
        String text = httpGet(textUrl);
        if (text == null || text.trim().isEmpty()) {
            return FetchResult.error("Could not fetch deck from TappedOut. Make sure the deck is public.");
        }

        // TappedOut's text export is a flat alphabetical list with no section headers.
        // Commander and sideboard sections cannot be detected; all cards are imported as main deck.
        List<String> mainCards = new ArrayList<>();
        int totalCards = 0;

        Pattern cardPattern = Pattern.compile("^(\\d+)x?\\s+(.+)$");
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            Matcher cm = cardPattern.matcher(line);
            if (cm.matches()) {
                int qty = Integer.parseInt(cm.group(1));
                String name = cm.group(2).trim();
                int hashIdx = name.indexOf('#');
                if (hashIdx > 0) name = name.substring(0, hashIdx).trim();
                int asterIdx = name.indexOf('*');
                if (asterIdx > 0) name = name.substring(0, asterIdx).trim();
                mainCards.add(qty + " " + name);
                totalCards++;
            }
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from TappedOut. The deck may be empty or private.");
        }

        StringBuilder sb = new StringBuilder();
        String deckName = deckSlug.replace("-", " ").replaceAll("\\s+$", "");
        appendDeckName(sb, deckName);
        for (String card : mainCards) {
            sb.append(card).append("\n");
        }

        return FetchResult.okWithNote(sb.toString().trim(), getSiteName(), totalCards,
                "Note: TappedOut's text export doesn't include commander or sideboard sections.");
    }
}
