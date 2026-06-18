package forge.adventure.archipelago;
import forge.adventure.data.ItemData;
import forge.adventure.scene.ArchipelagoSettingsScene;
import forge.adventure.util.Current;
import io.github.archipelagomw.parts.NetworkItem;

import java.util.*;

public class ArchipelagoRandomizer {
    private static final ArchipelagoRandomizer archipelagoRandomizerInstance = new ArchipelagoRandomizer();
    private final ArchipelagoData archipelagoDataInstance;

    protected final Set<ItemData> colorlessEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> whiteEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> blueEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> blackEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> redEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> greenEquipmentShopList = new HashSet<>();
    protected final Set<ItemData> whiteItemShopList = new HashSet<>();
    protected final Set<ItemData> blueItemShopList = new HashSet<>();
    protected final Set<ItemData> blackItemShopList = new HashSet<>();
    protected final Set<ItemData> redItemShopList = new HashSet<>();
    protected final Set<ItemData> greenItemShopList = new HashSet<>();
    protected final Set<ItemData> remainingEquipmentPool = new HashSet<>();
    protected final List<Long> locationQueue = new ArrayList<>();
    private SlotData slotData;
    private String lastIp = "";
    private String lastPort = "";
    private String lastSlotName = "";
    private String lastPassword = "";

    private ArchipelagoRandomizer() {
        archipelagoDataInstance = ArchipelagoData.getInstance();
    }

    public static ArchipelagoRandomizer getInstance() {
        return archipelagoRandomizerInstance;
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
        locationQueue.clear();
        slotData = null;
        ArchipelagoData.getInstance().setupFreshSaveFile(ArchipelagoMode.networked_archipelago);
    }

    public void unlockManaCrystalReward(Integer amount) {
        Current.player().addShards(amount);
        archipelagoDataInstance.addShards(amount);
    }

    public void unlockGoldReward(int amount) {
        Current.player().giveGold(amount);
        archipelagoDataInstance.addGold(amount);
    }

    // Todo: Verify that this is actually what we want and it's working
    public void unlockMultipleItemReward(Map<String, Integer> itemNamesAndAmounts) {
        for (Map.Entry<String, Integer> item : itemNamesAndAmounts.entrySet()) {
            for (int i = 0; i < item.getValue(); i++) {
                Current.player().addItem(item.getKey());
                archipelagoDataInstance.addItem(item.getKey());
            }
        }
    }

    public void unlockItemReward(String itemName) {
        Current.player().addItem(itemName);
        archipelagoDataInstance.addItem(itemName);
    }

    public void handleShopData(List<NetworkItem> shopLocationScounts) {
        for (NetworkItem shopLocation : shopLocationScounts) {
            if (shopLocation.locationID >= 1000 && shopLocation.locationID < 1100) {
                colorlessEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1100 && shopLocation.locationID < 1200) {
                whiteEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1200 && shopLocation.locationID < 1300) {
                whiteItemShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1300 && shopLocation.locationID < 1400) {
                blueEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1400 && shopLocation.locationID < 1500) {
                blueItemShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1500 && shopLocation.locationID < 1600) {
                blackEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1600 && shopLocation.locationID < 1700) {
                blackItemShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1700 && shopLocation.locationID < 1800) {
                redEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1800 && shopLocation.locationID < 1900) {
                redItemShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 1900 && shopLocation.locationID < 2000) {
                greenEquipmentShopList.add(new ItemData(shopLocation));
            } else if (shopLocation.locationID >= 2000 && shopLocation.locationID < 2100) {
                greenItemShopList.add(new ItemData(shopLocation));
            }
        }
        ArchipelagoData.getInstance().setTotalAmountOfSetUnlockChecks(slotData.SetUnlockCount);
    }

    public Set<ItemData> getShopItems(String shopName) {
        if (shopName.toLowerCase().contains("items")) {
            // Items shop name
            if (shopName.toLowerCase().contains("white")) {
                return whiteItemShopList;
            } else if (shopName.toLowerCase().contains("blue")) {
                return blueItemShopList;
            } else if (shopName.toLowerCase().contains("black")) {
                return blackItemShopList;
            } else if (shopName.toLowerCase().contains("red")) {
                return redItemShopList;
            } else if (shopName.toLowerCase().contains("green")) {
                return greenItemShopList;
            }
        } else {
            // Equipment shop name
            if (shopName.toLowerCase().contains("white")) {
                return whiteEquipmentShopList;
            } else if (shopName.toLowerCase().contains("blue")) {
                return blueEquipmentShopList;
            } else if (shopName.toLowerCase().contains("black")) {
                return blackEquipmentShopList;
            } else if (shopName.toLowerCase().contains("red")) {
                return redEquipmentShopList;
            } else if (shopName.toLowerCase().contains("green")) {
                return greenEquipmentShopList;
            } else {
                return colorlessEquipmentShopList;
            }
        }
        return null;
    }

    public void setSlotData(SlotData slotData) {
        this.slotData = slotData;
    }

    public SlotData getSlotData() {
        return slotData;
    }

    public void setLastIp(String ip) {
        lastIp = ip;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastPort(String port) {
        lastPort = port;
    }

    public String getLastPort() {
        return lastPort;
    }

    public void setLastSlotName(String slotName) {
        lastSlotName = slotName;
    }

    public String getLastSlotName() {
        return lastSlotName;
    }

    public void setLastPassword(String password) {
        lastPassword = password;
    }

    public String getLastPassword() {
        return lastPassword;
    }

    public void sendQueuedLocations() {
        Archipelago AP = Archipelago.getInstance();
        if (!locationQueue.isEmpty() && AP.checkLocations(locationQueue)) locationQueue.clear();
    }

    public void setupAPSettingScene() {
        ArchipelagoSettingsScene apSettingsScene = ArchipelagoSettingsScene.instance();
        apSettingsScene.setIpTextField(lastIp);
        apSettingsScene.setPortTextField(lastPort);
        apSettingsScene.setSlotNameTextField(lastSlotName);
        apSettingsScene.setPasswordTextField(lastPassword);
    }

    public void incrementLastArchipelagoRewardIndex() {
        archipelagoDataInstance.lastArchipelagoRewardIndex++;
    }

    public int getLastArchipelagoRewardIndex() {
        return archipelagoDataInstance.lastArchipelagoRewardIndex;
    }
}
