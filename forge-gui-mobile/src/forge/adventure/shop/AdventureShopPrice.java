package forge.adventure.shop;

import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Reward;

/**
 * Shared purchase-price calculation for physical Adventure shops and the Shop Catalog.
 */
public final class AdventureShopPrice {
    private AdventureShopPrice() {
    }

    public static int calculate(Reward reward, PointOfInterestChanges changes, int shopObjectId) {
        int price = CardUtil.getRewardPrice(reward);
        price *= AdventurePlayer.current().goldModifier();
        if (changes != null) {
            price *= changes.getTownPriceModifier() * changes.getShopPriceModifier(shopObjectId);
        }
        return price;
    }
}
