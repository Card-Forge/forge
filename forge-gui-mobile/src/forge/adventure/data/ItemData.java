package forge.adventure.data;

import com.badlogic.gdx.graphics.g2d.Sprite;
import forge.adventure.util.Config;

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
        dialogOnUse       = cpy.dialogOnUse;
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
