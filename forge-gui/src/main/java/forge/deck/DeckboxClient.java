package forge.deck;

import forge.util.Localizer;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only Deckbox.org access for public user deck listings (bulk sync).
 * Single-set URLs are handled by {@link DeckboxDeckUrlProvider}.
 */
final class DeckboxClient {
    private static final Localizer localizer = Localizer.getInstance();
    private static final Pattern FOLDER_NAME = Pattern.compile(
            "(?is)<img[^>]*sprite s_folder[^>]*>\\s*<div class=\"name[^\"]*\">([^<]+)</div>");
    private static final Pattern FOLDER_UL = Pattern.compile("(?is)<ul[^>]*class=\"[^\"]*\\bfolder\\b[^\"]*\"[^>]*>");
    private static final Pattern DECK_ENTRY = Pattern.compile(
            "(?is)<li[^>]*data-id=\"(\\d+)\"[^>]*class=\"[^\"]*\\bdeck\\b[^\"]*\"[^>]*>.*?<a href=\"/sets/\\1\"[^>]*>([^<]+)</a>");

    record RemoteDeckRef(String setId, String name) {
    }

    private DeckboxClient() {
    }

    static Map<String, List<RemoteDeckRef>> listFolders(final String username) throws IOException {
        final String profileUrl = "https://deckbox.org/users/" + encodePathSegment(username);
        final String html = DeckUrlLoader.readText(profileUrl, DeckboxDeckUrlProvider.PROVIDER_NAME);
        final Map<String, List<RemoteDeckRef>> folders = parseProfileDecks(html, username);
        boolean anyDecks = false;
        for (final List<RemoteDeckRef> decks : folders.values()) {
            if (decks != null && !decks.isEmpty()) {
                anyDecks = true;
                break;
            }
        }
        if (!anyDecks) {
            throw new IOException(localizer.getMessage("lblDeckboxNoPublicFolders", username));
        }
        return folders;
    }

    /**
     * Parse a Deckbox profile HTML into folder name → decks. Decks that are not
     * inside a named folder are placed in {@code {username}-root}.
     */
    static Map<String, List<RemoteDeckRef>> parseProfileDecks(final String html, final String username) {
        final Map<String, List<RemoteDeckRef>> folders = new LinkedHashMap<>();
        final String rootName = DeckboxUtil.getRootFolderName(username);
        final List<RemoteDeckRef> rootDecks = new ArrayList<>();
        folders.put(rootName, rootDecks);

        final List<int[]> folderUlRanges = new ArrayList<>();
        final Matcher folderNameMatcher = FOLDER_NAME.matcher(html);
        final List<int[]> folderNameSpans = new ArrayList<>();
        final List<String> folderNames = new ArrayList<>();
        while (folderNameMatcher.find()) {
            final String folderName = StringEscapeUtils.unescapeHtml4(folderNameMatcher.group(1)).trim();
            if (folderName.isEmpty()) {
                continue;
            }
            folderNames.add(folderName);
            folderNameSpans.add(new int[] { folderNameMatcher.start(), folderNameMatcher.end() });
        }
        for (int i = 0; i < folderNames.size(); i++) {
            final String folderName = folderNames.get(i);
            final int searchFrom = folderNameSpans.get(i)[1];
            final int searchTo = (i + 1 < folderNameSpans.size()) ? folderNameSpans.get(i + 1)[0] : html.length();
            final Matcher ulMatcher = FOLDER_UL.matcher(html);
            if (!ulMatcher.find(searchFrom) || ulMatcher.start() >= searchTo) {
                folders.putIfAbsent(folderName, new ArrayList<>());
                continue;
            }
            final int contentStart = ulMatcher.end();
            final int contentEnd = html.indexOf("</ul>", contentStart);
            if (contentEnd < 0 || contentEnd > searchTo) {
                folders.putIfAbsent(folderName, new ArrayList<>());
                continue;
            }
            folderUlRanges.add(new int[] { ulMatcher.start(), contentEnd + 5 });
            folders.put(folderName, parseDeckEntries(html.substring(contentStart, contentEnd)));
        }

        final Matcher deckMatcher = DECK_ENTRY.matcher(html);
        while (deckMatcher.find()) {
            if (isInsideRange(deckMatcher.start(), folderUlRanges)) {
                continue;
            }
            final RemoteDeckRef deck = toRemoteDeck(deckMatcher);
            if (deck != null) {
                rootDecks.add(deck);
            }
        }
        return folders;
    }

    private static List<RemoteDeckRef> parseDeckEntries(final String html) {
        final List<RemoteDeckRef> decks = new ArrayList<>();
        final Matcher deckMatcher = DECK_ENTRY.matcher(html);
        while (deckMatcher.find()) {
            final RemoteDeckRef deck = toRemoteDeck(deckMatcher);
            if (deck != null) {
                decks.add(deck);
            }
        }
        return decks;
    }

    private static RemoteDeckRef toRemoteDeck(final Matcher deckMatcher) {
        final String setId = deckMatcher.group(1);
        final String deckName = StringEscapeUtils.unescapeHtml4(deckMatcher.group(2)).trim();
        if (setId.isBlank() || deckName.isBlank()) {
            return null;
        }
        return new RemoteDeckRef(setId, deckName);
    }

    private static boolean isInsideRange(final int pos, final List<int[]> ranges) {
        for (final int[] range : ranges) {
            if (pos >= range[0] && pos < range[1]) {
                return true;
            }
        }
        return false;
    }

    static DeckUrlProvider.RemoteDeck loadDeck(final RemoteDeckRef ref, final Iterable<Deck> savedDecks) throws IOException {
        final String sourceUrl = "https://deckbox.org/sets/" + ref.setId();
        return DeckboxDeckUrlProvider.loadSet(ref.setId(), ref.name(), sourceUrl, savedDecks);
    }

    private static String encodePathSegment(final String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final StringBuilder out = new StringBuilder(bytes.length);
        for (final byte b : bytes) {
            final int ub = b & 0xFF;
            if ((ub >= 'a' && ub <= 'z') || (ub >= 'A' && ub <= 'Z') || (ub >= '0' && ub <= '9')
                    || ub == '-' || ub == '_' || ub == '.') {
                out.append((char) ub);
            } else {
                out.append(String.format("%%%02X", ub));
            }
        }
        return out.toString();
    }
}
