package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;
import forge.item.IPaperCard;
import forge.model.FModel;

import static forge.adventure.util.Paths.ITEMS_ATLAS;

/**
 * Data class that will be used to read Json configuration files
 * ItemData
 * contains the information possible hero sprite
 */
public class ItemData {
    public String name;
    public String equipmentSlot;
    public EffectData effect;
    public String iconName;
    public boolean questItem=false;
    public int cost=1000;

    public Sprite sprite()
    {
        if(itemAtlas==null)
        {
            itemAtlas=Config.instance().getAtlas(ITEMS_ATLAS);
        }
        return itemAtlas.createSprite(iconName);
    }

    private static TextureAtlas itemAtlas;
    private static Array<ItemData> itemList;
    public static Array<ItemData> getAllItems() {
        if (itemList == null) {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(Paths.ITEMS);
            if (handle.exists()) {
                Array readJson = json.fromJson(Array.class, ItemData.class, handle);
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
        String description = "";
        if(this.equipmentSlot != null && !this.equipmentSlot.isEmpty())
            description += "Slot: " + this.equipmentSlot + "\n";
        if(effect != null)
            description += effect.getDescription();
        return description;
    }
}
