package forge.adventure.shop;

import java.util.Objects;

/**
 * Stable identity for one displayed card offer in one generated shop inventory.
 */
public final class ShopOfferId {
    private final String pointOfInterestChangesKey;
    private final int shopObjectId;
    private final long shopSeed;
    private final int rewardIndex;

    public ShopOfferId(String pointOfInterestChangesKey, int shopObjectId, long shopSeed, int rewardIndex) {
        this.pointOfInterestChangesKey = pointOfInterestChangesKey;
        this.shopObjectId = shopObjectId;
        this.shopSeed = shopSeed;
        this.rewardIndex = rewardIndex;
    }

    public String getPointOfInterestChangesKey() {
        return pointOfInterestChangesKey;
    }

    public int getShopObjectId() {
        return shopObjectId;
    }

    public long getShopSeed() {
        return shopSeed;
    }

    public int getRewardIndex() {
        return rewardIndex;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ShopOfferId)) {
            return false;
        }
        ShopOfferId other = (ShopOfferId) object;
        return shopObjectId == other.shopObjectId
                && shopSeed == other.shopSeed
                && rewardIndex == other.rewardIndex
                && Objects.equals(pointOfInterestChangesKey, other.pointOfInterestChangesKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointOfInterestChangesKey, shopObjectId, shopSeed, rewardIndex);
    }
}
