package forge.deck;

import forge.game.GameFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maustin on 09/05/2017.
 */
public class CardThemedDeckGenerator extends DeckProxy implements Comparable<CardThemedDeckGenerator> {
    public static List<DeckProxy> getMatrixDecks(GameFormat format){
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for(String card: CardRelationMatrixGenerator.cardPools.get(format).keySet()) {
            decks.add(new CardThemedDeckGenerator(card, format));
        }
        return decks;
    }
    private final String name;
    private final int index;
    private final GameFormat format;


    private CardThemedDeckGenerator(String cardName, GameFormat format0) {
        super();
        name = cardName;
        index = 0;
        format=format0;
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

        return DeckgenUtil.buildCardGenDeck(name,format);
    }

    @Override
    public boolean isGeneratedDeck() {
        return true;
    }
}
