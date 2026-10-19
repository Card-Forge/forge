package forge.adventure.archipelago;
import com.badlogic.gdx.Gdx;
import forge.adventure.data.ItemData;
import forge.adventure.scene.ArchipelagoSettingsScene;
import forge.adventure.util.Current;
import io.github.archipelagomw.parts.NetworkItem;

import java.util.*;

public class ArchipelagoRandomizer {
    private static final ArchipelagoRandomizer archipelagoRandomizerInstance = new ArchipelagoRandomizer();
    private final ArchipelagoData archipelagoDataInstance;
    protected final Set<ItemData> colorlessEquipmentShopList = new LinkedHashSet<>();
    protected final Set<ItemData> whiteEquipmentShopList = new LinkedHashSet<>();
    protected final Set<ItemData> blueEquipmentShopList = new LinkedHashSet<>();
    protected final Set<ItemData> blackEquipmentShopList = new LinkedHashSet<>();
    protected final Set<ItemData> redEquipmentShopList = new LinkedHashSet<>();
    protected final Set<ItemData> greenEquipmentShopList = new LinkedHashSet<>();
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
        clearShopData();
        remainingEquipmentPool.clear();
        locationQueue.clear();
        slotData = null;
        ArchipelagoData.getInstance().setupFreshSaveFile(ArchipelagoMode.networked_archipelago);
    }

    private void clearShopData() {
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
    }

    public void updatePlayerChecks(ArchipelagoCheckTypes type) {
        if (slotData == null) {
            System.err.print("SlotData was null somehow. Should be impossible.");
        } else {
            switch (type) {
                case TOTAL_CARDS_EARNED -> {
                    for (Map.Entry<String, Long> entry : archipelagoDataInstance.cardsEarnedByRarity.entrySet()) {
                        switch (entry.getKey()) {
                            case "C" -> {
                                if (entry.getValue() > 0 && entry.getValue() % slotData.CommonCardsPerLocation == 0 && entry.getValue() / slotData.CommonCardsPerLocation <= slotData.CommonCardLocations) {
                                    Archipelago.getInstance().checkLocation(ArchipelagoUtil.COMMON_CARD_LOCATION_BASE + entry.getValue() / slotData.CommonCardsPerLocation);
                                }
                            }
                            case "U" -> {
                                if (entry.getValue() > 0 && entry.getValue() % slotData.UncommonCardsPerLocation == 0 && entry.getValue() / slotData.UncommonCardsPerLocation <= slotData.UncommonCardLocations) {
                                    Archipelago.getInstance().checkLocation(ArchipelagoUtil.UNCOMMON_CARD_LOCATION_BASE + entry.getValue() / slotData.UncommonCardsPerLocation);
                                }
                            }
                            case "R" -> {
                                if (entry.getValue() > 0 && entry.getValue() % slotData.RareCardsPerLocation == 0 && entry.getValue() / slotData.RareCardsPerLocation <= slotData.RareCardLocations) {
                                    Archipelago.getInstance().checkLocation(ArchipelagoUtil.RARE_CARD_LOCATION_BASE + entry.getValue() / slotData.RareCardsPerLocation);
                                }
                            }
                            case "M" -> {
                                if (entry.getValue() > 0 && entry.getValue() % slotData.MythicRareCardsPerLocation == 0 && entry.getValue() / slotData.MythicRareCardsPerLocation <= slotData.MythicRareCardLocations) {
                                    Archipelago.getInstance().checkLocation(ArchipelagoUtil.MYTHIC_RARE_CARD_LOCATION_BASE + entry.getValue() / slotData.MythicRareCardsPerLocation);
                                }
                            }
                        }
                    }
                }
                case COLORLESS_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonColorless > 0 && archipelagoDataInstance.totalBattlesWonColorless % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonColorless / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.WASTE) + archipelagoDataInstance.totalBattlesWonColorless / slotData.FightsPerLocation);
                    }
                }
                case WHITE_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonWhite > 0 && archipelagoDataInstance.totalBattlesWonWhite % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonWhite / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.WHITE) + archipelagoDataInstance.totalBattlesWonWhite / slotData.FightsPerLocation);
                    }
                }
                case BLUE_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonBlue > 0 && archipelagoDataInstance.totalBattlesWonBlue % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonBlue / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.BLUE) + archipelagoDataInstance.totalBattlesWonBlue / slotData.FightsPerLocation);
                    }
                }
                case BLACK_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonBlack > 0 && archipelagoDataInstance.totalBattlesWonBlack % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonBlack / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.BLACK) + archipelagoDataInstance.totalBattlesWonBlack / slotData.FightsPerLocation);
                    }
                }
                case RED_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonRed > 0 && archipelagoDataInstance.totalBattlesWonRed % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonRed / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.RED) + archipelagoDataInstance.totalBattlesWonRed / slotData.FightsPerLocation);
                    }
                }
                case GREEN_BATTLE_WON -> {
                    if (archipelagoDataInstance.totalBattlesWonGreen > 0 && archipelagoDataInstance.totalBattlesWonGreen % slotData.FightsPerLocation == 0 && archipelagoDataInstance.totalBattlesWonGreen / slotData.FightsPerLocation <= slotData.FightLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getBattleLocationBase(ArchipelagoRegion.GREEN) + archipelagoDataInstance.totalBattlesWonGreen / slotData.FightsPerLocation);
                    }
                }
                case COLORLESS_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.colorlessCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.colorlessCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.WASTE) + archipelagoDataInstance.colorlessCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case WHITE_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.whiteCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.whiteCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.WHITE) + archipelagoDataInstance.whiteCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case BLUE_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.blueCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.blueCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.BLUE) + archipelagoDataInstance.blueCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case BLACK_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.blackCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.blackCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.BLACK) + archipelagoDataInstance.blackCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case RED_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.redCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.redCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.RED) + archipelagoDataInstance.redCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case GREEN_TOWN_EVENTS -> {
                    if (!archipelagoDataInstance.greenCompletedTownInnEvents.isEmpty() && archipelagoDataInstance.greenCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum() <= slotData.EventLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getInnEventLocationBase(ArchipelagoRegion.GREEN) + archipelagoDataInstance.greenCompletedTownInnEvents.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case COLORLESS_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.colorlessCompletedTownQuests.isEmpty() && archipelagoDataInstance.colorlessCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.WASTE) + archipelagoDataInstance.colorlessCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case WHITE_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.whiteCompletedTownQuests.isEmpty() && archipelagoDataInstance.whiteCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.WHITE) + archipelagoDataInstance.whiteCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case BLUE_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.blueCompletedTownQuests.isEmpty() && archipelagoDataInstance.blueCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.BLUE) + archipelagoDataInstance.blueCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case BLACK_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.blackCompletedTownQuests.isEmpty() && archipelagoDataInstance.blackCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.BLACK) + archipelagoDataInstance.blackCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case RED_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.redCompletedTownQuests.isEmpty() && archipelagoDataInstance.redCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.RED) + archipelagoDataInstance.redCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case GREEN_TOWN_QUESTS -> {
                    if (!archipelagoDataInstance.greenCompletedTownQuests.isEmpty() && archipelagoDataInstance.greenCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum() <= slotData.QuestLocations) {
                        Archipelago.getInstance().checkLocation(ArchipelagoUtil.getTownQuestLocationBase(ArchipelagoRegion.GREEN) + archipelagoDataInstance.greenCompletedTownQuests.values().stream().mapToLong(Long::longValue).sum());
                    }
                }
                case WIN_CONDITION_CLEARED -> Archipelago.getInstance().goal();
                // Boss checks: the location id comes from the shared boss table
                default -> {
                    ArchipelagoUtil.Boss boss = ArchipelagoUtil.findBossByCheck(type);
                    if (boss != null && boss.locationId != ArchipelagoUtil.NO_LOCATION) {
                        Archipelago.getInstance().checkLocation(boss.locationId);
                    }
                }
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
        Current.player().addMaxLife(amount);
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

    public void handleShopData(List<NetworkItem> shopLocationScouts, Set<Long> checkedLocations) {
        clearShopData();
        for (NetworkItem shopLocation : shopLocationScouts) {
            boolean alreadyChecked = checkedLocations.contains(shopLocation.locationID);

            if (shopLocation.locationID >= 1000 && shopLocation.locationID < 1100) {
                colorlessEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1100 && shopLocation.locationID < 1200) {
                whiteEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1200 && shopLocation.locationID < 1300) {
                whiteItemShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1300 && shopLocation.locationID < 1400) {
                blueEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1400 && shopLocation.locationID < 1500) {
                blueItemShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1500 && shopLocation.locationID < 1600) {
                blackEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1600 && shopLocation.locationID < 1700) {
                blackItemShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1700 && shopLocation.locationID < 1800) {
                redEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1800 && shopLocation.locationID < 1900) {
                redItemShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 1900 && shopLocation.locationID < 2000) {
                greenEquipmentShopList.add(new ItemData(shopLocation, alreadyChecked));
            } else if (shopLocation.locationID >= 2000 && shopLocation.locationID < 2100) {
                greenItemShopList.add(new ItemData(shopLocation, alreadyChecked));
            }
        }
        ArchipelagoData.getInstance().setTotalAmountOfSetUnlockChecks(slotData.SetUnlockCount);
    }

    /**
     * Applies the locations the server reports as checked for our slot (on connect and on room updates) to this save:
     * shop items are marked as bought and the other progress is raised, never lowered, so progress that is still queued
     * for sending survives. Called from the client thread, so the work is done on the game thread.
     */
    public void applyServerCheckedLocations(Collection<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return;
        }
        Set<Long> checked = new HashSet<>(locationIds);
        Gdx.app.postRunnable(() -> {
            if (slotData == null || archipelagoDataInstance.getArchipelagoMode() != ArchipelagoMode.networked_archipelago) {
                return;
            }
            for (Set<ItemData> shop : List.of(colorlessEquipmentShopList, whiteEquipmentShopList, blueEquipmentShopList,
                    blackEquipmentShopList, redEquipmentShopList, greenEquipmentShopList, whiteItemShopList, blueItemShopList,
                    blackItemShopList, redItemShopList, greenItemShopList)) {
                for (ItemData item : shop) {
                    if (checked.contains(item.archilepagoLocationId)) {
                        item.archipelagoAlreadyChecked = true;
                    }
                }
            }
            syncLocalProgress(checked);
        });
    }

    // Raises the save's bosses, battles, inn events, town quests and cards to what the server reports as checked.
    private void syncLocalProgress(Set<Long> checked) {
        for (ArchipelagoUtil.Boss boss : ArchipelagoUtil.BOSSES) {
            if (boss.locationId != ArchipelagoUtil.NO_LOCATION && checked.contains(boss.locationId) && !archipelagoDataInstance.isBossOrMinibossDefeated(boss)) {
                (boss.castleBoss ? archipelagoDataInstance.bossesDefeatedByName : archipelagoDataInstance.miniBossesDefeatedByName).add(boss.name.toLowerCase());
            }
        }
        for (ArchipelagoRegion region : ArchipelagoRegion.values()) {
            syncBattlesWon(region, highestChecked(checked, ArchipelagoUtil.getBattleLocationBase(region), slotData.FightLocations) * slotData.FightsPerLocation);
            syncTownQuestOrInnEventCount(archipelagoDataInstance.getCompletedInnEventsByRegion(region), highestChecked(checked, ArchipelagoUtil.getInnEventLocationBase(region), slotData.EventLocations));
            syncTownQuestOrInnEventCount(archipelagoDataInstance.getCompletedTownQuestsByRegion(region), highestChecked(checked, ArchipelagoUtil.getTownQuestLocationBase(region), slotData.QuestLocations));
        }
        syncCardsEarned("C", highestChecked(checked, ArchipelagoUtil.COMMON_CARD_LOCATION_BASE, slotData.CommonCardLocations) * (long) slotData.CommonCardsPerLocation);
        syncCardsEarned("U", highestChecked(checked, ArchipelagoUtil.UNCOMMON_CARD_LOCATION_BASE, slotData.UncommonCardLocations) * (long) slotData.UncommonCardsPerLocation);
        syncCardsEarned("R", highestChecked(checked, ArchipelagoUtil.RARE_CARD_LOCATION_BASE, slotData.RareCardLocations) * (long) slotData.RareCardsPerLocation);
        syncCardsEarned("M", highestChecked(checked, ArchipelagoUtil.MYTHIC_RARE_CARD_LOCATION_BASE, slotData.MythicRareCardLocations) * (long) slotData.MythicRareCardsPerLocation);
    }

    // Highest n in 1..count for which the repeatable check base + n is checked (0 if none).
    private static int highestChecked(Set<Long> checked, long base, int count) {
        for (int n = count; n > 0; n--) {
            if (checked.contains(base + n)) {
                return n;
            }
        }
        return 0;
    }

    private void syncBattlesWon(ArchipelagoRegion region, int serverBattlesWonCount) {
        ArchipelagoData data = archipelagoDataInstance;
        switch (region) {
            case WHITE -> data.totalBattlesWonWhite = Math.max(data.totalBattlesWonWhite, serverBattlesWonCount);
            case BLUE -> data.totalBattlesWonBlue = Math.max(data.totalBattlesWonBlue, serverBattlesWonCount);
            case BLACK -> data.totalBattlesWonBlack = Math.max(data.totalBattlesWonBlack, serverBattlesWonCount);
            case RED -> data.totalBattlesWonRed = Math.max(data.totalBattlesWonRed, serverBattlesWonCount);
            case GREEN -> data.totalBattlesWonGreen = Math.max(data.totalBattlesWonGreen, serverBattlesWonCount);
            case WASTE -> data.totalBattlesWonColorless = Math.max(data.totalBattlesWonColorless, serverBattlesWonCount);
        }
    }

    // Raises a region's town quests or inn events (counts per town) to the server's total. The server doesn't know
    // which towns those were in, so the missing amount is put under a placeholder town.
    private static void syncTownQuestOrInnEventCount(Map<String, Long> localTownQuestOrInnEventCount, long serverTotal) {
        long missing = serverTotal - localTownQuestOrInnEventCount.values().stream().mapToLong(Long::longValue).sum();
        if (missing > 0) {
            localTownQuestOrInnEventCount.merge("(Archipelago server)", missing, Long::sum);
        }
    }

    private void syncCardsEarned(String rarity, long serverCardsEarned) {
        Map<String, Long> cardsEarnedByRarity = archipelagoDataInstance.cardsEarnedByRarity;
        if (serverCardsEarned > cardsEarnedByRarity.getOrDefault(rarity, 0L)) {
            cardsEarnedByRarity.put(rarity, serverCardsEarned);
        }
    }

    // `shopName` is the shop's map name from the game, e.g. "white equipment" or "blue items".
    public Set<ItemData> getShopItems(String shopName) {
        return getShopItems(ArchipelagoRegion.fromShopName(shopName), shopName.toLowerCase().contains("items"));
    }

    // The Wastes have no item shop, so that one is null.
    public Set<ItemData> getShopItems(ArchipelagoRegion region, boolean itemShop) {
        return switch (region) {
            case WASTE -> itemShop ? null : colorlessEquipmentShopList;
            case WHITE -> itemShop ? whiteItemShopList : whiteEquipmentShopList;
            case BLUE -> itemShop ? blueItemShopList : blueEquipmentShopList;
            case BLACK -> itemShop ? blackItemShopList : blackEquipmentShopList;
            case RED -> itemShop ? redItemShopList : redEquipmentShopList;
            case GREEN -> itemShop ? greenItemShopList : greenEquipmentShopList;
        };
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
        if (!locationQueue.isEmpty() && AP.sendQueuedLocations(locationQueue)) locationQueue.clear();
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
