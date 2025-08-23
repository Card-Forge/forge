package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

public class ItemListData
{
    private static Array<ItemData> itemList;
    private static Array<ItemData> getAllItems() {
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
        for (ItemData orig : new Array.ArrayIterator<>(getAllItems())) {
            if (orig.name.equalsIgnoreCase(name))
                return orig.clone();
        }
        return null;
    }
    public static Array<ItemData> getSketchBooks() {
        Array<ItemData> sketchbooks = new Array<>();
        for (ItemData orig : getAllItems()) {
            if (orig.questItem || !orig.getName().contains("Landscape Sketchbook"))
                continue;
            sketchbooks.add(orig.clone());
        }
        return  sketchbooks;
    }
}
