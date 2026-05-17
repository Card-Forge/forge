package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload POSTed to the sidecar's {@code /recognize} endpoint.
 * Field names map to the snake_case JSON contract via {@link SerializedName}.
 *
 * <p>This record is the Forge adapter's view of the client-agnostic
 * recognition contract (see {@code docs/ADAPTERS.md} in the sidecar). Any MTG
 * client can act as an adapter by producing this same JSON shape.</p>
 */
public record RecognitionRequest(
        /** Identifies the calling client/adapter — always {@code "forge"} here. */
        String client,
        @SerializedName("game_id") String gameId,
        String format,
        @SerializedName("opponent_seat") int opponentSeat,
        int turn,
        List<Observation> observations,
        /** The AI's own deck (card names) — lets the sidecar detect the
         *  precise format when Forge only reports a generic game type. */
        @SerializedName("deck_cards") List<String> deckCards) {

    /** Identifier this (Forge) adapter sends as the {@code client} field. */
    public static final String CLIENT = "forge";
}
