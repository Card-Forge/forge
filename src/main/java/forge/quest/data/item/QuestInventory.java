package forge.quest.data.item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * QuestInventory class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestInventory {

    /** The inventory. */
    private Map<String, QuestItemAbstract> inventory = new HashMap<String, QuestItemAbstract>();

    /**
     * <p>
     * Constructor for QuestInventory.
     * </p>
     */
    public QuestInventory() {
        final Set<QuestItemAbstract> allItems = QuestInventory.getAllItems();
        for (final QuestItemAbstract item : allItems) {
            this.inventory.put(item.getName(), item);
        }
    }

    /**
     * <p>
     * hasItem.
     * </p>
     * 
     * @param itemName
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean hasItem(final String itemName) {
        return this.inventory.containsKey(itemName) && (this.inventory.get(itemName).getLevel() > 0);
    }

    /**
     * <p>
     * addItem.
     * </p>
     * 
     * @param item
     *            a {@link forge.quest.data.item.QuestItemAbstract} object.
     */
    public final void addItem(final QuestItemAbstract item) {
        this.inventory.put(item.getName(), item);
    }

    /**
     * <p>
     * getItemLevel.
     * </p>
     * 
     * @param itemName
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public final int getItemLevel(final String itemName) {
        final QuestItemAbstract item = this.inventory.get(itemName);
        if (item == null) {
            return 0;
        }
        return item.getLevel();
    }

    /**
     * <p>
     * setItemLevel.
     * </p>
     * 
     * @param itemName
     *            a {@link java.lang.String} object.
     * @param level
     *            a int.
     */
    public final void setItemLevel(final String itemName, final int level) {
        this.inventory.get(itemName).setLevel(level);
    }

    /**
     * <p>
     * getAllItems.
     * </p>
     * 
     * @return a {@link java.util.Set} object.
     */
    private static Set<QuestItemAbstract> getAllItems() {
        final SortedSet<QuestItemAbstract> set = new TreeSet<QuestItemAbstract>();

        set.add(new QuestItemElixir());
        set.add(new QuestItemEstates());
        set.add(new QuestItemLuckyCoin());
        set.add(new QuestItemMap());
        set.add(new QuestItemSleight());
        set.add(new QuestItemZeppelin());

        return set;
    }

    // Magic to support added pet types when reading saves.
    /**
     * <p>
     * readResolve.
     * </p>
     * 
     * @return a {@link java.lang.Object} object.
     */
    private Object readResolve() {
        for (final QuestItemAbstract item : QuestInventory.getAllItems()) {
            if (!this.inventory.containsKey(item.getName())) {
                this.inventory.put(item.getName(), item);
            }
        }
        return this;
    }

    /**
     * <p>
     * getItems.
     * </p>
     * 
     * @return a {@link java.util.Collection} object.
     */
    public Collection<QuestItemAbstract> getItems() {
        return this.inventory.values();
    }

    /**
     * <p>
     * getItem.
     * </p>
     * 
     * @param itemName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.quest.data.item.QuestItemAbstract} object.
     */
    public QuestItemAbstract getItem(final String itemName) {
        return this.inventory.get(itemName);
    }
}
