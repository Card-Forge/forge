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

    private DeckRecognitionFeature() {
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
