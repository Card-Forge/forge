package forge.deck;

import forge.util.Localizer;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a single public Deckbox.org set URL (for example {@code https://deckbox.org/sets/3869467}).
 */
final class DeckboxDeckUrlProvider implements DeckUrlProvider {
    static final String PROVIDER_NAME = "Deckbox";
    private static final Pattern SET_URL = Pattern.compile("(?i)(?:^|/)sets/(\\d+)(?:[/?#]|$)");
    private static final Pattern TITLE = Pattern.compile("(?is)<title>\\s*(.*?)\\s*-\\s*Deckbox\\s*</title>");
    private static final Localizer localizer = Localizer.getInstance();

    @Override
    public RemoteDeck load(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String setId = getSetId(normalizedUrl);
        final String sourceUrl = "https://deckbox.org/sets/" + setId;
        final String html = DeckUrlLoader.readText(sourceUrl, PROVIDER_NAME);
        final String deckName = getDeckNameFromHtml(html, setId);
        return loadSet(setId, deckName, sourceUrl, savedDecks);
    }

    static RemoteDeck loadSet(final String setId, final String deckName, final String sourceUrl,
            final Iterable<Deck> savedDecks) throws IOException {
        final String exportUrl = "https://deckbox.org/sets/export/" + setId + "?format=csv&f=&s=&o=&columns=";
        final String csv = DeckUrlLoader.readText(exportUrl, PROVIDER_NAME);
        final String importText = csvToImportText(csv);
        return new RemoteDeck(
                DeckUrlLoader.getDeckName(deckName, setId, sourceUrl, savedDecks),
                DeckFormat.Constructed,
                sourceUrl,
                importText,
                PROVIDER_NAME);
    }

    static String getSetId(final String deckUrl) throws IOException {
        final Matcher matcher = SET_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IOException(localizer.getMessage("lblCouldNotFindDeckUrlId", PROVIDER_NAME));
    }

    private static String getDeckNameFromHtml(final String html, final String setId) {
        final Matcher titleMatcher = TITLE.matcher(html);
        if (titleMatcher.find()) {
            final String title = StringEscapeUtils.unescapeHtml4(titleMatcher.group(1)).trim();
            if (!title.isBlank()) {
                return title;
            }
        }
        return "Deckbox " + setId;
    }

    static String csvToImportText(final String csv) throws IOException {
        final StringBuilder main = new StringBuilder();
        final StringBuilder sideboard = new StringBuilder();
        final StringBuilder commanders = new StringBuilder();
        boolean headerSkipped = false;
        for (final String rawLine : csv.split("\\R")) {
            if (rawLine.isBlank()) {
                continue;
            }
            final List<String> cols = parseCsvLine(rawLine);
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            if (cols.size() < 3) {
                continue;
            }
            final int count;
            try {
                count = Integer.parseInt(cols.get(0).trim());
            } catch (final NumberFormatException ex) {
                continue;
            }
            if (count <= 0) {
                continue;
            }
            final String cardName = cols.get(1).trim();
            if (cardName.isBlank()) {
                continue;
            }
            final String section = cols.get(2).trim().toLowerCase(Locale.ROOT);
            final StringBuilder target;
            switch (section) {
            case "main":
            case "deck":
                target = main;
                break;
            case "sideboard":
            case "side":
            case "sb":
                target = sideboard;
                break;
            case "commander":
            case "commanders":
                target = commanders;
                break;
            default:
                // scratchpad / maybeboard / tokens are not mapped into playable Forge sections
                continue;
            }
            target.append(count).append(' ').append(cardName).append('\n');
        }
        if (main.isEmpty() && sideboard.isEmpty() && commanders.isEmpty()) {
            throw new IOException(localizer.getMessage("lblNoPlayableCardsInDeckUrl", PROVIDER_NAME));
        }
        final StringBuilder out = new StringBuilder();
        DeckUrlProvider.appendSection(out, DeckSection.Commander, commanders);
        DeckUrlProvider.appendSection(out, DeckSection.Main, main);
        DeckUrlProvider.appendSection(out, DeckSection.Sideboard, sideboard);
        return out.toString();
    }

    private static List<String> parseCsvLine(final String line) {
        final List<String> cols = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            final char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                cols.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cols.add(current.toString());
        return cols;
    }
}
