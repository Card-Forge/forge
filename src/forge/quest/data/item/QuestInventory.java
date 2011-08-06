package forge.quest.data.item;

import java.util.*;

public class QuestInventory {
    Map<String, QuestItemAbstract> inventory = new HashMap<String, QuestItemAbstract>();

    public QuestInventory() {
        Set<QuestItemAbstract> allItems = getAllItems();
        for (QuestItemAbstract item : allItems) {
            inventory.put(item.getName(), item);
        }
    }

    public boolean hasItem(String itemName) {
        return inventory.containsKey(itemName) && inventory.get(itemName).getLevel() > 0;
    }

    public void addItem(QuestItemAbstract item) {
        inventory.put(item.getName(), item);
    }

    public int getItemLevel(String itemName) {
        QuestItemAbstract item = inventory.get(itemName);
        if (item == null) {
            return 0;
        }
        return item.getLevel();
    }

    public void setItemLevel(String itemName, int level) {
        inventory.get(itemName).setLevel(level);
    }


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
    private Object readResolve() {
        for (QuestItemAbstract item : getAllItems()) {
            if (!inventory.containsKey(item.getName())) {
                inventory.put(item.getName(), item);
            }
        }
        return this;
    }


    public Collection<QuestItemAbstract> getItems() {
        return inventory.values();
    }

    public QuestItemAbstract getItem(String itemName) {
        return inventory.get(itemName);
    }
}
