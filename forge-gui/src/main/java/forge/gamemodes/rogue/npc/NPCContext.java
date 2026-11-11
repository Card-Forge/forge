package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.effect.NPCEffect;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data bag for NPC dialog display. Holds NPC identity, flavor text chunks, and available choices.
 */
public record NPCContext(NPC npc, List<String> flavorTextChunks, List<NPCChoice> choices, String displayNameOverride,
                         Integer avatarIndexOverride) {

    public NPCContext {
        flavorTextChunks = flavorTextChunks == null || flavorTextChunks.isEmpty()
            ? List.of("")
            : flavorTextChunks.stream().map(text -> text == null ? "" : text).collect(Collectors.toList());
        choices = choices == null ? List.of() : List.copyOf(choices);
    }

    public String flavorText() {
        return String.join("\n", flavorTextChunks);
    }

    public String displayName() {
        return displayNameOverride != null ? displayNameOverride : npc.name;
    }

    public int avatarIndex() {
        return avatarIndexOverride != null ? avatarIndexOverride : npc.avatarIndex;
    }

    public record NPCChoice(String label, NPCEffect npcEffect) {}
}
