package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * A scored action recommendation from the LLM sidecar piloting advice.
 * Each has an action type, optional target(s), a percentage (0-100) indicating
 * how strongly the sidecar recommends this action, and a reasoning string.
 */
public record ActionScore(
        @SerializedName("action_type") String actionType,
        String target,
        List<String> targets,
        double percentage,
        String reasoning) {

    /** Human-readable summary for the game log. */
    public String toLogMessage() {
        final String tgt = target != null && !target.isEmpty() ? " -> " + target : "";
        return String.format("[Sidecar] %s%s (%.0f%%): %s",
                actionType, tgt, percentage,
                reasoning != null ? reasoning : "");
    }
}
