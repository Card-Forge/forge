package forge.ai.llm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Set<String> ATTACHED_PLAYERS = ConcurrentHashMap.newKeySet();

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
            final boolean isAi = player.isAI();
            final boolean enabled = DeckRecognitionFeature.isEnabled(ai);
            final String sysProp = System.getProperty(DeckRecognitionFeature.SYS_PROP, "<unset>");
            Logger.info("DeckRecognition.attach: player='" + player + "' isAI=" + isAi
                    + " enabled=" + enabled + " sysprop=" + sysProp
                    + " game=" + game.getId() + " playerId=" + player.getId());
            if (!isAi) {
                Logger.info("DeckRecognition.attach: skipped (player.isAI() == false)");
                return;
            }
            if (!enabled) {
                Logger.info("DeckRecognition.attach: skipped (feature disabled — sysprop="
                        + sysProp + ")");
                return;
            }
            if (DeckRecognitionFeature.disabledSeats().contains(player.getId())) {
                Logger.info("DeckRecognition.attach: skipped (seat " + player.getId()
                        + " disabled via " + DeckRecognitionFeature.DISABLE_SEATS_SYS_PROP + ")");
                return;
            }
            final String key = game.getId() + ":" + player.getId();
            if (!ATTACHED_PLAYERS.add(key)) {
                Logger.info("DeckRecognition.attach: skipped (key already attached: " + key + ")");
                return;
            }
            final String url = DeckRecognitionFeature.sidecarUrl(ai, player.getId());
            final DeckRecognitionClient client = new DeckRecognitionClient(url);
            final boolean healthy = client.isSidecarHealthy();
            // Tell the controller so waitForSidecar() can skip the blocking wait
            // when the sidecar is offline — otherwise every decision would stall
            // for the full budget waiting on a call that will never succeed.
            ai.setSidecarHealthy(healthy);
            if (!healthy) {
                Logger.info("DeckRecognition: sidecar at " + url
                        + " did not respond to /health; attaching observer anyway "
                        + "(individual /recognize calls will fail-soft, "
                        + "synchronous wait disabled).");
            }
            final DeckRecognitionObserver observer = new DeckRecognitionObserver(player, game, client, ai);
            ai.setSidecarClient(client, observer.deckCards());
            ai.setDeckRecognitionObserver(observer);
            game.subscribeToEvents(observer);
            Logger.info("DeckRecognition: enabled for AI player '" + player
                    + "' via " + url + " (health=" + healthy + ")");
        } catch (final RuntimeException | LinkageError ex) {
            // Any failure here must not break AI construction.
            ATTACHED_PLAYERS.remove(game.getId() + ":" + player.getId());
            Logger.info("DeckRecognition.attach: failed: " + ex);
        }
    }
}
