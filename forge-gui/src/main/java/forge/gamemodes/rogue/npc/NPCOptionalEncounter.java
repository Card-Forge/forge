package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.RogueMetaProgress;
import java.util.List;

/**
 * An NPC encounter that is triggered independently of NPC level and delivered only once.
 */
public interface NPCOptionalEncounter extends NPCEncounter {

    String getId();

    default List<String> getStoryTextChunks() {
        return List.of();
    }

    @Override
    default int getRequiredLevel() {
        return 0;
    }

    @Override
    default NPCContext onBetweenRuns(RogueMetaProgress progress) {
        List<String> storyTextChunks = getStoryTextChunks();
        if (storyTextChunks.isEmpty()
            || !isTriggerConditionMet(progress)
            || progress.hasSeenNPCEncounter(getId())) {
            return null;
        }

        progress.markNPCEncounterSeen(getId());
        return buildContext(storyTextChunks, List.of());
    }
}
