package forge.adventure.archipelago;

import com.google.gson.annotations.SerializedName;

public class SlotData {
    @SerializedName("color_sanity")
    public boolean ColorSanity;
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
    public boolean IncludeMinibossLocations;
    @SerializedName("include_power")
    public boolean IncludePower;
    @SerializedName("include_cheat")
    public boolean IncludeCheat;
    @SerializedName("set_unlocks_percentage")
    public int SetUnlocksPercentage;
    @SerializedName("gift_pack")
    public boolean GiftPack;
    @SerializedName("gold_percentage")
    public int GoldPercentage;
    @SerializedName("mana_shard_percentage")
    public int ManaShardPercentage;
    @SerializedName("life_upgrade_percentage")
    public int LifeUpgradePercentage;
    @SerializedName("equipment_percentage")
    public int EquipmentPercentage;
    @SerializedName("try_include_all_equipment")
    public boolean TryIncludeAllEquipment;
    @SerializedName("min_shop_price")
    public int MinShopPrice;
    @SerializedName("max_shop_price")
    public int MaxShopPrice;
    @SerializedName("gold_multiplier_percentage")
    public int GoldMultiplierPercentage;
}
