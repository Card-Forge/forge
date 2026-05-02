package forge.adventure.data;

import forge.StaticData;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.*;
import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;

import java.util.*;

// This class will keep track of data relevant for the Archipelago implementation
// Persists and loads data inside/from the user's save file
public class ArchipelagoData implements SaveFileContent {
    private static ArchipelagoData instance = null;
    private ArchipelagoMode archipelagoMode = ArchipelagoMode.disabled;

    // Data we need from Forge
    private final CardEdition.Collection allEditions = StaticData.instance().getEditions();
    private final Iterable<CardEdition> allOrderedEditions = allEditions.getOrderedEditions();
    // Todo: This works fine for singleplayer even when updates come out but the fact that the list of all sets can grow will cause problems in Archipelago due to a variable amount of checks.
    private final Set<String> allCardSets = new HashSet<>();
    // List of teleportation runes that we use to gate regions
    private final Set<String> regionTeleportingRunes = new HashSet<>(Arrays.asList("White rune","Black rune","Blue rune","Red rune","Green rune"));
    // List of known main bosses that contribute to APWorld completion
    private final Set<String> mainBosses = new HashSet<>(Arrays.asList("Lorthos","Emrakul","Lathliss","Ghalta","Griselbrand","Akroma","Sliver Queen"));

    // Actual user data we want to store
    private final Map<String, Long> completedTownInnEvents = new HashMap<>();
    private final Map<String, Long> completedTownQuests = new HashMap<>();
    private final Map<String, Long> cardsEarnedByRarity = new HashMap<>();
    private final Map<String, Long> itemsGainedByName = new HashMap<>();
    private final Map<String, Long> packsEarnedBySet = new HashMap<>();
    private final Set<String> cardsUnlockedByName = new HashSet<>();
    private final Set<String> setsUnlockedByCode = new HashSet<>();
    private final Set<String> bossesDefeatedByName = new HashSet<>();
    private final Set<String> miniBossesDefeatedByName = new HashSet<>();
    private final Set<String> lockedWorldRegionsByName = new HashSet<>();
    private int lastArchipelagoRewardIndex = 0;
    private int totalGoldEarned = 0;
    private int totalExtraMaxLifeEarned = 0;
    private int totalShardsEarned = 0;
    private int totalBattlesWon = 0;

    // List of unlockable checks
    // Todo: Fill list based on archipelago xml contents
    private int receivedAmountOfSetUnlockChecks = 0;
    private float setUnlockChecksRestAmount = 0;

    private final Set<String> listOfUnlockableItems = new HashSet<>();
    private final int totalAmountOfSetUnlockChecks = 100; // Todo: This should be set based on the value we receive in the APWorld
    private final int totalBattlesWonBreakpoint = 3; // Reward for every 3 battles won.
    private final int totalTownQuestsAndEventsBreakpoint = 2; // Reward for every 2 town events or quests done.
    private final int totalCardsEarnedBreakPoint = 80; // Reward for every 80 unique cards gained.

    public enum ARCHIPELAGO_CHECK_TYPES {BATTLES_WON, TOWN_QUESTS_AND_EVENTS_DONE, TOTAL_CARDS_EARNED, BOSS_WHITE_DEFEATED, BOSS_BLUE_DEFEATED, BOSS_BLACK_DEFEATED, BOSS_RED_DEFEATED, BOSS_GREEN_DEFEATED, BOSS_COLORLESS_DEFEATED, BOSS_WUBRG_DEFEATED, WIN_CONDITION_CLEARED};

    public ArchipelagoData() {
        instance = this;
    }

    public static ArchipelagoData getInstance() {
        return instance == null ? instance = new ArchipelagoData() : instance;
    }

    // Keep this updated to reset any sets/maps/variables
    public void setupFreshSaveFile(ArchipelagoMode archipelagoMode) {
        GameHUD.getInstance().setApButtonVisibility(archipelagoMode == ArchipelagoMode.networked_archipelago);
        cardsUnlockedByName.clear();
        this.addCardUnlockedByName("Plains");
        this.addCardUnlockedByName("Forest");
        this.addCardUnlockedByName("Swamp");
        this.addCardUnlockedByName("Mountain");
        this.addCardUnlockedByName("Island");
        this.addCardUnlockedByName("Wastes");

        completedTownInnEvents.clear();
        completedTownQuests.clear();
        cardsEarnedByRarity.clear();
        itemsGainedByName.clear();
        packsEarnedBySet.clear();

        setsUnlockedByCode.clear();
        bossesDefeatedByName.clear();
        miniBossesDefeatedByName.clear();
        lockedWorldRegionsByName.clear();
        lockedWorldRegionsByName.addAll(new HashSet<>(Arrays.asList("white","blue","black","red","green")));

        lastArchipelagoRewardIndex = 0;
        totalGoldEarned = 0;
        totalExtraMaxLifeEarned = 0;
        totalShardsEarned = 0;
        totalBattlesWon = 0;

        receivedAmountOfSetUnlockChecks = 0;
        setUnlockChecksRestAmount = 0f;

        this.archipelagoMode = archipelagoMode;

        loadAllAvailableSets();
    }

    private void updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES type) {
        if (archipelagoMode == ArchipelagoMode.disabled) return;
        switch (type) {
            case BATTLES_WON -> {
                if (totalBattlesWon > 0 && totalBattlesWon % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case TOWN_QUESTS_AND_EVENTS_DONE -> {
                int totalTownQuestsAndEventsDone = 0;
                for (long count : completedTownInnEvents.values()) {
                    totalTownQuestsAndEventsDone += (int) count;
                }
                for (long count : completedTownQuests.values()) {
                    totalTownQuestsAndEventsDone += (int) count;
                }
                if (totalTownQuestsAndEventsDone > 0 && totalTownQuestsAndEventsDone % totalTownQuestsAndEventsBreakpoint == 0) {
                    unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest/event location is triggered
            }
            case TOTAL_CARDS_EARNED -> {
                long totalCardsEarned = 0;
                for (long value : cardsEarnedByRarity.values()) {
                    totalCardsEarned += value;
                }
                if (totalCardsEarned > 0 && totalCardsEarned % totalCardsEarnedBreakPoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next card location is triggered
            }
            case BOSS_WHITE_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_BLUE_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_BLACK_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_RED_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_GREEN_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_COLORLESS_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case BOSS_WUBRG_DEFEATED -> {
                // Todo: Signal the APWorld that the boss is defeated
            }
            case  WIN_CONDITION_CLEARED -> {
                // Todo: Signal the APWorld that the win condition is reached
            }
        }
    }

    public boolean isSetUnlocked(String setCode) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (setCode == null || !setsUnlockedByCode.contains(setCode)){
            return false;
        } else {
            return true;
        }
    }

    public boolean isRegionUnlocked(String regionName) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (lockedWorldRegionsByName.contains(regionName)) {
            return false;
        }
        return true;
    }

    public ArchipelagoMode getArchipelagoMode() {
        return archipelagoMode;
    }

    public boolean checkCardUnlocked(PaperCard card) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (card == null || card.getName() == null) {
            // If we don't have a valid card or cardname, just ignore it meaning returning true in this case.
            return true;
        }
        String cardName = card.getName();

        // Card explicitly unlocked
        if (cardsUnlockedByName.contains(cardName)) {
            return true;
        }

        // Card sets unlocked
        String setCode = card.getEdition();

        if (isSetUnlocked(setCode)) {
            return true;
        }

        // Neither card nor set is unlocked
        return false;
    }

    public boolean checkDeckUnlocked(Deck selectedDeck) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (selectedDeck == null) {
            return true;
        }

        CardPool pool = selectedDeck.getAllCardsInASinglePool(true, true);
        for (PaperCard card : pool.toFlatList()) {
            if (!checkCardUnlocked(card)) return false;
        }

        return true;
    }

    /// --- The checks below are mostly functional offline and should not be called from the networked part of the AP implementation. ---
    private void unlockSetByName(String setToUnlock) {
        addSetUnlockedByCode(setToUnlock);
        String setUnlockedText = "FORGE_ARCHIPELAGO: CARD SET REWARD: " + setToUnlock;
        // Some sets don't have booster packs such as full-art land sets (P23).
        var booster = StaticData.instance().getBoosters().get(setToUnlock);
        if (booster != null) {
            Current.player().addBooster(AdventureEventController.instance().generateBooster(setToUnlock));
            setUnlockedText = "FORGE_ARCHIPELAGO: CARD SET REWARD + BOOSTER DETECTED: " + setToUnlock;
        }
        System.out.println(setUnlockedText);
        if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
            generateGameNotification(setUnlockedText);
        }
    }

    public void unlockRandomSet() {
        // Subtract unlocked sets from full list.
        Set<String> lockedSets = new HashSet<>(allCardSets);
        lockedSets.removeAll(setsUnlockedByCode);

        // Nothing left to unlock
        if (lockedSets.isEmpty()) {
            return;
        }

        // If we already received all checks, unlock the entire list of locked sets
        if (receivedAmountOfSetUnlockChecks >= totalAmountOfSetUnlockChecks) {
            for (String set : lockedSets) {
                unlockSetByName(set);
            }
        }

        float amountOfSetsToUnlock = (float) allCardSets.size() / totalAmountOfSetUnlockChecks + setUnlockChecksRestAmount;
        int amountOfSetsToUnlockFloored = (int) Math.floor(amountOfSetsToUnlock);
        setUnlockChecksRestAmount = (amountOfSetsToUnlock - amountOfSetsToUnlockFloored);
        for (int i = 0; i < amountOfSetsToUnlockFloored; i++) {
            int targetSetIndex = new Random().nextInt(lockedSets.size());
            String setToUnlock = null;

            int setIndex = 0;
            for (String set : lockedSets) {
                if (setIndex++ == targetSetIndex) {
                    setToUnlock = set;
                    break;
                }
            }

            if (setToUnlock != null) {
                unlockSetByName(setToUnlock);
                lockedSets.remove(setToUnlock);
            }
        }
        receivedAmountOfSetUnlockChecks++;
    }

    public void unlockRegionByName(String regionName) {
        lockedWorldRegionsByName.remove(regionName);
    }

    public void unlockRandomRegion() {
        if (lockedWorldRegionsByName.isEmpty()) {
            return;
        }

        int targetRegionIndex = new Random().nextInt(lockedWorldRegionsByName.size());
        int setIndex = 0;
        for (String region : lockedWorldRegionsByName) {
            if (setIndex++ == targetRegionIndex) {
                for (String runeName : regionTeleportingRunes) {
                    if (runeName.toLowerCase().contains(region.toLowerCase())) {
                        Current.player().addItem(runeName);
                        return;
                    }
                }
            }
        }
    }
    /// --- End ---

    /// --- The checks below can be called from the networked part of the AP implementation. Note that `setLastArchipelagoRewardIndex` must be called manually. ---
    /// Todo: Add custom pop-up message to be shown upon receiving a check.
    public void unlockManaCrystalReward(Integer amount) {
        Current.player().addShards(amount);
        addShards(amount);
    }

    public void unlockGoldReward(int amount) {
        Current.player().giveGold(amount);
        addGold(amount);
    }

    // Todo: Verify that this is actually what we want and it's working
    public void unlockChallengeCoinReward(Map<String, Integer> itemNamesAndAmounts) {
        for (Map.Entry<String, Integer> item : itemNamesAndAmounts.entrySet()) {
            for (int i = 0; i < item.getValue(); i++) {
                Current.player().addItem(item.getKey());
                addItem(item.getKey());
            }
        }
    }

    public void unlockItemReward(String itemName) {
        Current.player().addItem(itemName);
        addItem(itemName);
    }

    // Todo: This should be called by the networked part of the AP implementation when we receive a reward.
    public void incrementLastArchipelagoRewardIndex() {
        lastArchipelagoRewardIndex++;
    }

    public void generateGameNotification(String message) {
        GameHUD.getInstance().addNotification(message, 0.5f, 3f, 0.5f);
    }
    /// --- End ---

    /// --- The functions below are responsible for mutating the user data we store in the save file ---
    // Todo: Make the functions below private where possible, rewrite other code to account for this.
    // Todo: Make sure the functions below trigger all related checks in `updatePlayerChecks` when applicable.
    // Todo: Defeating a (mini-)boss should probably count as a check.
    // Note that the name of a boss is not unique so we'll need to filter from all enemies which have a `boss` value of `true`.
    // Returns `true` if the boss was not already defeated before.
    public boolean addMiniBossDefeated(String miniBossName) {
        return miniBossesDefeatedByName.add(miniBossName);
    }

    public boolean addBossDefeated(String bossName) {
        boolean result = bossesDefeatedByName.add(bossName);
        switch (bossName.toLowerCase()) {
            case "akroma":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_WHITE_DEFEATED);
            case "lorthos":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_BLUE_DEFEATED);
            case "griselbrand":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_BLACK_DEFEATED);
            case "lathliss":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_RED_DEFEATED);
            case "ghalta":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_GREEN_DEFEATED);
            case "emrakul":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_COLORLESS_DEFEATED);
            case "sliver queen":
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_WUBRG_DEFEATED);
        }
        // Win condition is reached if all bosses have been defeated.
        if (bossesDefeatedByName.containsAll(mainBosses)) {
            updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.WIN_CONDITION_CLEARED);
        }
        return result;
    }

    public boolean addCardUnlockedByName(String cardName) {
        return cardsUnlockedByName.add(cardName);
    }

    public boolean addSetUnlockedByCode(String setCode) {
        return setsUnlockedByCode.add(setCode);
    }

    public void addCompletedTownInnEvents() {
        String townName = TileMapScene.instance().rootPoint.getDisplayName();
        completedTownInnEvents.merge(townName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: INN EVENT COMPLETION DETECTED: " + townName + " - " + completedTownInnEvents.get(townName));
        updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.TOWN_QUESTS_AND_EVENTS_DONE);
    }

    public void addCompletedQuests(AdventureQuestEvent event) {
        String townName = event.poi.getDisplayName();
        completedTownQuests.merge(townName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + completedTownQuests.get(townName));
        updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.TOWN_QUESTS_AND_EVENTS_DONE);
    }

    public void addCardByRarity(String rarity) {
        cardsEarnedByRarity.merge(rarity, 1L, Long::sum);
        // Todo: This method will be called a lot when we receive a large batch of cards, make sure this doesn't cause too much slowdown.
        updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.TOTAL_CARDS_EARNED);
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: GOLD REWARD DETECTED: " + amount);
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    // Todo: Verify that item names are unique.
    public void addItem(String itemName) {
        if (regionTeleportingRunes.contains(itemName)) {
            // Unlock the region based on the color found in the itemName
            if (itemName.toLowerCase().contains("white")) {
                unlockRegionByName("white");
            } else if (itemName.toLowerCase().contains("blue")) {
                unlockRegionByName("blue");
            } else if (itemName.toLowerCase().contains("black")) {
                unlockRegionByName("black");
            } else if (itemName.toLowerCase().contains("red")) {
                unlockRegionByName("red");
            } else if (itemName.toLowerCase().contains("green")) {
                unlockRegionByName("green");
            }
            String regionUnlockMessage = "FORGE_ARCHIPELAGO: REGION REWARD DETECTED: " + itemName;
            System.out.println(regionUnlockMessage);
            if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
                generateGameNotification(regionUnlockMessage);
            }
        }
        itemsGainedByName.merge(itemName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: ITEM REWARD DETECTED: " + itemName);
    }

    public void addPack(String boosterPackName) {
        packsEarnedBySet.merge(boosterPackName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: CARD PACK REWARD DETECTED: +" + boosterPackName);
    }

    public void addMaxLife(int amount) {
        totalExtraMaxLifeEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: MAX LIFE REWARD DETECTED: +" + amount);
    }

    public void addShards(int amount) {
        totalShardsEarned += amount;
        System.out.println("FORGE_ARCHIPELAGO: SHARD REWARD DETECTED: +" + amount);
    }

    public void addTotalBattlesWon(int amount) {
        totalBattlesWon += amount;
        updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BATTLES_WON);
    }
    /// --- End ---

    /// --- Helper functions for saving and loading ---
    private static void saveStringSet(SaveFileData parent, String key, Set<String> set) {
        parent.storeObject(key, set.toArray(new String[0]));
    }

    private static void loadStringSet(SaveFileData parent, String key, Set<String> set) {
        set.clear();

        if (!parent.containsKey(key)) return;

        String[] values = (String[]) parent.readObject(key);
        Collections.addAll(set, values);
    }

    private static void saveStringLongMap(SaveFileData parent, String prefix, Map<String, Long> map) {
        String[] keys = map.keySet().toArray(new String[0]);
        parent.storeObject(prefix + "_keys", keys);

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = new SaveFileData();
            valueData.store("value", map.get(keys[i]));
            parent.store(prefix + "_value_" + i, valueData);
            System.out.println(
                    "Saving " + prefix + ": " + map.size() + " entries"
            );
        }
    }

    private static void loadStringLongMap(SaveFileData parent, String prefix, Map<String, Long> map) {
        map.clear();

        if (!parent.containsKey(prefix + "_keys")) return;

        String[] keys = (String[]) parent.readObject(prefix + "_keys");

        for (int i = 0; i < keys.length; i++) {
            SaveFileData valueData = parent.readSubData(prefix + "_value_" + i);
            if (valueData != null) {
                map.put(keys[i], valueData.readLong("value"));
            }
        }
    }

    private void loadAllAvailableSets() {
        Set<String> newSetCodes = new HashSet<>();
        for (CardEdition edition : allOrderedEditions) {
            newSetCodes.add(edition.getCode());
        }
        if (!newSetCodes.equals(allCardSets)) {
            allCardSets.clear();
            allCardSets.addAll(newSetCodes);
        }
    }

    @Override
    public void load(SaveFileData data) {
        if (data == null) {
            // No archipelago data found, treat archipelago as inactive for this save file.
            setupFreshSaveFile(ArchipelagoMode.disabled);
            return;
        }
        loadAllAvailableSets();

        // Load save data
        loadStringLongMap(data, "townEvents", completedTownInnEvents);
        loadStringLongMap(data, "townQuests", completedTownQuests);
        loadStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        loadStringLongMap(data, "items", itemsGainedByName);
        loadStringLongMap(data, "packs", packsEarnedBySet);
        loadStringSet(data, "bossesDefeated", bossesDefeatedByName);
        loadStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);
        loadStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        loadStringSet(data, "setsUnlocked", setsUnlockedByCode);
        loadStringSet(data, "lockedRegions", lockedWorldRegionsByName);

        setUnlockChecksRestAmount = data.containsKey("setUnlocksReceivedRest") ? data.readFloat("setUnlocksReceivedRest") : 0;
        receivedAmountOfSetUnlockChecks = data.containsKey("setUnlocksReceived") ? data.readInt("setUnlocksReceived") : 0;
        totalBattlesWon = data.containsKey("totalBattlesWon") ? data.readInt("totalBattlesWon") : 0;
        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
        lastArchipelagoRewardIndex = data.containsKey("lastArchipelagoRewardIndex") ? data.readInt("lastArchipelagoRewardIndex") : 0;
        archipelagoMode = ArchipelagoMode.values()[data.containsKey("archipelagoMode") ? data.readInt("archipelagoMode") : 0];
        GameHUD.getInstance().setApButtonVisibility(archipelagoMode == ArchipelagoMode.networked_archipelago);
        if (archipelagoMode == ArchipelagoMode.networked_archipelago) {
            // 1. Init APWorld
            // 2. Get current "index"
            // 3. Store state in this object
            // 4. Optional autosave
        }
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();

        saveStringLongMap(data, "townEvents", completedTownInnEvents);
        saveStringLongMap(data, "townQuests", completedTownQuests);
        saveStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        saveStringLongMap(data, "items", itemsGainedByName);
        saveStringLongMap(data, "packs", packsEarnedBySet);
        saveStringSet(data, "bossesDefeated", bossesDefeatedByName);
        saveStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);
        saveStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        saveStringSet(data, "setsUnlocked", setsUnlockedByCode);
        saveStringSet(data, "lockedRegions", lockedWorldRegionsByName);

        data.store("setUnlocksReceivedRest", setUnlockChecksRestAmount);
        data.store("setUnlocksReceived", receivedAmountOfSetUnlockChecks);
        data.store("totalBattlesWon", totalBattlesWon);
        data.store("totalGold", totalGoldEarned);
        data.store("extraLife", totalExtraMaxLifeEarned);
        data.store("shards", totalShardsEarned);
        data.store("lastArchipelagoRewardIndex", lastArchipelagoRewardIndex);
        data.store("archipelagoMode", archipelagoMode.ordinal());

        return data;
    }
    ///  --- End ---
}
