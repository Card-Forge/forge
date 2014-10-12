package forge.game.card;

import java.util.Collection;
import forge.util.FCollection;

public class CardCollection extends FCollection<Card> implements CardCollectionView {
    public static final CardCollectionView EMPTY = new CardCollection();

    public static boolean hasCard(CardCollection cards) {
        return cards != null && !cards.isEmpty();
    }
    public static boolean hasCard(CardCollection cards, Card c) {
        return cards != null && cards.contains(c);
    }
    public static CardCollectionView getView(CardCollection cards) {
        return getView(cards, false);
    }
    public static CardCollectionView getView(CardCollection cards, boolean allowModify) {
        if (cards == null) {
            return EMPTY;
        }
        if (allowModify) { //create copy to allow modifying original set while iterating
            return new CardCollection(cards);
        }
        return cards;
    }
    public static CardCollectionView combine(CardCollectionView... views) {
        CardCollection newCol = null;
        CardCollectionView viewWithCards = null;
        for (CardCollectionView v : views) {
            if (!v.isEmpty()) {
                if (viewWithCards == null) {
                    viewWithCards = v;
                }
                else if (newCol == null) { //if multiple views have cards, we need to create a new collection
                    newCol = new CardCollection(viewWithCards);
                    newCol.addAll(v);
                    viewWithCards = newCol;
                }
                else {
                    newCol.addAll(v);
                }
            }
        }
        if (viewWithCards == null) {
            viewWithCards = CardCollection.EMPTY;
        }
        return viewWithCards;
    }

    public CardCollection() {
        super();
    }
    public CardCollection(Card card) {
        super(card);
    }
    public CardCollection(Collection<Card> cards) {
        super(cards);
    }
    public CardCollection(Iterable<Card> cards) {
        super(cards);
    }

    @Override
    protected FCollection<Card> createNew() {
        return new CardCollection();
    }

    public CardCollection subList(int fromIndex, int toIndex) {
        return (CardCollection)super.subList(fromIndex, toIndex);
    }
}
