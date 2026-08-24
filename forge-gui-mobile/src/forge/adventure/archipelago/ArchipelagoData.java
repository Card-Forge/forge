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
import org.checkerframework.checker.nullness.qual.NonNull;

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
    protected final Set<String> regionTeleportingRunes = new HashSet<>(Arrays.asList("white rune", "black rune", "blue rune", "red rune", "green rune"));

    // Actual user data we want to store
    protected final Map<String, Long> colorlessCompletedTownInnEvents = new HashMap<>();
    protected final Map<String, Long> whiteCompletedTownInnEvents = new HashMap<>();
    protected final Map<String, Long> blueCompletedTownInnEvents = new HashMap<>();
    protected final Map<String, Long> blackCompletedTownInnEvents = new HashMap<>();
    protected final Map<String, Long> redCompletedTownInnEvents = new HashMap<>();
    protected final Map<String, Long> greenCompletedTownInnEvents = new HashMap<>();

    protected final Map<String, Long> colorlessCompletedTownQuests = new HashMap<>();
    protected final Map<String, Long> whiteCompletedTownQuests = new HashMap<>();
    protected final Map<String, Long> blueCompletedTownQuests = new HashMap<>();
    protected final Map<String, Long> blackCompletedTownQuests = new HashMap<>();
    protected final Map<String, Long> redCompletedTownQuests = new HashMap<>();
    protected final Map<String, Long> greenCompletedTownQuests = new HashMap<>();

    protected final Map<String, Long> cardsEarnedByRarity = new HashMap<>();
    protected final Map<String, Long> itemsGainedByName = new HashMap<>();
    protected final Map<String, Long> packsEarnedBySet = new HashMap<>();
    protected final Set<String> cardsUnlockedByName = new HashSet<>();
    protected final Set<String> setsUnlockedByCode = new HashSet<>();
    protected final Set<String> bossesDefeatedByName = new HashSet<>();
    protected final Set<String> miniBossesDefeatedByName = new HashSet<>();
    protected final Set<String> lockedWorldRegionsByName = new HashSet<>();
    // Todo: Replace the String with another serializable object that is compatible with Archipelago i.e. `NetworkItem.java`
    protected int lastArchipelagoRewardIndex = 0;
    protected int totalGoldEarned = 0;
    protected int totalExtraMaxLifeEarned = 0;
    protected int totalShardsEarned = 0;
    protected int totalBattlesWonWhite = 0;
    protected int totalBattlesWonBlue = 0;
    protected int totalBattlesWonBlack = 0;
    protected int totalBattlesWonRed = 0;
    protected int totalBattlesWonGreen = 0;
    protected int totalBattlesWonColorless = 0;
    private String lastTraversedRegion = "waste";

    // List of unlockable checks
    // Todo: Fill list based on archipelago xml contents
    protected float setUnlockChecksRestAmount = 0;
    protected int totalAmountOfSetUnlockChecks = ArchipelagoUtil.TOTAL_AMOUNT_OF_SET_UNLOCK_CHECKS; // This is set based on the value we receive in the APWorld
    protected int receivedAmountOfSetUnlockChecks = 0;

    private ArchipelagoData() {
    }

    public static ArchipelagoData getInstance() {
        return archipelagoDataInstance;
    }

    // This must be updated to reset any sets/maps/variables otherwise things persist between loads of different save files!
    public void setupFreshSaveFile(ArchipelagoMode archipelagoMode) {
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
        lockedWorldRegionsByName.addAll(new HashSet<>(Arrays.asList("white", "blue", "black", "red", "green")));

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
        lastTraversedRegion = "waste";

        setUnlockChecksRestAmount = 0f;
        totalAmountOfSetUnlockChecks = ArchipelagoUtil.TOTAL_AMOUNT_OF_SET_UNLOCK_CHECKS;
        receivedAmountOfSetUnlockChecks = 0;

        this.archipelagoMode = archipelagoMode;

        loadAllAvailableSets();
    }

    private void updatePlayerChecks(ArchipelagoCheckTypes type, @NonNull String notificationMessage) {
        if (archipelagoMode == ArchipelagoMode.disabled) return;
        LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
        localRandomizer.updatePlayerChecks(type, notificationMessage);
    }

    public boolean isSetUnlocked(String setCode) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (setCode == null || !setsUnlockedByCode.contains(setCode)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isRegionUnlocked(String regionName) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        if (!Arrays.asList(ArchipelagoUtil.REGION_NAMES).contains(regionName.toLowerCase())) return true;
        lastTraversedRegion = regionName;
        return !archipelagoDataInstance.lockedWorldRegionsByName.contains(lastTraversedRegion);
    }

    public ArchipelagoMode getArchipelagoMode() {
        return archipelagoMode;
    }

    public void setTotalAmountOfSetUnlockChecks(int newTotalAmountOfSetUnlockChecks) {
        totalAmountOfSetUnlockChecks = newTotalAmountOfSetUnlockChecks;
    }

    public void setLastTraversedRegion(String lastTraversedRegion) {
        this.lastTraversedRegion = lastTraversedRegion;
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

        return isSetUnlocked(setCode);

        // Neither card nor set is unlocked
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
    private void unlockSetByName(String setToUnlock, String notificationMessage) {
        addSetUnlockedByCode(setToUnlock);
        String setUnlockedText = String.format("Set Unlock: %s%s{RESET}", ArchipelagoColors.Cyan, setToUnlock);
        // Some sets don't have booster packs such as full-art land sets (P23).
        if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
            var booster = StaticData.instance().getBoosters().get(setToUnlock);
            if (booster != null) {
                Current.player().addBooster(AdventureEventController.instance().generateBooster(setToUnlock));
                setUnlockedText += String.format(" + %s%s{RESET}", ArchipelagoColors.Green, "Matching Booster Pack");
            }
            // Archipelago does not know what set will be unlocked, this is randomized locally. Therefore, we always wanna show the player a notification.
            generateGameNotification(String.format("%s\n%s", notificationMessage, setUnlockedText));
        }
        System.out.println(setUnlockedText);
    }

    // This produces a different result each time it's rolled, the RNG isn't seeded.
    public void unlockRandomSet(String notificationMessage) {
        // Subtract unlocked sets from full list.
        Set<String> lockedSets = new HashSet<>(allCardSets);
        lockedSets.removeAll(setsUnlockedByCode);

        // Nothing left to unlock
        if (lockedSets.isEmpty()) {
            return;
        }

        // If we already received all checks, unlock the entire list of locked sets
        if (receivedAmountOfSetUnlockChecks >= totalAmountOfSetUnlockChecks) {
            for (String setToUnlock : lockedSets) {
                unlockSetByName(setToUnlock, notificationMessage);
            }
        }

        float amountOfSetsToUnlock = (float) allCardSets.size() / totalAmountOfSetUnlockChecks + setUnlockChecksRestAmount;
        int amountOfSetsToUnlockFloored = (int) Math.floor(amountOfSetsToUnlock);
        setUnlockChecksRestAmount = (amountOfSetsToUnlock - amountOfSetsToUnlockFloored);
        List<String> lockedList = new ArrayList<>(lockedSets);
        Random random = new Random();
        String setToUnlock;

        for (int i = 0; i < amountOfSetsToUnlockFloored; i++) {
            setToUnlock = lockedList.get(random.nextInt(lockedList.size()));
            unlockSetByName(setToUnlock, notificationMessage);
            lockedSets.remove(setToUnlock);
            receivedAmountOfSetUnlockChecks++;
        }
    }

    public void generateGameNotification(String message) {
        GameHUD.getInstance().addNotification(message, 0.5f, 3f, 0.5f);
    }
    /// --- End ---

    /// --- The functions below are responsible for mutating the user data we store in the save file for both modes ---

    public boolean addCardUnlockedByName(String cardName) {
        return cardsUnlockedByName.add(cardName);
    }

    public boolean addSetUnlockedByCode(String setCode) {
        return setsUnlockedByCode.add(setCode);
    }

    public void addCompletedTownInnEvents() {
        String townName = TileMapScene.instance().rootPoint.getDisplayName();
        switch (lastTraversedRegion) {
            case "waste" -> {
                colorlessCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + colorlessCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_TOWN_EVENTS, notificationMessage);
            }
            case "white" -> {
                whiteCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + whiteCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_TOWN_EVENTS,  notificationMessage);
            }
            case "blue" -> {
                blueCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + blueCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_TOWN_EVENTS, notificationMessage);
            }
            case "black" -> {
                blackCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + blackCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_TOWN_EVENTS, notificationMessage);
            }
            case "red" -> {
                redCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + redCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_TOWN_EVENTS,  notificationMessage);
            }
            case "green" -> {
                greenCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + greenCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.GREEN_TOWN_EVENTS, notificationMessage);
            }
        }
    }

    public void addCompletedQuests(AdventureQuestEvent event) {
        String townName = event.poi.getDisplayName();

        switch (lastTraversedRegion) {
            case "waste" -> {
                colorlessCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + colorlessCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_TOWN_QUESTS, notificationMessage);
            }
            case "white" -> {
                whiteCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + whiteCompletedTownQuests.get(townName),  ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_TOWN_QUESTS, notificationMessage);
            }
            case "blue" -> {
                blueCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + blueCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_TOWN_QUESTS, notificationMessage);
            }
            case "black" -> {
                blackCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + blackCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_TOWN_QUESTS, notificationMessage);
            }
            case "red" -> {
                redCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + redCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_TOWN_QUESTS, notificationMessage);
            }
            case "green" -> {
                greenCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + greenCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.GREEN_TOWN_QUESTS, notificationMessage);
            }
        }
    }

    public void addCardByRarity(String rarity) {
        cardsEarnedByRarity.merge(rarity, 1L, Long::sum);
        updatePlayerChecks(ArchipelagoCheckTypes.TOTAL_CARDS_EARNED, String.format("%sRandomizer:{RESET}\nTotal cards earned: ", ArchipelagoColors.Plum));
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
    }

    private void addUnlockedRegionByName(String regionName) {
        lockedWorldRegionsByName.remove(regionName);
        System.out.println("Randomizer:\n Region Unlocked: " + regionName);
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    public void addItem(String itemName) {
        if (regionTeleportingRunes.contains(itemName.toLowerCase())) {
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
        }
        itemsGainedByName.merge(itemName, 1L, Long::sum);
    }

    // This function only tracks how many packs the player has earned of each type.
    // There are no checks that depend on this at the moment.
    public void addPack(String boosterPackName) {
        packsEarnedBySet.merge(boosterPackName, 1L, Long::sum);
    }

    public void addMaxLife(int amount) {
        totalExtraMaxLifeEarned += amount;
    }

    public void addShards(int amount) {
        totalShardsEarned += amount;
    }

    public void addTotalBattlesWon(int amount) {
        switch (lastTraversedRegion) {
            case "waste" -> {
                totalBattlesWonColorless += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nWastes Region Battles Won: " + totalBattlesWonColorless, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_BATTLE_WON, notificationMessage);
            }
            case "white" -> {
                totalBattlesWonWhite += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nWhite Region Battles Won: " + totalBattlesWonWhite, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_BATTLE_WON, notificationMessage);
            }
            case "blue" -> {
                totalBattlesWonBlue += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nBlue Region Battles Won: " + totalBattlesWonBlue, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_BATTLE_WON, notificationMessage);
            }
            case "black" -> {
                totalBattlesWonBlack += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nBlack Region Battles Won: " + totalBattlesWonBlack, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_BATTLE_WON, notificationMessage);
            }
            case "red" -> {
                totalBattlesWonRed += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nRed Region Battles Won: " + totalBattlesWonRed, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_BATTLE_WON, notificationMessage);
            }
            case "green" -> {
                totalBattlesWonGreen += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nGreen Region Battles Won: " + totalBattlesWonGreen, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.GREEN_BATTLE_WON, notificationMessage);
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
        archipelagoMode = ArchipelagoMode.values()[data.containsKey("archipelagoMode") ? data.readInt("archipelagoMode") : 0];
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
            loadStringSet(data, "blueItemShop", localRandomizer.blueItemShopList);
            loadStringSet(data, "blackItemShop", localRandomizer.blackItemShopList);
            loadStringSet(data, "redItemShop", localRandomizer.redItemShopList);
            loadStringSet(data, "greenItemShop", localRandomizer.greenItemShopList);
            loadStringSet(data, "remainingEquipment", localRandomizer.remainingEquipmentPool);
            if (data.containsKey("checksSinceLastRegionReward")) localRandomizer.checksSinceLastRegionReward = data.readInt("checksSinceLastRegionReward");
        }

        setUnlockChecksRestAmount = data.containsKey("setUnlocksReceivedRest") ? data.readFloat("setUnlocksReceivedRest") : 0;
        receivedAmountOfSetUnlockChecks = data.containsKey("setUnlocksReceived") ? data.readInt("setUnlocksReceived") : 0;
        totalBattlesWonColorless = data.containsKey("totalBattlesWonColorless") ? data.readInt("totalBattlesWonColorless") : 0;
        totalBattlesWonWhite = data.containsKey("totalBattlesWonWhite") ? data.readInt("totalBattlesWonWhite") : 0;
        totalBattlesWonBlue = data.containsKey("totalBattlesWonBlue") ? data.readInt("totalBattlesWonBlue") : 0;
        totalBattlesWonBlack = data.containsKey("totalBattlesWonBlack") ? data.readInt("totalBattlesWonBlack") : 0;
        totalBattlesWonRed = data.containsKey("totalBattlesWonRed") ? data.readInt("totalBattlesWonRed") : 0;
        totalBattlesWonGreen = data.containsKey("totalBattlesWonGreen") ? data.readInt("totalBattlesWonGreen") : 0;
        lastTraversedRegion = data.containsKey("lastTraversedRegion") ? data.readString("lastTraversedRegion") : "waste";
        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
        lastArchipelagoRewardIndex = data.containsKey("lastArchipelagoRewardIndex") ? data.readInt("lastArchipelagoRewardIndex") : 0;
        setTotalAmountOfSetUnlockChecks(data.containsKey("totalSetUnlockChecks") ? data.readInt("totalSetUnlockChecks") : 100);
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
            saveStringSet(data, "blueItemShop", localRandomizer.blueItemShopList);
            saveStringSet(data, "blackItemShop", localRandomizer.blackItemShopList);
            saveStringSet(data, "redItemShop", localRandomizer.redItemShopList);
            saveStringSet(data, "greenItemShop", localRandomizer.greenItemShopList);
            saveStringSet(data, "remainingEquipment", localRandomizer.remainingEquipmentPool);
            data.store("checksSinceLastRegionReward", localRandomizer.checksSinceLastRegionReward);
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
