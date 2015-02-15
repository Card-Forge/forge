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
package forge.deck.generation;

import com.google.common.collect.Lists;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.DeckFormat;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;

/**
 * <p>
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Generate2ColorDeck.java 19765 2013-02-20 03:01:37Z myk $
 */
public class DeckGeneratorMonoColor extends DeckGeneratorBase {
    @Override protected final float getLandsPercentage() { return 0.39f; }
    @Override protected final float getCreatPercentage() { return 0.36f; }
    @Override protected final float getSpellPercentage() { return 0.25f; }

    @SuppressWarnings("unchecked")
    final List<ImmutablePair<FilterCMC, Integer>> cmcLevels = Lists.newArrayList(
        ImmutablePair.of(new FilterCMC(0, 2), 10),
        ImmutablePair.of(new FilterCMC(3, 4), 8),
        ImmutablePair.of(new FilterCMC(5, 6), 5),
        ImmutablePair.of(new FilterCMC(7, 20), 3)
    );

    // mana curve of the card pool
    // 20x 0 - 2
    // 16x 3 - 4
    // 12x 5 - 6
    // 4x 7 - 20
    // = 52x - card pool (before further random filtering)

    public DeckGeneratorMonoColor(IDeckGenPool pool0, DeckFormat format0, final String clr1) {
        super(pool0, format0);
        if (MagicColor.fromName(clr1) == 0) {
            int color1 = r.nextInt(5);
            colors = ColorSet.fromMask(MagicColor.WHITE << color1);
        } else {
            colors = ColorSet.fromNames(clr1);
        }
    }


    @Override
    public final CardPool getDeck(final int size, final boolean forAi) {
        addCreaturesAndSpells(size, cmcLevels, forAi);

        // Add lands
        int numLands = (int) (getLandsPercentage() * size);

        tmpDeck.append("numLands:").append(numLands).append("\n");

        addBasicLand(numLands);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");

        adjustDeckSize(size);
        tmpDeck.append("DeckSize:").append(tDeck.countAll()).append("\n");

        return tDeck;
    }
}
