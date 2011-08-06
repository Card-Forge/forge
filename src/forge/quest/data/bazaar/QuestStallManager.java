package forge.quest.data.bazaar;

import forge.AllZone;

import java.util.*;

public class QuestStallManager {

    static Map<String, QuestStallDefinition> stalls ;
    static Map<String, SortedSet<QuestStallPurchasable>> items;

    public static void buildStalls(){
        stalls = new HashMap<String, QuestStallDefinition>();
        stalls.put(ALCHEMIST,
                new QuestStallDefinition(ALCHEMIST,
                "Alchemist",
                "The walls of this alchemist's stall are covered with shelves with potions, oils, " +
                        "powders, poultices and elixirs, each meticulously labeled.",
                "BottlesIconSmall.png"));
        stalls.put(BANKER,
                new QuestStallDefinition(BANKER,
                "Banker",
                "A large book large enough to be seen from the outside rests on the Banker's desk.",
                "CoinIconSmall.png"));
        stalls.put(BOOKSTORE,
                new QuestStallDefinition(BOOKSTORE,
                "Bookstore",
                "Tomes of different sizes are stacked in man-high towers.",
                "BookIconSmall.png"));
        stalls.put(GEAR,
                new QuestStallDefinition(GEAR,
                "Adventuring Gear",
                "This adventurer's market has a tool for every need ... or so the plaque on the wall claims.",
                "GearIconSmall.png"));
        stalls.put(NURSERY,
                new QuestStallDefinition(NURSERY,
                "Nursery",
                "The smells of the one hundred and one different plants forms a unique fragrance.",
                "LeafIconSmall.png"));
        stalls.put(PET_SHOP,
            new QuestStallDefinition(PET_SHOP,
                "Pet Shop",
                "This large stall echoes with a multitude of animal noises.",
                "FoxIconSmall.png"));
    }

    public static List<String> getStallNames(){
        List<String> ret = new ArrayList<String>();
        ret.add(ALCHEMIST);
        ret.add(BANKER);
        ret.add(BOOKSTORE);
        ret.add(GEAR);
        ret.add(NURSERY);
        ret.add(PET_SHOP);
        return ret;
    }

    public static QuestStallDefinition getStall(String stallName){
        if (stalls == null){
            buildStalls();
        }

        return stalls.get(stallName);
    }

    public static void buildItems(){
        SortedSet<QuestStallPurchasable> itemSet = new TreeSet<QuestStallPurchasable>();

        itemSet.addAll(AllZone.QuestData.getInventory().getItems());
        itemSet.addAll(AllZone.QuestData.getPetManager().getPetsAndPlants());

        items = new HashMap<String, SortedSet<QuestStallPurchasable>>();

        for (String stallName : getStallNames()) {
            items.put(stallName, new TreeSet<QuestStallPurchasable>());
        }

        for (QuestStallPurchasable purchasable : itemSet) {
            items.get(purchasable.getStallName()).add(purchasable);
        }

    }

    public static List<QuestStallPurchasable> getItems(String stallName) {
        if (items == null){
            buildItems();
        }

        List<QuestStallPurchasable> ret = new ArrayList<QuestStallPurchasable>();

        for (QuestStallPurchasable purchasable : items.get(stallName)) {
            if (purchasable.isAvailableForPurchase()){
                ret.add(purchasable);
            }
        }
        return ret;
    }

    public static final String ALCHEMIST = "Alchemist";
    public static final String BANKER = "Banker";
    public static final String BOOKSTORE = "Bookstore";
    public static final String GEAR = "Gear";
    public static final String NURSERY = "Nursery";
    public static final String PET_SHOP = "Pet Shop";

}
