package forge.gui.deckeditor;

import forge.Singletons;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.InventoryItem;
import forge.util.storage.IStorage;

public class DeckProxy implements InventoryItem {
    public final Deck deck;
    
    // cached values
    private ColorSet color;
    private Iterable<GameFormat> formats;
    private int mainSize = Integer.MIN_VALUE;
    private int sbSize = Integer.MIN_VALUE;
    
    public DeckProxy(Deck deck, GameType type, IStorage<Deck> storage) {
        this.deck = deck;
        // gametype could give us a hint whether the storage is updateable and enable choice of right editor for this deck  
    }

    @Override
    public String getName() {
        return deck.getName();
    }

    @Override
    public String getItemType() {
        // Could distinguish decks depending on gametype
        return "Deck";
    }
    
    public void invalidateCache() {
        color = null;
        formats = null;
        mainSize = Integer.MIN_VALUE;
        sbSize = Integer.MIN_VALUE;
    }
    
    
    public ColorSet getColor() {
        if ( color == null )
            color = deck.getColor();
        return color;
    }
    
    public Iterable<GameFormat> getFormats() {
        if ( formats == null )
            formats = Singletons.getModel().getFormats().getAllFormatsOfDeck(deck);
        return formats;
    }
    
    public int getMainSize() {
        if ( mainSize < 0 )
            mainSize = deck.getMain().countAll();
        return mainSize;
    }
    
    public int getSideSize() { 
        if ( sbSize < 0 ) {
            if ( deck.has(DeckSection.Sideboard) )
                sbSize = deck.get(DeckSection.Sideboard).countAll();
            else
                sbSize = 0;
        }
        return sbSize;
    }
    
    public void updateInStorage() {
        // if storage is not readonly, save the deck there.
    }

    public void deleteFromStorage() {
        // if storage is not readonly, delete the deck from there.
    }    

}
