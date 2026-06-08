package forge.adventure.archipelago;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SlotData implements Serializable {
    @SerializedName("color_sanity")
    public int ColorSanity;
    @SerializedName("starting_color")
    public int StartColor;
    @SerializedName("fight_locations")
    public int FightLocations;
    @SerializedName("fight_amount_per_location")
    public int FightsPerLocation;
    @SerializedName("quest_locations")
    public int QuestLocations;
    @SerializedName("event_locations")
    public int EventLocations;
    @SerializedName("include_miniboss_locations")
    public int IncludeMinibossLocations;
    @SerializedName("include_power")
    public int IncludePower;
    @SerializedName("include_cheat")
    public int IncludeCheat;
    @SerializedName("set_unlocks_percentage")
    public int SetUnlocksPercentage;
    @SerializedName("gift_pack")
    public int GiftPack;
    @SerializedName("gold_percentage")
    public int GoldPercentage;
    @SerializedName("mana_shard_percentage")
    public int ManaShardPercentage;
    @SerializedName("life_upgrade_percentage")
    public int LifeUpgradePercentage;
    @SerializedName("equipment_percentage")
    public int EquipmentPercentage;
    @SerializedName("try_include_all_equipment")
    public int TryIncludeAllEquipment;
    @SerializedName("min_shop_price")
    public int MinShopPrice;
    @SerializedName("max_shop_price")
    public int MaxShopPrice;
    @SerializedName("gold_multiplier_percentage")
    public int GoldMultiplierPercentage;
    @SerializedName("seed")
    public String Seed;
}
