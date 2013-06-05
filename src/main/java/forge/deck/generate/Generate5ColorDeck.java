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
package forge.deck.generate;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.Lists;

import forge.card.ColorSet;
import forge.deck.generate.GenerateDeckUtil.FilterCMC;
import forge.item.PaperCard;
import forge.item.ItemPoolView;

/**
 * <p>
 * Generate5ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Generate5ColorDeck extends GenerateColoredDeckBase {
    @SuppressWarnings("unchecked")
    final List<ImmutablePair<FilterCMC, Integer>> cmcLevels = Lists.newArrayList(
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(0, 2), 3),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(3, 5), 2),
        ImmutablePair.of(new GenerateDeckUtil.FilterCMC(6, 20), 1)
    );

    // resulting mana curve of the card pool
    // 30x 0 - 2
    // 20x 3 - 5
    // 10x 6 - 20
    // =60x - card pool

    /**
     * Instantiates a new generate5 color deck.
     */
    public Generate5ColorDeck() {
        colors = ColorSet.fromMask(0).inverse();
    }


    @Override
    public final ItemPoolView<PaperCard> getDeck(final int size, final boolean forAi) {
        addCreaturesAndSpells(size, cmcLevels, forAi);

        // Add lands
        int numLands = Math.round(size * getLandsPercentage());
        adjustDeckSize(size - numLands);
        tmpDeck.append("numLands:").append(numLands).append("\n");

        // Add dual lands
        List<String> duals = GenerateDeckUtil.getDualLandList(colors);
        for (String s : duals) {
            this.cardCounts.put(s, 0);
        }

        int dblsAdded = addSomeStr((numLands / 4), duals);
        numLands -= dblsAdded;

        addBasicLand(numLands);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");
        return tDeck;
    }
}
