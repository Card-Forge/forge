package forge.gamemodes.rogue;

import org.apache.commons.lang3.StringUtils;
import org.tinylog.Logger;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.AetherEffect;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.IgnoringXStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tracks persistent meta-progression for Rogue Commander mode.
 * This data persists across all runs and tracks unlocks, achievements, and statistics.
 */
public class RogueMetaProgress {

    private static final String META_PROGRESS_FILE = "meta_progress.dat";
    private static RogueMetaProgress instance;

    // Per-commander tracking
    private Map<String, Integer> runsStartedPerCommander;
    private Map<String, Integer> runsWonPerCommander;

    // Per-Planebound match tracking, keyed by deck path
    private Map<String, PlaneboundStats> planeboundStats;

    // Progression-eligible stats and custom-run stats, keyed by RogueStats.conditionKey
    private Map<String, Integer> statValues;
    private Map<String, Integer> customStatValues;

    // Codex discovery state
    private CodexProgress codexProgress;

    // Per-commander descension tracking
    private Map<String, Integer> maxDescensionWonPerCommander = new HashMap<>();

    // Endless Mode is unlocked by a Custom Run victory. Old saves can also use customStatValues.
    private boolean endlessModeUnlocked;
    private Map<String, Integer> maxEndlessCompletedPerCommander = new HashMap<>();

    // Aether system - persistent Echoes, Sparks, and Aetherworks
    private int totalEchoes;                        // Persistent echo currency
    private int totalSparks;                        // Earned from Descension wins
    private int aetherUpgradeLevel = 0;             // XStream defaults int to 0 for old saves
    private Map<String, Integer> aetherEffectRanks; // Aetherwork ID -> current rank (0 = not unlocked)
    private Set<String> activeAetherEffects;        // Currently active Aetherwork IDs (max slots via getActiveAetherEffectSlots())

    // NPC progression levels (npcId -> level)
    private Map<String, Integer> npcLevels;

    // Optional NPC encounters that have already been delivered
    private Set<String> seenNPCEncounters;

    // Unlock notification tracking - which unlocks have been shown to the player
    private Set<String> notifiedCommanderUnlocks;

    // Tutorial tracking - which tutorials have been shown
    private Set<String> seenTutorials;

    // Run history
    private List<RogueRunHistoryEntry> runHistory;
    private long totalPlaytimeMillis;

    // Dev-only: bypass all commander unlock checks (not serialized)
    private transient boolean devUnlockAll;

    // Private constructor for singleton
    private RogueMetaProgress() {
        runsStartedPerCommander = new HashMap<>();
        runsWonPerCommander = new HashMap<>();
        planeboundStats = new HashMap<>();
        statValues = new HashMap<>();
        customStatValues = new HashMap<>();
        codexProgress = new CodexProgress();

        // Initialize Aether system
        totalEchoes = 0;
        aetherEffectRanks = new HashMap<>();
        activeAetherEffects = new HashSet<>();

        // Initialize unlock notification tracking
        notifiedCommanderUnlocks = new HashSet<>();

        // Initialize tutorial tracking
        seenTutorials = new HashSet<>();

        // Initialize optional NPC encounter tracking
        seenNPCEncounters = new HashSet<>();
    }

    /**
     * Get the singleton instance, loading from disk if needed.
     */
    public static synchronized RogueMetaProgress getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * Force reload from disk (useful after external changes).
     */
    public static synchronized void reload() {
        instance = load();
    }

    /**
     * Reset all statistics to initial values and save.
     */
    public void reset() {
        runsStartedPerCommander = new HashMap<>();
        runsWonPerCommander = new HashMap<>();
        planeboundStats = new HashMap<>();
        statValues = new HashMap<>();
        customStatValues = new HashMap<>();
        maxDescensionWonPerCommander = new HashMap<>();
        endlessModeUnlocked = false;
        maxEndlessCompletedPerCommander = new HashMap<>();
        codexProgress = new CodexProgress();

        // Reset Aether system
        totalEchoes = 0;
        totalSparks = 0;
        aetherUpgradeLevel = 0;
        aetherEffectRanks = new HashMap<>();
        activeAetherEffects = new HashSet<>();
        notifiedCommanderUnlocks = new HashSet<>();
        npcLevels = new HashMap<>();
        seenNPCEncounters = new HashSet<>();

        // Reset run history
        runHistory = new ArrayList<>();
        totalPlaytimeMillis = 0;

        // Note: Tutorials are NOT reset here - use resetTutorials() separately
        save();
    }

    // ==================== Stat System ====================

    /**
     * Get the displayed total for a stat key across normal and custom runs.
     */
    public int getStatValue(String key) {
        int normal = getProgressionStatValue(key);
        int custom = getStatValueForCategory(key, true);
        RogueStats stat = RogueStats.fromKey(key);
        return stat != null && stat.isCounterStat() ? normal + custom : Math.max(normal, custom);
    }

    public int getProgressionStatValue(String key) {
        if (statValues == null) statValues = new HashMap<>();
        return statValues.getOrDefault(key, 0);
    }

    int getStatValueForRun(String key, RogueRun run) {
        return getStatValueForCategory(key, run.isCustomRun());
    }

    private int getStatValueForCategory(String key, boolean customRun) {
        if (!customRun) {
            return getProgressionStatValue(key);
        }
        if (customStatValues == null) customStatValues = new HashMap<>();
        return customStatValues.getOrDefault(key, 0);
    }

    /**
     * Update a stat value if the new value exceeds the stored value.
     * Works for both counters (pass current + 1) and max-value tracking (pass snapshot).
     * Only normal-run values can satisfy unlock conditions.
     */
    public void updateStat(RogueStats stat, int value, RogueRun run) {
        boolean customRun = run.isCustomRun();
        Map<String, Integer> values;
        if (customRun) {
            if (customStatValues == null) customStatValues = new HashMap<>();
            values = customStatValues;
        } else {
            if (statValues == null) statValues = new HashMap<>();
            values = statValues;
        }
        String key = stat.getConditionKey();
        if (value > values.getOrDefault(key, 0)) {
            values.put(key, value);
        }
        if (!customRun) {
            checkForNewUnlocks(stat);
        }
    }

    // ==================== Codex Progress ====================

    CodexProgress getCodexProgress() {
        if (codexProgress == null) {
            codexProgress = new CodexProgress();
        }
        return codexProgress;
    }

    public boolean hasSeenCommanderRewardCard(String commanderName, PaperCard card) {
        return getCodexProgress().hasSeenCommanderRewardCard(commanderName, getNormalizedCardName(card));
    }

    public boolean hasAcquiredCommanderRewardCard(String commanderName, PaperCard card) {
        return getCodexProgress().hasAcquiredCommanderRewardCard(commanderName, getNormalizedCardName(card));
    }

    public boolean hasEncounteredPlanebound(RoguePlanebound planebound) {
        return getCodexProgress().hasEncounteredPlanebound(getPlaneboundKey(planebound));
    }

    public boolean hasSeenPlaneboundCard(RoguePlanebound planebound, PaperCard card) {
        return getCodexProgress().hasSeenPlaneboundCard(getPlaneboundKey(planebound),
            getNormalizedCardName(card));
    }

    public boolean hasSeenTrait(RogueEffect effect) {
        return getCodexProgress().hasSeenTrait(getEffectKey(effect));
    }

    public boolean hasAcquiredTrait(RogueEffect effect) {
        return getCodexProgress().hasAcquiredTrait(getEffectKey(effect));
    }

    void markCommanderRewardCardSeen(String commanderName, PaperCard card) {
        afterCodexProgressChanged(getCodexProgress().markCommanderRewardCardSeen(commanderName,
            getNormalizedCardName(card)));
    }

    void markCommanderRewardCardAcquired(String commanderName, PaperCard card) {
        afterCodexProgressChanged(getCodexProgress().markCommanderRewardCardAcquired(commanderName,
            getNormalizedCardName(card)));
    }

    void markPlaneboundEncountered(RoguePlanebound planebound) {
        afterCodexProgressChanged(getCodexProgress().markPlaneboundEncountered(getPlaneboundKey(planebound)));
    }

    void markPlaneboundCardSeen(RoguePlanebound planebound, PaperCard card) {
        afterCodexProgressChanged(getCodexProgress().markPlaneboundCardSeen(getPlaneboundKey(planebound),
            getNormalizedCardName(card)));
    }

    void markTraitSeen(RogueEffect effect) {
        afterCodexProgressChanged(getCodexProgress().markTraitSeen(getEffectKey(effect)));
    }

    void markTraitAcquired(RogueEffect effect) {
        afterCodexProgressChanged(getCodexProgress().markTraitAcquired(getEffectKey(effect)));
    }

    void updateCodexProgress(Predicate<CodexProgress> updater) {
        if (updater == null) {
            return;
        }
        boolean changed = updater.test(getCodexProgress());
        afterCodexProgressChanged(changed);
    }

    private void afterCodexProgressChanged(boolean changed) {
        if (!changed) {
            return;
        }
        save();
    }

    private static String getNormalizedCardName(PaperCard card) {
        return card == null || card.getRules() == null ? null : card.getRules().getNormalizedName();
    }

    private static String getPlaneboundKey(RoguePlanebound planebound) {
        return planebound == null ? null : planebound.deckPath();
    }

    private static String getEffectKey(RogueEffect effect) {
        return effect == null ? null : effect.getId();
    }

    // ==================== Per-Commander Tracking ====================

    /**
     * Track per-commander run start count.
     */
    public void trackCommanderStarted(RogueRun run) {
        if (!run.isCustomRun()) {
            runsStartedPerCommander.merge(run.getSelectedRogueDeck().getCommanderCardName(), 1, Integer::sum);
        }
    }

    public void mergeRunsWonPerCommander(RogueRun run) {
        if (!run.isCustomRun()) {
            runsWonPerCommander.merge(run.getCurrentCommanderName(), 1, Integer::sum);
        }
    }

    // ==================== Unlock Checking ====================

    /**
     * Check for new unlocks relevant to the given stat.
     */
    void checkForNewUnlocks(RogueStats stat) {
        if (notifiedCommanderUnlocks == null) notifiedCommanderUnlocks = new HashSet<>();
        String key = stat.getConditionKey();

        boolean changed = false;
        for (RogueDeck deck : RogueConfig.loadRogueDecks()) {
            RogueUnlockCondition condition = deck.getUnlockCondition();
            if (condition == null || condition.isDefault() || !condition.hasCondition(key)) {
                continue;
            }

            String name = deck.getCommanderCardName();
            if (deck.isUnlocked() && !notifiedCommanderUnlocks.contains(name)) {
                notifiedCommanderUnlocks.add(name);
                changed = true;

                PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
                ISkinImage image = GuiBase.getInterface()
                    .createLayeredImage(card, FSkinProp.IMG_SPECIAL_TROPHY,
                        ForgeConstants.CACHE_ACHIEVEMENTS_DIR
                            + "/unlock_" + name.replace(" ", "_") + ".png", 1f);
                String unlockDesc = condition.getDescription();
                SoundSystem.instance.play(SoundEffectType.Enchantment,
                    SoundEffectType.Enchantment.isSynced());
                GuiBase.getInterface().showImageDialog(image,
                    name + "\n" + unlockDesc,
                    "Commander Unlocked!");
            }
        }

        if (stat == RogueStats.RUNS_WON && notifyRunModeUnlocks()) {
            changed = true;
        }

        RogueCommanderAchievements.instance.evaluateCommanderUnlockAchievements();

        if (changed) {
            save();
        }
    }

    private boolean notifyRunModeUnlocks() {
        boolean changed = false;
        if (isCustomRunsUnlocked() && notifiedCommanderUnlocks.add("CUSTOM_RUNS")) {
            changed = true;
            GuiBase.getInterface().showImageDialog(null,
                "You have won 2 Runs!\nCustom Runs are now unlocked.",
                "Custom Runs Unlocked!");
        }

        if (isDescensionModeUnlocked() && notifiedCommanderUnlocks.add("DESCENSION_MODE")) {
            changed = true;
            GuiBase.getInterface().showImageDialog(null,
                "You have won Runs with 3 different Commanders!\n" +
                    "Descension Mode is now unlocked.",
                "Descension Mode Unlocked!");
        }
        return changed;
    }

    // ==================== Getters for Unlock Condition Evaluation ====================

    public int getTotalRunsStarted() {
        return getStatValue(RogueStats.RUNS_STARTED.getConditionKey());
    }

    public int getProgressionTotalRunsStarted() {
        return getProgressionStatValue(RogueStats.RUNS_STARTED.getConditionKey());
    }

    public int getTotalRunsCompleted() {
        return getStatValue(RogueStats.RUNS_COMPLETED.getConditionKey());
    }

    public int getProgressionTotalRunsCompleted() {
        return getProgressionStatValue(RogueStats.RUNS_COMPLETED.getConditionKey());
    }

    public int getTotalRunsWon() {
        return getStatValue(RogueStats.RUNS_WON.getConditionKey());
    }

    public int getProgressionTotalRunsWon() {
        return getProgressionStatValue(RogueStats.RUNS_WON.getConditionKey());
    }

    public boolean isCustomRunsUnlocked() {
        return getProgressionTotalRunsWon() >= 2;
    }

    public boolean isEndlessModeUnlocked() {
        return endlessModeUnlocked || getStatValueForCategory(RogueStats.RUNS_WON.getConditionKey(), true) > 0;
    }

    public void unlockEndlessMode() {
        if (!isEndlessModeUnlocked()) {
            endlessModeUnlocked = true;
            save();
        }
        notifyEndlessModeUnlocked();
    }

    public void notifyEndlessModeUnlocked() {
        if (!isEndlessModeUnlocked()) return;
        if (notifiedCommanderUnlocks == null) notifiedCommanderUnlocks = new HashSet<>();
        if (!notifiedCommanderUnlocks.add("ENDLESS_MODE")) return;
        save();
        SoundSystem.instance.play(SoundEffectType.Enchantment,
            SoundEffectType.Enchantment.isSynced());
        GuiBase.getInterface().showImageDialog(null,
            "You have won a Custom Run!\nEndless Mode is now unlocked.",
            "Endless Mode Unlocked!");
    }

    public int getMaxEndlessCompleted(String commanderName) {
        if (maxEndlessCompletedPerCommander == null) maxEndlessCompletedPerCommander = new HashMap<>();
        return maxEndlessCompletedPerCommander.getOrDefault(commanderName, 0);
    }

    public void recordEndlessLevelCompleted(RogueRun run) {
        int level = run.getEndlessLevel();
        if (level <= getMaxEndlessCompleted(run.getCurrentCommanderName())) return;
        maxEndlessCompletedPerCommander.put(run.getCurrentCommanderName(), level);
        save();
    }

    public int getTotalMatchesWon() {
        return getStatValue(RogueStats.MATCHES_WON.getConditionKey());
    }

    public int getTotalMatchesLost() {
        return getStatValue(RogueStats.MATCHES_LOST.getConditionKey());
    }

    public int getRunsWonWithCommander(String commanderName) {
        return runsWonPerCommander.getOrDefault(commanderName, 0);
    }

    public boolean hasWonWithCommander(String commanderName) {
        return runsWonPerCommander.getOrDefault(commanderName, 0) > 0;
    }

    public int getMaxCreatureTypesInDeck() {
        return getStatValue(RogueStats.CREATURE_TYPES.getConditionKey());
    }

    public int getMaxArtifactsInDeck() {
        return getStatValue(RogueStats.MAX_ARTIFACTS.getConditionKey());
    }

    public int getMaxSharedCreatureTypeInDeck() {
        return getStatValue(RogueStats.MAX_SHARED_CREATURE_TYPE.getConditionKey());
    }

    public int getMaxLegendaryPermanentsInDeck() {
        return getStatValue(RogueStats.LEGENDARY_PERMANENTS.getConditionKey());
    }

    public int getMaxNonbasicLandsInDeck() {
        return getStatValue(RogueStats.NONBASIC_LANDS.getConditionKey());
    }

    public int getMaxLifeInRun() {
        return getStatValue(RogueStats.MAX_LIFE.getConditionKey());
    }

    public int getMaxGoldInRun() {
        return getStatValue(RogueStats.MAX_GOLD.getConditionKey());
    }

    public int getRunsStartedWithCommander(String commanderName) {
        return runsStartedPerCommander.getOrDefault(commanderName, 0);
    }

    public int getDistinctCommandersWon() {
        int count = 0;
        for (int wins : runsWonPerCommander.values()) {
            if (wins > 0) count++;
        }
        return count;
    }

    // ==================== Per-Planebound Tracking ====================

    void recordPlaneboundResult(RoguePlanebound planebound, boolean won) {
        String key = getPlaneboundKey(planebound);
        if (StringUtils.isBlank(key)) {
            return;
        }
        if (planeboundStats == null) {
            planeboundStats = new HashMap<>();
        }
        PlaneboundStats stats = planeboundStats.computeIfAbsent(key, ignored -> new PlaneboundStats());
        if (won) {
            stats.wins++;
        } else {
            stats.losses++;
        }
    }

    public int getPlaneboundWins(RoguePlanebound planebound) {
        if (planeboundStats == null) {
            return 0;
        }
        PlaneboundStats stats = planeboundStats.get(getPlaneboundKey(planebound));
        return stats == null ? 0 : stats.wins;
    }

    public int getPlaneboundLosses(RoguePlanebound planebound) {
        if (planeboundStats == null) {
            return 0;
        }
        PlaneboundStats stats = planeboundStats.get(getPlaneboundKey(planebound));
        return stats == null ? 0 : stats.losses;
    }

    // ==================== Descension Management ====================

    public int getMaxDescensionWon(String commanderName) {
        if (maxDescensionWonPerCommander == null) maxDescensionWonPerCommander = new HashMap<>();
        return maxDescensionWonPerCommander.getOrDefault(commanderName, 0);
    }

    public int getHighestDescensionWon() {
        if (maxDescensionWonPerCommander == null) return 0;
        return maxDescensionWonPerCommander.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /** Returns max descension level available to START. Normal win = Level 1 unlocked. */
    public int getMaxDescensionUnlocked(String commanderName) {
        if (!hasWonWithCommander(commanderName)) return 0;
        return 1 + getMaxDescensionWon(commanderName);
    }

    public void notifyDescensionL1IfFirstWin(RogueRun run) {
        if (run.isCustomRun()) return;
        String commanderName = run.getCurrentCommanderName();
        if (!isDescensionModeUnlocked() || getRunsWonWithCommander(commanderName) != 1) return;
        forge.item.PaperCard card = forge.model.FModel.getMagicDb().getCommonCards().getCard(commanderName);
        forge.localinstance.skin.ISkinImage image = forge.gui.GuiBase.getInterface()
            .createLayeredImage(card, forge.localinstance.skin.FSkinProp.IMG_SPECIAL_TROPHY,
                forge.localinstance.properties.ForgeConstants.CACHE_ACHIEVEMENTS_DIR
                    + "/descension_" + commanderName.replace(" ", "_") + "_1.png", 1f);
        showDescensionUnlockDialog(image,
            "You unlocked Descension Mode for " + commanderName + "!",
            "Descension Level 1 Unlocked!");
    }

    public boolean recordDescensionWin(RogueRun run) {
        if (run.isCustomRun() || run.getDescensionLevel() == 0) return false;
        String commanderName = run.getCurrentCommanderName();
        int level = run.getDescensionLevel();
        if (maxDescensionWonPerCommander == null) maxDescensionWonPerCommander = new HashMap<>();
        if (level > getMaxDescensionWon(commanderName)) {
            maxDescensionWonPerCommander.put(commanderName, level);
            totalSparks++;
            int unlockedLevel = level + 1;
            if (unlockedLevel <= DescensionLevel.getMaxLevel()) {
                DescensionLevel descensionLevel = DescensionLevel.forLevel(unlockedLevel);
                forge.item.PaperCard card = forge.model.FModel.getMagicDb()
                    .getCommonCards().getCard(commanderName);
                forge.localinstance.skin.ISkinImage image = forge.gui.GuiBase.getInterface()
                    .createLayeredImage(card, forge.localinstance.skin.FSkinProp.IMG_SPECIAL_TROPHY,
                        forge.localinstance.properties.ForgeConstants.CACHE_ACHIEVEMENTS_DIR
                            + "/descension_" + commanderName.replace(" ", "_") + "_" + unlockedLevel + ".png", 1f);
              if (descensionLevel == null) return false;
              showDescensionUnlockDialog(image,
                    "You unlocked Descension Level " + unlockedLevel + ": " + descensionLevel.name + " for " + commanderName + "!",
                    "Descension Level " + unlockedLevel + " Unlocked!");
            }
            save();
            return true;
        }
        return false;
    }

    private static void showDescensionUnlockDialog(ISkinImage image, String message, String title) {
        FThreads.invokeInEdtLater(() -> GuiBase.getInterface().showImageDialog(image, message, title));
    }

    public boolean isDescensionModeUnlocked() {
        return getDistinctCommandersWon() >= 3;
    }

    public boolean isDevUnlockAll() { return devUnlockAll; }
    public void setDevUnlockAll(boolean value) { devUnlockAll = value; }

    // ==================== Aether System ====================

    public int getTotalEchoes() {
        return totalEchoes;
    }

    public void setTotalEchoes(int echoes) {
        this.totalEchoes = Math.max(0, echoes);
    }

    public int getTotalSparks() {
        return totalSparks;
    }

    public int getAetherUpgradeLevel() {
        return aetherUpgradeLevel;
    }

    public void setAetherUpgradeLevel(int level) {
        aetherUpgradeLevel = level;
        RogueCommanderAchievements.instance.evaluateUpgradeAchievements(this);
        save();
    }

    /**
     * Purchase the next Aether Upgrade in sequence (must be purchased in order).
     * @param level The level to purchase (must equal current level + 1)
     * @return true if purchase succeeded
     */
    public boolean purchaseAetherUpgrade(int level) {
        AetherUpgrade upgrade = AetherUpgrade.forLevel(level);
        if (upgrade == null || aetherUpgradeLevel >= level || level != aetherUpgradeLevel + 1) return false;
        if (totalSparks < upgrade.sparkCost || totalEchoes < upgrade.echoCost) return false;
        totalSparks -= upgrade.sparkCost;
        totalEchoes -= upgrade.echoCost;
        aetherUpgradeLevel = level;
        RogueCommanderAchievements.instance.evaluateUpgradeAchievements(this);
        save();
        return true;
    }

    /**
     * Get the number of active Aetherwork slots from base capacity plus purchased Aether Upgrades.
     */
    public int getActiveAetherEffectSlots() {
        int slots = 3;
        for (int l = 1; l <= aetherUpgradeLevel; l++) {
            AetherUpgrade u = AetherUpgrade.forLevel(l);
            if (u != null) slots += u.extraEnergy;
        }
        return slots;
    }

    public void addEchoes(int amount) {
        if (amount > 0) {
            totalEchoes += amount;
            save();
        }
    }

    public void addSparks(int amount) {
        if (amount > 0) {
            totalSparks += amount;
            save();
        }
    }

    /**
     * Get the current rank of an Aetherwork (0 = not unlocked).
     */
    public int getAetherEffectRank(AetherEffect aetherEffect) {
        if (aetherEffectRanks == null) {
            aetherEffectRanks = new HashMap<>();
        }
        return aetherEffectRanks.getOrDefault(aetherEffect.getId(), 0);
    }

    /**
     * Attempt to upgrade an Aetherwork to the next rank.
     * @return true if upgrade was successful, false if not enough echoes or already max rank
     */
    public boolean upgradeAetherEffect(AetherEffect aetherEffect) {
        if (aetherEffectRanks == null) {
            aetherEffectRanks = new HashMap<>();
        }

        if (!aetherEffect.isAccessibleAt(aetherUpgradeLevel)) {
            return false;
        }

        int currentRank = getAetherEffectRank(aetherEffect);
        if (currentRank >= aetherEffect.getEffectiveMaxRank(aetherUpgradeLevel)) {
            return false; // Already max rank
        }

        int cost = aetherEffect.getEchoCostForRank(currentRank + 1);
        if (totalEchoes < cost) {
            return false; // Not enough echoes
        }

        totalEchoes -= cost;
        aetherEffectRanks.put(aetherEffect.getId(), currentRank + 1);
        save();
        return true;
    }

    /**
     * Get all currently active Aetherworks.
     */
    public Set<AetherEffect> getActiveAetherEffects() {
        Set<AetherEffect> active = new HashSet<>();
        if (activeAetherEffects == null) {
            activeAetherEffects = new HashSet<>();
            return active;
        }
        for (String id : activeAetherEffects) {
            AetherEffect aetherEffect = AetherEffect.fromId(id);
            if (aetherEffect != null) {
                active.add(aetherEffect);
            }
        }
        return active;
    }

    /**
     * Get the count of currently active Aetherworks.
     */
    public int getActiveAetherEffectCount() {
        if (activeAetherEffects == null) {
            activeAetherEffects = new HashSet<>();
        }
        return activeAetherEffects.size();
    }

    /**
     * Activate an Aetherwork (maximum determined by getActiveAetherEffectSlots()).
     */
    public void activateAetherEffect(AetherEffect aetherEffect) {
        if (activeAetherEffects == null) {
            activeAetherEffects = new HashSet<>();
        }

        // Must be accessible at current upgrade level
        if (!aetherEffect.isAccessibleAt(aetherUpgradeLevel)) {
            return;
        }

        // Must be unlocked (rank > 0)
        if (getAetherEffectRank(aetherEffect) == 0) {
            return;
        }

        // If already active, nothing to do
        if (activeAetherEffects.contains(aetherEffect.getId())) {
            return;
        }

        // Check Aether Energy capacity
        if (activeAetherEffects.size() >= getActiveAetherEffectSlots()) {
            return;
        }

        activeAetherEffects.add(aetherEffect.getId());
        save();
    }

    /**
     * Deactivate an Aetherwork.
     */
    public void deactivateAetherEffect(AetherEffect aetherEffect) {
        if (activeAetherEffects == null) {
            activeAetherEffects = new HashSet<>();
        }
        activeAetherEffects.remove(aetherEffect.getId());
        save();
    }

    /**
     * Reset all Aetherworks to rank 0 and refund all spent Echoes.
     * @return The amount of echoes refunded
     */
    public int resetAetherEffects() {
        if (aetherEffectRanks == null) {
            aetherEffectRanks = new HashMap<>();
        }
        if (activeAetherEffects == null) {
            activeAetherEffects = new HashSet<>();
        }

        // Calculate total Echoes spent on all Aetherworks
        int refund = 0;
        for (AetherEffect aetherEffect : AetherEffect.values()) {
            int rank = getAetherEffectRank(aetherEffect);
            // Sum costs for each rank from 1 to current rank
            for (int r = 1; r <= rank; r++) {
                refund += aetherEffect.getEchoCostForRank(r);
            }
        }

        // Refund echoes
        totalEchoes += refund;

        // Clear all Aetherwork data
        aetherEffectRanks.clear();
        activeAetherEffects.clear();

        save();
        return refund;
    }

    // ==================== Run History ====================

    public List<RogueRunHistoryEntry> getRunHistory() {
        if (runHistory == null) {
            runHistory = new ArrayList<>();
        }
        return runHistory;
    }

    public long getTotalPlaytimeMillis() {
        return totalPlaytimeMillis;
    }

    public void addRunHistoryEntry(RogueRunHistoryEntry entry) {
        if (runHistory == null) {
            runHistory = new ArrayList<>();
        }
        runHistory.add(entry);
        totalPlaytimeMillis += Math.max(0, entry.getRunTimeMillis());
        save();
    }

    public void clearRunHistory() {
        runHistory = new ArrayList<>();
        save();
    }

    // ==================== Tutorial Tracking ====================

    /**
     * Check if a tutorial has been seen.
     */
    public boolean hasSeenTutorial(RogueTutorial tutorial) {
        if (seenTutorials == null) {
            seenTutorials = new HashSet<>();
        }
        return seenTutorials.contains(tutorial.getId());
    }

    /**
     * Mark a tutorial as seen.
     */
    public void markTutorialSeen(RogueTutorial tutorial) {
        if (seenTutorials == null) {
            seenTutorials = new HashSet<>();
        }
        seenTutorials.add(tutorial.getId());
        save();
    }

    /**
     * Reset all tutorials to unseen state.
     */
    public void resetTutorials() {
        if (seenTutorials == null) {
            seenTutorials = new HashSet<>();
        }
        seenTutorials.clear();
        save();
    }

    public static class CodexProgress {
        private Map<String, CardDiscovery> commanderRewardCards = new HashMap<>();
        private Map<String, CardDiscovery> planeboundCards = new HashMap<>();
        private Set<String> encounteredPlanebounds = new HashSet<>();
        private CardDiscovery traits = new CardDiscovery();

        private boolean hasSeenCommanderRewardCard(String commanderName, String cardName) {
            return getCommanderCardDiscovery(commanderName, false).hasSeen(cardName);
        }

        private boolean hasAcquiredCommanderRewardCard(String commanderName, String cardName) {
            return getCommanderCardDiscovery(commanderName, false).hasAcquired(cardName);
        }

        private boolean hasEncounteredPlanebound(String planeboundKey) {
            if (encounteredPlanebounds == null) {
                encounteredPlanebounds = new HashSet<>();
            }
            return planeboundKey != null && encounteredPlanebounds.contains(planeboundKey);
        }

        private boolean hasSeenPlaneboundCard(String planeboundKey, String cardName) {
            return getPlaneboundCardDiscovery(planeboundKey, false).hasSeen(cardName);
        }

        private boolean hasSeenTrait(String effectId) {
            return getTraitDiscovery().hasSeen(effectId);
        }

        private boolean hasAcquiredTrait(String effectId) {
            return getTraitDiscovery().hasAcquired(effectId);
        }

        boolean markCommanderRewardCardSeen(String commanderName, String cardName) {
            if (StringUtils.isBlank(commanderName)) {
                return false;
            }
            return getCommanderCardDiscovery(commanderName, true).markSeen(cardName);
        }

        boolean markCommanderRewardCardAcquired(String commanderName, String cardName) {
            if (StringUtils.isBlank(commanderName)) {
                return false;
            }
            return getCommanderCardDiscovery(commanderName, true).markAcquired(cardName);
        }

        boolean markPlaneboundEncountered(String planeboundKey) {
            if (StringUtils.isBlank(planeboundKey)) {
                return false;
            }
            if (encounteredPlanebounds == null) {
                encounteredPlanebounds = new HashSet<>();
            }
            return encounteredPlanebounds.add(planeboundKey);
        }

        boolean markPlaneboundCardSeen(String planeboundKey, String cardName) {
            if (StringUtils.isBlank(planeboundKey)) {
                return false;
            }
            return getPlaneboundCardDiscovery(planeboundKey, true).markSeen(cardName);
        }

        boolean markTraitSeen(String effectId) {
            return getTraitDiscovery().markSeen(effectId);
        }

        boolean markTraitAcquired(String effectId) {
            return getTraitDiscovery().markAcquired(effectId);
        }

        private CardDiscovery getCommanderCardDiscovery(String sourceKey, boolean create) {
            if (commanderRewardCards == null) {
                commanderRewardCards = new HashMap<>();
            }
            return getCardDiscovery(commanderRewardCards, sourceKey, create);
        }

        private CardDiscovery getPlaneboundCardDiscovery(String sourceKey, boolean create) {
            if (planeboundCards == null) {
                planeboundCards = new HashMap<>();
            }
            return getCardDiscovery(planeboundCards, sourceKey, create);
        }

        private CardDiscovery getCardDiscovery(Map<String, CardDiscovery> discoveryMap, String sourceKey,
                                               boolean create) {
            if (StringUtils.isBlank(sourceKey)) {
                return CardDiscovery.EMPTY;
            }
            if (!create) {
                CardDiscovery discovery = discoveryMap.get(sourceKey);
                return discovery == null ? CardDiscovery.EMPTY : discovery;
            }
            return discoveryMap.computeIfAbsent(sourceKey, key -> new CardDiscovery());
        }

        private CardDiscovery getTraitDiscovery() {
            if (traits == null) {
                traits = new CardDiscovery();
            }
            return traits;
        }
    }

    private static class PlaneboundStats {
        private int wins;
        private int losses;
    }

    public static class CardDiscovery {
        private static final CardDiscovery EMPTY = new CardDiscovery();

        private Set<String> seen = new HashSet<>();
        private Set<String> acquired = new HashSet<>();

        private boolean hasSeen(String cardName) {
            if (seen == null) {
                seen = new HashSet<>();
            }
            return cardName != null && seen.contains(cardName);
        }

        private boolean hasAcquired(String cardName) {
            if (acquired == null) {
                acquired = new HashSet<>();
            }
            return cardName != null && acquired.contains(cardName);
        }

        private boolean markSeen(String cardName) {
            if (StringUtils.isBlank(cardName)) {
                return false;
            }
            if (seen == null) {
                seen = new HashSet<>();
            }
            return seen.add(cardName);
        }

        private boolean markAcquired(String cardName) {
            if (StringUtils.isBlank(cardName)) {
                return false;
            }
            if (seen == null) {
                seen = new HashSet<>();
            }
            if (acquired == null) {
                acquired = new HashSet<>();
            }
            boolean changed = seen.add(cardName);
            changed |= acquired.add(cardName);
            return changed;
        }
    }

    // ==================== NPC Progression ====================

    public int getNPCLevel(String id) {
        if (npcLevels == null) npcLevels = new HashMap<>();
        return npcLevels.getOrDefault(id, 0);
    }

    public void setNPCLevelIfHigher(String id, int level) {
        if (npcLevels == null) npcLevels = new HashMap<>();
        if (level > npcLevels.getOrDefault(id, 0)) {
            npcLevels.put(id, level);
            save();
        }
    }

    /**
     * Sets the stored NPC progression level directly.
     */
    public void setNPCLevel(String id, int level) {
        if (npcLevels == null) npcLevels = new HashMap<>();
        if (level <= 0) {
            npcLevels.remove(id);
        } else {
            npcLevels.put(id, level);
        }
        RogueCommanderAchievements.instance.evaluateUpgradeAchievements(this);
        save();
    }

    public boolean hasSeenNPCEncounter(String encounterId) {
        if (seenNPCEncounters == null) seenNPCEncounters = new HashSet<>();
        return seenNPCEncounters.contains(encounterId);
    }

    public void markNPCEncounterSeen(String encounterId) {
        if (seenNPCEncounters == null) seenNPCEncounters = new HashSet<>();
        if (seenNPCEncounters.add(encounterId)) {
            save();
        }
    }

    // ==================== Persistence ====================

    private static File getMetaProgressFile() {
        return new File(RogueIO.ROGUE_SAVE_DIR, META_PROGRESS_FILE);
    }

    private static XStream getSerializer() {
        XStream xStream = new IgnoringXStream();
        xStream.addPermission(NoTypePermission.NONE);
        xStream.addPermission(NullPermission.NULL);
        xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
        xStream.allowTypeHierarchy(String.class);
        xStream.allowTypeHierarchy(HashMap.class);
        xStream.allowTypeHierarchy(HashSet.class);
        xStream.allowTypeHierarchy(ArrayList.class);
        xStream.allowTypeHierarchy(Map.class);
        xStream.allowTypeHierarchy(Set.class);
        xStream.allowTypeHierarchy(List.class);
        xStream.allowTypeHierarchy(RogueMetaProgress.class);
        xStream.allowTypeHierarchy(CodexProgress.class);
        xStream.allowTypeHierarchy(CardDiscovery.class);
        xStream.allowTypeHierarchy(PlaneboundStats.class);
        xStream.allowTypesByWildcard(new String[] {
            RogueMetaProgress.class.getPackage().getName() + ".*",
            "forge.deck.*",
            "forge.item.*",
            "forge.card.*",
            "java.util.*",
            "java.lang.*"
        });
        xStream.ignoreUnknownElements();
        xStream.autodetectAnnotations(true);
        return xStream;
    }

    private static RogueMetaProgress load() {
        File file = getMetaProgressFile();
        if (!file.exists()) {
            return new RogueMetaProgress();
        }

        try (GZIPInputStream zin = new GZIPInputStream(Files.newInputStream(file.toPath()));
             InputStreamReader reader = new InputStreamReader(zin)) {
            return (RogueMetaProgress) getSerializer().fromXML(reader);
        } catch (Exception e) {
            Logger.error(e, "Error loading Rogue meta progress from {}, starting fresh", file);
            return new RogueMetaProgress();
        }
    }

    public void save() {
        File file = getMetaProgressFile();
        try {
            Files.createDirectories(file.toPath().getParent());
            savePacked(file);
        } catch (Exception e) {
            Logger.error(e, "Error saving Rogue meta progress: {}", file);
        }
    }

    private void savePacked(File file) throws IOException {
        try (BufferedOutputStream bout = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
             GZIPOutputStream zout = new GZIPOutputStream(bout)) {
            getSerializer().toXML(this, zout);
            zout.flush();
        }
    }
}
