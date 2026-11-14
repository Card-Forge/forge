package forge.gamemodes.rogue;

import forge.deck.Deck;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.EndlessMode;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.path.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight snapshot of a completed/abandoned Rogue Commander run for history display.
 */
public class RogueRunHistoryEntry {

    private String commanderName;
    private int avatarIndex;
    private String outcome; // "VICTORY", "DEFEAT", "ABANDONED"
    private String bossOrDefeatedBy;
    private List<String> path;
    private int finalLife;
    private int finalGold;
    private String timestamp;
    private Deck deckSnapshot;
    private int descensionLevel;                   // 0 = not used; XStream defaults to 0 for old saves
    private int endlessLevel;
    private List<EffectSnapshot> activeEffects;    // null for old saves
    private long runTimeMillis;                    // XStream defaults to 0 for old saves

    public static RogueRunHistoryEntry fromRun(RogueRun run, String outcome, String bossOrDefeatedBy) {
        RogueRunHistoryEntry entry = new RogueRunHistoryEntry();
        entry.commanderName = run.getCurrentCommanderName();
        entry.avatarIndex = run.getSelectedRogueDeck().getAvatarIndex();
        entry.outcome = outcome;
        entry.bossOrDefeatedBy = bossOrDefeatedBy != null ? bossOrDefeatedBy : "";
        entry.finalLife = run.getCurrentLife();
        entry.finalGold = run.getCurrentGold();
        entry.timestamp = LocalDate.now().toString();
        entry.deckSnapshot = run.getCurrentDeck() != null ? new Deck(run.getCurrentDeck()) : null;
        entry.runTimeMillis = run.getRunTimeMillis();

        entry.descensionLevel = run.getDescensionLevel();
        entry.endlessLevel = run.getEndlessLevel();

        // Capture active permanent effects from run snapshot using the same display style as the map header
        entry.activeEffects = new ArrayList<>();
        for (RogueEffect effect : RogueEffectComposite.getAllEffects(run)) {
            if (effect instanceof DescensionLevel || effect instanceof EndlessMode) {
                continue;
            }
            entry.activeEffects.add(new EffectSnapshot(effect.getUIDisplayText(), effect.getActiveDescription(run)));
        }

        entry.path = capturePath(run);
        return entry;
    }

    private static List<String> capturePath(RogueRun run) {
        List<String> pathLabels = new ArrayList<>();
        RoguePath path = run.getPath();
        if (path == null) {
            return pathLabels;
        }

        RoguePathNode currentNode = run.getCurrentNode();
        for (RoguePathNode node : path.getNodes()) {
            if (!node.isCompleted() && node != currentNode) {
                continue;
            }
            if (node instanceof NodePlanebound planeboundNode) {
                RoguePlanebound planebound = planeboundNode.getRoguePlanebound();
                if (planebound != null) {
                    pathLabels.add(planebound.planeName() + " (" + planebound.planeboundName() + ")");
                }
            } else if (node instanceof NodeSanctum) {
                pathLabels.add("Sanctum");
            } else if (node instanceof NodeBazaar) {
                pathLabels.add("Bazaar");
            } else if (node instanceof NodeChest) {
                pathLabels.add("Loot");
            } else if (node instanceof NodeEvent) {
                pathLabels.add("Event");
            }
        }

        return pathLabels;
    }

    // Getters
    public String getCommanderName() { return commanderName; }
    public int getAvatarIndex() { return avatarIndex; }
    public String getOutcome() { return outcome; }
    public String getBossOrDefeatedBy() { return bossOrDefeatedBy; }
    public List<String> getPath() { return path != null ? path : new ArrayList<>(); }
    public String getTimestamp() { return timestamp; }
    public Deck getDeckSnapshot() { return deckSnapshot; }
    public int getDescensionLevel() { return descensionLevel; }
    public int getEndlessLevel() { return endlessLevel; }
    public long getRunTimeMillis() { return runTimeMillis; }
    public String getFormattedRunTime() { return TextHelper.formatHoursMinutesSeconds(runTimeMillis); }
    public List<EffectSnapshot> getActiveEffects() {
        if (activeEffects != null) {
            return activeEffects;
        }

        return Collections.emptyList();
    }

    public static class EffectSnapshot {
        private String displayText;
        private String tooltipText;

        public EffectSnapshot() {
            // XStream's pure Java reflection provider uses this constructor when loading saves.
        }

        public EffectSnapshot(String displayText, String tooltipText) {
            this.displayText = displayText;
            this.tooltipText = tooltipText;
        }

        public String getDisplayText() { return displayText; }
        public String getTooltipText() { return tooltipText; }
    }
}
