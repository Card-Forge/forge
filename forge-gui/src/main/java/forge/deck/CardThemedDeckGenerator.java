package forge.deck;

import forge.card.CardEdition;
import forge.game.GameFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maustin on 09/05/2017.
 */
public class CardThemedDeckGenerator extends DeckProxy implements Comparable<CardThemedDeckGenerator> {
    public static List<DeckProxy> getMatrixDecks(GameFormat format, boolean isForAi){
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for(String card: CardRelationMatrixGenerator.cardPools.get(format).keySet()) {
            decks.add(new CardThemedDeckGenerator(card, format, isForAi));
        }
        return decks;
    }
    private final String name;
    private final int index;
    private final GameFormat format;
    private final boolean isForAi;


    private CardThemedDeckGenerator(String cardName, GameFormat format0, boolean isForAi0) {
        super();
        name = cardName;
        index = 0;
        format=format0;
        isForAi=isForAi0;
    }

    public CardEdition getEdition() {
        return CardEdition.UNKNOWN;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final CardThemedDeckGenerator d) {
        return name.compareTo(d.getName());
    }

    @Override
    public Deck getDeck() {

        return DeckgenUtil.buildCardGenDeck(name,format,isForAi);
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}
