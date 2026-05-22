package forge.adventure.archipelago;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.ItemData;
import forge.adventure.util.*;

import java.util.*;

/// --- The checks below are mostly functional offline and should not be called from the networked part of the AP implementation. ---
public class LocalRandomizer extends ArchipelagoData {
    protected static LocalRandomizer localRandomizerInstance = null;

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

    public LocalRandomizer() {
        localRandomizerInstance = this;
    }

    public static LocalRandomizer getInstance() {
        if (archipelagoDataInstance == null) {
            ArchipelagoData.getInstance();
        }
        return localRandomizerInstance == null ? localRandomizerInstance = new LocalRandomizer() : localRandomizerInstance;
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
        ArchipelagoData.getInstance().setupFreshSaveFile(ArchipelagoMode.solo_randomizer);
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
}
/// --- End ---
