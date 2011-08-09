package forge.quest.data.item;

import java.util.*;

/**
 * <p>QuestInventory class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestInventory {
    Map<String, QuestItemAbstract> inventory = new HashMap<String, QuestItemAbstract>();

    /**
     * <p>Constructor for QuestInventory.</p>
     */
    public QuestInventory() {
        Set<QuestItemAbstract> allItems = getAllItems();
        for (QuestItemAbstract item : allItems) {
            inventory.put(item.getName(), item);
        }
    }

    /**
     * <p>hasItem.</p>
     *
     * @param itemName a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasItem(String itemName) {
        return inventory.containsKey(itemName) && inventory.get(itemName).getLevel() > 0;
    }

    /**
     * <p>addItem.</p>
     *
     * @param item a {@link forge.quest.data.item.QuestItemAbstract} object.
     */
    public void addItem(QuestItemAbstract item) {
        inventory.put(item.getName(), item);
    }

    /**
     * <p>getItemLevel.</p>
     *
     * @param itemName a {@link java.lang.String} object.
     * @return a int.
     */
    public int getItemLevel(String itemName) {
        QuestItemAbstract item = inventory.get(itemName);
        if (item == null) {
            return 0;
        }
        return item.getLevel();
    }

    /**
     * <p>setItemLevel.</p>
     *
     * @param itemName a {@link java.lang.String} object.
     * @param level a int.
     */
    public void setItemLevel(String itemName, int level) {
        inventory.get(itemName).setLevel(level);
    }


    /**
     * <p>getAllItems.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    private static Set<QuestItemAbstract> getAllItems() {
        SortedSet<QuestItemAbstract> set = new TreeSet<QuestItemAbstract>();

        set.add(new QuestItemElixir());
        set.add(new QuestItemEstates());
        set.add(new QuestItemLuckyCoin());
        set.add(new QuestItemMap());
        set.add(new QuestItemSleight());
        set.add(new QuestItemZeppelin());

        return set;
    }

    //Magic to support added pet types when reading saves.
    /**
     * <p>readResolve.</p>
     *
     * @return a {@link java.lang.Object} object.
     */
    private Object readResolve() {
        for (QuestItemAbstract item : getAllItems()) {
            if (!inventory.containsKey(item.getName())) {
                inventory.put(item.getName(), item);
            }
        }
        return this;
    }


    /**
     * <p>getItems.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<QuestItemAbstract> getItems() {
        return inventory.values();
    }

    /**
     * <p>getItem.</p>
     *
     * @param itemName a {@link java.lang.String} object.
     * @return a {@link forge.quest.data.item.QuestItemAbstract} object.
     */
    public QuestItemAbstract getItem(String itemName) {
        return inventory.get(itemName);
    }
}
