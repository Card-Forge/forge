package forge.gamemodes.rogue;

import forge.card.CardRarity;
import forge.gamemodes.rogue.effect.BazaarContext;
import forge.gamemodes.rogue.effect.BazaarItem;
import forge.item.PaperCard;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Pricing utility for Bazaar nodes.
 * Centralized card and Trait prices and discount calculations.
 */
public class BazaarPricing {
    private BazaarPricing() {
        /* This utility class should not be instantiated */
    }

    public static final int TRAIT_PRICE = 8;
    public static final int DISCOUNTED_TRAIT_PRICE = TRAIT_PRICE / 2;

    // Rarity-based gold pricing
    private static final Map<CardRarity, Integer> RARITY_PRICES = new HashMap<>();

    static {
        RARITY_PRICES.put(CardRarity.Common, 1);
        RARITY_PRICES.put(CardRarity.Uncommon, 2);
        RARITY_PRICES.put(CardRarity.Rare, 3);
        RARITY_PRICES.put(CardRarity.MythicRare, 4);
        RARITY_PRICES.put(CardRarity.Special, 4);
    }

    /**
     * Get the gold price for a card based on its rarity.
     */
    public static int getCardPrice(PaperCard card) {
        return RARITY_PRICES.getOrDefault(card.getRarity(), 1);
    }

    public static int calculateDiscountedPrice(int basePrice, int discountAmount) {
        return Math.max(0, basePrice - discountAmount);
    }

    public static int calculateSpecialDiscountedPrice(BazaarItem item, BazaarContext bazaarCtx, int basePrice) {
        if (item.type() == BazaarItem.Type.TRAIT) {
            return Math.max(0, DISCOUNTED_TRAIT_PRICE + bazaarCtx.priceAdjustment);
        }
        if (item.type() == BazaarItem.Type.CARRY_CARD) {
            return calculateDiscountedPrice(basePrice, bazaarCtx.discountAmount);
        }
        return basePrice;
    }

    public static int calculateItemsTotalCost(Set<BazaarItem> items) {
        int totalCost = 0;
        for (BazaarItem item : items) {
            totalCost += item.getPrice();
        }
        return totalCost;
    }
}
