package forge.deck;

import forge.util.Localizer;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TappedOutDeckUrlProvider implements DeckUrlProvider {
    private static final Pattern DECK_URL = Pattern.compile("(?i)(?:^|/)mtg-decks/([^/?#]+)/?");
    private static final Pattern CARD_LINE = Pattern.compile("^(\\d+)x?\\s++(.+?)(?:\\s++\\(([A-Z0-9_]{2,7})\\)\\s++\\S+)?$");
    private static final Pattern TITLE = Pattern.compile("(?is)<title>\\s*+([^<]*?)\\s*+(?:\\([^<]*+MTG Deck\\))?+\\s*+</title>");
    private static final Pattern OG_TITLE = Pattern.compile("(?is)<meta\\s++property=\"og:title\"\\s++content=\"(?:MTG Deck:\\s*+)?+([^\"]*+)\"\\s*+/?>");
    private static final String MTGA_TEXTAREA_ID = "id=\"mtga-textarea\"";
    private static final String MTGA_TEXTAREA_CLOSE = "</textarea>";
    private static final String MTGA_TEXTAREA_OPEN = "<textarea";
    private static final String PROVIDER_NAME = "TappedOut";
    private static final Localizer localizer = Localizer.getInstance();

    @Override
    public RemoteDeck load(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckSlug = getDeckSlug(normalizedUrl);
        final String deckPage = "https://tappedout.net/mtg-decks/" + deckSlug + "/";
        final String html = DeckUrlLoader.readText(deckPage, PROVIDER_NAME);
        final String deckName = getDeckName(html, deckSlug);

        return new RemoteDeck(
                DeckUrlLoader.getDeckName(deckName, deckSlug, normalizedUrl, savedDecks),
                isCommanderPage(html) ? DeckFormat.Commander : DeckFormat.Constructed,
                normalizedUrl,
                toImportText(html),
                PROVIDER_NAME);
    }

    static String getDeckSlug(final String deckUrl) throws IOException {
        final Matcher matcher = DECK_URL.matcher(deckUrl);
        if (matcher.find() && !matcher.group(1).isBlank()) {
            return matcher.group(1);
        }
        throw new IOException(localizer.getMessage("lblCouldNotFindDeckUrlId", PROVIDER_NAME));
    }

    static String toImportText(final String html) throws IOException {
        final String deckText = extractMtgaTextarea(html);
        if (deckText == null) {
            throw new IOException(localizer.getMessage("lblDeckUrlUnexpectedResponse", PROVIDER_NAME));
        }

        final StringBuilder main = new StringBuilder();
        final StringBuilder commanders = new StringBuilder();
        final StringBuilder sideboard = new StringBuilder();
        StringBuilder currentSection = null;
        for (final String line : StringEscapeUtils.unescapeHtml4(deckText).split("\\R")) {
            final String trimmed = line.trim();
            if ("Commander".equalsIgnoreCase(trimmed)) {
                currentSection = commanders;
                continue;
            }
            if ("Deck".equalsIgnoreCase(trimmed)) {
                currentSection = main;
                continue;
            }
            if ("Sideboard".equalsIgnoreCase(trimmed)) {
                currentSection = sideboard;
                continue;
            }
            if (trimmed.isBlank() || trimmed.equalsIgnoreCase("About") || trimmed.startsWith("Name ")) {
                continue;
            }
            if (currentSection != null) {
                appendCardLine(currentSection, trimmed);
            }
        }
        if (commanders.isEmpty() && main.isEmpty() && sideboard.isEmpty()) {
            throw new IOException(localizer.getMessage("lblNoPlayableCardsInDeckUrl", PROVIDER_NAME));
        }
        final StringBuilder out = new StringBuilder();
        DeckUrlProvider.appendSection(out, DeckSection.Commander, commanders);
        DeckUrlProvider.appendSection(out, DeckSection.Main, main);
        DeckUrlProvider.appendSection(out, DeckSection.Sideboard, sideboard);
        return out.toString();
    }

    static String getDeckName(final String html, final String deckSlug) {
        final String title = getFirstMatch(OG_TITLE, html);
        if (title != null) {
            return title;
        }
        final String pageTitle = getFirstMatch(TITLE, html);
        if (pageTitle != null) {
            return pageTitle;
        }
        return deckSlug.replace('-', ' ').trim();
    }

    static String extractMtgaTextarea(final String html) {
        final int idIndex = html.indexOf(MTGA_TEXTAREA_ID);
        if (idIndex < 0) {
            return null;
        }
        final int open = html.lastIndexOf(MTGA_TEXTAREA_OPEN, idIndex);
        if (open < 0) {
            return null;
        }
        final int openEnd = html.indexOf('>', open);
        if (openEnd < 0 || openEnd <= idIndex) {
            return null;
        }
        final int close = html.indexOf(MTGA_TEXTAREA_CLOSE, openEnd);
        if (close < 0) {
            return null;
        }
        return html.substring(openEnd + 1, close);
    }

    private static void appendCardLine(final StringBuilder out, final String line) {
        final Matcher matcher = CARD_LINE.matcher(line.trim());
        if (!matcher.matches()) {
            return;
        }
        if ("SUNF".equalsIgnoreCase(matcher.group(3))) {
            return;
        }
        String cardName = matcher.group(2).trim();
        cardName = stripAfter(cardName, '#');
        cardName = stripAfter(cardName, '*');
        if (!cardName.isBlank()) {
            out.append(matcher.group(1)).append(' ').append(cardName).append('\n');
        }
    }

    private static String getFirstMatch(final Pattern pattern, final String html) {
        final Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        final String value = StringEscapeUtils.unescapeHtml4(matcher.group(1)).trim();
        return value.isBlank() ? null : value;
    }

    private static boolean isCommanderPage(final String html) {
        return html.toLowerCase(Locale.ROOT).contains("commander / edh");
    }

    private static String stripAfter(final String value, final char marker) {
        final int markerIndex = value.indexOf(marker);
        return markerIndex < 0 ? value : value.substring(0, markerIndex).trim();
    }
}
