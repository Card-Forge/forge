package forge.limited;

import forge.item.PaperCard;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;

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
