package forge.adventure.archipelago;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.ItemData;
import forge.adventure.util.*;

import java.util.*;

/// --- The checks below are mostly functional offline and should not be called from the networked part of the AP implementation. ---
public class LocalRandomizer {
    private static final LocalRandomizer localRandomizerInstance = new LocalRandomizer();
    private final ArchipelagoData archipelagoDataInstance;

    protected final Set<String> colorlessEquipmentShopList = new HashSet<>();
    protected final Set<String> whiteEquipmentShopList = new HashSet<>();
    protected final Set<String> blueEquipmentShopList = new HashSet<>();
    protected final Set<String> blackEquipmentShopList = new HashSet<>();
    protected final Set<String> redEquipmentShopList = new HashSet<>();
    protected final Set<String> greenEquipmentShopList = new HashSet<>();
    protected final Set<String> whiteItemShopList = new HashSet<>();
    protected final Set<String> blueItemShopList = new HashSet<>();
    protected final Set<String> blackItemShopList = new HashSet<>();
    protected final Set<String> redItemShopList = new HashSet<>();
    protected final Set<String> greenItemShopList = new HashSet<>();
    protected final Set<String> remainingEquipmentPool = new HashSet<>();
    protected int checksSinceLastRegionReward = 0;

    private final int totalBattlesWonBreakpoint = 3; // Reward for every 3 battles won.
    private final int totalTownQuestsBreakpoint = 1; // Reward for every 1 town quests done.
    private final int totalTownEventsBreakpoint = 1; // Reward for every 1 town events done.
    private final int totalCardsEarnedBreakPoint = 80; // Reward for every 80 unique cards gained.
    private final int regionUnlockBreakpoint = 8;
    private final int regionUnlockChance = 15;
    private final int goldRewardChance = 25;
    private final int manaRewardChance = 25;

    private LocalRandomizer() {
        archipelagoDataInstance = ArchipelagoData.getInstance();
    }

    public static LocalRandomizer getInstance() {
        return localRandomizerInstance;
    }

    public void setupFreshSaveFile() {
        colorlessEquipmentShopList.clear();
        whiteEquipmentShopList.clear();
        blueEquipmentShopList.clear();
        blackEquipmentShopList.clear();
        redEquipmentShopList.clear();
        greenEquipmentShopList.clear();
        whiteItemShopList.clear();
        blueItemShopList.clear();
        blackItemShopList.clear();
        redItemShopList.clear();
        greenItemShopList.clear();
        remainingEquipmentPool.clear();
        randomizeLocalEquipment();
        ArchipelagoData.getInstance().setupFreshSaveFile(ArchipelagoMode.solo_randomizer);
    }

    public void updatePlayerChecks(ArchipelagoCheckTypes type) {
        switch (type) {
            case TOTAL_CARDS_EARNED -> {
                long totalCardsEarned = 0;
                for (long value : archipelagoDataInstance.cardsEarnedByRarity.values()) {
                    totalCardsEarned += value;
                }
                if (totalCardsEarned > 0 && totalCardsEarned % totalCardsEarnedBreakPoint == 0) {
                    generateRandomizedReward();
                }
            }
            case COLORLESS_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonColorless > 0 && archipelagoDataInstance.totalBattlesWonColorless % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case WHITE_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonWhite > 0 && archipelagoDataInstance.totalBattlesWonWhite % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case BLUE_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonBlue > 0 && archipelagoDataInstance.totalBattlesWonBlue % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case BLACK_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonBlack > 0 && archipelagoDataInstance.totalBattlesWonBlack % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case RED_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonRed > 0 && archipelagoDataInstance.totalBattlesWonRed % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case GREEN_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonGreen > 0 && archipelagoDataInstance.totalBattlesWonGreen % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward();
                }
            }
            case COLORLESS_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.colorlessCompletedTownInnEvents);
            case WHITE_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.whiteCompletedTownInnEvents);
            case BLUE_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.blueCompletedTownInnEvents);
            case BLACK_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.blackCompletedTownInnEvents);
            case RED_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.redCompletedTownInnEvents);
            case GREEN_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.greenCompletedTownInnEvents);
            case COLORLESS_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.colorlessCompletedTownQuests);
            case WHITE_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.whiteCompletedTownQuests);
            case BLUE_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.blueCompletedTownQuests);
            case BLACK_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.blackCompletedTownQuests);
            case RED_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.redCompletedTownQuests);
            case GREEN_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.greenCompletedTownQuests);
        }
    }

    private void handleTownEventDone(Map<String, Long> completedTownEventsList) {
        int totalTownEventsDone = 0;
        for (long count : completedTownEventsList.values()) {
            totalTownEventsDone += (int) count;
        }
        if (archipelagoDataInstance.archipelagoMode == ArchipelagoMode.solo_randomizer) {
            LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
            if (totalTownEventsDone > 0 && totalTownEventsDone % totalTownEventsBreakpoint == 0) {
                generateRandomizedReward();
            }
        }
    }

    private void handleTownQuestDone(Map<String, Long> completedTownQuestsList) {
        int totalTownQuestsDone = 0;
        for (long count : completedTownQuestsList.values()) {
            totalTownQuestsDone += (int) count;
        }
        if (archipelagoDataInstance.archipelagoMode == ArchipelagoMode.solo_randomizer) {
            LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
            if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                generateRandomizedReward();
            }
        }
    }

    // Randomly picks between the 4 reward options.
    private void generateRandomizedReward() {
        Random random = new Random();
        int roll = random.nextInt(100);
        // Guarantee a region reward after X checks where X is regionUnlockBreakpoint.
        if (roll < regionUnlockChance || checksSinceLastRegionReward >= regionUnlockBreakpoint) {
            unlockRandomRegion();
            checksSinceLastRegionReward = 0;
        } else {
            checksSinceLastRegionReward++;
            if (roll < regionUnlockChance + goldRewardChance) {
                roll = random.nextInt(3);
                int goldAmount = 750;
                switch (roll) {
                    case 1 -> goldAmount = 1500;
                    case 2 -> goldAmount = 3000;
                }
                archipelagoDataInstance.unlockGoldReward(goldAmount);
            } else if (roll < regionUnlockChance + goldRewardChance + manaRewardChance) {
                roll = random.nextInt(3);
                int manaAmount = 20;
                switch (roll) {
                    case 1 -> manaAmount = 30;
                    case 2 -> manaAmount = 50;
                }
                archipelagoDataInstance.unlockManaCrystalReward(manaAmount);
            } else {
                // If no other change was hit, generate a set unlock. At the moment there is a 35% chance of this.
                archipelagoDataInstance.unlockRandomSet();
            }
        }
    }

    // Todo: Make the max life upgrades available through another method (more checks perhaps)
    // Todo: Figure out what to do with the "overpowered" equipment.
    // Each reward has a RewardType of "item" and comes pre-defined with an itemName.
    //  They are defined in Shandalar/Shops.json as Equipment, <Color>Item and <Color>Equipment. We can dynamically detect those names and replace their items if AP mode is enabled here.
    //  Equipment: 6 slots to randomize
    //  <Color>Equipment: 6 slots to randomize
    //  <Color>Items: 8 slots to randomize including 1 slot that is not equipment but rather a max health upgrade
    public void randomizeLocalEquipment() {
        // First we get the names of all the items in the pool.
        ArrayList<String> equipmentNames = new ArrayList<>();
        // We also filter out the "overpowered" cards into this separate list, they might be fun to throw in later.
        ArrayList<String> powerEquipmentNames = new ArrayList<>();
        FileHandle handle = Config.instance().getFile(Paths.ITEMS);
        if (handle.exists()) {
            Json json = new Json();
            Array<ItemData> shopList = json.fromJson(Array.class, ItemData.class, handle);

            // First we read all the items from `adventure/common/world/items.json`
            for (int i = 0; i < shopList.size; i++) {
                if (shopList.get(i).equipmentSlot != null && !shopList.get(i).equipmentSlot.isEmpty()) {
                    // We've found an equipment item, add it to the list but exclude the "overpowered" items.
                    if (shopList.get(i).name.toLowerCase().contains("rune")) continue;
                    if (shopList.get(i).name.toLowerCase().contains("mox") || shopList.get(i).name.toLowerCase().contains("black lotus") || shopList.get(i).name.toLowerCase().contains("sol ring") || shopList.get(i).name.toLowerCase().contains("cheat")) {
                        powerEquipmentNames.add(shopList.get(i).name);
                        continue;
                    }
                    equipmentNames.add(shopList.get(i).name);
                }
            }
        }

        // Scramble the equipment names
        Collections.shuffle(equipmentNames);
        // Return if we didn't find enough items.
        if (equipmentNames.size() < 72) return;
        // Redistribute the items over each list.
        LocalRandomizer localRandomizer = LocalRandomizer.getInstance();
        colorlessEquipmentShopList.addAll(equipmentNames.subList(0, 6));
        whiteEquipmentShopList.addAll(equipmentNames.subList(6, 12));
        blueEquipmentShopList.addAll(equipmentNames.subList(12, 18));
        blackEquipmentShopList.addAll(equipmentNames.subList(18, 24));
        redEquipmentShopList.addAll(equipmentNames.subList(24, 30));
        greenEquipmentShopList.addAll(equipmentNames.subList(30, 36));
        whiteItemShopList.addAll(equipmentNames.subList(36, 43));
        blueItemShopList.addAll(equipmentNames.subList(43, 50));
        blackItemShopList.addAll(equipmentNames.subList(50, 57));
        redItemShopList.addAll(equipmentNames.subList(57, 64));
        greenItemShopList.addAll(equipmentNames.subList(64, 71));
        // Remove the first 72 items from the equipment list so we can use the remaining ones for future equipment rewards.
        equipmentNames.removeAll(equipmentNames.subList(0, 71));
        remainingEquipmentPool.addAll(equipmentNames);
    }

    // Todo: Expand this function to be able to return a list of rewards so that we can award the player gold, shards & a pack in place of their item.
    // Returns & rewards the player a random item from the remainingEquipmentPool and then removes it from the list. If no item is left, the player is instead rewarded with gold or shards.
    // This produces a different result each time it's rolled, the RNG isn't seeded.
    public Reward takeSingleEquipmentOutOfRemainingPool() {
        Random random = new Random();
        while (true) {
            if (!remainingEquipmentPool.isEmpty()) {
                List<String> remainingEquipmentList = new ArrayList<>(remainingEquipmentPool);
                String equipmentCandidate = remainingEquipmentList.get(random.nextInt(remainingEquipmentList.size()));
                remainingEquipmentPool.remove(equipmentCandidate);
                return ArchipelagoUtil.generateReward("item", 1, equipmentCandidate);
            } else {
                // Generate a random standard reward in place of the item.
                int chosenItemType = random.nextInt(2);
                switch (chosenItemType) {
                    case 0:
                        return new Reward(Reward.Type.Gold, 3000);
                    case 1:
                        return new Reward(Reward.Type.Shards, 75);
                }
            }
        }
    }

    // Todo: Create a function that returns a list of equipment for any given shop to sell based on the previously randomized lists.
    public Object[] getItemsForEquipmentShop(String shopName) {
        if (shopName.toLowerCase().contains("items")) {
            // Items shop name
            if (shopName.toLowerCase().contains("white")) {
                return whiteItemShopList.toArray();
            } else if (shopName.toLowerCase().contains("blue")) {
                return blueItemShopList.toArray();
            } else if (shopName.toLowerCase().contains("black")) {
                return blackItemShopList.toArray();
            } else if (shopName.toLowerCase().contains("red")) {
                return redItemShopList.toArray();
            } else if (shopName.toLowerCase().contains("green")) {
                return greenItemShopList.toArray();
            }
        } else {
            // Equipment shop name
            if (shopName.toLowerCase().contains("white")) {
                return whiteEquipmentShopList.toArray();
            } else if (shopName.toLowerCase().contains("blue")) {
                return blueEquipmentShopList.toArray();
            } else if (shopName.toLowerCase().contains("black")) {
                return blackEquipmentShopList.toArray();
            } else if (shopName.toLowerCase().contains("red")) {
                return redEquipmentShopList.toArray();
            } else if (shopName.toLowerCase().contains("green")) {
                return greenEquipmentShopList.toArray();
            } else {
                return colorlessEquipmentShopList.toArray();
            }
        }
        return null;
    }

    public void unlockRandomRegion() {
        if (archipelagoDataInstance.lockedWorldRegionsByName.isEmpty()) {
            return;
        }

        int targetRegionIndex = new Random().nextInt(archipelagoDataInstance.lockedWorldRegionsByName.size());
        int setIndex = 0;
        for (String region : archipelagoDataInstance.lockedWorldRegionsByName) {
            if (setIndex++ == targetRegionIndex) {
                for (String runeName : archipelagoDataInstance.regionTeleportingRunes) {
                    if (runeName.toLowerCase().contains(region.toLowerCase())) {
                        Current.player().addItem(runeName);
                        return;
                    }
                }
            }
        }
    }
}
/// --- End ---
