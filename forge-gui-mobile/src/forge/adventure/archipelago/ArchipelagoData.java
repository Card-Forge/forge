package forge.adventure.archipelago;

import forge.StaticData;
import forge.adventure.data.WorldData;
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
    private static final ArchipelagoData archipelagoDataInstance = new ArchipelagoData();
    protected ArchipelagoMode archipelagoMode = ArchipelagoMode.disabled;

    // Data we need from Forge
    private final CardEdition.Collection allEditions = StaticData.instance().getEditions();
    private final Iterable<CardEdition> allOrderedEditions = allEditions.getOrderedEditions();
    // Todo: This works fine for singleplayer even when updates come out but the fact that the list of all sets can grow will cause problems in Archipelago due to a variable amount of checks.
    protected final Set<String> allCardSets = new HashSet<>();
    // List of teleportation runes that we use to gate regions
    protected final Set<String> regionTeleportingRunes = new HashSet<>(Arrays.asList("White rune","Black rune","Blue rune","Red rune","Green rune"));
    // List of known main bosses that contribute to APWorld completion
    private final Set<String> mainBosses = new HashSet<>(Arrays.asList("Lorthos","Emrakul","Lathliss","Ghalta","Griselbrand","Akroma","Sliver Queen"));

    // Actual user data we want to store
    private final Map<String, Long> colorlessCompletedTownInnEvents = new HashMap<>();
    private final Map<String, Long> whiteCompletedTownInnEvents = new HashMap<>();
    private final Map<String, Long> blueCompletedTownInnEvents = new HashMap<>();
    private final Map<String, Long> blackCompletedTownInnEvents = new HashMap<>();
    private final Map<String, Long> redCompletedTownInnEvents = new HashMap<>();
    private final Map<String, Long> greenCompletedTownInnEvents = new HashMap<>();

    private final Map<String, Long> colorlessCompletedTownQuests = new HashMap<>();
    private final Map<String, Long> whiteCompletedTownQuests = new HashMap<>();
    private final Map<String, Long> blueCompletedTownQuests = new HashMap<>();
    private final Map<String, Long> blackCompletedTownQuests = new HashMap<>();
    private final Map<String, Long> redCompletedTownQuests = new HashMap<>();
    private final Map<String, Long> greenCompletedTownQuests = new HashMap<>();

    private final Map<String, Long> cardsEarnedByRarity = new HashMap<>();
    protected final Map<String, Long> itemsGainedByName = new HashMap<>();
    private final Map<String, Long> packsEarnedBySet = new HashMap<>();
    private final Set<String> cardsUnlockedByName = new HashSet<>();
    protected final Set<String> setsUnlockedByCode = new HashSet<>();
    private final Set<String> bossesDefeatedByName = new HashSet<>();
    private final Set<String> miniBossesDefeatedByName = new HashSet<>();
    protected final Set<String> lockedWorldRegionsByName = new HashSet<>();
    // Todo: Replace the String with another serializable object that is compatible with Archipelago i.e. `NetworkItem.java`
    protected int lastArchipelagoRewardIndex = 0;
    private int totalGoldEarned = 0;
    private int totalExtraMaxLifeEarned = 0;
    private int totalShardsEarned = 0;
    private int totalBattlesWonWhite = 0;
    private int totalBattlesWonBlue = 0;
    private int totalBattlesWonBlack = 0;
    private int totalBattlesWonRed = 0;
    private int totalBattlesWonGreen = 0;
    private int totalBattlesWonColorless = 0;
    private String lastTraversedRegion = "wastes";

    // List of unlockable checks
    // Todo: Fill list based on archipelago xml contents
    protected int receivedAmountOfSetUnlockChecks = 0;
    protected float setUnlockChecksRestAmount = 0;

    protected int totalAmountOfSetUnlockChecks = 100; // This is set based on the value we receive in the APWorld
    private final int totalBattlesWonBreakpoint = 3; // Reward for every 3 battles won.
    private final int totalTownQuestsBreakpoint = 1; // Reward for every 1 town quests done.
    private final int totalTownEventsBreakpoint = 1; // Reward for every 1 town events done.
    private final int totalCardsEarnedBreakPoint = 80; // Reward for every 80 unique cards gained.

    public enum ARCHIPELAGO_CHECK_TYPES {COLORLESS_BATTLE_WON, WHITE_BATTLE_WON, BLUE_BATTLE_WON, BLACK_BATTLE_WON, RED_BATTLE_WON, GREEN_BATTLE_WON, COLORLESS_TOWN_QUESTS, WHITE_TOWN_QUESTS, BLUE_TOWN_QUESTS, BLACK_TOWN_QUESTS, RED_TOWN_QUESTS, GREEN_TOWN_QUESTS, TOWN_EVENTS, TOTAL_CARDS_EARNED, BOSS_WHITE_DEFEATED, BOSS_BLUE_DEFEATED, BOSS_BLACK_DEFEATED, BOSS_RED_DEFEATED, BOSS_GREEN_DEFEATED, BOSS_COLORLESS_DEFEATED, BOSS_WUBRG_DEFEATED, WIN_CONDITION_CLEARED};

    private ArchipelagoData() {}

    public static ArchipelagoData getInstance() {
        return archipelagoDataInstance;
    }

    // This must be updated to reset any sets/maps/variables otherwise things persist between loads of different save files!
    public void setupFreshSaveFile(ArchipelagoMode archipelagoMode) {
        GameHUD.getInstance().setApButtonVisibility(archipelagoMode == ArchipelagoMode.networked_archipelago);
        cardsUnlockedByName.clear();
        this.addCardUnlockedByName("Plains");
        this.addCardUnlockedByName("Forest");
        this.addCardUnlockedByName("Swamp");
        this.addCardUnlockedByName("Mountain");
        this.addCardUnlockedByName("Island");
        this.addCardUnlockedByName("Wastes");

        colorlessCompletedTownInnEvents.clear();
        whiteCompletedTownInnEvents.clear();
        blueCompletedTownInnEvents.clear();
        blackCompletedTownInnEvents.clear();
        redCompletedTownInnEvents.clear();
        blackCompletedTownInnEvents.clear();

        colorlessCompletedTownQuests.clear();
        whiteCompletedTownQuests.clear();
        blueCompletedTownQuests.clear();
        blackCompletedTownQuests.clear();
        redCompletedTownQuests.clear();
        greenCompletedTownQuests.clear();
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
        totalBattlesWonWhite = 0;
        totalBattlesWonBlue = 0;
        totalBattlesWonBlack = 0;
        totalBattlesWonRed = 0;
        totalBattlesWonGreen = 0;
        totalBattlesWonColorless = 0;
        lastTraversedRegion = "wastes";

        receivedAmountOfSetUnlockChecks = 0;
        setUnlockChecksRestAmount = 0f;

        this.archipelagoMode = archipelagoMode;

        // Todo: Swap with ArchipelagoRandomizer implementation if that's enabled
        if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
            LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
            localRandomizer.randomizeLocalEquipment();
        }
        loadAllAvailableSets();
    }

    private void updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES type) {
        if (archipelagoMode == ArchipelagoMode.disabled) return;
        switch (type) {
            case COLORLESS_BATTLE_WON -> {
                if (totalBattlesWonColorless > 0 && totalBattlesWonColorless % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case WHITE_BATTLE_WON -> {
                if (totalBattlesWonWhite > 0 && totalBattlesWonWhite % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case BLUE_BATTLE_WON -> {
                if (totalBattlesWonBlue > 0 && totalBattlesWonBlue % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case BLACK_BATTLE_WON -> {
                if (totalBattlesWonBlack > 0 && totalBattlesWonBlack % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case RED_BATTLE_WON -> {
                if (totalBattlesWonRed > 0 && totalBattlesWonRed % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case GREEN_BATTLE_WON -> {
                if (totalBattlesWonGreen > 0 && totalBattlesWonGreen % totalBattlesWonBreakpoint == 0) {
                    unlockRandomSet();
                }
                // Todo: Signal the APWorld that the next battles won location is triggered
            }
            case COLORLESS_TOWN_QUESTS -> {
                int totalTownQuestsDone = 0;
                for (long count : colorlessCompletedTownQuests.values()) {
                    totalTownQuestsDone += (int) count;
                }
                if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest location is triggered
            }
            case WHITE_TOWN_QUESTS -> {
                int totalTownQuestsDone = 0;
                for (long count : whiteCompletedTownQuests.values()) {
                    totalTownQuestsDone += (int) count;
                }
                if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest location is triggered
            }
            case BLUE_TOWN_QUESTS -> {
                int totalTownQuestsDone = 0;
                for (long count : blueCompletedTownQuests.values()) {
                    totalTownQuestsDone += (int) count;
                }
                if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest location is triggered
            }
            case RED_TOWN_QUESTS -> {
                int totalTownQuestsDone = 0;
                for (long count : redCompletedTownQuests.values()) {
                    totalTownQuestsDone += (int) count;
                }
                if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest location is triggered
            }
            case GREEN_TOWN_QUESTS -> {
                int totalTownQuestsDone = 0;
                for (long count : greenCompletedTownQuests.values()) {
                    totalTownQuestsDone += (int) count;
                }
                if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next quest location is triggered
            }
            case TOWN_EVENTS -> {
                int totalTownEventsDone = 0;
                for (long count : colorlessCompletedTownQuests.values()) {
                    totalTownEventsDone += (int) count;
                }
                if (totalTownEventsDone > 0 && totalTownEventsDone % totalTownEventsBreakpoint == 0) {
                    LocalRandomizer.getInstance().unlockRandomRegion();
                }
                // Todo: Signal the APWorld that the next event location is triggered
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
        lastTraversedRegion = regionName;
        if (archipelagoDataInstance.lockedWorldRegionsByName.contains(lastTraversedRegion)) {
            return false;
        }
        return true;
    }

    public ArchipelagoMode getArchipelagoMode() {
        return archipelagoMode;
    }

    public void setTotalAmountOfSetUnlockChecks(int newTotalAmountOfSetUnlockChecks) {
        totalAmountOfSetUnlockChecks = newTotalAmountOfSetUnlockChecks;
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

    /// --- The checks below can be called from the networked part of the AP implementation. Note that `setLastArchipelagoRewardIndex` must be called manually. ---
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

    // This produces a different result each time it's rolled, the RNG isn't seeded.
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
        List<String> lockedList = new ArrayList<>(lockedSets);
        Random random = new Random();

        String setToUnlock = lockedList.get(random.nextInt(lockedList.size()));
        unlockSetByName(setToUnlock);
        lockedSets.remove(setToUnlock);
        receivedAmountOfSetUnlockChecks++;
    }

    public void generateGameNotification(String message) {
        GameHUD.getInstance().addNotification(message, 0.5f, 3f, 0.5f);
    }
    /// --- End ---

    /// --- The functions below are responsible for mutating the user data we store in the save file for both modes ---
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
            case "akroma" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_WHITE_DEFEATED);
            }
            case "lorthos" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_BLUE_DEFEATED);
            }
            case "griselbrand" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_BLACK_DEFEATED);
            }
            case "lathliss" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_RED_DEFEATED);
            }
            case "ghalta" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_GREEN_DEFEATED);
            }
            case "emrakul" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_COLORLESS_DEFEATED);
            }
            case "sliver queen" -> {
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BOSS_WUBRG_DEFEATED);
            }
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
        colorlessCompletedTownQuests.merge(townName, 1L, Long::sum);
        System.out.println("FORGE_ARCHIPELAGO: INN EVENT COMPLETION DETECTED: " + townName + " - " + colorlessCompletedTownQuests.get(townName));
        updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.TOWN_EVENTS);
    }

    public void addCompletedQuests(AdventureQuestEvent event) {
        String townName = event.poi.getDisplayName();

        switch (lastTraversedRegion) {
            case "wastes" -> {
                colorlessCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + colorlessCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.COLORLESS_TOWN_QUESTS);
            }
            case "white" -> {
                whiteCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + whiteCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.WHITE_TOWN_QUESTS);
            }
            case "blue" -> {
                blueCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + blueCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BLUE_TOWN_QUESTS);
            }
            case "black" -> {
                blackCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + blackCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BLACK_TOWN_QUESTS);
            }
            case "red" -> {
                redCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + redCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.RED_TOWN_QUESTS);
            }
            case "green" -> {
                greenCompletedTownQuests.merge(townName, 1L, Long::sum);
                System.out.println("FORGE_ARCHIPELAGO: QUEST COMPLETION DETECTED: " + townName + " - " + greenCompletedTownQuests.get(townName));
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.GREEN_TOWN_QUESTS);
            }
        }
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

    private void addUnlockedRegionByName(String regionName) {
        lockedWorldRegionsByName.remove(regionName);
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    // Todo: Verify that item names are unique.
    public void addItem(String itemName) {
        if (regionTeleportingRunes.contains(itemName)) {
            // Unlock the region based on the color found in the itemName
            if (itemName.toLowerCase().contains("white")) {
                addUnlockedRegionByName("white");
            } else if (itemName.toLowerCase().contains("blue")) {
                addUnlockedRegionByName("blue");
            } else if (itemName.toLowerCase().contains("black")) {
                addUnlockedRegionByName("black");
            } else if (itemName.toLowerCase().contains("red")) {
                addUnlockedRegionByName("red");
            } else if (itemName.toLowerCase().contains("green")) {
                addUnlockedRegionByName("green");
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
        switch (lastTraversedRegion) {
            case "wastes" -> {
                totalBattlesWonColorless += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.COLORLESS_BATTLE_WON);
            }
            case "white" -> {
                totalBattlesWonWhite += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.WHITE_BATTLE_WON);
            }
            case "blue" -> {
                totalBattlesWonBlue += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BLUE_BATTLE_WON);
            }
            case "black" -> {
                totalBattlesWonBlack += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.BLACK_BATTLE_WON);
            }
            case "red" -> {
                totalBattlesWonRed += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.RED_BATTLE_WON);
            }
            case "green" -> {
                totalBattlesWonGreen += amount;
                updatePlayerChecks(ARCHIPELAGO_CHECK_TYPES.GREEN_BATTLE_WON);
            }
        }
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
        LocalRandomizer localRandomizer = LocalRandomizer.getInstance();

        // Load save data
        loadStringLongMap(data, "colorlessTownEvents", colorlessCompletedTownInnEvents);
        loadStringLongMap(data, "whiteTownEvents", whiteCompletedTownInnEvents);
        loadStringLongMap(data, "blueTownEvents", blueCompletedTownInnEvents);
        loadStringLongMap(data, "blackTownEvents", blackCompletedTownInnEvents);
        loadStringLongMap(data, "redTownEvents", redCompletedTownInnEvents);
        loadStringLongMap(data, "greenTownEvents", greenCompletedTownInnEvents);
        loadStringLongMap(data, "colorlessTownQuests", colorlessCompletedTownQuests);
        loadStringLongMap(data, "whiteTownQuests", whiteCompletedTownQuests);
        loadStringLongMap(data, "blueTownQuests", blueCompletedTownQuests);
        loadStringLongMap(data, "blackTownQuests", blackCompletedTownQuests);
        loadStringLongMap(data, "redTownQuests", redCompletedTownQuests);
        loadStringLongMap(data, "greenTownQuests", greenCompletedTownQuests);
        loadStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        loadStringLongMap(data, "items", itemsGainedByName);
        loadStringLongMap(data, "packs", packsEarnedBySet);
        loadStringSet(data, "bossesDefeated", bossesDefeatedByName);
        loadStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);
        loadStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        loadStringSet(data, "setsUnlocked", setsUnlockedByCode);
        loadStringSet(data, "lockedRegions", lockedWorldRegionsByName);
        // Todo: Swap this for the ArchipelagoRandomizer data when that's enabled
        if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
            WorldData.resetShopLists();
            loadStringSet(data, "colorlessEquipmentShop", localRandomizer.colorlessEquipmentShopList);
            loadStringSet(data, "whiteEquipmentShop", localRandomizer.whiteEquipmentShopList);
            loadStringSet(data, "blueEquipmentShop", localRandomizer.blueEquipmentShopList);
            loadStringSet(data, "blackEquipmentShop", localRandomizer.blackEquipmentShopList);
            loadStringSet(data, "redEquipmentShop", localRandomizer.redEquipmentShopList);
            loadStringSet(data, "greenEquipmentShop", localRandomizer.greenEquipmentShopList);
            loadStringSet(data, "whiteItemShop", localRandomizer.whiteItemShopList);
            loadStringSet(data, "blueEquipmentShop", localRandomizer.blueItemShopList);
            loadStringSet(data, "blackEquipmentShop", localRandomizer.blackItemShopList);
            loadStringSet(data, "redEquipmentShop", localRandomizer.redItemShopList);
            loadStringSet(data, "greenItemShop", localRandomizer.greenItemShopList);
            loadStringSet(data, "remainingEquipment", localRandomizer.remainingEquipmentPool);
        }

        setUnlockChecksRestAmount = data.containsKey("setUnlocksReceivedRest") ? data.readFloat("setUnlocksReceivedRest") : 0;
        receivedAmountOfSetUnlockChecks = data.containsKey("setUnlocksReceived") ? data.readInt("setUnlocksReceived") : 0;
        totalBattlesWonColorless = data.containsKey("totalBattlesWonColorless") ? data.readInt("totalBattlesWonColorless") : 0;
        totalBattlesWonWhite = data.containsKey("totalBattlesWonWhite") ? data.readInt("totalBattlesWonWhite") : 0;
        totalBattlesWonBlue = data.containsKey("totalBattlesWonBlue") ? data.readInt("totalBattlesWonBlue") : 0;
        totalBattlesWonBlack = data.containsKey("totalBattlesWonBlack") ? data.readInt("totalBattlesWonBlack") : 0;
        totalBattlesWonRed = data.containsKey("totalBattlesWonRed") ? data.readInt("totalBattlesWonRed") : 0;
        totalBattlesWonGreen = data.containsKey("totalBattlesWonGreen") ? data.readInt("totalBattlesWonGreen") : 0;
        lastTraversedRegion = data.containsKey("lastTraversedRegion") ? data.readString("lastTraversedRegion") : "wastes";
        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
        lastArchipelagoRewardIndex = data.containsKey("lastArchipelagoRewardIndex") ? data.readInt("lastArchipelagoRewardIndex") : 0;
        archipelagoMode = ArchipelagoMode.values()[data.containsKey("archipelagoMode") ? data.readInt("archipelagoMode") : 0];
        setTotalAmountOfSetUnlockChecks(data.containsKey("totalSetUnlockChecks") ? data.readInt("totalSetUnlockChecks") : 100);
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
        LocalRandomizer localRandomizer = LocalRandomizer.getInstance();

        saveStringLongMap(data, "colorlessTownEvents", colorlessCompletedTownInnEvents);
        saveStringLongMap(data, "whiteTownEvents", whiteCompletedTownInnEvents);
        saveStringLongMap(data, "blueTownEvents", blueCompletedTownInnEvents);
        saveStringLongMap(data, "blackTownEvents", blackCompletedTownInnEvents);
        saveStringLongMap(data, "redTownEvents", redCompletedTownInnEvents);
        saveStringLongMap(data, "greenTownEvents", greenCompletedTownInnEvents);
        saveStringLongMap(data, "colorlessTownQuests", colorlessCompletedTownQuests);
        saveStringLongMap(data, "whiteTownQuests", whiteCompletedTownQuests);
        saveStringLongMap(data, "blueTownQuests", blueCompletedTownQuests);
        saveStringLongMap(data, "blackTownQuests", blackCompletedTownQuests);
        saveStringLongMap(data, "redTownQuests", redCompletedTownQuests);
        saveStringLongMap(data, "greenTownQuests", greenCompletedTownQuests);
        saveStringLongMap(data, "cardsByRarity", cardsEarnedByRarity);
        saveStringLongMap(data, "items", itemsGainedByName);
        saveStringLongMap(data, "packs", packsEarnedBySet);
        saveStringSet(data, "bossesDefeated", bossesDefeatedByName);
        saveStringSet(data, "miniBossesDefeated", miniBossesDefeatedByName);
        saveStringSet(data, "cardsUnlocked", cardsUnlockedByName);
        saveStringSet(data, "setsUnlocked", setsUnlockedByCode);
        saveStringSet(data, "lockedRegions", lockedWorldRegionsByName);
        // Todo: Swap this for the ArchipelagoRandomizer data when that's enabled
        if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
            saveStringSet(data, "colorlessEquipmentShop", localRandomizer.colorlessEquipmentShopList);
            saveStringSet(data, "whiteEquipmentShop", localRandomizer.whiteEquipmentShopList);
            saveStringSet(data, "blueEquipmentShop", localRandomizer.blueEquipmentShopList);
            saveStringSet(data, "blackEquipmentShop", localRandomizer.blackEquipmentShopList);
            saveStringSet(data, "redEquipmentShop", localRandomizer.redEquipmentShopList);
            saveStringSet(data, "greenEquipmentShop", localRandomizer.greenEquipmentShopList);
            saveStringSet(data, "whiteItemShop", localRandomizer.whiteItemShopList);
            saveStringSet(data, "blueEquipmentShop", localRandomizer.blueItemShopList);
            saveStringSet(data, "blackEquipmentShop", localRandomizer.blackItemShopList);
            saveStringSet(data, "redEquipmentShop", localRandomizer.redItemShopList);
            saveStringSet(data, "greenItemShop", localRandomizer.greenItemShopList);
            saveStringSet(data, "remainingEquipment", localRandomizer.remainingEquipmentPool);
        }

        data.store("setUnlocksReceivedRest", setUnlockChecksRestAmount);
        data.store("setUnlocksReceived", receivedAmountOfSetUnlockChecks);
        data.store("totalBattlesWonColorless", totalBattlesWonColorless);
        data.store("totalBattlesWonWhite", totalBattlesWonWhite);
        data.store("totalBattlesWonBlue", totalBattlesWonBlue);
        data.store("totalBattlesWonBlack", totalBattlesWonBlack);
        data.store("totalBattlesWonRed", totalBattlesWonRed);
        data.store("totalBattlesWonGreen", totalBattlesWonGreen);
        data.store("lastTraversedRegion", lastTraversedRegion);
        data.store("totalGold", totalGoldEarned);
        data.store("extraLife", totalExtraMaxLifeEarned);
        data.store("shards", totalShardsEarned);
        data.store("lastArchipelagoRewardIndex", lastArchipelagoRewardIndex);
        data.store("archipelagoMode", archipelagoMode.ordinal());
        data.store("totalSetUnlockChecks", totalAmountOfSetUnlockChecks);

        return data;
    }
    ///  --- End ---
}
