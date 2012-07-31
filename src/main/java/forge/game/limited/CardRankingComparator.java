package forge.game.limited;

import java.util.Comparator;

/**
 * Sorts cards by rank.
 * 
 */
public class CardRankingComparator implements Comparator<LimitedDeck.CardRankingBean> {
    @Override
    public int compare(final LimitedDeck.CardRankingBean a, final LimitedDeck.CardRankingBean b) {
        return a.getRank().compareTo(b.getRank());
    }
}
