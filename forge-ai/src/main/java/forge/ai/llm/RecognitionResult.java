package forge.ai.llm;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

/**
 * Response payload returned by the sidecar's {@code /recognize} endpoint: the
 * LLM's guess at the opponent's deck archetype, plus piloting advice for the
 * AI's own deck.
 */
public record RecognitionResult(
        String archetype,
        double confidence,
        String reasoning,
        List<String> alternatives,
        /** Advice on how the AI should pilot its own deck; {@code null} on old sidecars. */
        PilotingAdvice piloting,
        @SerializedName("schema_version") int schemaVersion) {

    /**
     * Piloting advice for the AI's own deck. Nested in the {@code /recognize}
     * response under the {@code piloting} key.
     */
    public record PilotingAdvice(
            @SerializedName("own_archetype") String ownArchetype,
            @SerializedName("guide_source") String guideSource,
            @SerializedName("recommended_play") String recommendedPlay,
            String reasoning,
            List<String> alternatives,
            @SerializedName("mulligan_advice") String mulliganAdvice,
            List<ActionScore> actions) {

        public PilotingAdvice {
            if (actions == null) {
                actions = List.of();
            }
        }
    }

    /** Human-readable one-liner suitable for the game log. */
    public String toLogMessage() {
        return String.format("LLM opponent-deck guess: human opponent is playing '%s' (confidence %.0f%%)",
                archetype, confidence * 100.0);
    }

    /**
     * Human-readable piloting advice for the game log, or {@code null} if the
     * sidecar returned no usable advice.
     */
    public String toPilotingLogMessage() {
        if (piloting == null) {
            return null;
        }
        final boolean hasMulligan = piloting.mulliganAdvice() != null
                && !piloting.mulliganAdvice().isEmpty();
        final String advice = hasMulligan ? piloting.mulliganAdvice() : piloting.recommendedPlay();
        final StringBuilder sb = new StringBuilder();
        if (advice != null && !advice.isEmpty()) {
            sb.append(String.format("LLM piloting advice for AI's own deck ('%s'): %s",
                    piloting.ownArchetype(), advice));
        }
        // Append structured action scores
        if (piloting.actions() != null && !piloting.actions().isEmpty()) {
            final String actionLine = piloting.actions().stream()
                    .limit(3)
                    .map(ActionScore::toLogMessage)
                    .collect(Collectors.joining(" | "));
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("Actions: ").append(actionLine);
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
