package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload for the sidecar's {@code /identify-own-archetype} endpoint.
 *
 * <p>Sent once when deck recognition attaches, so the sidecar can heuristically
 * identify the AI's own archetype from its decklist <em>before any opponent
 * action</em> triggers a regular {@code /recognize} call.</p>
 */
public record OwnArchetypeRequest(
        @SerializedName("game_id") String gameId,
        String format,
        @SerializedName("deck_cards") List<String> deckCards) {
}
