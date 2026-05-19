package forge.ai.llm;

import org.tinylog.Logger;

import forge.ai.AiController;
import forge.game.Game;
import forge.game.player.Player;

/**
 * Entry point that wires the LLM deck-recognition feature into an AI player.
 *
 * <p>Called from the {@link AiController} constructor. It is fully fail-soft:
 * if the feature is disabled or the sidecar is unreachable, nothing is attached
 * and the game proceeds exactly as before.</p>
 */
public final class DeckRecognitionManager {

    private DeckRecognitionManager() {
    }

    /**
     * Attach deck recognition to an AI player, if enabled and the sidecar is
     * reachable. Safe to call unconditionally — it gates itself.
     *
     * @param ai     the AI controller being constructed
     * @param player the AI player
     * @param game   the current game
     */
    public static void attach(final AiController ai, final Player player, final Game game) {
        try {
            if (!DeckRecognitionFeature.isEnabled(ai)) {
                return;
            }
            final String url = DeckRecognitionFeature.sidecarUrl(ai);
            final DeckRecognitionClient client = new DeckRecognitionClient(url);
            if (!client.isSidecarHealthy()) {
                Logger.debug("DeckRecognition: sidecar at " + url
                        + " is unavailable; feature disabled for this game.");
                client.shutdown();
                return;
            }
            final DeckRecognitionObserver observer = new DeckRecognitionObserver(player, game, client);
            game.subscribeToEvents(observer);
            Logger.info("DeckRecognition: enabled for AI player '" + player + "' via " + url);
        } catch (final RuntimeException | LinkageError ex) {
            // Any failure here must not break AI construction.
            Logger.debug("DeckRecognition: failed to attach: " + ex.getMessage());
        }
    }
}
