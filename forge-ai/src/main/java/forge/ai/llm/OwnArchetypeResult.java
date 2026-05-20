package forge.ai.llm;

import com.google.gson.annotations.SerializedName;

/**
 * Response from the sidecar's {@code /identify-own-archetype} endpoint:
 * the heuristic (no-LLM) decklist -> archetype match.
 */
public record OwnArchetypeResult(
        @SerializedName("own_archetype") String ownArchetype,
        @SerializedName("strategy_type") String strategyType,
        @SerializedName("guide_source") String guideSource,
        @SerializedName("resolved_format") String resolvedFormat) {

    public boolean isKnown() {
        return ownArchetype != null && !ownArchetype.isEmpty()
                && !"Unknown".equalsIgnoreCase(ownArchetype);
    }
}
