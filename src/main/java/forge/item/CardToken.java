package forge.item;

import forge.Card;
import forge.ImageCache;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.cardfactory.CardFactory;
import forge.game.player.Player;

public class CardToken implements InventoryItemFromSet, IPaperCard {
    private String name;
    private CardEdition edition;
    private String imageFileName;
    private CardRules card;

    // Constructor is private. All non-foiled instances are stored in CardDb
    public CardToken(final CardRules c, CardEdition edition0, final String imageFileName) {
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;
        this.imageFileName = String.format("%s%s%s",
                        null == edition || CardEdition.UNKNOWN == edition ? "" : edition.getCode(),
                        ImageCache.TOKEN_PREFIX, imageFileName);
    }
    
    @Override public String getName() { return name; }
    @Override public String getDescription() { return name; }

    @Override public String getEdition() { return edition.getCode(); }

    @Override public int getArtIndex() { return 0; } // This might change however
    @Override public boolean isFoil() { return false; }
    @Override public CardRules getRules() { return card; }

    @Override public CardRarity getRarity() { return CardRarity.Common; } // They don't have rarity though!

    @Override public String getImageFilename() { return imageFileName; }

    @Override public String getItemType() { return "Token"; }
    @Override public Card getMatchingForgeCard() { return toForgeCard(null); } // hope this won't be queried too frequently, so no cache 

    @Override
    public Card toForgeCard(Player owner) { 
        final Card c = CardFactory.getCard(this, owner);
        return c;
    }

    @Override public boolean isToken() { return true; }
}
