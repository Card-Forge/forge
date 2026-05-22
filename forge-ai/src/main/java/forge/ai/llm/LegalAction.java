package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/** One legal action candidate the sidecar may choose from. */
public record LegalAction(
        @SerializedName("action_type") String actionType,
        String card,
        String target,
        List<String> colors,
        @SerializedName("enters_tapped") boolean entersTapped,
        List<String> produces) {
}
