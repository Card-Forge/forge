package forge.deck;

import forge.util.Localizer;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MtgGoldfishDeckUrlProvider implements DeckUrlProvider {
    private static final Pattern DECK_URL = Pattern.compile("(?i)(?:^|/)deck/(\\d+)(?:[/?#]|$)");
    private static final Pattern CARD_LINE = Pattern.compile("^(\\d+)\\s+(.+)$");
    private static final Pattern TITLE = Pattern.compile("(?is)<title>\\s*(.*?)\\s*(?:-\\s*Original Deck)?\\s*</title>");
    private static final Pattern FORMAT = inputValuePattern("deck_input_format");
    private static final Pattern COMMANDER = inputValuePattern("deck_input_commander");
    private static final Pattern COMMANDER_ALT = inputValuePattern("deck_input_commander_alt");
    private static final String PROVIDER_NAME = "MTGGoldfish";
    private static final Localizer localizer = Localizer.getInstance();

    @Override
    public RemoteDeck load(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckId = getDeckId(normalizedUrl);
        final String html = DeckUrlLoader.readText("https://www.mtggoldfish.com/deck/" + deckId, PROVIDER_NAME);
        final String text = DeckUrlLoader.readText("https://www.mtggoldfish.com/deck/download/" + deckId, PROVIDER_NAME);
        final String deckName = getDeckName(html);

        return new RemoteDeck(
                DeckUrlLoader.getDeckName(deckName, deckId, normalizedUrl, savedDecks),
                getDeckFormat(html),
                normalizedUrl,
                toSectionedImportText(text, getCommanders(html)),
                PROVIDER_NAME);
    }

    static String getDeckId(final String deckUrl) throws IOException {
        final Matcher matcher = DECK_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IOException(localizer.getMessage("lblCouldNotFindDeckUrlId", PROVIDER_NAME));
    }

    static String toSectionedImportText(final String text) {
        return toSectionedImportText(text, Collections.emptySet());
    }

    static String toSectionedImportText(final String text, final Set<String> commanders) {
        final StringBuilder commander = new StringBuilder();
        final StringBuilder main = new StringBuilder();
        final StringBuilder sideboard = new StringBuilder();
        boolean wroteMain = false;
        boolean afterBlankLine = false;
        boolean inSideboard = false;

        for (final String line : text.split("\\R")) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                afterBlankLine = true;
                continue;
            }
            final Matcher matcher = CARD_LINE.matcher(trimmed);
            if (!matcher.matches()) {
                afterBlankLine = false;
                continue;
            }
            if (!inSideboard && afterBlankLine && wroteMain) {
                inSideboard = true;
            }
            final String cardName = matcher.group(2).trim();
            if (!inSideboard && commanders.contains(cardName)) {
                commander.append(matcher.group(1)).append(' ').append(cardName).append('\n');
            } else {
                (inSideboard ? sideboard : main).append(trimmed).append('\n');
            }
            wroteMain = true;
            afterBlankLine = false;
        }

        final StringBuilder out = new StringBuilder();
        DeckUrlProvider.appendSection(out, DeckSection.Commander, commander);
        DeckUrlProvider.appendSection(out, DeckSection.Main, main);
        DeckUrlProvider.appendSection(out, DeckSection.Sideboard, sideboard);
        return out.toString();
    }

    static Set<String> getCommanders(final String html) {
        final Set<String> commanders = new LinkedHashSet<>();
        addInputValue(commanders, html, COMMANDER);
        addInputValue(commanders, html, COMMANDER_ALT);
        return commanders;
    }

    static String getDeckName(final String html) {
        final Matcher matcher = TITLE.matcher(html);
        if (!matcher.find()) {
            return localizer.getMessage("lblDeckUrlDefaultDeckName", PROVIDER_NAME);
        }
        final String title = StringEscapeUtils.unescapeHtml4(matcher.group(1)).trim();
        return title.isBlank() ? localizer.getMessage("lblDeckUrlDefaultDeckName", PROVIDER_NAME) : title;
    }

    private static DeckFormat getDeckFormat(final String html) {
        final String format = getInputValue(html, FORMAT);
        return "commander".equalsIgnoreCase(format)
                ? DeckFormat.Commander
                : DeckFormat.Constructed;
    }

    private static void addInputValue(final Set<String> values, final String html, final Pattern inputPattern) {
        final String value = getInputValue(html, inputPattern);
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }

    private static String getInputValue(final String html, final Pattern inputPattern) {
        final Matcher matcher = inputPattern.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return StringEscapeUtils.unescapeHtml4(matcher.group(1)).trim();
    }

    private static Pattern inputValuePattern(final String inputId) {
        return Pattern.compile("(?is)<input\\b[^>]*\\bid=\"" + Pattern.quote(inputId) + "\"[^>]*\\bvalue=\"([^\"]*)\"[^>]*>");
    }
}
