package forge.gamemodes.rogue.effect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Context for Bazaar-style shopping interactions.
 * Used by both ordinary Bazaar nodes and custom event inventories.
 */
public class BazaarContext {
    /** Optional custom dialog title. Null = default Bazaar title. */
    public String title;

    /** Custom or NPC-provided inventory. Ordinary Bazaar card generation adds its own card items. */
    public final List<BazaarItem> inventory = new ArrayList<>();

    /** True when this Bazaar should generate trait offers in addition to ordinary cards. */
    public boolean offersTraits;

    /** True when this Bazaar should generate carry-card offers in addition to ordinary cards. */
    public boolean offersCarryCards;

    /** Number of random trait/carry-card offers to discount. */
    public int specialDiscountCount;

    /** Bazaar entries the player purchased (populated after shopping, before onAfterBazaarPurchase). */
    public final Set<BazaarItem> purchasedItems = new HashSet<>();

    /** Number of random cards to discount (set by encounters, applied by bazaar code). */
    public int discountCount;

    /** Discount amount in gold per discounted card. */
    public int discountAmount;

    /** Flat adjustment applied to every final Bazaar offer price. */
    public int priceAdjustment;
}
