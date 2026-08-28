package forge.adventure.shop;

import forge.item.PaperCard;

/**
 * Persistent description of a card offer observed in an Adventure shop.
 */
public final class ShopCatalogOffer {
    private final ShopOfferId id;
    private final String cardRequest;
    private final String functionalVariant;
    private final PaperCard card;
    private final String shopName;
    private final String locationName;
    private final boolean unlimited;
    private boolean available;

    ShopCatalogOffer(ShopOfferId id, String cardRequest, String functionalVariant, PaperCard card,
                     String shopName, String locationName, boolean unlimited, boolean available) {
        this.id = id;
        this.cardRequest = cardRequest;
        this.functionalVariant = functionalVariant == null ? "" : functionalVariant;
        this.card = card;
        this.shopName = shopName == null ? "" : shopName;
        this.locationName = locationName == null ? "" : locationName;
        this.unlimited = unlimited;
        this.available = available;
    }

    public ShopOfferId getId() {
        return id;
    }

    public String getCardRequest() {
        return cardRequest;
    }

    public String getFunctionalVariant() {
        return functionalVariant;
    }

    public PaperCard getCard() {
        return card;
    }

    public String getShopName() {
        return shopName;
    }

    public String getLocationName() {
        return locationName;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean isAvailable() {
        return available;
    }

    void consume() {
        if (!unlimited) {
            available = false;
        }
    }
}
