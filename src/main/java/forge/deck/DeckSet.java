package forge.deck;

import java.util.ArrayList;
import java.util.List;

import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.IHasName;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckSet extends DeckBase implements IHasName {

    public DeckSet(String name0) {
        super(name0);
    }

    private static final long serialVersionUID = -1628725522049635829L;
    private Deck humanDeck;
    private List<Deck> aiDecks = new ArrayList<Deck>();

    public final Deck getHumanDeck() {
        return humanDeck;
    }
    public final List<Deck> getAiDecks() {
        return aiDecks;
    }
    public final void setHumanDeck(Deck humanDeck) { this.humanDeck = humanDeck; }

    public final void addAiDeck(Deck aiDeck) {
        if (aiDeck != null) {
            this.aiDecks.add(aiDeck);
            }
        }

    @Override
    public ItemPoolView<CardPrinted> getCardPool() {
        return getHumanDeck().getMain();
    }

    public void addAiDecks(Deck[] computer) {
        for (int i = 0; i < computer.length; i++) {
            aiDecks.add(computer[i]);
        }
    }
    /* (non-Javadoc)
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(String name0) {
        return new DeckSet(name0);
    }

}
