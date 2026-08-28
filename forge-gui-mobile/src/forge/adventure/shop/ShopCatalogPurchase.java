package forge.adventure.shop;

import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.Reward;
import forge.adventure.world.WorldSave;

/**
 * Performs a catalog purchase without retaining any scene or actor references.
 */
public final class ShopCatalogPurchase {
    public enum Status {
        Success,
        InsufficientGold,
        Unavailable
    }

    public static final class Result {
        private final Status status;
        private final int price;

        private Result(Status status, int price) {
            this.status = status;
            this.price = price;
        }

        public Status getStatus() {
            return status;
        }

        public int getPrice() {
            return price;
        }
    }

    private ShopCatalogPurchase() {
    }

    public static Result purchase(ShopOfferId offerId) {
        WorldSave save = WorldSave.getCurrentSave();
        ShopCatalog catalog = save.getShopCatalog();
        synchronized (catalog) {
            ShopCatalogOffer offer = catalog.get(offerId);
            if (offer == null || !offer.isAvailable()) {
                return new Result(Status.Unavailable, 0);
            }

            PointOfInterestChanges changes = save.getPointOfInterestChanges(
                    offerId.getPointOfInterestChangesKey());
            Reward reward = new Reward(offer.getCard());
            int price = AdventureShopPrice.calculate(reward, changes, offerId.getShopObjectId());
            AdventurePlayer player = AdventurePlayer.current();
            if (player.getGold() < price) {
                return new Result(Status.InsufficientGold, price);
            }

            player.takeGold(price);
            player.addReward(reward);
            if (!offer.isUnlimited()) {
                if (changes.getShopSeed(offerId.getShopObjectId()) == offerId.getShopSeed()) {
                    changes.buyCard(offerId.getShopObjectId(), offerId.getRewardIndex());
                }
                catalog.consume(offerId);
            }
            return new Result(Status.Success, price);
        }
    }
}
