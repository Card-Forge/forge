package forge.ai.llm;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * A single permanent on a battlefield zone, with the fields the sidecar uses
 * for board-aware scoring. Power/toughness are sent only for creatures; null
 * otherwise. Tapped state is part of the public information the AI is
 * allowed to look at.
 *
 * @param name      the card's printed name
 * @param power     printed power if the permanent is a creature; otherwise null
 * @param toughness printed toughness if creature; otherwise null
 * @param types     card types ("Creature", "Artifact", ...)
 * @param tapped    true if the permanent is currently tapped
 */
public record BoardCard(
        String name,
        Integer power,
        Integer toughness,
        List<String> types,
        @SerializedName("is_creature") boolean isCreature,
        boolean tapped) {
}
