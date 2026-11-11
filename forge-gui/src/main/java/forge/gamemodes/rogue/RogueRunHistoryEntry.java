package forge.gamemodes.rogue;

import forge.deck.Deck;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.EndlessMode;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.path.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

    public RogueRunHistoryEntry() {
    }

    public static RogueRunHistoryEntry fromRun(RogueRun run, String outcome, String bossOrDefeatedBy) {
        RogueRunHistoryEntry entry = new RogueRunHistoryEntry();
        entry.commanderName = run.getCurrentCommanderName();
        entry.avatarIndex = run.getSelectedRogueDeck().getAvatarIndex();
        entry.outcome = outcome;
        entry.bossOrDefeatedBy = bossOrDefeatedBy != null ? bossOrDefeatedBy : "";
        entry.finalLife = run.getCurrentLife();
        entry.finalGold = run.getCurrentGold();
        entry.timestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
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

        entry.path = new ArrayList<>();

        if (run.getPath() != null) {
            RoguePathNode currentNode = run.getCurrentNode();
            for (RoguePathNode node : run.getPath().getNodes()) {
                if (!node.isCompleted() && node != currentNode) {
                    continue;
                }
                if (node instanceof NodePlanebound) {
                    NodePlanebound pb = (NodePlanebound) node;
                    if (pb.getRoguePlanebound() != null) {
                        entry.path.add(pb.getRoguePlanebound().planeName()
                                + " (" + pb.getRoguePlanebound().planeboundName() + ")");
                    }
                } else if (node instanceof NodeSanctum) {
                    entry.path.add("Sanctum");
                } else if (node instanceof NodeBazaar) {
                    entry.path.add("Bazaar");
                } else if (node instanceof NodeChest) {
                    entry.path.add("Loot");
                } else if (node instanceof NodeEvent) {
                    entry.path.add("Event");
                }
            }
        }

        return entry;
    }

    // Getters
    public String getCommanderName() { return commanderName; }
    public int getAvatarIndex() { return avatarIndex; }
    public String getOutcome() { return outcome; }
    public String getBossOrDefeatedBy() { return bossOrDefeatedBy; }
    public List<String> getPath() { return path != null ? path : new ArrayList<>(); }
    public int getFinalLife() { return finalLife; }
    public int getFinalGold() { return finalGold; }
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
        }

        public EffectSnapshot(String displayText, String tooltipText) {
            this.displayText = displayText;
            this.tooltipText = tooltipText;
        }

        public String getDisplayText() { return displayText; }
        public String getTooltipText() { return tooltipText; }
    }
}
