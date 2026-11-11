package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.EventEffect;
import forge.gamemodes.rogue.path.NodeEvent;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Henzie "Toolbox" Torre - Capenna contract NPC.
 * Presents a contract every third Event until accepting one unlocks his boons.
 */
public enum HenzieEncounter implements NPCEncounter {

    BEFORE_REVEAL(0),

    REVEAL(333),

    OFFERING_BOONS(666) {
        @Override
        public NPCContext onRunStart(RogueRun run) {
            return buildOfferingBoonsContext(run);
        }
    };

    private static final List<RogueEvent> CONTRACT_EVENTS = List.of(
        RogueEvent.STREET_OF_CONCEALMENT,
        RogueEvent.STREET_OF_GREED,
        RogueEvent.STREET_OF_FORCEFULNESS
    );

    private final int requiredLevel;

    HenzieEncounter(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public NPC getNpc() {
        return NPC.HENZIE;
    }

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }

    @Override
    public RogueEvent onBeforeEvent(RogueEvent event, RogueRun run) {
        int level = RogueMetaProgress.getInstance().getNPCLevel(getNpc().id);
        if (level >= OFFERING_BOONS.requiredLevel || (level + 1) % 3 != 0) {
            return event;
        }

        List<RogueEvent> availableContracts = new ArrayList<>(CONTRACT_EVENTS);
        for (RoguePathNode node : run.getPath().getNodes()) {
            if (node instanceof NodeEvent eventNode) {
                availableContracts.remove(eventNode.getEvent());
            }
        }
        List<RogueEvent> contractPool = availableContracts.isEmpty() ? CONTRACT_EVENTS : availableContracts;
        return contractPool.get(MyRandom.getRandom().nextInt(contractPool.size()));
    }

    @Override
    public NPCContext onAfterEventChoice(RogueEvent event, RogueEvent.EventChoice choice,
                                         EventEffect effect, RogueRun run) {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        int level = progress.getNPCLevel(getNpc().id);
        if (level >= OFFERING_BOONS.requiredLevel) {
            return null;
        }

        if (isAcceptedContract(effect)) {
            progress.setNPCLevel(getNpc().id, OFFERING_BOONS.requiredLevel);
            RogueCommanderAchievements.instance.evaluateNpcBoonUnlockAchievements(progress);
            return buildAcceptedContractContext();
        }

        if (level < REVEAL.requiredLevel && CONTRACT_EVENTS.contains(event)) {
            progress.setNPCLevel(getNpc().id, REVEAL.requiredLevel);
            return buildDeclinedContractContext();
        }

        incrementNpcLevel();
        if (level + 1 == OFFERING_BOONS.requiredLevel) {
            return buildAutomaticUnlockContext();
        }
        return null;
    }

    @Override
    public List<String> getOfferingBoonMonologues() {
        return List.of(
            "Henzie checks the buckles on your pack. \"Heading out already? Hold on. I've been putting a few things " +
                "aside for you. Take a look before you go getting yourself killed with somebody else's stuff.\"",
            "Henzie folds a supplier's letter and pockets it with a grin. \"Took a little asking around, but I've got " +
                "stuff for you. Take your time. I already did the part where we hurry.\"",
            "Henzie pulls up a chair. \"What do you need, Commander? Let me guess...Illegal stuff the whole goddamn Multiverse is after? " +
                "Well, you're in luck. Best offers since a long time.\""
        );
    }

    private NPCContext buildAcceptedContractContext() {
        return buildContext(
            List.of(
                "The devil folds the signed contract and slips it inside his coat. \"Smarter than I thought. Name's Henzie 'Toolbox' Torre. " +
                    "You need something acquired, come find me.\"",
                "He glances over your belongings and outfit. \"Actually, find me before your next trip. I can get you better " +
                    "than that. First lot's on me. You keep coming back from places most people won't go near, " +
                    "I reckon we'll have plenty of business.\""
            ),
            List.of()
        );
    }

    private NPCContext buildDeclinedContractContext() {
        return buildContext(
            "???", getNpc().avatarIndex,
            List.of(
                "The unsigned contract begins to fade, but the devil catches it between two claws and studies you " +
                    "with theatrical disappointment.",
                "\"Bad choice. Would've guessed you Commanders were a little smarter than that. All right, I guess " +
                    "no meta-progression for this one. No unlocked mobster devil at the start " +
                    "of each Run offering crazy starting boons until a contract gets signed. You do you, pal.\""
            ),
            List.of()
        );
    }

    private NPCContext buildAutomaticUnlockContext() {
        return buildContext(
            List.of(
                "The devil spots you and puts away the contract he was reaching for. \"All right. I've spent " +
                    "enough on ink trying to sell you something. Let's try letting you see what I can do.\"",
                "He holds out a hand. \"Henzie 'Toolbox' Torre. Come see me before you head out. I'll set you up " +
                    "with a sample. You like the results, keep coming back. I've got plenty more where that came from.\""
            ),
            List.of()
        );
    }

    private static boolean isAcceptedContract(EventEffect effect) {
        return effect == EventEffect.STREET_OF_CONCEALMENT_ACCEPT
            || effect == EventEffect.STREET_OF_GREED_ACCEPT
            || effect == EventEffect.STREET_OF_FORCEFULNESS_ACCEPT;
    }
}
