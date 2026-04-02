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

        List<String> commanderCards = new ArrayList<>();
        List<String> mainCards = new ArrayList<>();
        List<String> sideCards = new ArrayList<>();
        String currentSection = "main";
        int totalCards = 0;

        Pattern cardPattern = Pattern.compile("^(\\d+)x?\\s+(.+)$");
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();
            if (lower.startsWith("commander")) {
                currentSection = "commander";
                continue;
            }
            if (lower.startsWith("sideboard")) {
                currentSection = "sideboard";
                continue;
            }

            Matcher cm = cardPattern.matcher(line);
            if (cm.matches()) {
                int qty = Integer.parseInt(cm.group(1));
                String name = cm.group(2).trim();
                int hashIdx = name.indexOf('#');
                if (hashIdx > 0) name = name.substring(0, hashIdx).trim();
                int asterIdx = name.indexOf('*');
                if (asterIdx > 0) name = name.substring(0, asterIdx).trim();
                String cardLine = qty + " " + name;
                switch (currentSection) {
                    case "commander": commanderCards.add(cardLine); break;
                    case "sideboard": sideCards.add(cardLine); break;
                    default: mainCards.add(cardLine); break;
                }
                totalCards++;
            }
        }

        if (totalCards == 0) {
            return FetchResult.error("Could not parse deck from TappedOut. The deck may be empty or private.");
        }

        StringBuilder sb = new StringBuilder();

        String deckName = deckSlug.replace("-", " ").replaceAll("\\s+$", "");
        appendDeckName(sb, deckName);

        if (!commanderCards.isEmpty()) {
            sb.append("Commander\n");
            for (String card : commanderCards) {
                sb.append(card).append("\n");
            }
            sb.append("\n");
        }
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
