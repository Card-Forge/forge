package forge.ai.llm;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Request payload POSTed to the sidecar's {@code /recognize} endpoint.
 * Field names map to the snake_case JSON contract via {@link SerializedName}.
 *
 * <p>This record is the Forge adapter's view of the client-agnostic
 * recognition contract (see {@code docs/ADAPTERS.md} in the sidecar). Any MTG
 * client can act as an adapter by producing this same JSON shape.</p>
 *
 * <p>The live-state fields ({@code hand}, {@code ownBoard}, ...) feed the
 * sidecar's piloting advice. They are optional: the sidecar still returns
 * archetype-level advice when they are empty. Capturing real board state is a
 * follow-up — see {@code docs/ADAPTERS.md}.</p>
 */
public record RecognitionRequest(
        /** Identifies the calling client/adapter — always {@code "forge"} here. */
        String client,
        @SerializedName("game_id") String gameId,
        String format,
        @SerializedName("opponent_seat") int opponentSeat,
        int turn,
        List<Observation> observations,
        /** The AI's own deck (card names) — lets the sidecar detect the precise
         *  format and identify the AI's own archetype for piloting advice. */
        @SerializedName("deck_cards") List<String> deckCards,
        /** Cards in the AI's hand. */
        List<String> hand,
        /** Card names on the AI's battlefield. */
        @SerializedName("own_board") List<String> ownBoard,
        /** Card names on the opponent's battlefield. */
        @SerializedName("opponent_board") List<String> opponentBoard,
        /** Card names in the AI's graveyard. */
        @SerializedName("your_graveyard") List<String> yourGraveyard,
        /** Card names in the opponent's graveyard. */
        @SerializedName("opponent_graveyard") List<String> opponentGraveyard,
        /** Player name/seat -> life total. */
        @SerializedName("life_totals") Map<String, Integer> lifeTotals,
        /** Current game phase string, e.g. "MAIN1". */
        String phase,
        /** Mana sources the AI can tap right now, as color strings (W/U/B/R/G/C). */
        @SerializedName("available_mana") List<String> availableMana,
        /** AI personality traits from the AI profile. */
        Map<String, Object> personality,
        /** Number of cards in the AI's hand (public info, redundant with hand.size()). */
        @SerializedName("ai_hand_size") int aiHandSize,
        /** Number of cards in the opponent's hand. Public info in MTG. */
        @SerializedName("opp_hand_size") int oppHandSize,
        /** Number of cards in the AI's library. */
        @SerializedName("ai_library_size") int aiLibrarySize,
        /** Number of cards in the opponent's library. */
        @SerializedName("opp_library_size") int oppLibrarySize,
        /** Detailed view of the AI's battlefield (P/T, types, tapped). */
        @SerializedName("own_board_details") List<BoardCard> ownBoardDetails,
        /** Detailed view of the opponent's battlefield (P/T, types, tapped). */
        @SerializedName("opponent_board_details") List<BoardCard> opponentBoardDetails,
        /** Colors of mana the OPPONENT has produced from lands so far — used to
         *  detect color screw. Letter codes (W/U/B/R/G/C). */
        @SerializedName("opponent_mana_colors_seen") List<String> opponentManaColorsSeen,
        /** v6: mana the opponent could tap right now (untapped sources) — their
         *  open interaction mana, used to predict their next turn. */
        @SerializedName("opp_mana_available") int oppManaAvailable,
        /** v6: mana the opponent has committed this turn (tapped sources). */
        @SerializedName("opp_mana_spent_this_turn") int oppManaSpentThisTurn,
        /** v6: which decision the AI is making — lets the sidecar spend extra
         *  effort on high-impact decisions. One of "mulligan" | "combat" |
         *  "priority" | "critical". Empty means a routine priority decision. */
        @SerializedName("decision_type") String decisionType,
        /** v6: one entry per untapped opponent mana source, each the colors that
         *  source can produce (W/U/B/R/G/C). Lets the sidecar reason about which
         *  color combinations the opponent has open this turn. */
        @SerializedName("opp_untapped_sources") List<List<String>> oppUntappedSources) {

    /** Identifier this (Forge) adapter sends as the {@code client} field. */
    public static final String CLIENT = "forge";

    /**
     * Convenience constructor for the recognition-only payload: the live-state
     * fields default to empty until board-state capture is wired up.
     */
    public RecognitionRequest(final String client, final String gameId, final String format,
                              final int opponentSeat, final int turn,
                              final List<Observation> observations, final List<String> deckCards) {
        this(client, gameId, format, opponentSeat, turn, observations, deckCards,
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(),
                "", List.of(), Map.of(),
                0, 0, 0, 0, List.of(), List.of(), List.of(),
                0, 0, "", List.of());
    }

    /**
     * Convenience constructor including personality but no live-state fields.
     */
    public RecognitionRequest(final String client, final String gameId, final String format,
                              final int opponentSeat, final int turn,
                              final List<Observation> observations, final List<String> deckCards,
                              final Map<String, Object> personality) {
        this(client, gameId, format, opponentSeat, turn, observations, deckCards,
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(),
                "", List.of(), personality,
                0, 0, 0, 0, List.of(), List.of(), List.of(),
                0, 0, "", List.of());
    }
}
