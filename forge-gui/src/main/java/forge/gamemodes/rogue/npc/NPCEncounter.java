package forge.gamemodes.rogue.npc;

import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.BazaarContext;
import forge.gamemodes.rogue.effect.EventEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.SanctumContext;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.util.MyRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for NPC encounters that trigger at specific points during or between Rogue Commander runs.
 * Each trigger returns an NPCContext if the NPC wants to interact, or null to skip.
 */
public interface NPCEncounter {

    /** The NPC this encounter belongs to. */
    NPC getNpc();

    /** Minimum NPC level required for this encounter to fire. */
    int getRequiredLevel();

    /** Builds an NPCContext using shared NPC identity and the given per-level data. */
    default NPCContext buildContext(List<String> flavorTextChunks, List<NPCContext.NPCChoice> choices) {
        return new NPCContext(getNpc(), flavorTextChunks, choices, null, null);
    }

    default NPCContext buildContext(String displayNameOverride, int avatarIndexOverride,
                                    List<String> flavorTextChunks, List<NPCContext.NPCChoice> choices) {
        return new NPCContext(getNpc(), flavorTextChunks, choices, displayNameOverride, avatarIndexOverride);
    }

    /** Run-start boon monologues for NPCs that offer choices. */
    default List<String> getOfferingBoonMonologues() {
        return List.of();
    }

    /** Picks one run-start boon monologue, or an empty string if none are configured. */
    default String getRandomBoonMonologue() {
        List<String> monologues = getOfferingBoonMonologues();
        if (monologues.isEmpty()) {
            return "";
        }
        return monologues.get(MyRandom.getRandom().nextInt(monologues.size()));
    }

    /** Builds the standard run-start boon choice context for NPCs that offer effects. */
    default NPCContext buildOfferingBoonsContext(RogueRun run) {
        List<NPCEffect> pool = NPCEffect.getEffectsForNpc(getNpc(), run);
        if (pool.isEmpty()) {
            return null;
        }

        Collections.shuffle(pool, MyRandom.getRandom());
        int choiceCount = Math.min(3, pool.size());
        return buildContext(
            List.of(getRandomBoonMonologue()),
            pool.subList(0, choiceCount).stream()
                .map(effect -> new NPCContext.NPCChoice(effect.getDisplayName(), effect))
                .collect(Collectors.toList())
        );
    }

    /** Increments this NPC's level by 1. */
    default void incrementNpcLevel() {
        RogueMetaProgress p = RogueMetaProgress.getInstance();
        p.setNPCLevelIfHigher(getNpc().id, p.getNPCLevel(getNpc().id) + 1);
        RogueCommanderAchievements.instance.evaluateNpcBoonUnlockAchievements(p);
        RogueCommanderAchievements.instance.evaluateUpgradeAchievements(p);
    }

    /** Returns whether this encounter's trigger condition has been met. */
    default boolean isTriggerConditionMet(RogueMetaProgress progress) { return true; }

    /** Fired between runs after meta-progression has been loaded. Return non-null to show NPC dialog. */
    default NPCContext onBetweenRuns(RogueMetaProgress progress) { return null; }

    /** Fired after each match. Return non-null to show NPC dialog. */
    default NPCContext onAfterMatch(RogueRun run) { return null; }

    /** Fired once when a new run is created. Return non-null to show NPC dialog. */
    default NPCContext onRunStart(RogueRun run) { return null; }

    /** Fired before a Sanctum dialog is shown. Modify ctx to inject dialogs or choices. */
    default void onBeforeSanctum(SanctumContext ctx, RogueRun run) {}

    /** Fired after a custom Sanctum choice is selected. */
    default NPCContext onSanctumChoice(SanctumContext.SanctumChoice choice, RogueRun run) { return null; }

    /** Fired before an Event dialog is shown. Return the Event that should be presented. */
    default RogueEvent onBeforeEvent(RogueEvent event, RogueRun run) { return event; }

    /** Fired after an event choice resolves. Return non-null to show NPC dialog. */
    default NPCContext onAfterEventChoice(RogueEvent event, RogueEvent.EventChoice choice,
                                          EventEffect effect, RogueRun run) { return null; }

    /** Fired before bazaar opens. Modify ctx to inject cards, special offers, or override prices. */
    default void onBeforeBazaar(BazaarContext ctx, RogueRun run) {}

    /** Fired after bazaar purchase. Return non-null to show NPC dialog. */
    default NPCContext onAfterBazaarPurchase(BazaarContext ctx) { return null; }
}
