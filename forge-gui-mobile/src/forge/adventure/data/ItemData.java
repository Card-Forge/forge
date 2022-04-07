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
    public int lifeModifier=0;
    public int changeStartCards=0;
    public String[] startBattleWithCard;
    public String iconName;
    public float moveSpeed=1.0f;
    public boolean questItem=false;
    public int cost=1000;
    //not an item on it owns but effects will be applied to the opponent
    public ItemData opponent;


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

    public Array<IPaperCard> startBattleWithCards() {

        Array<IPaperCard> startCards=new Array<>();
        if(startBattleWithCard!=null)
        {
            for (String name:startBattleWithCard)
            {
                if(FModel.getMagicDb().getCommonCards().contains(name))
                    startCards.add(FModel.getMagicDb().getCommonCards().getCard(name));
                else if (FModel.getMagicDb().getAllTokens().containsRule(name))
                    startCards.add(FModel.getMagicDb().getAllTokens().getToken(name));
                else
                {
                    System.err.print("Can not find card "+name+"\n");
                }
            }
        }
        return startCards;
    }
    public String cardNames() {
        String ret="";
        Array<IPaperCard> array=startBattleWithCards();
        for(int i =0;i<array.size;i++)
        {
            ret+=array.get(i).toString();
            if(i!=array.size-1)
                ret+=" , ";
        }
        return ret;
    }

    public String getDescription() {
        String description = "";
        if(this.equipmentSlot != null && !this.equipmentSlot.equals(""))
            description += "Slot: " + this.equipmentSlot + "\n";
        if(this.lifeModifier != 0)
            description += "Life: " + ((this.lifeModifier > 0) ? "+" : "") + this.lifeModifier + "\n";
        if(this.startBattleWithCard != null && this.startBattleWithCard.length != 0)
            description+="Cards on battlefield: \n" + this.cardNames() + "\n";
        if(this.moveSpeed!=0 && this.moveSpeed != 1)
            description+="Movement speed: " + ((this.lifeModifier > 0) ? "+" : "") + Math.round((this.moveSpeed-1.f)*100) + "%\n";
        if(this.changeStartCards != 0)
            description+="Starting hand: " + this.changeStartCards + "\n";
        if(this.opponent != null) {
            String oppEffect=this.opponent.getDescription();
            if(oppEffect != "") {
                description += "Gives Opponent:\n";
                description += oppEffect;
            }
        }
        return description;
    }
}
