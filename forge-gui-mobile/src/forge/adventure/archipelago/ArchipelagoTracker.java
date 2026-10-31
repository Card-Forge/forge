package forge.adventure.archipelago;

import forge.StaticData;
import forge.adventure.data.ItemData;
import forge.card.CardEdition;

import java.util.*;

// Read-only summary of the networked Archipelago progress, used by the tracker screen.
public final class ArchipelagoTracker {

    public enum Icon { BOSS, EQUIPMENT_SHOP, ITEM_SHOP, TOWN_QUESTS, INN_EVENTS, BATTLES }

    // One tile in a "region" row.
    public static final class Entry {
        public final Icon icon;
        public final String enemy; // enemy data name used for the portrait (bosses only)
        public final String description;
        public final boolean done;

        Entry(Icon icon, String enemy, String description, boolean done) {
            this.icon = icon;
            this.enemy = enemy;
            this.description = description;
            this.done = done;
        }
    }

    public static final class Region {
        public final String name;
        public final String color; // TextraLabel color token
        public final List<Entry> entries = new ArrayList<>();

        Region(String name, String color) {
            this.name = name;
            this.color = color;
        }
    }

    public static final class Progress {
        public final String label;
        public final long done;
        public final long target;

        Progress(String label, long done, long target) {
            this.label = label;
            this.done = done;
            this.target = target;
        }
    }

    private ArchipelagoTracker() {
    }

    // Returns true when the tracker has data to show from a networked save with slot data.
    public static boolean isAvailable() {
        ArchipelagoData data = ArchipelagoData.getInstance();
        return data.archipelagoMode == ArchipelagoMode.networked_archipelago && ArchipelagoRandomizer.getInstance().getSlotData() != null;
    }

    public static List<Region> getRegions() {
        List<Region> regions = new ArrayList<>();
        if (!isAvailable()) return regions;
        ArchipelagoData data = ArchipelagoData.getInstance();
        ArchipelagoRandomizer randomizer = ArchipelagoRandomizer.getInstance();
        SlotData slotData = randomizer.getSlotData();

        for (ArchipelagoRegion apRegion : ArchipelagoRegion.values()) {
            Region region = new Region(apRegion.label, apRegion.textColor);
            for (ArchipelagoUtil.Boss boss : ArchipelagoUtil.getBossesInRegion(apRegion)) {
                if (boss.castleBoss) {
                    region.entries.add(new Entry(Icon.BOSS, boss.enemyName, boss.name + " - Castle Boss", data.isBossOrMinibossDefeated(boss)));
                } else if (slotData.IncludeMinibossLocations == 1) {
                    region.entries.add(new Entry(Icon.BOSS, boss.enemyName, boss.name + " - Miniboss", data.isBossOrMinibossDefeated(boss)));
                }
            }

            addShop(region, Icon.EQUIPMENT_SHOP, "Equipment shop items left: ", randomizer.getShopItems(apRegion, false));
            if (apRegion != ArchipelagoRegion.WASTE) { // the Wastes have no item shop
                addShop(region, Icon.ITEM_SHOP, "Item shop items left: ", randomizer.getShopItems(apRegion, true));
            }
            addRemaining(region, Icon.TOWN_QUESTS, "Town quests left: ", data.getCompletedTownQuests(apRegion), slotData.QuestLocations);
            addRemaining(region, Icon.INN_EVENTS, "Inn events left: ", data.getCompletedTownInnEvents(apRegion), slotData.EventLocations);

            long battleTarget = (long) slotData.FightLocations * slotData.FightsPerLocation;
            if (battleTarget > 0) {
                long won = data.getBattlesWon(apRegion);
                region.entries.add(new Entry(Icon.BATTLES, null, "Battles won: " + Math.min(won, battleTarget) + "/" + battleTarget, won >= battleTarget));
            }
            regions.add(region);
        }
        return regions;
    }

    public static Progress getGoalProgress() {
        if (!isAvailable()) return new Progress("Castle bosses defeated", 0, 0);
        return new Progress("Castle bosses defeated", ArchipelagoData.getInstance().getCastleBossesDefeatedCount(),
                ArchipelagoRandomizer.getInstance().getSlotData().CastlesRequired);
    }

    public static List<Progress> getCardProgress() {
        List<Progress> progress = new ArrayList<>();
        if (!isAvailable()) return progress;
        SlotData s = ArchipelagoRandomizer.getInstance().getSlotData();
        Map<String, Long> earned = ArchipelagoData.getInstance().cardsEarnedByRarity;
        addCards(progress, "Common", earned.getOrDefault("C", 0L), (long) s.CommonCardLocations * s.CommonCardsPerLocation);
        addCards(progress, "Uncommon", earned.getOrDefault("U", 0L), (long) s.UncommonCardLocations * s.UncommonCardsPerLocation);
        addCards(progress, "Rare", earned.getOrDefault("R", 0L), (long) s.RareCardLocations * s.RareCardsPerLocation);
        addCards(progress, "Mythic Rare", earned.getOrDefault("M", 0L), (long) s.MythicRareCardLocations * s.MythicRareCardsPerLocation);
        return progress;
    }

    // Number of sets that can be unlocked (all card sets known to the randomizer).
    public static int getTotalSetCount() {
        return ArchipelagoData.getInstance().allCardSets.size();
    }

    // Unlocked sets as {code, name}, sorted by name.
    public static List<String[]> getUnlockedSets() {
        List<String[]> sets = new ArrayList<>();
        CardEdition.Collection editions = StaticData.instance().getEditions();
        for (String code : ArchipelagoData.getInstance().setsUnlockedByCode) {
            CardEdition edition = editions.get(code);
            sets.add(new String[]{code, edition != null ? edition.getName() : code});
        }
        sets.sort(Comparator.comparing((String[] s) -> s[1], String.CASE_INSENSITIVE_ORDER));
        return sets;
    }

    private static void addShop(Region region, Icon icon, String label, Set<ItemData> items) {
        if (items == null || items.isEmpty()) return; // not scouted yet
        long left = items.stream().filter(item -> !item.archipelagoAlreadyChecked).count();
        region.entries.add(new Entry(icon, null, label + left, left == 0));
    }

    private static void addRemaining(Region region, Icon icon, String label, long done, int target) {
        if (target <= 0) return;
        long left = Math.max(0, target - done);
        region.entries.add(new Entry(icon, null, label + left, left == 0));
    }

    private static void addCards(List<Progress> progress, String label, long earned, long target) {
        if (target > 0) progress.add(new Progress(label, Math.min(earned, target), target));
    }
}
