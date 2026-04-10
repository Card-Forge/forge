package forge.deck;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Dispatches deck URL fetching to site-specific fetcher classes.
 * <p>
 * Supported sites: Moxfield, Archidekt, EDHREC, TappedOut, MTGGoldfish
 */
public class DeckUrlFetcher {

    /** Result of a URL fetch operation. */
    public static class FetchResult {
        private final boolean success;
        private final String deckText;
        private final String message;
        private final String siteName;

        private FetchResult(boolean success, String deckText, String message, String siteName) {
            this.success = success;
            this.deckText = deckText;
            this.message = message;
            this.siteName = siteName;
        }

        public static FetchResult ok(String deckText, String siteName, int cardCount) {
            return new FetchResult(true, deckText,
                    String.format("Deck loaded from %s (%d cards)", siteName, cardCount), siteName);
        }

        public static FetchResult okWithNote(String deckText, String siteName, int cardCount, String note) {
            return new FetchResult(true, deckText,
                    String.format("Deck loaded from %s (%d cards). %s", siteName, cardCount, note), siteName);
        }

        public static FetchResult error(String message) {
            return new FetchResult(false, null, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getDeckText() { return deckText; }
        public String getMessage() { return message; }
        public String getSiteName() { return siteName; }
    }

    private static final List<DeckSiteFetcher> FETCHERS = Arrays.asList(
            new MoxfieldFetcher(),
            new ArchidektFetcher(),
            new EdhrecFetcher(),
            new TappedOutFetcher(),
            new MtgGoldfishFetcher()
    );

    /**
     * Returns true if the given string looks like a supported deck URL.
     */
    public static boolean isSupportedUrl(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String url = text.trim();
        for (DeckSiteFetcher fetcher : FETCHERS) {
            if (fetcher.getUrlPattern().matcher(url).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches a deck from a URL by delegating to the appropriate site fetcher.
     */
    public static FetchResult fetch(String url) {
        if (url == null || url.trim().isEmpty()) {
            return FetchResult.error("Please enter a URL.");
        }
        url = url.trim();

        for (DeckSiteFetcher fetcher : FETCHERS) {
            Matcher m = fetcher.getUrlPattern().matcher(url);
            if (m.find()) {
                try {
                    return fetcher.fetchDeck(m.group(1));
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("HTTP")) {
                        return FetchResult.error(msg);
                    }
                    return FetchResult.error("Connection error. Please check your internet connection.");
                }
            }
        }

        return FetchResult.error("Site not supported. Supported: Moxfield, Archidekt, EDHREC, TappedOut, MTGGoldfish");
    }
}
