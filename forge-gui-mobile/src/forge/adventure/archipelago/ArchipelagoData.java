package forge.adventure.archipelago;

import forge.StaticData;
import forge.adventure.data.ItemData;
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
    private final Set<String> COLOR_SET_NAMES = ArchipelagoUtil.COLOR_SET_NAMES;
    protected final Set<String> allCardSets = new HashSet<>();

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
    protected final Set<ArchipelagoRegion> lockedWorldRegions = EnumSet.noneOf(ArchipelagoRegion.class);
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
    private ArchipelagoRegion lastTraversedRegion = ArchipelagoRegion.WASTE;

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
        GameHUD.getInstance().setApButtonVisibility(archipelagoMode == ArchipelagoMode.networked_archipelago);
        cardsUnlockedByName.clear();
        this.addCardUnlockedByName("Plains");
        this.addCardUnlockedByName("Forest");
        this.addCardUnlockedByName("Swamp");
        this.addCardUnlockedByName("Mountain");
        this.addCardUnlockedByName("Island");
        this.addCardUnlockedByName("Wastes");

        setsUnlockedByCode.clear();

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
        bossesDefeatedByName.clear();
        miniBossesDefeatedByName.clear();
        lockedWorldRegions.clear();
        lockedWorldRegions.addAll(ArchipelagoRegion.LOCKABLE);

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
        lastTraversedRegion = ArchipelagoRegion.WASTE;

        setUnlockChecksRestAmount = 0f;
        totalAmountOfSetUnlockChecks = ArchipelagoUtil.TOTAL_AMOUNT_OF_SET_UNLOCK_CHECKS;
        receivedAmountOfSetUnlockChecks = 0;

        this.archipelagoMode = archipelagoMode;

        loadAllAvailableSets();
    }

    private void updatePlayerChecks(ArchipelagoCheckTypes type, @NonNull String notificationMessage) {
        if (archipelagoMode == ArchipelagoMode.disabled) return;
        boolean networkedAP = archipelagoMode == ArchipelagoMode.networked_archipelago;
        if (networkedAP) {
            ArchipelagoRandomizer apRandomizer = ArchipelagoRandomizer.getInstance();
            apRandomizer.updatePlayerChecks(type);
        } else {
            LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
            localRandomizer.updatePlayerChecks(type, notificationMessage);
        }
    }

    public boolean isSetUnlocked(String setCode) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        // Colour packs contain cards from all sets, so they are never locked.
        if (COLOR_SET_NAMES.contains(setCode)) return true;
        if (setCode == null || !setsUnlockedByCode.contains(setCode)) {
            return false;
        } else {
            return true;
        }
    }

    // biomeName is the game's name for the biome the player is in (BiomeData.name).
    public boolean isRegionUnlocked(String biomeName) {
        if (archipelagoMode == ArchipelagoMode.disabled) return true;
        ArchipelagoRegion region = ArchipelagoRegion.fromBiomeName(biomeName);
        if (region == null) return true;
        lastTraversedRegion = region;
        return !lockedWorldRegions.contains(region);
    }

    public ArchipelagoMode getArchipelagoMode() {
        return archipelagoMode;
    }

    public void setTotalAmountOfSetUnlockChecks(int newTotalAmountOfSetUnlockChecks) {
        totalAmountOfSetUnlockChecks = newTotalAmountOfSetUnlockChecks;
    }

    public void setLastTraversedRegion(ArchipelagoRegion lastTraversedRegion) {
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
    // Unlocks the set and gives its booster pack if gift packs apply. Returns true if a booster pack was given.
    private boolean unlockSetByName(String setToUnlock) {
        addSetUnlockedByCode(setToUnlock);
        // Some sets don't have booster packs such as full-art land sets (P23).
        if (archipelagoMode == ArchipelagoMode.solo_randomizer || (archipelagoMode == ArchipelagoMode.networked_archipelago && ArchipelagoRandomizer.getInstance().getSlotData() != null && ArchipelagoRandomizer.getInstance().getSlotData().GiftPack == 1)) {
            var booster = StaticData.instance().getBoosters().get(setToUnlock);
            if (booster != null) {
                Current.player().addBooster(AdventureEventController.instance().generateBooster(setToUnlock));
                return true;
            }
        }
        return false;
    }

    // Shows one notification for all sets unlocked by a single check.
    private void notifySetsUnlocked(List<String> unlockedSets, int boosterPacks, String notificationMessage) {
        if (unlockedSets.isEmpty()) return;
        String setUnlockedText = String.format("%s: %s%s{RESET}", unlockedSets.size() == 1 ? "Set Unlock" : "Set Unlocks",
                ArchipelagoColors.Cyan, String.join(", ", unlockedSets));
        if (boosterPacks > 0) {
            setUnlockedText += String.format(" + %s%d Matching Booster Pack%s{RESET}", ArchipelagoColors.Green, boosterPacks, boosterPacks == 1 ? "" : "s");
        }
        // Archipelago does not know what set will be unlocked, this is randomized locally. Therefore, we always wanna show the player a notification.
        if (archipelagoMode == ArchipelagoMode.networked_archipelago) {
            generateGameNotification(String.format("%s%s{RESET}%s", ArchipelagoColors.Salmon, "Forge AP:\n", setUnlockedText));
        } else if (archipelagoMode == ArchipelagoMode.solo_randomizer) {
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

        float amountOfSetsToUnlock = (float) allCardSets.size() / totalAmountOfSetUnlockChecks + setUnlockChecksRestAmount;
        int amountOfSetsToUnlockFloored = (int) Math.floor(amountOfSetsToUnlock);
        setUnlockChecksRestAmount = (amountOfSetsToUnlock - amountOfSetsToUnlockFloored);
        List<String> lockedList = new ArrayList<>(lockedSets);
        List<String> setsToUnlock = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < amountOfSetsToUnlockFloored && !lockedList.isEmpty(); i++) {
            setsToUnlock.add(lockedList.remove(random.nextInt(lockedList.size())));
        }

        int boosterPacks = 0;
        for (String setToUnlock : setsToUnlock) {
            if (unlockSetByName(setToUnlock)) boosterPacks++;
        }
        notifySetsUnlocked(setsToUnlock, boosterPacks, notificationMessage);
        receivedAmountOfSetUnlockChecks++;
        unlockRemainingSetsIfAllReceived();
    }

    /**
     * Once every set unlock check was received, unlocks the sets that rounding left locked, without gift packs.
     * Also repairs saves that received their set unlocks before the slot's set_unlock_count was known (they were split
     * over the default of 100 checks), which is why this also runs on connect.
     */
    public void unlockRemainingSetsIfAllReceived() {
        if (totalAmountOfSetUnlockChecks <= 0 || receivedAmountOfSetUnlockChecks < totalAmountOfSetUnlockChecks) return;
        List<String> remainingSets = new ArrayList<>(allCardSets);
        remainingSets.removeAll(setsUnlockedByCode);
        if (remainingSets.isEmpty()) return;

        setsUnlockedByCode.addAll(remainingSets);
        String text = String.format("All set unlocks received, the remaining %s%d{RESET} sets are unlocked", ArchipelagoColors.Cyan, remainingSets.size());
        generateGameNotification(String.format("%s%s{RESET}%s", ArchipelagoColors.Salmon, archipelagoMode == ArchipelagoMode.networked_archipelago ? "Forge AP:\n" : "Randomizer:\n", text));
        System.out.println(text);
    }

    public void generateGameNotification(String message) {
        GameHUD.getInstance().addNotification(message, 0.5f, 3.5f, 0.5f);
    }
    /// --- End ---

    /// --- The functions below are responsible for mutating the user data we store in the save file for both modes ---
    // Todo: Make the functions below private where possible, rewrite other code to account for this.
    // Todo: Make sure the functions below trigger all related checks in `updatePlayerChecks` when applicable.

    /**
     * Records a miniboss defeat and sends its check.
     * Returns true if this name wasn't recorded as defeated before.
     */
    public boolean addMiniBossDefeated(String miniBossName) {
        boolean result = miniBossesDefeatedByName.add(miniBossName.toLowerCase());
        String notificationMessage = String.format("%sRandomizer:{RESET}\nMiniboss Defeated: " + miniBossName, ArchipelagoColors.Plum);
        System.out.println(notificationMessage);
        ArchipelagoUtil.Boss boss = ArchipelagoUtil.findBoss(miniBossName);
        if (boss != null && !boss.castleBoss) updatePlayerChecks(boss.check, notificationMessage);
        return result;
    }

    /**
     * Records a castle boss defeat and sends its check. Bosses that aren't castle bosses in ArchipelagoUtil.BOSSES (like
     * Wounded Sliver, whose deck is also in decks/boss) are ignored.
     * Returns true if this name wasn't recorded as defeated before.
     */
    public boolean addBossDefeated(String bossName) {
        ArchipelagoUtil.Boss boss = ArchipelagoUtil.findBoss(bossName);
        if (boss == null || !boss.castleBoss) return false;
        boolean result = bossesDefeatedByName.add(bossName.toLowerCase());
        String notificationMessage = String.format("%sRandomizer:{RESET}\nCastle Boss Defeated: " + bossName, ArchipelagoColors.Plum);
        System.out.println(notificationMessage);
        updatePlayerChecks(boss.check, notificationMessage);
        // The goal is reached once castles_required castle bosses are defeated.
        if (isGoalReached()) {
            updatePlayerChecks(ArchipelagoCheckTypes.WIN_CONDITION_CLEARED, notificationMessage);
        }

        return result;
    }

    // Returns true if this save has defeated the given (mini)boss.
    public boolean isBossOrMinibossDefeated(ArchipelagoUtil.Boss boss) {
        Set<String> defeatedNames = boss.castleBoss ? bossesDefeatedByName : miniBossesDefeatedByName;
        for (String defeatedName : defeatedNames) {
            if (boss.matches(defeatedName)) return true;
        }
        return false;
    }

    // Progress per region.
    public long getCompletedTownQuests(ArchipelagoRegion region) {
        return getCompletedTownQuestsByRegion(region).values().stream().mapToLong(Long::longValue).sum();
    }

    Map<String, Long> getCompletedTownQuestsByRegion(ArchipelagoRegion region) {
        return switch (region) {
            case WHITE -> whiteCompletedTownQuests;
            case BLUE -> blueCompletedTownQuests;
            case BLACK -> blackCompletedTownQuests;
            case RED -> redCompletedTownQuests;
            case GREEN -> greenCompletedTownQuests;
            case WASTE -> colorlessCompletedTownQuests;
        };
    }

    public long getCompletedTownInnEvents(ArchipelagoRegion region) {
        return getCompletedInnEventsByRegion(region).values().stream().mapToLong(Long::longValue).sum();
    }

    Map<String, Long> getCompletedInnEventsByRegion(ArchipelagoRegion region) {
        return switch (region) {
            case WHITE -> whiteCompletedTownInnEvents;
            case BLUE -> blueCompletedTownInnEvents;
            case BLACK -> blackCompletedTownInnEvents;
            case RED -> redCompletedTownInnEvents;
            case GREEN -> greenCompletedTownInnEvents;
            case WASTE -> colorlessCompletedTownInnEvents;
        };
    }

    public int getBattlesWon(ArchipelagoRegion region) {
        return switch (region) {
            case WHITE -> totalBattlesWonWhite;
            case BLUE -> totalBattlesWonBlue;
            case BLACK -> totalBattlesWonBlack;
            case RED -> totalBattlesWonRed;
            case GREEN -> totalBattlesWonGreen;
            case WASTE -> totalBattlesWonColorless;
        };
    }

    public boolean isGoalReached() {
        if (archipelagoMode == ArchipelagoMode.networked_archipelago) {
            SlotData slotData = ArchipelagoRandomizer.getInstance().getSlotData();
            return slotData != null
                    && getCastleBossesDefeatedCount() >= slotData.CastlesRequired;
        }
        return false;
    }

    // Castle bosses (the five colored castles and Emrakul) this save has defeated, i.e. the progress towards the goal.
    // Counted from the BOSSES table, so a stray name in bossesDefeatedByName (e.g. from an older save) doesn't count.
    public int getCastleBossesDefeatedCount() {
        int count = 0;
        for (ArchipelagoUtil.Boss boss : ArchipelagoUtil.BOSSES) {
            if (boss.castleBoss && isBossOrMinibossDefeated(boss)) count++;
        }
        return count;
    }

    public boolean addCardUnlockedByName(String cardName) {
        return cardsUnlockedByName.add(cardName);
    }

    public boolean addSetUnlockedByCode(String setCode) {
        return setsUnlockedByCode.add(setCode);
    }

    public void addCompletedTownInnEvents() {
        String townName = TileMapScene.instance().rootPoint.getDisplayName();
        switch (lastTraversedRegion) {
            case WASTE -> {
                colorlessCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + colorlessCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_TOWN_EVENTS, notificationMessage);
            }
            case WHITE -> {
                whiteCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + whiteCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_TOWN_EVENTS,  notificationMessage);
            }
            case BLUE -> {
                blueCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + blueCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_TOWN_EVENTS, notificationMessage);
            }
            case BLACK -> {
                blackCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + blackCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_TOWN_EVENTS, notificationMessage);
            }
            case RED -> {
                redCompletedTownInnEvents.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nInn Event Completed: " + townName + " - " + redCompletedTownInnEvents.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_TOWN_EVENTS,  notificationMessage);
            }
            case GREEN -> {
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
            case WASTE -> {
                colorlessCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + colorlessCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_TOWN_QUESTS, notificationMessage);
            }
            case WHITE -> {
                whiteCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + whiteCompletedTownQuests.get(townName),  ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_TOWN_QUESTS, notificationMessage);
            }
            case BLUE -> {
                blueCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + blueCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_TOWN_QUESTS, notificationMessage);
            }
            case BLACK -> {
                blackCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + blackCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_TOWN_QUESTS, notificationMessage);
            }
            case RED -> {
                redCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + redCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_TOWN_QUESTS, notificationMessage);
            }
            case GREEN -> {
                greenCompletedTownQuests.merge(townName, 1L, Long::sum);
                String notificationMessage = String.format("%sRandomizer:{RESET}\nQuest Completed: " + townName + " - " + greenCompletedTownQuests.get(townName), ArchipelagoColors.Plum);
                System.out.println(notificationMessage);
                updatePlayerChecks(ArchipelagoCheckTypes.GREEN_TOWN_QUESTS, notificationMessage);
            }
        }
    }

    public void addCardByRarity(String rarity) {
        cardsEarnedByRarity.merge(rarity, 1L, Long::sum);
        updatePlayerChecks(ArchipelagoCheckTypes.TOTAL_CARDS_EARNED, String.format("%sRandomizer:{RESET}\nCards of Rarity: " + rarity + " - " + cardsEarnedByRarity.get(rarity), ArchipelagoColors.Plum));
    }

    public void addGold(int amount) {
        totalGoldEarned += amount;
    }

    private void addUnlockedRegion(ArchipelagoRegion region) {
        lockedWorldRegions.remove(region);
        System.out.println("Randomizer:\n Region Unlocked: " + region.label);
    }

    // Due to MapDialog.SetEffects() using just a name string to add items to the player's inventory, it's likely that the name is unique.
    public void addItem(String itemName) {
        ArchipelagoRegion runeRegion = ArchipelagoRegion.fromRuneItemName(itemName);
        if (runeRegion != null) {
            addUnlockedRegion(runeRegion);
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
            case WASTE -> {
                totalBattlesWonColorless += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nWastes Region Battles Won: " + totalBattlesWonColorless, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.COLORLESS_BATTLE_WON, notificationMessage);
            }
            case WHITE -> {
                totalBattlesWonWhite += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nWhite Region Battles Won: " + totalBattlesWonWhite, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.WHITE_BATTLE_WON, notificationMessage);
            }
            case BLUE -> {
                totalBattlesWonBlue += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nBlue Region Battles Won: " + totalBattlesWonBlue, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.BLUE_BATTLE_WON, notificationMessage);
            }
            case BLACK -> {
                totalBattlesWonBlack += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nBlack Region Battles Won: " + totalBattlesWonBlack, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.BLACK_BATTLE_WON, notificationMessage);
            }
            case RED -> {
                totalBattlesWonRed += amount;
                String notificationMessage = String.format("%sRandomizer:{RESET}\nRed Region Battles Won: " + totalBattlesWonRed, ArchipelagoColors.Plum);
                updatePlayerChecks(ArchipelagoCheckTypes.RED_BATTLE_WON, notificationMessage);
            }
            case GREEN -> {
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

    // Regions are stored by their biome name, the same strings older saves used.
    private static void saveRegionSet(SaveFileData parent, String key, Set<ArchipelagoRegion> regions) {
        parent.storeObject(key, regions.stream().map(region -> region.biomeName).toArray(String[]::new));
    }

    private static void loadRegionSet(SaveFileData parent, String key, Set<ArchipelagoRegion> regions) {
        regions.clear();

        if (!parent.containsKey(key)) return;

        for (String biomeName : (String[]) parent.readObject(key)) {
            ArchipelagoRegion region = ArchipelagoRegion.fromBiomeName(biomeName);
            if (region != null) regions.add(region);
        }
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

    private static void saveItemDataSet(SaveFileData parent, String key, Set<ItemData> set) {
        parent.storeObject(key, set.toArray(new ItemData[0]));
    }

    private static void loadItemDataSet(SaveFileData parent, String key, Set<ItemData> set) {
        set.clear();

        if (!parent.containsKey(key)) return;

        ItemData[] values = (ItemData[]) parent.readObject(key);
        Collections.addAll(set, values);
    }

    private static void saveLongList(SaveFileData parent, String key, List<Long> list) {
        parent.storeObject(key, list.toArray(new Long[0]));
    }

    private static void loadLongList(SaveFileData parent, String key, List<Long> list) {
        list.clear();

        if (!parent.containsKey(key)) return;

        Long[] values = (Long[]) parent.readObject(key);
        Collections.addAll(list, values);
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
        ArchipelagoRandomizer networkedRandomizer = ArchipelagoRandomizer.getInstance();
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
        loadRegionSet(data, "lockedRegions", lockedWorldRegions);
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
        if (archipelagoMode == ArchipelagoMode.networked_archipelago) {
            WorldData.resetShopLists();
            loadItemDataSet(data, "colorlessEquipmentShop", networkedRandomizer.colorlessEquipmentShopList);
            loadItemDataSet(data, "whiteEquipmentShop", networkedRandomizer.whiteEquipmentShopList);
            loadItemDataSet(data, "blueEquipmentShop", networkedRandomizer.blueEquipmentShopList);
            loadItemDataSet(data, "blackEquipmentShop", networkedRandomizer.blackEquipmentShopList);
            loadItemDataSet(data, "redEquipmentShop", networkedRandomizer.redEquipmentShopList);
            loadItemDataSet(data, "greenEquipmentShop", networkedRandomizer.greenEquipmentShopList);
            loadItemDataSet(data, "whiteItemShop", networkedRandomizer.whiteItemShopList);
            loadItemDataSet(data, "blueItemShop", networkedRandomizer.blueItemShopList);
            loadItemDataSet(data, "blackItemShop", networkedRandomizer.blackItemShopList);
            loadItemDataSet(data, "redItemShop", networkedRandomizer.redItemShopList);
            loadItemDataSet(data, "greenItemShop", networkedRandomizer.greenItemShopList);
            loadLongList(data,"locationQueue", networkedRandomizer.locationQueue);
            if (data.containsKey("slotData")) networkedRandomizer.setSlotData((SlotData) data.readObject("slotData"));
            if (data.containsKey("lastIp")) networkedRandomizer.setLastIp(data.readString("lastIp"));
            if (data.containsKey("lastPort")) networkedRandomizer.setLastPort(data.readString("lastPort"));
            if (data.containsKey("lastSlotName")) networkedRandomizer.setLastSlotName(data.readString("lastSlotName"));
            if (data.containsKey("lastPassword")) networkedRandomizer.setLastPassword(data.readString("lastPassword"));
            networkedRandomizer.setupAPSettingScene();
        }

        setUnlockChecksRestAmount = data.containsKey("setUnlocksReceivedRest") ? data.readFloat("setUnlocksReceivedRest") : 0;
        receivedAmountOfSetUnlockChecks = data.containsKey("setUnlocksReceived") ? data.readInt("setUnlocksReceived") : 0;
        totalBattlesWonColorless = data.containsKey("totalBattlesWonColorless") ? data.readInt("totalBattlesWonColorless") : 0;
        totalBattlesWonWhite = data.containsKey("totalBattlesWonWhite") ? data.readInt("totalBattlesWonWhite") : 0;
        totalBattlesWonBlue = data.containsKey("totalBattlesWonBlue") ? data.readInt("totalBattlesWonBlue") : 0;
        totalBattlesWonBlack = data.containsKey("totalBattlesWonBlack") ? data.readInt("totalBattlesWonBlack") : 0;
        totalBattlesWonRed = data.containsKey("totalBattlesWonRed") ? data.readInt("totalBattlesWonRed") : 0;
        totalBattlesWonGreen = data.containsKey("totalBattlesWonGreen") ? data.readInt("totalBattlesWonGreen") : 0;
        ArchipelagoRegion savedRegion = data.containsKey("lastTraversedRegion") ? ArchipelagoRegion.fromBiomeName(data.readString("lastTraversedRegion")) : null;
        lastTraversedRegion = savedRegion != null ? savedRegion : ArchipelagoRegion.WASTE;
        totalGoldEarned = data.containsKey("totalGold") ? data.readInt("totalGold") : 0;
        totalExtraMaxLifeEarned = data.containsKey("extraLife") ? data.readInt("extraLife") : 0;
        totalShardsEarned = data.containsKey("shards") ? data.readInt("shards") : 0;
        lastArchipelagoRewardIndex = data.containsKey("lastArchipelagoRewardIndex") ? data.readInt("lastArchipelagoRewardIndex") : 0;
        setTotalAmountOfSetUnlockChecks(data.containsKey("totalSetUnlockChecks") ? data.readInt("totalSetUnlockChecks") : 100);
        GameHUD.getInstance().setApButtonVisibility(archipelagoMode == ArchipelagoMode.networked_archipelago);
        if (archipelagoMode == ArchipelagoMode.networked_archipelago && Archipelago.getInstance().isConnected()) {
            Archipelago.getInstance().disconnect();
        }
    }

    @Override
    public SaveFileData save() {
        SaveFileData data = new SaveFileData();
        LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
        ArchipelagoRandomizer networkedRandomizer = ArchipelagoRandomizer.getInstance();

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
        saveRegionSet(data, "lockedRegions", lockedWorldRegions);
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
        if (archipelagoMode == ArchipelagoMode.networked_archipelago) {
            saveItemDataSet(data, "colorlessEquipmentShop", networkedRandomizer.colorlessEquipmentShopList);
            saveItemDataSet(data, "whiteEquipmentShop", networkedRandomizer.whiteEquipmentShopList);
            saveItemDataSet(data, "blueEquipmentShop", networkedRandomizer.blueEquipmentShopList);
            saveItemDataSet(data, "blackEquipmentShop", networkedRandomizer.blackEquipmentShopList);
            saveItemDataSet(data, "redEquipmentShop", networkedRandomizer.redEquipmentShopList);
            saveItemDataSet(data, "greenEquipmentShop", networkedRandomizer.greenEquipmentShopList);
            saveItemDataSet(data, "whiteItemShop", networkedRandomizer.whiteItemShopList);
            saveItemDataSet(data, "blueItemShop", networkedRandomizer.blueItemShopList);
            saveItemDataSet(data, "blackItemShop", networkedRandomizer.blackItemShopList);
            saveItemDataSet(data, "redItemShop", networkedRandomizer.redItemShopList);
            saveItemDataSet(data, "greenItemShop", networkedRandomizer.greenItemShopList);
            saveLongList(data,"locationQueue", networkedRandomizer.locationQueue);
            data.storeObject("slotData", networkedRandomizer.getSlotData());
            data.store("lastIp", networkedRandomizer.getLastIp());
            data.store("lastPort", networkedRandomizer.getLastPort());
            data.store("lastSlotName", networkedRandomizer.getLastSlotName());
            data.store("lastPassword", networkedRandomizer.getLastPassword());
        }

        data.store("setUnlocksReceivedRest", setUnlockChecksRestAmount);
        data.store("setUnlocksReceived", receivedAmountOfSetUnlockChecks);
        data.store("totalBattlesWonColorless", totalBattlesWonColorless);
        data.store("totalBattlesWonWhite", totalBattlesWonWhite);
        data.store("totalBattlesWonBlue", totalBattlesWonBlue);
        data.store("totalBattlesWonBlack", totalBattlesWonBlack);
        data.store("totalBattlesWonRed", totalBattlesWonRed);
        data.store("totalBattlesWonGreen", totalBattlesWonGreen);
        data.store("lastTraversedRegion", lastTraversedRegion.biomeName);
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
