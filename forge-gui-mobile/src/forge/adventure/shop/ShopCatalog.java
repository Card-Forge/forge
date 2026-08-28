package forge.adventure.shop;

import forge.card.CardDb;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Save-backed ledger of card offers observed in Adventure shops.
 */
public final class ShopCatalog implements SaveFileContent {
    private final Map<ShopOfferId, ShopCatalogOffer> offers = new LinkedHashMap<>();

    public synchronized ShopCatalogOffer observe(ShopOfferId id, PaperCard card, String shopName,
                                                  String locationName, boolean unlimited) {
        return observe(id, CardDb.CardRequest.compose(card), card.getFunctionalVariant(), card,
                shopName, locationName, unlimited);
    }

    synchronized ShopCatalogOffer observe(ShopOfferId id, String cardRequest, PaperCard card,
                                          String shopName, String locationName, boolean unlimited) {
        return observe(id, cardRequest, "", card, shopName, locationName, unlimited);
    }

    synchronized ShopCatalogOffer observe(ShopOfferId id, String cardRequest, String functionalVariant,
                                          PaperCard card, String shopName, String locationName,
                                          boolean unlimited) {
        ShopCatalogOffer existing = offers.get(id);
        if (existing != null) {
            return existing;
        }
        ShopCatalogOffer offer = new ShopCatalogOffer(id, cardRequest, functionalVariant, card,
                shopName, locationName, unlimited, true);
        offers.put(id, offer);
        return offer;
    }

    public synchronized ShopCatalogOffer get(ShopOfferId id) {
        return offers.get(id);
    }

    public synchronized boolean consume(ShopOfferId id) {
        ShopCatalogOffer offer = offers.get(id);
        if (offer == null || !offer.isAvailable()) {
            return false;
        }
        offer.consume();
        return true;
    }

    public synchronized Collection<ShopCatalogOffer> getAvailableOffers() {
        List<ShopCatalogOffer> available = new ArrayList<>();
        for (ShopCatalogOffer offer : offers.values()) {
            if (offer.isAvailable()) {
                available.add(offer);
            }
        }
        return Collections.unmodifiableList(available);
    }

    public synchronized int size() {
        return offers.size();
    }

    public synchronized void clear() {
        offers.clear();
    }

    @Override
    public synchronized void load(SaveFileData data) {
        offers.clear();
        if (data == null || !data.containsKey("count")) {
            return;
        }
        int count = data.readInt("count");
        for (int i = 0; i < count; i++) {
            try {
                SaveFileData offerData = data.readSubData("offer_" + i);
                if (offerData == null) {
                    continue;
                }
                String cardRequest = offerData.readString("card");
                String functionalVariant = offerData.readString("functionalVariant");
                PaperCard card = resolveCard(cardRequest, functionalVariant);
                if (card == null) {
                    Logger.warn("Skipping unresolved Adventure shop catalog card: {}", cardRequest);
                    continue;
                }
                ShopOfferId id = new ShopOfferId(
                        offerData.readString("pointOfInterestChangesKey"),
                        offerData.readInt("shopObjectId"),
                        offerData.readLong("shopSeed"),
                        offerData.readInt("rewardIndex"));
                ShopCatalogOffer offer = new ShopCatalogOffer(id, cardRequest, functionalVariant, card,
                        offerData.readString("shopName"), offerData.readString("locationName"),
                        offerData.readBool("unlimited"), offerData.readBool("available"));
                offers.put(id, offer);
            } catch (RuntimeException exception) {
                Logger.warn(exception, "Skipping invalid Adventure shop catalog offer {}", i);
            }
        }
    }

    @Override
    public synchronized SaveFileData save() {
        SaveFileData data = new SaveFileData();
        data.store("count", offers.size());
        int index = 0;
        for (ShopCatalogOffer offer : offers.values()) {
            SaveFileData offerData = new SaveFileData();
            ShopOfferId id = offer.getId();
            offerData.store("pointOfInterestChangesKey", id.getPointOfInterestChangesKey());
            offerData.store("shopObjectId", id.getShopObjectId());
            offerData.store("shopSeed", id.getShopSeed());
            offerData.store("rewardIndex", id.getRewardIndex());
            offerData.store("card", offer.getCardRequest());
            offerData.store("functionalVariant", offer.getFunctionalVariant());
            offerData.store("shopName", offer.getShopName());
            offerData.store("locationName", offer.getLocationName());
            offerData.store("unlimited", offer.isUnlimited());
            offerData.store("available", offer.isAvailable());
            data.store("offer_" + index++, offerData);
        }
        return data;
    }

    private static PaperCard resolveCard(String cardRequest, String functionalVariant) {
        if (cardRequest == null) {
            return null;
        }
        String expectedVariant = functionalVariant == null ? "" : functionalVariant;
        for (CardDb database : FModel.getMagicDb().getAvailableDatabases().values()) {
            PaperCard card = database.getCard(cardRequest);
            if (card == null) {
                continue;
            }
            if (expectedVariant.equals(card.getFunctionalVariant())) {
                return card;
            }
            for (PaperCard candidate : database.getAllCards(card.getName())) {
                if (card.equals(candidate) && expectedVariant.equals(candidate.getFunctionalVariant())) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
