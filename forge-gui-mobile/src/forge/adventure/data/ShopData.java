package forge.adventure.data;

import com.badlogic.gdx.utils.Array;

/**
 * Data class that will be used to read Json configuration files
 * SettingData
 * contains data for a Shop on the map
 */
public class ShopData {

    public String name;
    public String description;
    public int restockPrice;
    public String spriteAtlas;
    public String sprite;
    public boolean unlimited;
    public Array<RewardData> rewards;
    public String overlaySprite = "";





}
