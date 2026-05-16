package forge.ai.llm;

import java.util.List;

/**
 * A single observed opponent play, collected from public game events and sent
 * to the LLM sidecar as part of a {@link RecognitionRequest}.
 *
 * @param turn   the game turn on which the play happened
 * @param event  the kind of play: "spell", "land", "permanent" or "graveyard"
 * @param card   the card name
 * @param cmc    the card's mana value
 * @param colors the card's colors as single-letter codes (W/U/B/R/G)
 * @param types  the card's card types (Creature, Instant, ...)
 */
public record Observation(int turn, String event, String card, int cmc,
                          List<String> colors, List<String> types) {
}
