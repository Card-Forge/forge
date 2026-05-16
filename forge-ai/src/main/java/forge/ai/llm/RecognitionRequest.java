package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload POSTed to the sidecar's {@code /recognize} endpoint.
 * Field names map to the snake_case JSON contract via {@link SerializedName}.
 */
public record RecognitionRequest(
        @SerializedName("game_id") String gameId,
        String format,
        @SerializedName("opponent_seat") int opponentSeat,
        int turn,
        List<Observation> observations,
        /** The AI's own deck (card names) — lets the sidecar detect the
         *  precise format when Forge only reports a generic game type. */
        @SerializedName("deck_cards") List<String> deckCards) {
}
