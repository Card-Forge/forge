package forge.quest.data.item;

import java.util.HashMap;
import java.util.Map;

public class QuestInventory {
    Map<String, QuestItemAbstract> inventory = new HashMap<String, QuestItemAbstract>();

    public boolean hasItem(String itemName){
        return inventory.containsKey(itemName);
    }

    public void addItem(QuestItemAbstract item){
        inventory.put(item.getName(),item);
    }

    public int getItemLevel(String itemName){
        QuestItemAbstract item = inventory.get(itemName);
        if (item == null){
            return 0;
        }
        return item.getLevel();
    }

    public static final String ALCHEMIST = "Alchemist";
    public static final String BANKER = "Banker";
    public static final String BOOKSTORE = "Bookstore";
    public static final String GEAR = "Gear";

    public void setItemLevel(String itemName, int level) {
        inventory.get(itemName).setLevel(level);
    }
}
