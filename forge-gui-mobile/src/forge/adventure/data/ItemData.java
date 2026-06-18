package forge.adventure.data;

import com.badlogic.gdx.graphics.g2d.Sprite;
import forge.adventure.archipelago.Archipelago;
import forge.adventure.util.Config;
import io.github.archipelagomw.parts.NetworkItem;

import java.io.Serializable;
import java.util.UUID;

/**
 * Data class that will be used to read Json configuration files
 * ItemData
 * contains the information for equipment and items.
 */
public class ItemData implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    public String name;
    public String equipmentSlot;
    public EffectData effect;
    public String description; //Manual description of the item.
    public String iconName;
    public boolean questItem=false;
    public int cost=1000;

    public boolean usableOnWorldMap;
    public boolean usableInPoi;
    public boolean isCracked;
    public boolean isEquipped;
    public Long longID;
    public String commandOnUse;
    public int shardsNeeded;
    public DialogData dialogOnUse;
    public long archipelagoItemId = -1;
    public long archilepagoLocationId = -1;
    public int archipelagoPlayerId = -1;
    public int archipelagoFlags = 0;
    public String archipelagoItemName = "";
    public String archipelagoLocationName = "";
    public String archipelagoPlayerName = "";

    public ItemData() {

    }

    public ItemData(ItemData cpy) {
        name                    = cpy.name;
        equipmentSlot           = cpy.equipmentSlot;
        effect                  = new EffectData(cpy.effect);
        description             = cpy.description;
        iconName                = cpy.iconName;
        questItem               = cpy.questItem;
        cost                    = cpy.cost;
        usableInPoi             = cpy.usableInPoi;
        usableOnWorldMap        = cpy.usableOnWorldMap;
        commandOnUse            = cpy.commandOnUse;
        shardsNeeded            = cpy.shardsNeeded;
        dialogOnUse             = cpy.dialogOnUse;
        archipelagoItemId       = cpy.archipelagoItemId;
        archilepagoLocationId   = cpy.archilepagoLocationId;
        archipelagoPlayerId     = cpy.archipelagoPlayerId;
        archipelagoFlags        = cpy.archipelagoFlags;
        archipelagoItemName     = cpy.archipelagoItemName;
        archipelagoLocationName = cpy.archipelagoLocationName;
        archipelagoPlayerName   = cpy.archipelagoPlayerName;
    }

    public ItemData(NetworkItem networkItem) {
        String itemType = "Filler";
        switch (networkItem.flags){
            case 0b001:
                itemType = "Progression";
                break;
            case 0b010:
                itemType = "Useful";
                break;
            case 0b100:
                itemType = "Trap";
                break;
        }

        name = networkItem.itemName;
        if (!itemType.isEmpty()) name += "\n" + itemType + " item for " + networkItem.playerName;
        iconName = "APIconSmall";
        // Item name is recognised as a known item, show its information:
        if (ItemListData.getItem(networkItem.itemName) != null) {
            ItemData data = ItemListData.getItem(networkItem.itemName);
            iconName = data.iconName;
            equipmentSlot = data.equipmentSlot;
            effect = data.effect;
            description = data.description;
        }
        cost = Archipelago.getInstance().getRandomItemPrice();

        archipelagoItemId       = networkItem.itemID;
        archilepagoLocationId   = networkItem.locationID;
        archipelagoPlayerId     = networkItem.playerID;
        archipelagoFlags        = networkItem.flags;
        archipelagoItemName     = networkItem.itemName;
        archipelagoLocationName = networkItem.locationName;
        archipelagoPlayerName   = networkItem.playerName;
    }

    public Sprite sprite() {
        return Config.instance().getItemSprite(iconName);
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

    public String getName() {
        return name;
    }

    @Override
    public ItemData clone() {
        try {
            ItemData clone = (ItemData) super.clone();
            clone.longID = UUID.randomUUID().getMostSignificantBits();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
