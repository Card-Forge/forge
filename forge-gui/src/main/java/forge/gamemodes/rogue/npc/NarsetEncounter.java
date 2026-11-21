package forge.gamemodes.rogue.npc;

import com.google.common.collect.ImmutableList;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import java.util.List;

public enum NarsetEncounter implements NPCEncounter {

    REVEAL(0) {
        @Override
        public NPCContext onAfterMatch(RogueRun run) {
            RogueMetaProgress progress = RogueMetaProgress.getInstance();
            if (progress.getTotalMatchesWon() < 5) {
                return null;
            }
            progress.setNPCLevel(getNpc().id, OFFERING_BOONS.getRequiredLevel());
            return buildContext(
                ImmutableList.of(
                    "Out of nowhere, an Omenpath opens at the edge of the battlefield. A woman in " +
                        "travel-worn Jeskai robes stumbles out of it. " +
                        "She catches herself with one hand and looks back as the passage closes.\n" +
                        "\"That was closer than I intended.\" She rises, her attention already on the last " +
                        "flicker of strange magic above the ground.",
                    "\"There! On the other Plane, it lasted long enough for me to...\" The distortion " +
                        "vanishes. She lets out a disappointed sigh, then notices you watching her.\n" +
                        "\"Forgive me. You've just fought a battle, and I'm just complaining about problems that are clearly not yours. " +
                        "Are you hurt? I'm Narset.\"",
                    "She puts away the scroll she was reaching for.\n" +
                        "\"I've been following these disturbances. I want to understand them before too many " +
                        "people step through these affected Omenpaths and, well... never come back.\n" +
                        "Looks like you had to fight through one. I'd like to hear how you managed, " +
                        "when you've caught your breath.\"\n" +
                        "When you tell her about the Aether, she listens closely. \"I'd like to join you there. " +
                        "I've learned a few things on my travels that could help with yours.\""
                ),
                ImmutableList.of());
        }
    },

    /** Level 3+: offer boons at run start. */
    OFFERING_BOONS(3) {
        @Override
        public NPCContext onRunStart(RogueRun run) {
            return buildOfferingBoonsContext(run);
        }
    };

    private final int requiredLevel;
    NarsetEncounter(int requiredLevel) { this.requiredLevel = requiredLevel; }
    @Override public NPC getNpc() { return NPC.NARSET; }
    @Override public int getRequiredLevel() { return requiredLevel; }

    @Override
    public List<String> getOfferingBoonMonologues() {
        return ImmutableList.of(
            "Narset closes her book as you approach, keeping a finger between the pages.\n" +
                "\"I once thought I understood a Plane because I'd read everything I could find about it. " +
                "I had to abandon quite a few of those conclusions when I actually got there.\"\n" +
                "She gives you her full attention. \"Let me share what proved actually useful.\"",
            "\"I'd like to hear about the next world that surprises you. Even if it proves everything " +
                "I've told you wrong. Especially then.\" Narset makes room for you beside her.\n" +
                "\"Before you go, there's something from my travels I'd like to share. See what you can use.\"",
            "Narset is halfway through a slow sequence of strikes when you arrive. She finishes the " +
                "movement and lowers her hands. \"Leaving already? Wait a moment.\"\n" +
                "She comes to meet you. \"On some worlds, the land itself can turn a battle against you. " +
                "Let me show you what helped me survive out there. It may help you on this Trail too.\""
        );
    }
}
