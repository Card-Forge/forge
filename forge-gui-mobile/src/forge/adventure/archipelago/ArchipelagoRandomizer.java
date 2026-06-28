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

    public void updatePlayerChecks(ArchipelagoCheckTypes type) {
        if (slotData == null) {
            System.err.print("SlotData was null somehow. Should be impossible.");
        } else {
            switch (type) {
                case TOTAL_CARDS_EARNED -> {
                    // Todo: Signal the APWorld that the next card location is triggered
                }
                case COLORLESS_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonColorless > 0 && archipelagoDataInstance.totalBattlesWonColorless % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonColorless / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(9999L + archipelagoDataInstance.totalBattlesWonColorless / slotData.FightsPerLocation);
                    }
                }
                case WHITE_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonWhite > 0 && archipelagoDataInstance.totalBattlesWonWhite % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonWhite / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(19999L + archipelagoDataInstance.totalBattlesWonWhite / slotData.FightsPerLocation);
                    }
                }
                case BLUE_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonBlue > 0 && archipelagoDataInstance.totalBattlesWonBlue % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonBlue / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(29999L + archipelagoDataInstance.totalBattlesWonBlue / slotData.FightsPerLocation);
                    }
                }
                case BLACK_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonBlack > 0 && archipelagoDataInstance.totalBattlesWonBlack % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonBlack / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(39999L + archipelagoDataInstance.totalBattlesWonBlack / slotData.FightsPerLocation);
                    }
                }
                case RED_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonRed > 0 && archipelagoDataInstance.totalBattlesWonRed % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonRed / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(49999L + archipelagoDataInstance.totalBattlesWonRed / slotData.FightsPerLocation);
                    }
                }
                case GREEN_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonGreen > 0 && archipelagoDataInstance.totalBattlesWonGreen % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonGreen / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(59999L + archipelagoDataInstance.totalBattlesWonGreen / slotData.FightsPerLocation);
                    }
                }
                case COLORLESS_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.colorlessCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.colorlessCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(10999L + archipelagoDataInstance.colorlessCompletedTownInnEvents.size());
                    }
                }
                case WHITE_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.whiteCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.whiteCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(20999L + archipelagoDataInstance.whiteCompletedTownInnEvents.size());
                    }
                }
                case BLUE_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.blueCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.blueCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(30999L + archipelagoDataInstance.blueCompletedTownInnEvents.size());
                    }
                }
                case BLACK_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.blackCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.blackCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(40999L + archipelagoDataInstance.blackCompletedTownInnEvents.size());
                    }
                }
                case RED_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.redCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.redCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(50999L + archipelagoDataInstance.redCompletedTownInnEvents.size());
                    }
                }
                case GREEN_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.greenCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.greenCompletedTownInnEvents.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(60999L + archipelagoDataInstance.greenCompletedTownInnEvents.size());
                    }
                }
                case COLORLESS_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.colorlessCompletedTownQuests.isEmpty() && archipelagoDataInstance.colorlessCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(11999L + archipelagoDataInstance.colorlessCompletedTownQuests.size());
                    }
                }
                case WHITE_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.whiteCompletedTownQuests.isEmpty() && archipelagoDataInstance.whiteCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(21999L + archipelagoDataInstance.whiteCompletedTownQuests.size());
                    }
                }
                case BLUE_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.blueCompletedTownQuests.isEmpty() && archipelagoDataInstance.blueCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(31999L + archipelagoDataInstance.blueCompletedTownQuests.size());
                    }
                }
                case BLACK_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.blackCompletedTownQuests.isEmpty() && archipelagoDataInstance.blackCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(41999L + archipelagoDataInstance.blackCompletedTownQuests.size());
                    }
                }
                case RED_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.redCompletedTownQuests.isEmpty() && archipelagoDataInstance.redCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(51999L + archipelagoDataInstance.redCompletedTownQuests.size());
                    }
                }
                case GREEN_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.greenCompletedTownQuests.isEmpty() && archipelagoDataInstance.greenCompletedTownQuests.size() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(61999L + archipelagoDataInstance.greenCompletedTownQuests.size());
                    }
                }
                case SLIME_MOTHER_DEFEATED -> Archipelago.getInstance().checkLocation(100L);
                case SLOBAD_DEFEATED -> Archipelago.getInstance().checkLocation(101L);
                case XIRA_DEFEATED -> Archipelago.getInstance().checkLocation(102L);
                case NAHIRI_DEFEATED -> Archipelago.getInstance().checkLocation(200L);
                case VALYX_DEFEATED -> Archipelago.getInstance().checkLocation(201L);
                case JACE_DEFEATED -> Archipelago.getInstance().checkLocation(300L);
                case KIORA_DEFEATED -> Archipelago.getInstance().checkLocation(301L);
                case MYR_SUPERION_DEFEATED -> Archipelago.getInstance().checkLocation(302L);
                case SLIVER_QUEEN_DEFEATED -> Archipelago.getInstance().checkLocation(303L);
                case TEFERI_DEFEATED -> Archipelago.getInstance().checkLocation(304L);
                case GROLNOK_DEFEATED -> Archipelago.getInstance().checkLocation(400L);
                case GUARDIAN_ANGEL_DEFEATED -> Archipelago.getInstance().checkLocation(401L);
                case LILIANA_DEFEATED -> Archipelago.getInstance().checkLocation(402L);
                case SLIMEFOOT_DEFEATED -> Archipelago.getInstance().checkLocation(403L);
                case SORIN_DEFEATED -> Archipelago.getInstance().checkLocation(404L);
                case CHANDRA_DEFEATED -> Archipelago.getInstance().checkLocation(500L);
                case TIBALTS_TORTURER_DEFEATED -> Archipelago.getInstance().checkLocation(501L);
                case TIBALT_DEFEATED -> Archipelago.getInstance().checkLocation(502L);
                case ZEDRUUS_COOK_DEFEATED -> Archipelago.getInstance().checkLocation(503L);
                case CONJURER_DEFEATED -> Archipelago.getInstance().checkLocation(504L);
                case ZEDRUU_DEFEATED -> Archipelago.getInstance().checkLocation(505L);
                case GARRUK_DEFEATED -> Archipelago.getInstance().checkLocation(600L);
                case HYDRA_OF_SHANDALAAR_DEFEATED -> Archipelago.getInstance().checkLocation(601L);
                case SCARECROW_CAPTAIN_DEFEATED -> Archipelago.getInstance().checkLocation(602L);
                case BOSS_COLORLESS_DEFEATED -> Archipelago.getInstance().checkLocation(1L);
                case BOSS_WHITE_DEFEATED -> Archipelago.getInstance().checkLocation(2L);
                case BOSS_BLUE_DEFEATED -> Archipelago.getInstance().checkLocation(3L);
                case BOSS_BLACK_DEFEATED -> Archipelago.getInstance().checkLocation(4L);
                case BOSS_RED_DEFEATED -> Archipelago.getInstance().checkLocation(5L);
                case BOSS_GREEN_DEFEATED -> Archipelago.getInstance().checkLocation(6L);
                case WIN_CONDITION_CLEARED -> Archipelago.getInstance().goal();
            }
        }
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

    public void unlockCardPackReward(String boosterPackName) {
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Forge AP:\n", "Card Pack Reward: ", ArchipelagoColors.Cyan, boosterPackName));
        archipelagoDataInstance.addPack(boosterPackName);
    }

    public void unlockMaxLifeReward(int amount) {
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Forge AP:\n", "Max Life Reward: ", ArchipelagoColors.Cyan, amount));
        archipelagoDataInstance.addMaxLife(amount);
    }

    public void unlockItemReward(String itemName) {
        Current.player().addItem(itemName);
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Forge AP:\n", "Item Reward: ", ArchipelagoColors.Cyan, itemName));
        archipelagoDataInstance.addItem(itemName);
    }

    public void unlockManaCrystalReward(Integer amount) {
        Current.player().addShards(amount);
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Forge AP:\n", "Shard Reward: ", ArchipelagoColors.Cyan, amount));
        archipelagoDataInstance.addShards(amount);
    }

    public void unlockGoldReward(int amount) {
        Current.player().giveGold(amount);
        System.out.println(String.format("%s%s{RESET}%s%s%s{RESET}", ArchipelagoColors.Salmon, "Forge AP:\n", "Gold Reward: ", ArchipelagoColors.Cyan, amount));
        archipelagoDataInstance.addGold(amount);
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
