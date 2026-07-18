package forge.adventure.archipelago;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class SlotData implements Serializable {
    @SerializedName("castles_required")
    public int CastlesRequired;
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
    @SerializedName("common_card_locations")
    public int CommonCardLocations;
    @SerializedName("common_cards_per_location")
    public int CommonCardsPerLocation;
    @SerializedName("uncommon_card_locations")
    public int UncommonCardLocations;
    @SerializedName("uncommon_cards_per_location")
    public int UncommonCardsPerLocation;
    @SerializedName("rare_card_locations")
    public int RareCardLocations;
    @SerializedName("rare_cards_per_location")
    public int RareCardsPerLocation;
    @SerializedName("mythic_rare_card_locations")
    public int MythicRareCardLocations;
    @SerializedName("mythic_rare_cards_per_location")
    public int MythicRareCardsPerLocation;
    @SerializedName("gift_pack")
    public int GiftPack;
    @SerializedName("min_shop_price")
    public int MinShopPrice;
    @SerializedName("max_shop_price")
    public int MaxShopPrice;
    @SerializedName("gold_multiplier_percentage")
    public int GoldMultiplierPercentage;
    @SerializedName("death_link")
    public int DeathLink;
    @SerializedName("set_unlock_count")
    public int SetUnlockCount;
    @SerializedName("seed")
    public String Seed;
}
