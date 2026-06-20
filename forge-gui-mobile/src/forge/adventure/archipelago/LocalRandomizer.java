package forge.adventure.archipelago;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.ItemData;
import forge.adventure.scene.TileMapScene;
import forge.adventure.util.*;
import org.checkerframework.checker.nullness.qual.NonNull;

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
    private final int regionUnlockChance = 10;
    private final int goldRewardChance = 20;
    private final int manaRewardChance = 20;
    private final int maxLifeRewardChance = 5;
    private final int equipmentRewardChance = 5;

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

    public void updatePlayerChecks(ArchipelagoCheckTypes type, @NonNull String notificationMessage) {
        switch (type) {
            case TOTAL_CARDS_EARNED -> {
                long totalCardsEarned = 0;
                for (long value : archipelagoDataInstance.cardsEarnedByRarity.values()) {
                    totalCardsEarned += value;
                }
                if (totalCardsEarned > 0 && totalCardsEarned % totalCardsEarnedBreakPoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case COLORLESS_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonColorless > 0 && archipelagoDataInstance.totalBattlesWonColorless % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case WHITE_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonWhite > 0 && archipelagoDataInstance.totalBattlesWonWhite % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case BLUE_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonBlue > 0 && archipelagoDataInstance.totalBattlesWonBlue % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case BLACK_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonBlack > 0 && archipelagoDataInstance.totalBattlesWonBlack % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case RED_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonRed > 0 && archipelagoDataInstance.totalBattlesWonRed % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case GREEN_BATTLE_WON -> {
                if (archipelagoDataInstance.totalBattlesWonGreen > 0 && archipelagoDataInstance.totalBattlesWonGreen % totalBattlesWonBreakpoint == 0) {
                    generateRandomizedReward(notificationMessage);
                }
            }
            case COLORLESS_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.colorlessCompletedTownInnEvents, notificationMessage);
            case WHITE_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.whiteCompletedTownInnEvents, notificationMessage);
            case BLUE_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.blueCompletedTownInnEvents, notificationMessage);
            case BLACK_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.blackCompletedTownInnEvents, notificationMessage);
            case RED_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.redCompletedTownInnEvents, notificationMessage);
            case GREEN_TOWN_EVENTS -> handleTownEventDone(archipelagoDataInstance.greenCompletedTownInnEvents, notificationMessage);
            case COLORLESS_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.colorlessCompletedTownQuests, notificationMessage);
            case WHITE_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.whiteCompletedTownQuests, notificationMessage);
            case BLUE_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.blueCompletedTownQuests, notificationMessage);
            case BLACK_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.blackCompletedTownQuests, notificationMessage);
            case RED_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.redCompletedTownQuests, notificationMessage);
            case GREEN_TOWN_QUESTS -> handleTownQuestDone(archipelagoDataInstance.greenCompletedTownQuests, notificationMessage);
        }
    }

    private void handleTownEventDone(Map<String, Long> completedTownEventsList, String notificationMessage) {
        int totalTownEventsDone = 0;
        for (long count : completedTownEventsList.values()) {
            totalTownEventsDone += (int) count;
        }
        if (archipelagoDataInstance.archipelagoMode == ArchipelagoMode.solo_randomizer) {
            if (totalTownEventsDone > 0 && totalTownEventsDone % totalTownEventsBreakpoint == 0) {
                generateRandomizedReward(notificationMessage);
            }
        }
    }

    private void handleTownQuestDone(Map<String, Long> completedTownQuestsList, String notificationMessage) {
        int totalTownQuestsDone = 0;
        for (long count : completedTownQuestsList.values()) {
            totalTownQuestsDone += (int) count;
        }
        if (archipelagoDataInstance.archipelagoMode == ArchipelagoMode.solo_randomizer) {
            if (totalTownQuestsDone > 0 && totalTownQuestsDone % totalTownQuestsBreakpoint == 0) {
                generateRandomizedReward(notificationMessage);
            }
        }
    }

    // Randomly picks between the 4 reward options.
    private void generateRandomizedReward(String notificationMessage) {
        Random random = new Random();
        int roll;
        if (archipelagoDataInstance.lockedWorldRegionsByName.isEmpty()) {
            // Skip the region reward chance if the pool is empty.
            roll = random.nextInt(regionUnlockChance, 100);
        } else {
            roll = random.nextInt(100);
        }

        // Guarantee a region reward after X checks where X is regionUnlockBreakpoint.
        if (!archipelagoDataInstance.lockedWorldRegionsByName.isEmpty() && (roll < regionUnlockChance || checksSinceLastRegionReward >= regionUnlockBreakpoint)) {
            unlockRandomRegion(notificationMessage);
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
                archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s%s{RESET}", ArchipelagoColors.Cyan, goldAmount, "G"));
                archipelagoDataInstance.unlockGoldReward(goldAmount);
            } else if (roll < regionUnlockChance + goldRewardChance + manaRewardChance) {
                roll = random.nextInt(3);
                int manaAmount = 20;
                switch (roll) {
                    case 1 -> manaAmount = 30;
                    case 2 -> manaAmount = 50;
                }
                archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s%s{RESET}", ArchipelagoColors.Cyan, manaAmount, " Shards"));
                archipelagoDataInstance.unlockManaCrystalReward(manaAmount);
            } else if (roll < regionUnlockChance + goldRewardChance + manaRewardChance + maxLifeRewardChance) {
                archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s{RESET}", ArchipelagoColors.Cyan, "Max Life +1"));
                unlockMaxLifeReward(1);
            } else if (roll < regionUnlockChance + goldRewardChance + manaRewardChance + maxLifeRewardChance + equipmentRewardChance) {
                Reward reward = takeSingleEquipmentOutOfRemainingPool();
                switch (reward.getType()) {
                    case Gold -> {
                        archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s%s{RESET}", ArchipelagoColors.Cyan, reward.getCount(), "G"));
                        archipelagoDataInstance.unlockGoldReward(reward.getCount());
                    }
                    case Shards -> {
                        archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s%s{RESET}", ArchipelagoColors.Cyan, reward.getCount(), "S"));
                        archipelagoDataInstance.unlockManaCrystalReward(reward.getCount());
                    }
                    case Item -> {
                        archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\n%s%s{RESET}", ArchipelagoColors.Cyan, reward.getItem().name));
                        unlockItemReward(reward.getItem().name);
                    }
                }
            } else {
                // If no other change was hit, generate a set unlock. At the moment there is a 40% chance of this by default which becomes 50% once all regions are unlocked.
                archipelagoDataInstance.unlockRandomSet(notificationMessage);
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
                        return new Reward(Reward.Type.Shards, 50);
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

    public void unlockMaxLifeReward(int amount) {
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Randomizer:\n", "Max Life Reward: ", ArchipelagoColors.Cyan, amount));
        archipelagoDataInstance.addMaxLife(amount);
    }

    public void unlockItemReward(String itemName) {
        Current.player().addItem(itemName);
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Randomizer:\n", "Item Reward: ", ArchipelagoColors.Cyan, itemName));
        archipelagoDataInstance.addItem(itemName);
    }

    public void unlockRandomRegion(String notificationMessage) {
        if (archipelagoDataInstance.lockedWorldRegionsByName.isEmpty()) {
            return;
        }

        int targetRegionIndex = new Random().nextInt(archipelagoDataInstance.lockedWorldRegionsByName.size());
        int setIndex = 0;
        for (String region : archipelagoDataInstance.lockedWorldRegionsByName) {
            if (setIndex++ == targetRegionIndex) {
                for (String runeName : archipelagoDataInstance.regionTeleportingRunes) {
                    if (runeName.toLowerCase().contains(region.toLowerCase())) {
                        archipelagoDataInstance.generateGameNotification(String.format(notificationMessage + ":\nRegion Rune: %s%s{RESET}", ArchipelagoColors.Cyan, runeName));
                        Current.player().addItem(runeName);
                        return;
                    }
                }
            }
        }
    }
}
/// --- End ---
