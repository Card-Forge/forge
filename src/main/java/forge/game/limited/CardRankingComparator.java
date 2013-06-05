package forge.game.limited;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;

import forge.item.PaperCard;

/**
 * Sorts cards by rank.
 * 
 */
public class CardRankingComparator implements Comparator<Pair<Double, PaperCard>> {
    @Override
    public int compare(final Pair<Double, PaperCard> a, final Pair<Double, PaperCard> b) {
        return a.getKey().compareTo(b.getKey());
    }
}
