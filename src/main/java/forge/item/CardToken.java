package forge.item;

import forge.Card;
import forge.ImageCache;
import forge.Singletons;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.player.Player;

public class CardToken implements InventoryItemFromSet, IPaperCard {

    private String name;
    private String edition;
    private String imageFileName;
    private CardRules card;


    // Constructor is private. All non-foiled instances are stored in CardDb
    public CardToken(final CardRules c, final String edition0, final String imageFileName) {
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;
    }
    
    @Override public String getName() { return name; }
    @Override public String getEdition() { return edition; }

    @Override public int getArtIndex() { return 0; } // This might change however
    @Override public boolean isFoil() { return false; }
    @Override public CardRules getRules() { return card; }

    @Override public CardRarity getRarity() { return CardRarity.Common; } // They don't have rarity though!

    @Override public String getImageFilename() { return ImageCache.TOKEN + imageFileName; }

    @Override public String getItemType() { return "Token"; }
    @Override public Card getMatchingForgeCard() { return toForgeCard(null); } // hope this won't be queried too frequently, so no cache 

    @Override
    public Card toForgeCard(Player owner) { 
        final Card c = Singletons.getModel().getCardFactory().getCard(this, owner);
        return c;
    }


    @Override public boolean isToken() { return true; }

}
