package forge.deck;

import forge.deck.DeckRecognizer.Token;
import forge.deck.DeckRecognizer.TokenType;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Localizer;
import forge.util.storage.StorageImmediatelySerialized;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DeckUrlLoader {
    private static final Pattern PRINTING_HINT = Pattern.compile(
            "^(\\s*\\d+\\s+.+?)\\s+\\[[A-Z0-9_]{2,7}\\](?:\\s+\\*?[0-9A-Z]+(?:\\S[0-9A-Z]*)?)?\\s*$");
    private static final String URL_DECK_DIR_NAME = "URL";
    private static final String SUPPORTED_PROVIDERS = "Moxfield, Archidekt, TappedOut, MTGGoldfish";
    private static final Localizer localizer = Localizer.getInstance();

    public static DeckProxy load(final String deckUrl) throws IOException {
        final String normalizedUrl = normalizeUrl(deckUrl);
        final DeckUrlProvider provider = getProvider(normalizedUrl);
        final StorageImmediatelySerialized<Deck> storage = getStorage();
        final DeckUrlProvider.RemoteDeck remoteDeck = provider.load(normalizedUrl, storage);
        final Deck deck = importDeck(remoteDeck);

        storage.add(deck);
        return new DeckProxy(deck, localizer.getMessage("lblUrlDeck"), GameType.Constructed, storage);
    }

    public static List<DeckProxy> getUrlDecks() {
        final List<DeckProxy> decks = new ArrayList<>();
        final StorageImmediatelySerialized<Deck> storage = getStorage();
        for (final Deck deck : storage) {
            decks.add(new DeckProxy(deck, localizer.getMessage("lblUrlDeck"), GameType.Constructed, storage));
        }
        return decks;
    }

    private static DeckUrlProvider getProvider(final String normalizedUrl) throws IOException {
        final String host = getHost(normalizedUrl);
        if (host.endsWith("moxfield.com")) {
            return new MoxfieldDeckUrlProvider();
        }
        if (host.endsWith("archidekt.com")) {
            return new ArchidektDeckUrlProvider();
        }
        if (host.endsWith("tappedout.net")) {
            return new TappedOutDeckUrlProvider();
        }
        if (host.endsWith("mtggoldfish.com")) {
            return new MtgGoldfishDeckUrlProvider();
        }
        throw new IOException(localizer.getMessage("lblOnlySupportedDeckUrls", SUPPORTED_PROVIDERS));
    }

    private static Deck importDeck(final DeckUrlProvider.RemoteDeck remoteDeck) throws IOException {
        final DeckRecognizer recognizer = new DeckRecognizer();
        recognizer.forceImportBannedAndRestrictedCards();
        final List<Token> tokens = recognizer.parseCardList(getRecognizableImportLines(recognizer, remoteDeck.importText()));
        final Deck deck = new Deck(remoteDeck.name());
        for (final Token token : tokens) {
            final TokenType type = token.getType();
            if (type == TokenType.UNKNOWN_CARD || type == TokenType.UNSUPPORTED_CARD) {
                throw new IOException(localizer.getMessage("lblDeckUrlCardNotFound", remoteDeck.providerName(), token.getText()));
            }
            if (!token.isTokenForDeck() || type == TokenType.DECK_NAME) {
                continue;
            }
            deck.getOrCreate(token.getTokenSection()).add(token.getCard(), token.getQuantity());
        }

        deck.setDeckFormat(remoteDeck.format());
        deck.setSourceUrl(remoteDeck.sourceUrl());
        requirePlayableCards(deck, remoteDeck.providerName());
        return deck;
    }

    private static String[] getRecognizableImportLines(final DeckRecognizer recognizer, final String importText) {
        final String[] lines = importText.split("\n");
        DeckSection section = null;
        for (int i = 0; i < lines.length; i++) {
            final Token token = recognizer.recognizeLine(lines[i], section);
            if (token == null) {
                continue;
            }
            if (token.getType() == TokenType.DECK_SECTION_NAME) {
                section = DeckSection.valueOf(token.getText());
                continue;
            }
            if (token.getType() == TokenType.UNKNOWN_CARD) {
                final String fallbackLine = PRINTING_HINT.matcher(lines[i]).replaceFirst("$1");
                if (!fallbackLine.equals(lines[i]) && isCardToken(recognizer.recognizeLine(fallbackLine, section))) {
                    lines[i] = fallbackLine;
                }
            }
        }
        return lines;
    }

    private static boolean isCardToken(final Token token) {
        return token != null && token.isCardToken();
    }

    private static void requirePlayableCards(final Deck deck, final String providerName) throws IOException {
        if (deck.getMain().isEmpty() && !deck.has(DeckSection.Commander)) {
            throw new IOException(localizer.getMessage("lblNoPlayableCardsInDeckUrl", providerName));
        }
    }

    static String getDeckName(final Map<?, ?> root, final String deckId, final String sourceUrl, final String defaultName,
            final Iterable<Deck> savedDecks) throws IOException {
        return getDeckName(getString(root.get("name"), defaultName), deckId, sourceUrl, savedDecks);
    }

    static String getDeckName(final String requestedName, final String deckId, final String sourceUrl,
            final Iterable<Deck> savedDecks) throws IOException {
        for (final Deck deck : savedDecks) {
            if (isSameSourceDeck(sourceUrl, deck.getSourceUrl())) {
                continue;
            }
            if (requestedName.equals(deck.getName())) {
                return requestedName + " " + deckId;
            }
        }
        return requestedName;
    }

    private static boolean isSameSourceDeck(final String sourceUrl, final String savedSourceUrl) throws IOException {
        try {
            return sourceUrl.equals(savedSourceUrl) || Objects.equals(getSourceDeckKey(sourceUrl), getSourceDeckKey(savedSourceUrl));
        } catch (final IOException ignored) {
            return false;
        }
    }

    static DeckFormat getDeckFormat(final Object formatValue) {
        try {
            return DeckFormat.smartValueOf(getString(formatValue, null), DeckFormat.Constructed);
        } catch (final IllegalArgumentException ex) {
            return DeckFormat.Constructed;
        }
    }

    private static StorageImmediatelySerialized<Deck> getStorage() {
        return new StorageImmediatelySerialized<>("URL decks",
                new DeckStorage(new File(ForgeConstants.DECK_BASE_DIR + URL_DECK_DIR_NAME + ForgeConstants.PATH_SEPARATOR),
                        ForgeConstants.DECK_BASE_DIR));
    }

    private static String getSourceDeckKey(final String deckUrl) throws IOException {
        if (deckUrl == null || deckUrl.isBlank()) {
            return null;
        }
        final String normalizedUrl = normalizeUrl(deckUrl);
        final String host = getHost(normalizedUrl);
        if (host.endsWith("moxfield.com")) {
            return "moxfield:" + MoxfieldDeckUrlProvider.getDeckId(normalizedUrl);
        }
        if (host.endsWith("archidekt.com")) {
            return "archidekt:" + ArchidektDeckUrlProvider.getDeckId(normalizedUrl);
        }
        if (host.endsWith("tappedout.net")) {
            return "tappedout:" + TappedOutDeckUrlProvider.getDeckSlug(normalizedUrl);
        }
        if (host.endsWith("mtggoldfish.com")) {
            return "mtggoldfish:" + MtgGoldfishDeckUrlProvider.getDeckId(normalizedUrl);
        }
        return null;
    }

    private static String normalizeUrl(final String deckUrl) {
        final String trimmed = deckUrl == null ? "" : deckUrl.trim();
        if (trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private static String getHost(final String deckUrl) throws IOException {
        try {
            final String host = new URI(deckUrl).getHost();
            if (host == null) {
                throw new IOException(localizer.getMessage("lblInvalidDeckUrl"));
            }
            return host.toLowerCase();
        } catch (final URISyntaxException ex) {
            throw new IOException(localizer.getMessage("lblInvalidDeckUrl"), ex);
        }
    }

    static String readText(final String requestUrl, final String providerName) throws IOException {
        return readUrl(requestUrl, providerName, "text/plain, text/html, */*");
    }

    private static String readUrl(final String requestUrl, final String providerName) throws IOException {
        return readUrl(requestUrl, providerName, "application/json");
    }

    private static String readUrl(final String requestUrl, final String providerName, final String accept) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", "Forge Deck URL Loader");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        final int status = conn.getResponseCode();
        try (InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
            final String body = stream == null ? "" : readAll(stream);
            if (status >= 400) {
                throw new IOException(localizer.getMessage("lblDeckUrlHttpRequestFailed", providerName, status));
            }
            return body;
        }
    }

    private static String readAll(final InputStream stream) throws IOException {
        final StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    static Map<?, ?> readJsonObject(final String requestUrl, final String providerName) throws IOException {
        final Object parsed = JsonUtil.parse(readUrl(requestUrl, providerName));
        if (parsed instanceof Map<?, ?> root) {
            return root;
        }
        throw new IOException(localizer.getMessage("lblDeckUrlUnexpectedResponse", providerName));
    }

    static String getNestedString(final Map<?, ?> map, final String... keys) {
        Object value = map;
        for (final String key : keys) {
            if (!(value instanceof Map<?, ?> current)) {
                return null;
            }
            value = current.get(key);
        }
        return getString(value, null);
    }

    static String getString(final Object value, final String defaultValue) {
        return value instanceof String str && !str.isBlank() ? str : defaultValue;
    }

    static int getInt(final Object value, final int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (final NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private DeckUrlLoader() {
    }
}
