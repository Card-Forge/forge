package forge.deck;

import forge.StaticData;
import forge.card.CardDb;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.item.PaperCard;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeckUrlLoader {
    private static final Pattern MOXFIELD_DECK_URL = Pattern.compile("(?i)(?:^|/)decks/([^/?#]+)");
    private static final Pattern ARCHIDEKT_DECK_URL = Pattern.compile("(?i)(?:^|/)decks/(\\d+)(?:[/?#]|$)");
    private static final String MOXFIELD_API_BASE = "https://api.moxfield.com/v2/decks/all/";
    private static final String ARCHIDEKT_API_BASE = "https://archidekt.com/api/decks/";
    private static final String URL_DECK_DIR_NAME = "URL";
    private static final Localizer localizer = Localizer.getInstance();

    public static DeckProxy load(final String deckUrl) throws IOException {
        final String normalizedUrl = normalizeUrl(deckUrl);
        final String host = getHost(normalizedUrl);
        final StorageImmediatelySerialized<Deck> storage = getStorage();
        final Deck deck;
        if (host.endsWith("moxfield.com")) {
            deck = loadMoxfieldDeck(normalizedUrl, storage);
        } else if (host.endsWith("archidekt.com")) {
            deck = loadArchidektDeck(normalizedUrl, storage);
        } else {
            throw new IOException(localizer.getMessage("lblOnlySupportedDeckUrls"));
        }

        storage.add(deck);
        return new DeckProxy(deck, localizer.getMessage("lblUrlDeck"), GameType.Constructed, storage);
    }

    private static Deck loadMoxfieldDeck(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckId = getMoxfieldDeckId(normalizedUrl);
        final Map<?, ?> root = readJsonObject(MOXFIELD_API_BASE + deckId, "Moxfield", "lblMoxfieldUnexpectedResponse");

        final Deck deck = new Deck(getDeckName(root, deckId, normalizedUrl, localizer.getMessage("lblMoxfieldDeck"), savedDecks));
        deck.setSourceUrl(normalizedUrl);
        deck.setDeckFormat(getDeckFormat(root.get("format")));
        addMoxfieldSection(deck, root.get("commanders"), DeckSection.Commander);
        addMoxfieldSection(deck, root.get("mainboard"), DeckSection.Main);
        addMoxfieldSection(deck, root.get("sideboard"), DeckSection.Sideboard);
        addMoxfieldSection(deck, root.get("companions"), DeckSection.Sideboard);

        requirePlayableCards(deck, "lblNoPlayableCardsInMoxfieldDeck");
        return deck;
    }

    private static Deck loadArchidektDeck(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckId = getArchidektDeckId(normalizedUrl);
        final Map<?, ?> root = readJsonObject(ARCHIDEKT_API_BASE + deckId + "/", "Archidekt", "lblArchidektUnexpectedResponse");

        final Deck deck = new Deck(getDeckName(root, deckId, normalizedUrl, localizer.getMessage("lblArchidektDeck"), savedDecks));
        deck.setSourceUrl(normalizedUrl);
        deck.setDeckFormat(getArchidektDeckFormat(root.get("deckFormat")));
        addArchidektCards(deck, root);

        requirePlayableCards(deck, "lblNoPlayableCardsInArchidektDeck");
        return deck;
    }

    public static List<DeckProxy> getUrlDecks() {
        final List<DeckProxy> decks = new ArrayList<>();
        final StorageImmediatelySerialized<Deck> storage = getStorage();
        for (final Deck deck : storage) {
            decks.add(new DeckProxy(deck, localizer.getMessage("lblUrlDeck"), GameType.Constructed, storage));
        }
        return decks;
    }

    private static void addMoxfieldSection(final Deck deck, final Object sectionValue, final DeckSection section) throws IOException {
        if (!(sectionValue instanceof Map<?, ?> cards)) {
            return;
        }
        final CardPool pool = deck.getOrCreate(section);
        for (final Map.Entry<?, ?> entry : cards.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> cardEntry)) {
                continue;
            }

            final int quantity = getInt(cardEntry.get("quantity"), 1);
            String cardName = getNestedString(cardEntry, "card", "name");
            if (cardName == null) {
                cardName = String.valueOf(entry.getKey());
            }
            if (!cardName.isBlank() && quantity > 0) {
                final String setCode = getNestedString(cardEntry, "card", "set");
                final String collectorNumber = getNestedString(cardEntry, "card", "cn");
                pool.add(getRequiredCard(cardName, setCode, collectorNumber, "lblMoxfieldCardNotFound"), quantity);
            }
        }
    }

    private static void addArchidektCards(final Deck deck, final Map<?, ?> root) throws IOException {
        if (!(root.get("cards") instanceof List<?> cards)) {
            throw new IOException(localizer.getMessage("lblArchidektUnexpectedResponse"));
        }

        final Set<String> excludedCategories = getArchidektExcludedCategories(root);
        for (final Object cardValue : cards) {
            if (!(cardValue instanceof Map<?, ?> cardEntry) || cardEntry.get("deletedAt") != null) {
                continue;
            }

            final int quantity = getInt(cardEntry.get("quantity"), 1);
            if (quantity <= 0 || isArchidektExcludedCard(cardEntry, excludedCategories)) {
                continue;
            }

            final String cardName = getArchidektCardName(cardEntry);
            if (cardName == null) {
                continue;
            }

            final String setCode = getNestedString(cardEntry, "card", "edition", "editioncode");
            final String collectorNumber = getNestedString(cardEntry, "card", "collectorNumber");
            deck.getOrCreate(getArchidektSection(cardEntry)).add(
                    getRequiredCard(cardName, setCode, collectorNumber, "lblArchidektCardNotFound"), quantity);
        }
    }

    private static void requirePlayableCards(final Deck deck, final String messageKey) throws IOException {
        if (deck.getMain().isEmpty() && !deck.has(DeckSection.Commander)) {
            throw new IOException(localizer.getMessage(messageKey));
        }
    }

    private static PaperCard getRequiredCard(final String cardName, final String setCode, final String collectorNumber,
            final String messageKey) throws IOException {
        final PaperCard card = findCard(cardName, setCode, collectorNumber);
        if (card == null) {
            throw new IOException(localizer.getMessage(messageKey, cardName));
        }
        return card;
    }

    private static Set<String> getArchidektExcludedCategories(final Map<?, ?> root) {
        final Set<String> excludedCategories = new HashSet<>();
        if (!(root.get("categories") instanceof List<?> categories)) {
            return excludedCategories;
        }
        for (final Object categoryValue : categories) {
            if (categoryValue instanceof Map<?, ?> category && Boolean.FALSE.equals(category.get("includedInDeck"))) {
                final String name = getString(category.get("name"), null);
                if (name != null && !"Sideboard".equalsIgnoreCase(name)) {
                    excludedCategories.add(name);
                }
            }
        }
        return excludedCategories;
    }

    private static boolean isArchidektExcludedCard(final Map<?, ?> cardEntry, final Set<String> excludedCategories) {
        if (!(cardEntry.get("categories") instanceof List<?> categories)) {
            return false;
        }
        for (final Object categoryValue : categories) {
            if (categoryValue instanceof String category && excludedCategories.contains(category)) {
                return true;
            }
        }
        return false;
    }

    private static DeckSection getArchidektSection(final Map<?, ?> cardEntry) {
        if (Boolean.TRUE.equals(cardEntry.get("companion"))) {
            return DeckSection.Sideboard;
        }
        if (cardEntry.get("categories") instanceof List<?> categories) {
            for (final Object categoryValue : categories) {
                if (!(categoryValue instanceof String category)) {
                    continue;
                }
                if ("Commander".equalsIgnoreCase(category)) {
                    return DeckSection.Commander;
                }
                if ("Sideboard".equalsIgnoreCase(category)) {
                    return DeckSection.Sideboard;
                }
            }
        }
        return DeckSection.Main;
    }

    private static String getArchidektCardName(final Map<?, ?> cardEntry) {
        String cardName = getNestedString(cardEntry, "card", "displayName");
        if (cardName == null) {
            cardName = getNestedString(cardEntry, "card", "oracleCard", "name");
        }
        return cardName;
    }

    private static String getDeckName(final Map<?, ?> root, final String deckId, final String sourceUrl, final String defaultName,
            final Iterable<Deck> savedDecks) throws IOException {
        final String requestedName = getString(root.get("name"), defaultName);
        for (final Deck deck : savedDecks) {
            if (isSameSourceDeck(sourceUrl, deck.getSourceUrl())) {
                return deck.getName();
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

    private static DeckFormat getDeckFormat(final Object formatValue) {
        try {
            return DeckFormat.smartValueOf(getString(formatValue, null), DeckFormat.Constructed);
        } catch (final IllegalArgumentException ex) {
            return DeckFormat.Constructed;
        }
    }

    private static DeckFormat getArchidektDeckFormat(final Object formatValue) {
        if (!(formatValue instanceof Number number)) {
            return DeckFormat.Constructed;
        }
        return switch (number.intValue()) {
            case 3, 11, 12 -> DeckFormat.Commander;
            case 6 -> DeckFormat.Pauper;
            case 13 -> DeckFormat.Brawl;
            default -> DeckFormat.Constructed;
        };
    }

    private static StorageImmediatelySerialized<Deck> getStorage() {
        return new StorageImmediatelySerialized<>("URL decks",
                new DeckStorage(new File(ForgeConstants.DECK_BASE_DIR + URL_DECK_DIR_NAME + ForgeConstants.PATH_SEPARATOR),
                        ForgeConstants.DECK_BASE_DIR));
    }

    private static PaperCard findCard(final String cardName, final String setCode, final String collectorNumber) {
        PaperCard card = findCardInDatabases(cardName, setCode, collectorNumber);
        if (card != null) {
            return card;
        }

        StaticData.instance().attemptToLoadCard(cardName, setCode);
        card = findCardInDatabases(cardName, setCode, collectorNumber);
        if (card != null) {
            return card;
        }

        final String frontFaceName = getFrontFaceName(cardName);
        if (frontFaceName.equals(cardName)) {
            return null;
        }

        card = findCardInDatabases(frontFaceName, setCode, collectorNumber);
        if (card != null) {
            return card;
        }

        StaticData.instance().attemptToLoadCard(frontFaceName, setCode);
        return findCardInDatabases(frontFaceName, setCode, collectorNumber);
    }

    private static PaperCard findCardInDatabases(final String cardName, final String setCode, final String collectorNumber) {
        final String normalizedSetCode = setCode == null ? null : setCode.toUpperCase(Locale.ROOT);
        for (final CardDb db : StaticData.instance().getAvailableDatabases().values()) {
            PaperCard card = collectorNumber == null ? null : db.getCard(cardName, normalizedSetCode, collectorNumber);
            if (card == null) {
                card = db.getCard(cardName, normalizedSetCode);
            }
            if (card == null) {
                card = db.getCard(cardName);
            }
            if (card != null) {
                return card;
            }
        }
        return null;
    }

    private static String getFrontFaceName(final String cardName) {
        final int splitIndex = cardName.indexOf(" // ");
        return splitIndex < 0 ? cardName : cardName.substring(0, splitIndex);
    }

    private static String getMoxfieldDeckId(final String deckUrl) throws IOException {
        final Matcher matcher = MOXFIELD_DECK_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        final int lastSlash = deckUrl.lastIndexOf('/');
        final String id = lastSlash >= 0 ? deckUrl.substring(lastSlash + 1) : deckUrl;
        final int query = id.indexOf('?');
        final String cleanId = query >= 0 ? id.substring(0, query) : id;
        if (cleanId.isBlank()) {
            throw new IOException(localizer.getMessage("lblCouldNotFindMoxfieldDeckId"));
        }
        return cleanId;
    }

    private static String getArchidektDeckId(final String deckUrl) throws IOException {
        final Matcher matcher = ARCHIDEKT_DECK_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IOException(localizer.getMessage("lblCouldNotFindArchidektDeckId"));
    }

    private static String getSourceDeckKey(final String deckUrl) throws IOException {
        if (deckUrl == null || deckUrl.isBlank()) {
            return null;
        }
        final String normalizedUrl = normalizeUrl(deckUrl);
        final String host = getHost(normalizedUrl);
        if (host.endsWith("moxfield.com")) {
            return "moxfield:" + getMoxfieldDeckId(normalizedUrl);
        }
        if (host.endsWith("archidekt.com")) {
            return "archidekt:" + getArchidektDeckId(normalizedUrl);
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

    private static String readUrl(final String requestUrl, final String providerName) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
        conn.setRequestProperty("Accept", "application/json");
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
                out.append(line);
            }
        }
        return out.toString();
    }

    private static Map<?, ?> readJsonObject(final String requestUrl, final String providerName, final String unexpectedResponseKey) throws IOException {
        final Object parsed = new JsonParser(readUrl(requestUrl, providerName)).parse();
        if (parsed instanceof Map<?, ?> root) {
            return root;
        }
        throw new IOException(localizer.getMessage(unexpectedResponseKey));
    }

    private static String getNestedString(final Map<?, ?> map, final String... keys) {
        Object value = map;
        for (final String key : keys) {
            if (!(value instanceof Map<?, ?> current)) {
                return null;
            }
            value = current.get(key);
        }
        return getString(value, null);
    }

    private static String getString(final Object value, final String defaultValue) {
        return value instanceof String str && !str.isBlank() ? str : defaultValue;
    }

    private static int getInt(final Object value, final int defaultValue) {
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

    private static final class JsonParser {
        private final String input;
        private int pos;

        private JsonParser(final String input) {
            this.input = input;
        }

        private Object parse() throws IOException {
            final Object value = parseValue();
            skipWhitespace();
            if (pos != input.length()) {
                throw error("Unexpected trailing JSON content");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (pos >= input.length()) {
                throw error("Unexpected end of JSON");
            }
            return switch (input.charAt(pos)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            final Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            do {
                skipWhitespace();
                final String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return map;
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            final List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            do {
                list.add(parseValue());
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return list;
        }

        private String parseString() throws IOException {
            expect('"');
            final StringBuilder out = new StringBuilder();
            while (pos < input.length()) {
                final char c = input.charAt(pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (pos >= input.length()) {
                    throw error("Unterminated escape sequence");
                }
                final char escaped = input.charAt(pos++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> out.append(parseUnicodeEscape());
                    default -> throw error("Invalid escape sequence");
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() throws IOException {
            if (pos + 4 > input.length()) {
                throw error("Invalid unicode escape");
            }
            try {
                final char value = (char) Integer.parseInt(input.substring(pos, pos + 4), 16);
                pos += 4;
                return value;
            } catch (final NumberFormatException ex) {
                throw error("Invalid unicode escape");
            }
        }

        private Object parseNumber() throws IOException {
            final int start = pos;
            if (peek('-')) {
                pos++;
            }
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            if (peek('.')) {
                pos++;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
            }
            if (peek('e') || peek('E')) {
                pos++;
                if (peek('+') || peek('-')) {
                    pos++;
                }
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
            }
            if (start == pos) {
                throw error("Expected JSON value");
            }
            final String number = input.substring(start, pos);
            try {
                return number.contains(".") || number.contains("e") || number.contains("E")
                        ? Double.parseDouble(number)
                        : Long.parseLong(number);
            } catch (final NumberFormatException ex) {
                throw error("Invalid number");
            }
        }

        private Object parseLiteral(final String literal, final Object value) throws IOException {
            if (!input.startsWith(literal, pos)) {
                throw error("Invalid JSON literal");
            }
            pos += literal.length();
            return value;
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private void expect(final char expected) throws IOException {
            if (!peek(expected)) {
                throw error("Expected '" + expected + "'");
            }
            pos++;
        }

        private boolean consume(final char expected) {
            if (peek(expected)) {
                pos++;
                return true;
            }
            return false;
        }

        private boolean peek(final char expected) {
            return pos < input.length() && input.charAt(pos) == expected;
        }

        private IOException error(final String message) {
            return new IOException(message + " at position " + pos + ".");
        }
    }

    private DeckUrlLoader() {
    }
}
