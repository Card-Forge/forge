package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.SanctumContext;
import java.util.List;

/**
 * Tyvar Kell - Commander Trainer NPC.
 * Progresses through Sanctum encounters before offering boons on future runs.
 */
public enum TyvarEncounter implements NPCEncounter {

    /** Hidden buildup phase: each entered Sanctum advances Tyvar's reveal progress. */
    BEFORE_REVEAL(0) {
        @Override
        public void onBeforeSanctum(SanctumContext ctx, RogueRun run) {
            incrementNpcLevel();
            if (RogueMetaProgress.getInstance().getNPCLevel(getNpc().id) < 2) {
                return;
            }
            ctx.preSanctumDialogs.add(buildContext(
                "Stranger", STRANGER_AVATAR_INDEX,
                List.of(
                    "A man in torn clothes drags himself across the Sanctum's stone floor, leaving a thin trail " +
                        "of blood. He reaches for a pillar, but his arm gives way.",
                    "He presses a hand against his side and looks up at you. \"Here. Help me keep pressure on it. " +
                        "I can't stop the bleeding.\""
                ),
                List.of()
            ));
            addHelpStrangerChoice(ctx);
        }
    },

    /** Tyvar has been found and waits for help at a Sanctum. */
    WAITING_FOR_HELP(2) {
        @Override
        public void onBeforeSanctum(SanctumContext ctx, RogueRun run) {
            addHelpStrangerChoice(ctx);
        }

        @Override
        public NPCContext onSanctumChoice(SanctumContext.SanctumChoice choice, RogueRun run) {
            if (!HELP_STRANGER_CHOICE_ID.equals(choice.id())) {
                return null;
            }
            incrementNpcLevel();
            return buildContext(
                List.of(
                    "The stranger eases himself upright against the pillar. \"You needed this rest yourself, " +
                        "didn't you? And you spent it on me.\" He offers his hand. \"Tyvar Kell. I'm glad you found me.\"",
                    "He tries to stand, winces, and settles back with a short laugh. \"Give me a little time. " +
                        "I'll meet you in the Aether. Come find me before you head out, and we'll get you ready " +
                        "for a proper fight. As often as you need, friend. I owe you more than a thank-you.\""
                ),
                List.of()
            );
        }
    },

    /** After being helped, Tyvar offers Commander training boons at the start of runs. */
    OFFERING_BOONS(3) {
        @Override
        public NPCContext onRunStart(RogueRun run) {
            return buildOfferingBoonsContext(run);
        }
    };

    private static final String HELP_STRANGER_CHOICE_ID = "tyvar_help_stranger";
    private static final int STRANGER_AVATAR_INDEX = 125;

    private final int requiredLevel;

    TyvarEncounter(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public NPC getNpc() { return NPC.TYVAR; }

    @Override
    public int getRequiredLevel() { return requiredLevel; }

    private static void addHelpStrangerChoice(SanctumContext ctx) {
        ctx.extraChoices.add(new SanctumContext.SanctumChoice(
            HELP_STRANGER_CHOICE_ID, "Help Stranger", "???"));
    }

    @Override
    public List<String> getOfferingBoonMonologues() {
        return List.of(
            "Tyvar waves you over to a bench piled with gear. \"There you are! I've been sorting through this " +
                "for you. You can thank me by coming back to tell me how the fight went. In detail, mind you.\"",
            "Tyvar rests a hand on the stone wall. Gray spreads across his knuckles, then fades as he pulls away. " +
                "\"A good place to fight, stone underfoot. Plenty to work with. Come, let's see what we can put " +
                "to use for you.\"",
            "Tyvar grins as you approach. \"Ready? Good. I've a few ideas, and I want to hear yours. " +
                "If you find a better use for what I give you, I'll want the whole story when you get back.\""
        );
    }
}
