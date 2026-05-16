package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Response payload returned by the sidecar's {@code /recognize} endpoint:
 * the LLM's guess at the opponent's deck archetype.
 */
public record RecognitionResult(
        String archetype,
        double confidence,
        String reasoning,
        List<String> alternatives,
        @SerializedName("schema_version") int schemaVersion) {

    /** Human-readable one-liner suitable for the game log. */
    public String toLogMessage() {
        return String.format("LLM deck guess: opponent is playing '%s' (confidence %.0f%%)",
                archetype, confidence * 100.0);
    }
}
