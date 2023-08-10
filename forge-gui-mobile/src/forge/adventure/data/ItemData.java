package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

/**
 * Data class that will be used to read Json configuration files
 * ItemData
 * contains the information for equipment and items.
 */
public class ItemData {
    public String name;
    public String equipmentSlot;
    public EffectData effect;
    public String description; //Manual description of the item.
    public String iconName;
    public boolean questItem=false;
    public int cost=1000;

    public boolean usableOnWorldMap;
    public boolean usableInPoi;
    public String commandOnUse;
    public int shardsNeeded;


    public ItemData()
    {

    }
    public ItemData(ItemData cpy)
    {
        name              = cpy.name;
        equipmentSlot     = cpy.equipmentSlot;
        effect            = new EffectData(cpy.effect);
        description       = cpy.description;
        iconName          = cpy.iconName;
        questItem         = cpy.questItem;
        cost              = cpy.cost;
        usableInPoi       = cpy.usableInPoi;
        usableOnWorldMap  = cpy.usableOnWorldMap;
        commandOnUse      = cpy.commandOnUse;
        shardsNeeded      = cpy.shardsNeeded;
    }

    public Sprite sprite() {
        return Config.instance().getItemSprite(iconName);
    }
    private static Array<ItemData> itemList;
    public static Array<ItemData> getAllItems() {
        if (itemList == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.ITEMS);
            if (handle.exists()) {
                Array<ItemData> readJson = json.fromJson(Array.class, ItemData.class, handle);
                itemList = readJson;

            }

        }
        return itemList;
    }
    public static ItemData getItem(String name) {
        for(ItemData data: new Array.ArrayIterator<>(getAllItems()))
        {
            if(data.name.equals(name))
                return data;
        }
        return null;
    }

    public String getDescription() {
        String result = "";
        if(this.description != null && !this.description.isEmpty())
            result += description + "\n";
        if(this.equipmentSlot != null && !this.equipmentSlot.isEmpty())
            result += "Slot: " + this.equipmentSlot + "\n";
        if(effect != null)
            result += effect.getDescription();
        if(shardsNeeded != 0)
            result +=  shardsNeeded+" [+Shards]";
        return result;
    }

}
