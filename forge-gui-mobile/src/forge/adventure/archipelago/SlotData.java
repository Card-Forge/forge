package forge.adventure.archipelago;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class SlotData implements Serializable {
    // Saves store this class serialized. This is the id it had before fields were added in 0.2.0, keep it so older saves
    // still load (fields missing from an older save are left at null/0).
    private static final long serialVersionUID = -8304315340413927436L;

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
    @SerializedName("no_starting_deck")
    public int NoStartingDeck;
    @SerializedName("auto_sell_deck")
    public int AutoSellDeck;
    @SerializedName("event_miniboss_checks")
    public int EventMinibossChecks;
    // Starting inventory (null for slots generated before APWorld 0.2.0)
    @SerializedName("starting_cards")
    public HashMap<String, Integer> StartingCards; // card name -> amount
    @SerializedName("starting_sets")
    public ArrayList<String> StartingSets; // set codes
    @SerializedName("starting_boosters")
    public HashMap<String, Integer> StartingBoosters; // set code -> amount
    @SerializedName("starting_equipment")
    public ArrayList<Long> StartingEquipment; // item ids, see ItemRegistry
    @SerializedName("seed")
    public String Seed;
}
