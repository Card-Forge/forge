package forge.quest.data.bazaar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import forge.AllZone;

/**
 * <p>
 * QuestStallManager class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestStallManager {

    /** Constant <code>stalls</code>. */
    private static Map<String, QuestStallDefinition> stalls;
    /** Constant <code>items</code>. */
    private static Map<String, SortedSet<QuestStallPurchasable>> items;

    /**
     * <p>
     * buildStalls.
     * </p>
     */
    public static void buildStalls() {
        QuestStallManager.stalls = new HashMap<String, QuestStallDefinition>();
        QuestStallManager.stalls.put(QuestStallManager.ALCHEMIST, new QuestStallDefinition(QuestStallManager.ALCHEMIST,
                "Alchemist", "The walls of this alchemist's stall are covered with shelves with potions, oils, "
                        + "powders, poultices and elixirs, each meticulously labeled.", "BottlesIconSmall.png"));
        QuestStallManager.stalls.put(QuestStallManager.BANKER, new QuestStallDefinition(QuestStallManager.BANKER,
                "Banker", "A large book large enough to be seen from the outside rests on the Banker's desk.",
                "CoinIconSmall.png"));
        QuestStallManager.stalls.put(QuestStallManager.BOOKSTORE, new QuestStallDefinition(QuestStallManager.BOOKSTORE,
                "Bookstore", "Tomes of different sizes are stacked in man-high towers.", "BookIconSmall.png"));
        QuestStallManager.stalls.put(QuestStallManager.GEAR, new QuestStallDefinition(QuestStallManager.GEAR,
                "Adventuring Gear",
                "This adventurer's market has a tool for every need ... or so the plaque on the wall claims.",
                "GearIconSmall.png"));
        QuestStallManager.stalls.put(QuestStallManager.NURSERY, new QuestStallDefinition(QuestStallManager.NURSERY,
                "Nursery", "The smells of the one hundred and one different plants forms a unique fragrance.",
                "LeafIconSmall.png"));
        QuestStallManager.stalls.put(QuestStallManager.PET_SHOP, new QuestStallDefinition(QuestStallManager.PET_SHOP,
                "Pet Shop", "This large stall echoes with a multitude of animal noises.", "FoxIconSmall.png"));
    }

    /**
     * <p>
     * getStallNames.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public static List<String> getStallNames() {
        final List<String> ret = new ArrayList<String>();
        ret.add(QuestStallManager.ALCHEMIST);
        ret.add(QuestStallManager.BANKER);
        ret.add(QuestStallManager.BOOKSTORE);
        ret.add(QuestStallManager.GEAR);
        ret.add(QuestStallManager.NURSERY);
        ret.add(QuestStallManager.PET_SHOP);
        return ret;
    }

    /**
     * <p>
     * getStall.
     * </p>
     * 
     * @param stallName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.quest.data.bazaar.QuestStallDefinition} object.
     */
    public static QuestStallDefinition getStall(final String stallName) {
        if (QuestStallManager.stalls == null) {
            QuestStallManager.buildStalls();
        }

        return QuestStallManager.stalls.get(stallName);
    }

    /**
     * <p>
     * buildItems.
     * </p>
     */
    public static void buildItems() {
        final SortedSet<QuestStallPurchasable> itemSet = new TreeSet<QuestStallPurchasable>();

        itemSet.addAll(AllZone.getQuestData().getInventory().getItems());
        itemSet.addAll(AllZone.getQuestData().getPetManager().getPetsAndPlants());

        QuestStallManager.items = new HashMap<String, SortedSet<QuestStallPurchasable>>();

        for (final String stallName : QuestStallManager.getStallNames()) {
            QuestStallManager.items.put(stallName, new TreeSet<QuestStallPurchasable>());
        }

        for (final QuestStallPurchasable purchasable : itemSet) {
            QuestStallManager.items.get(purchasable.getStallName()).add(purchasable);
        }

    }

    /**
     * <p>
     * Getter for the field <code>items</code>.
     * </p>
     * 
     * @param stallName
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public static List<QuestStallPurchasable> getItems(final String stallName) {
        if (QuestStallManager.items == null) {
            QuestStallManager.buildItems();
        }

        final List<QuestStallPurchasable> ret = new ArrayList<QuestStallPurchasable>();

        for (final QuestStallPurchasable purchasable : QuestStallManager.items.get(stallName)) {
            if (purchasable.isAvailableForPurchase()) {
                ret.add(purchasable);
            }
        }
        return ret;
    }

    /** Constant <code>ALCHEMIST="Alchemist"</code>. */
    public static final String ALCHEMIST = "Alchemist";

    /** Constant <code>BANKER="Banker"</code>. */
    public static final String BANKER = "Banker";

    /** Constant <code>BOOKSTORE="Bookstore"</code>. */
    public static final String BOOKSTORE = "Bookstore";

    /** Constant <code>GEAR="Gear"</code>. */
    public static final String GEAR = "Gear";

    /** Constant <code>NURSERY="Nursery"</code>. */
    public static final String NURSERY = "Nursery";

    /** Constant <code>PET_SHOP="Pet Shop"</code>. */
    public static final String PET_SHOP = "Pet Shop";

}
