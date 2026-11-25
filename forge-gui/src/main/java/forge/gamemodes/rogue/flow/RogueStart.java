package forge.gamemodes.rogue.flow;

import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gamemodes.rogue.effect.CustomRunModification;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** What the player chose on the start page. A generated commander is played instead of the deck, which may then be null. */
public record RogueStart(RogueDeck deck, boolean custom, Set<CustomRunModification> modifications, int descension) {

    public static List<RogueDeck> roster(boolean experimental) {
        List<RogueDeck> availableDecks = RogueConfig.loadRogueDecks();
        availableDecks.removeIf(deck -> deck.isExperimental() != experimental);

        // Sort commanders: unlocked first (alphabetically), then locked (alphabetically)
        availableDecks.sort(Comparator
            .comparing((RogueDeck d) -> !d.isUnlocked())  // false (unlocked) before true (locked)
            .thenComparing(RogueDeck::getName));
        return availableDecks;
    }

    /** Ticking clears what conflicts; a generated commander brings the modifications included with it and takes them away again. */
    public static Set<CustomRunModification> toggle(Set<CustomRunModification> on, CustomRunModification modification, boolean tick) {
        boolean wasGenerated = generatedCommander(on);
        Set<CustomRunModification> result = EnumSet.noneOf(CustomRunModification.class);
        result.addAll(on);
        if (tick) {
            result.add(modification);
            result.removeIf(modification::conflictsWith);
        } else {
            result.remove(modification);
        }
        boolean generated = generatedCommander(result);
        if (generated || wasGenerated || modification.isGeneratedCommander()) {
            for (CustomRunModification included : CustomRunModification.values()) {
                if (!included.isIncludedWithGeneratedCommander()) {
                    continue;
                }
                if (generated) {
                    result.add(included);
                } else {
                    result.remove(included);
                }
            }
        }
        return result;
    }

    /** Ticked and fixed by a generated commander. */
    public static boolean isForced(Set<CustomRunModification> on, CustomRunModification modification) {
        return modification.isIncludedWithGeneratedCommander() && generatedCommander(on);
    }

    private static boolean generatedCommander(Set<CustomRunModification> on) {
        return on.stream().anyMatch(CustomRunModification::isGeneratedCommander);
    }

    /** Null when the level can be started. Level 0 asks only whether Descension is open; deck may be null in a custom run. */
    public static String descensionLockText(RogueMetaProgress progress, RogueDeck deck, int level, boolean custom) {
        Localizer localizer = Localizer.getInstance();
        if (!progress.isDescensionModeUnlocked()) {
            return localizer.getMessage("lblRogueUnlockDescensionHint");
        }
        if (custom) {
            return null;
        }
        String commanderName = deck.getCommanderCardName();
        int maxUnlocked = progress.getMaxDescensionUnlocked(commanderName);
        if (maxUnlocked == 0) {
            return localizer.getMessage("lblRogueUnlockCommanderDescensionHint", commanderName);
        }
        return level > maxUnlocked
            ? localizer.getMessage("lblRogueUnlockNextDescensionHint", commanderName, level - 1, level)
            : null;
    }

    /** Aether, History and Codex open after the first completed run. */
    public static boolean tabsUnlocked(RogueMetaProgress progress) {
        return progress.getTotalRunsCompleted() > 0;
    }

    public boolean canBegin(RogueMetaProgress progress) {
        if (custom && !progress.isCustomRunsUnlocked()) {
            return false;
        }
        if (descension > 0 && !progress.isDescensionModeUnlocked()) {
            return false;
        }
        List<CustomRunModification> runModifications = runModifications();
        if (custom && runModifications.isEmpty()) {
            return false;
        }
        for (CustomRunModification modification : runModifications) {
            if (runModifications.stream().anyMatch(modification::conflictsWith)) {
                return false;
            }
        }
        if (!isGeneratedCommander() && (deck == null || !deck.isUnlocked())) {
            return false;
        }
        // The level after the last one counts as unlocked once the last one is won
        return descension >= 0 && descension <= DescensionLevel.getMaxLevel()
            && (custom || descension <= progress.getMaxDescensionUnlocked(deck.getCommanderCardName()));
    }

    boolean isGeneratedCommander() {
        return custom && modifications.stream().anyMatch(CustomRunModification::isGeneratedCommander);
    }

    /** The traits the run would begin with: the active Aether effects, its Descension levels, then the custom run's changes. */
    public List<RogueEffect> startingTraits(RogueMetaProgress progress) {
        // In the Aether page's order, as the active effects are kept in no order
        List<RogueEffect> traits = new ArrayList<>(progress.getActiveAetherEffects().stream().sorted().toList());
        for (int level = 1; level <= descension; level++) {
            DescensionLevel descensionLevel = DescensionLevel.forLevel(level);
            if (descensionLevel != null) {
                traits.add(descensionLevel);
            }
        }
        traits.addAll(runModifications());
        return traits;
    }

    /** What a starting trait does; an Aether effect at the rank the run would take it at. */
    public static String startingTraitText(RogueEffect trait, RogueMetaProgress progress) {
        return trait instanceof AetherEffect aether ? aether.getDescriptionForRank(progress.getAetherEffectRank(aether)) : trait.getDescription();
    }

    /** In the order they apply. */
    List<CustomRunModification> runModifications() {
        List<CustomRunModification> runModifications = new ArrayList<>();
        if (!custom) {
            return runModifications;
        }
        boolean generatedCommander = isGeneratedCommander();
        for (CustomRunModification modification : CustomRunModification.values()) {
            if (modifications.contains(modification)
                && (!generatedCommander || !modification.isIncludedWithGeneratedCommander())) {
                runModifications.add(modification);
            }
        }
        return runModifications;
    }
}
