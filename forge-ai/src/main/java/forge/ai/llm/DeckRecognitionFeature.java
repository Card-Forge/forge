package forge.ai.llm;

import forge.ai.AiController;
import forge.ai.AiProps;

/**
 * Resolves whether the LLM deck-recognition feature is enabled and where the
 * sidecar lives. The feature is off by default; it can be turned on either
 * through an AI profile property or the {@code forge.ai.deckRecognition}
 * system property (handy for quick testing without editing a profile).
 */
public final class DeckRecognitionFeature {

    /** System property override, e.g. {@code -Dforge.ai.deckRecognition=true}. */
    public static final String SYS_PROP = "forge.ai.deckRecognition";

    /** System property the self-play runner sets to {@code solve} for goldfish
     *  runs. Unset (production) resolves to {@code normal}, so the production
     *  recognition path is never affected. */
    public static final String PILOT_MODE_SYS_PROP = "forge.ai.deckRecognition.pilotMode";

    private DeckRecognitionFeature() {
    }

    /** @return the pilot mode to send on /recognize ("normal" or "solve"). */
    public static String pilotMode() {
        final String fromSys = System.getProperty(PILOT_MODE_SYS_PROP);
        if (fromSys != null && !fromSys.isBlank()) {
            return fromSys.trim();
        }
        return "normal";
    }

    /** @return true if deck recognition should be active for the given AI. */
    public static boolean isEnabled(final AiController ai) {
        if (Boolean.getBoolean(SYS_PROP)) {
            return true;
        }
        try {
            return ai.getBoolProperty(AiProps.DECK_RECOGNITION_ENABLE);
        } catch (final RuntimeException ex) {
            return false;
        }
    }

    /** System property listing seat/player ids (comma-separated) for which
     *  recognition should be skipped — used by the self-play goldfish runner to
     *  silence the passive opponent seat. */
    public static final String DISABLE_SEATS_SYS_PROP = "forge.ai.deckRecognition.disableSeats";

    /** @return seat/player ids for which recognition should be skipped. */
    public static java.util.Set<Integer> disabledSeats() {
        final String raw = System.getProperty(DISABLE_SEATS_SYS_PROP, "");
        if (raw == null || raw.isBlank()) {
            return java.util.Set.of();
        }
        final java.util.Set<Integer> out = new java.util.HashSet<>();
        for (final String tok : raw.split(",")) {
            try {
                out.add(Integer.parseInt(tok.trim()));
            } catch (final NumberFormatException ignored) {
                // skip non-numeric tokens
            }
        }
        return out;
    }

    /** Per-seat sidecar URL: checks {@code forge.ai.deckRecognition.url.<seat>}
     *  first (so the self-play runner can point each mirror seat at its own
     *  sidecar/dashboard), then falls back to the global resolver. */
    public static String sidecarUrl(final AiController ai, final int seat) {
        final String perSeat = System.getProperty("forge.ai.deckRecognition.url." + seat);
        if (perSeat != null && !perSeat.isBlank()) {
            return perSeat.trim();
        }
        return sidecarUrl(ai);
    }

    /** @return the base URL of the LLM sidecar service. */
    public static String sidecarUrl(final AiController ai) {
        final String fromSys = System.getProperty("forge.ai.deckRecognition.url");
        if (fromSys != null && !fromSys.isBlank()) {
            return fromSys.trim();
        }
        try {
            final String url = ai.getProperty(AiProps.DECK_RECOGNITION_SIDECAR_URL);
            if (url != null && !url.isBlank()) {
                return url.trim();
            }
        } catch (final RuntimeException ignored) {
            // fall through to default
        }
        return "http://localhost:8000";
    }
}
