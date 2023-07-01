/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.limited;

import java.util.Map.Entry;

import forge.deck.Deck;
import forge.item.PaperCard;

/**
 * <p>
 * LimitedDeckEvaluator class.
 * </p>
 * 
 * @author Forge
 * @version $Id: LimitedDeckEvaluator.java 32932 2017-01-02 05:16:54Z Agetian $
 * @since 1.5.58
 */
public class LimitedDeckEvaluator {
    
    public static double getDeckDraftValue(Deck d) {
        double value = 0;
        double divider = 0;

        if (d.getMain().isEmpty()) {
            return 0;
        }

        double best = 1.0;

        for (Entry<PaperCard, Integer> kv : d.getMain()) {
            PaperCard evalCard = kv.getKey();
            int count = kv.getValue();
            if (DraftRankCache.getRanking(evalCard.getName(), evalCard.getEdition()) != null) {
                double add = DraftRankCache.getRanking(evalCard.getName(), evalCard.getEdition());
                // System.out.println(evalCard.getName() + " is worth " + add);
                value += add * count;
                divider += count;
                if (best > add) {
                    best = add;
                }
            }
        }

        if (divider == 0 || value == 0) {
            return 0;
        }

        value /= divider;

        return (20.0 / (best + (2 * value)));
    }

    public static class LimitedDeckComparer implements java.util.Comparator<Deck> {
        @Override
        public int compare(Deck o1, Deck o2) {
            double delta = getDeckDraftValue(o1) - getDeckDraftValue(o2);
            if ( delta > 0 ) return 1;
            if ( delta < 0 ) return -1;
            return 0;
        }
    }
}
