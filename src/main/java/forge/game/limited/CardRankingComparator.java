package forge.game.limited;

import java.util.Comparator;

import org.apache.commons.lang3.tuple.Pair;

import forge.item.CardPrinted;

/**
 * Sorts cards by rank.
 * 
 */
public class CardRankingComparator implements Comparator<Pair<Double, CardPrinted>> {
    @Override
    public int compare(final Pair<Double, CardPrinted> a, final Pair<Double, CardPrinted> b) {
        return a.getKey().compareTo(b.getKey());
    }
}
