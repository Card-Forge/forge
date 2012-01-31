package forge.deck;

import forge.Card;
import forge.CardList;
import forge.item.*;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSection extends ItemPool<CardPrinted> {

    public DeckSection() {
        super(CardPrinted.class);
    }
    /**
     * Clear main.
     */
    public void clearMain() {
        this.clear();
    
    }

    public void set(final Iterable<String> cardNames) {
        this.clear();
        this.addAllCards(CardDb.instance().getCards(cardNames));
    }
    
    public void add(final Card card){
        this.add(CardDb.instance().getCard(card));
    }
    
    public void add(final String cardName, final String setCode)
    {
        add(CardDb.instance().getCard(cardName, setCode));
    }
    
    public void add(final CardList cardList) {
        for(Card c : cardList) 
            add(c);
    }

}
