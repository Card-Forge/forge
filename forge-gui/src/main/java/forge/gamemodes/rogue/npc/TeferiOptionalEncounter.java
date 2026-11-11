package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.RogueMetaProgress;
import java.util.List;

/**
 * Teferi encounters that can occur independently of his linear story progression.
 */
public enum TeferiOptionalEncounter implements NPCOptionalEncounter {

    TEMPORAL_ANCHOR("teferi:temporal_anchor") {
        @Override
        public List<String> getStoryTextChunks() {
            return List.of(
                "Teferi offers you his hand. \"Easy, Commander. You're back in the Aether. Take a moment.\"",
                "He traces a small circle in the air between you. For a moment, its light holds still against " +
                    "your skin. \"A temporal link, anchored here. I establish it just before you leave. If you're " +
                    "killed, or the expedition is lost, I'll revert the time and bring you back here as you were before you left. " +
                    "It sounds weirder than it is, trust me. That's how I brought you back this time. We'll both remember what happened.\"",
                "And what's even more important: You'll keep the Echoes gained in all Runs, Win or Loss. Use these memories of the Aether "
                + "to rebuild lost Aetherworks that make you stronger in future Runs.",
                "He lowers his hand. \"Well, at least now you know why I have to stay here. Someone has to be here to " +
                    "bring you back. Just in case you were wondering why I just keep talking instead of coming with you.\""
            );
        }

        @Override
        public boolean isTriggerConditionMet(RogueMetaProgress progress) {
            return progress.getProgressionTotalRunsCompleted() > progress.getProgressionTotalRunsWon();
        }
    },
    CODEX_AND_HISTORY("teferi:codex_and_history") {
        @Override
        public List<String> getStoryTextChunks() {
            return List.of(
                "Back already? I meant...keep it up, Commander. You're getting stronger every second that I am watching you.",
                "Oh, and did you know that I try to keep track of your memories ad findings during your Runs?\n"
                    + "Open the 'Codex' to view and reset your overall game progress, stats, unlocked cards and tutorials.\n"
                    + "View all your past Runs and Rogue Decks in the 'History'."
            );
        }

        @Override
        public boolean isTriggerConditionMet(RogueMetaProgress progress) {
            return progress.getProgressionTotalRunsCompleted() > progress.getProgressionTotalRunsWon() + 1;
        }
    },
    AUTO_UPDATE("teferi:auto_update") {
        @Override
        public List<String> getStoryTextChunks() {
            return List.of(
                "Umm...It might be a bad timing but...Just a quick reminder that you can (and probably should) check 'Content Downloaders' (on the left) and then select 'Check for Updates' from time to time.\n"
                    + "That way you won't miss any new features, fixes and content.",
                "Make sure 'Auto updater' in 'Preferences' is set to 'Snapshot.\n'"
                    + "Then return to 'Content Downloaders' and select 'Check for Updates'. If a new version is " +
                    "available, choose 'Update Now'. Skip all Proxy settings unless you know what you are doing. Press 'Start'. Once the download finishes, confirm with 'OK'. Forge will " +
                    "close and launch the installer.",
                "In the installer, just tell the game where you want to put the game files. You probably just want to choose the folder where your game is located at the moment as well.\n"
                    + "That's it, the new Game version will install and you can open it as usual.",
                "That...probably sounded more complicated than it is..."
            );
        }

        @Override
        public boolean isTriggerConditionMet(RogueMetaProgress progress) {
            return progress.getProgressionTotalRunsCompleted() > progress.getProgressionTotalRunsWon() + 2;
        }
    };

    private final String id;

    TeferiOptionalEncounter(String id) {
        this.id = id;
    }

    @Override
    public NPC getNpc() {
        return NPC.TEFERI;
    }

    @Override
    public String getId() {
        return id;
    }
}
